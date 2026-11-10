from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import BasePage, DEFAULT_TIMEOUT


class UserMenu(BasePage):
    MENU_BUTTON = (By.CSS_SELECTOR, "button[aria-label='Korisnički izbornik']")
    OPTIONS = (By.CSS_SELECTOR, "ul[aria-label='Korisničke opcije']")
    ADMINISTRATOR = (By.XPATH, "//*[normalize-space()='Administrator']")
    PROFILE_ITEM = (By.XPATH, "//li[normalize-space()='Profil']")
    LOGOUT = (By.XPATH, "//li[normalize-space()='Odjava']")

    def open_menu(self):
        self.wait_visible(self.MENU_BUTTON)
        self.click(self.MENU_BUTTON)
        WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
            lambda driver: driver.find_element(
                *self.MENU_BUTTON
            ).get_attribute("aria-expanded") == "true"
        )
        return self.wait_visible(self.OPTIONS)

    def logout(self):
        self.click(self.LOGOUT)

    def administrator_is_displayed(self):
        return self.driver.find_element(*self.ADMINISTRATOR).is_displayed()

    def has_profile_item(self):
        return bool(self.driver.find_elements(*self.PROFILE_ITEM))

    def role_is_displayed(self, role):
        role_locator = (
            By.XPATH,
            f"//ul[@aria-label='Korisničke opcije']"
            f"//span[normalize-space()='{role}']",
        )
        return self.wait_visible(role_locator).is_displayed()
