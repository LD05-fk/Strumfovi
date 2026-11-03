from selenium.webdriver import ActionChains
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from page_objects.base_page import DEFAULT_TIMEOUT, BasePage

MOBILE_WIDTH = 375
MOBILE_HEIGHT = 812


class LayoutPage(BasePage):

    path = "/"

    SKIP_LINK_TEXT = "Preskoči na sadržaj"
    APP_NAME = "ArtConnect"
    NAV_ITEM_TEXT = "Prilike"
    LOGIN_LABEL = "Prijava s Googleom"
    NOT_FOUND_HEADING = "Stranica ne postoji."
    HOME_LINK_TEXT = "Na početnu"

    SKIP_LINK = (By.XPATH, f"//a[normalize-space()='{SKIP_LINK_TEXT}']")
    HEADER = (By.TAG_NAME, "header")
    LOGO = (By.XPATH, f"//header//a[normalize-space()='{APP_NAME}']")
    MAIN_NAV_ITEM = (
        By.XPATH, "//nav[@aria-label='Glavna navigacija']" f"//a[normalize-space()='{NAV_ITEM_TEXT}']",)
    HEADER_NAV_LINKS = (By.CSS_SELECTOR, "nav[aria-label='Glavna navigacija'] a")
    LOGIN_BUTTON = (By.XPATH, f"//header//a[@aria-label='{LOGIN_LABEL}']")
    FOOTER = (By.TAG_NAME, "footer")
    MENU_BUTTON = (By.CSS_SELECTOR, "button[aria-label='Otvori izbornik']")
    DRAWER_NAV = (By.CSS_SELECTOR, "nav[aria-label='Mobilna navigacija']")
    NOT_FOUND_TITLE = (
        By.XPATH, f"//h1[normalize-space()='{NOT_FOUND_HEADING}']")
    HOME_LINK = (By.XPATH, f"//a[normalize-space()='{HOME_LINK_TEXT}']")

    def use_mobile_viewport(self):
        self.driver.execute_cdp_cmd(
            "Emulation.setDeviceMetricsOverride",
            {
                "width": MOBILE_WIDTH,
                "height": MOBILE_HEIGHT,
                "deviceScaleFactor": 1,
                "mobile": True,
            },
        )
        return self

    def wait_present(self, locator, timeout=DEFAULT_TIMEOUT):
        return WebDriverWait(self.driver, timeout).until(
            EC.presence_of_element_located(locator)
        )

    def wait_until_title(self, title, timeout=DEFAULT_TIMEOUT):
        WebDriverWait(self.driver, timeout).until(EC.title_is(title))

    def wait_until_gone(self, locator, timeout=DEFAULT_TIMEOUT):
        WebDriverWait(self.driver, timeout).until(
            EC.invisibility_of_element_located(locator)
        )

    def open_menu(self):
        self.click(self.MENU_BUTTON)
        return self.wait_visible(self.DRAWER_NAV)

    def press_escape(self):
        ActionChains(self.driver).send_keys(Keys.ESCAPE).perform()

    def press_tab(self):
        ActionChains(self.driver).send_keys(Keys.TAB).perform()

    def focused_text(self):
        active = self.driver.switch_to.active_element
        return (active.get_attribute("textContent") or "").strip()

    def wait_for_header_nav_labels(self, labels, timeout=DEFAULT_TIMEOUT):
        def matching_links(driver):
            links = driver.find_elements(*self.HEADER_NAV_LINKS)
            visible_links = [
                link for link in links if link.is_displayed()
            ]
            actual_labels = [
                link.get_attribute("textContent").strip()
                for link in visible_links
            ]
            return visible_links if actual_labels == labels else False

        return WebDriverWait(self.driver, timeout).until(matching_links)

    def tab_to(self, element, max_tabs=20):
        self.driver.execute_script(
            "document.body.setAttribute('tabindex', '-1'); document.body.focus();"
        )
        for _ in range(max_tabs):
            self.press_tab()
            if self.driver.execute_script(
                "return document.activeElement === arguments[0];", element
            ):
                return
        raise AssertionError("Stavka zaglavlja nije dosegnuta tipkom Tab.")

    def focused_element_is_focus_visible(self):
        return self.driver.execute_script(
            "return document.activeElement.matches(':focus-visible');"
        )
