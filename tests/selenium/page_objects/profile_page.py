from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import BasePage


class ProfilePage(BasePage):
    path = "/profile"
    BIOGRAPHY = (By.ID, "artist-profile-biography")
    ART_FIELDS = (By.ID, "artist-profile-art-fields")
    LINK_URL = (By.CSS_SELECTOR, 'input[name="links.0.url"]')
    ADD_LINK = (By.XPATH, "//button[normalize-space()='Dodaj poveznicu']")
    SAVE = (By.XPATH, "//button[normalize-space()='Spremi profil']")
    SAVED = (By.XPATH, "//*[@role='status'][normalize-space()='Profil spremljen']")
    ORGANIZATION = (By.ID, "organizationName")
    USER_MENU = (By.CSS_SELECTOR, 'button[aria-label="Korisnički izbornik"]')

    def fill_biography(self, biography):
        field = self.wait_visible(self.BIOGRAPHY)
        field.clear()
        field.send_keys(biography)

    def select_art_field(self, name):
        field = self.wait_visible(self.ART_FIELDS)
        field.send_keys(name)
        option = WebDriverWait(self.driver, 10).until(
            lambda driver: next(
                (item for item in driver.find_elements(By.CSS_SELECTOR, '[role="option"]')
                 if item.text == name),
                None,
            )
        )
        option.click()
        field.send_keys(Keys.ESCAPE)

    def add_link(self, url):
        self.click(self.ADD_LINK)
        self.wait_visible(self.LINK_URL).send_keys(url)

    def save(self):
        self.click(self.SAVE)

    def wait_for_url_error(self):
        field = self.wait_visible(self.LINK_URL)
        WebDriverWait(self.driver, 10).until(
            lambda _: field.get_attribute("aria-invalid") == "true"
        )
        error_id = field.get_attribute("aria-describedby")
        return self.wait_visible((By.ID, error_id)).text

    def selected_art_fields(self):
        return [chip.text for chip in self.driver.find_elements(
            By.CSS_SELECTOR, '.MuiAutocomplete-root .MuiChip-label'
        )]
