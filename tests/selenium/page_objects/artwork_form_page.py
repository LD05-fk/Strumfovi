from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import BasePage


class ArtworkFormPage(BasePage):
    """Obrazac za novo djelo i uređivanje vidljivosti postojećeg djela."""

    path = "/portfolio/artworks/new"
    TITLE = (By.ID, "artwork-title")
    SAVE = (By.CSS_SELECTOR, 'form button[type="submit"]')
    PRIVATE = (By.CSS_SELECTOR, 'button[value="PRIVATE"]')
    SAVED = (By.XPATH, "//*[@role='status'][normalize-space()='Djelo spremljeno']")

    def open_artwork(self, artwork_id):
        self.open_path(f"/portfolio/artworks/{artwork_id}")
        self.wait_visible(self.TITLE)
        return self

    def save(self):
        self.click(self.SAVE)

    def field_error(self, name):
        # Poruku čitamo preko veze polja i njegove greške, kao čitač zaslona.
        locator = (By.ID, f"artwork-{name}")
        WebDriverWait(self.driver, 10).until(
            lambda driver: driver.find_element(*locator).get_attribute("aria-invalid") == "true"
        )
        error_id = self.wait_visible(locator).get_attribute("aria-describedby")
        return self.wait_visible((By.ID, error_id)).text

    def make_private_and_save(self):
        self.click(self.PRIVATE)
        self.save()
        self.wait_visible(self.SAVED)
        # Spremanje vraća na popis. Čekamo završetak prije sljedeće navigacije.
        WebDriverWait(self.driver, 10).until(
            EC.url_to_be(self.base_url + "/portfolio")
        )
