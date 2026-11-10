from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import BasePage


class PublicPortfolioPage(BasePage):
    """Javni popis: prije provjere odsutnosti djela čekamo učitanu galeriju."""

    GALLERY = (By.CSS_SELECTOR, 'section[aria-labelledby="artworks-heading"]')
    ARTIST_NAME = (By.CSS_SELECTOR, "main h1")
    ARTIST_NOT_FOUND = (By.XPATH, "//h1[normalize-space()='Umjetnik nije pronađen']")
    PAST_EXHIBITIONS = (By.CSS_SELECTOR, 'section[aria-labelledby="past-exhibitions-heading"]')
    PARTICIPATIONS = (By.CSS_SELECTOR, 'section[aria-labelledby="participations-heading"]')
    LIGHTBOX = (By.CSS_SELECTOR, '[role="dialog"][aria-labelledby="artwork-lightbox-title"]')

    def open_artist(self, artist_id):
        self.open_path(f"/artists/{artist_id}")
        # Čekaj da se učita portfolio ili stranica za nepostojećeg umjetnika.
        WebDriverWait(self.driver, 10).until(
            EC.any_of(
                EC.visibility_of_element_located(self.GALLERY),
                EC.visibility_of_element_located(self.ARTIST_NOT_FOUND),
            )
        )
        return self

    def wait_for_artist(self):
        return self.wait_visible(self.ARTIST_NAME)

    def wait_for_exhibition(self, title):
        locator = (
            By.XPATH,
            f'//section[@aria-labelledby="past-exhibitions-heading"]'
            f'//h3[normalize-space()="{title}"]',
        )
        return self.wait_visible(locator)

    def wait_for_artwork(self, title, year):
        return self.wait_visible((By.CSS_SELECTOR, f'button[aria-label="{title}, {year}"]'))

    def open_artwork(self, title, year):
        self.click((By.CSS_SELECTOR, f'button[aria-label="{title}, {year}"]'))
        return self.wait_visible(self.LIGHTBOX)

    def close_lightbox_with_escape(self):
        self.driver.find_element(By.TAG_NAME, "body").send_keys(Keys.ESCAPE)
        WebDriverWait(self.driver, 10).until(
            EC.invisibility_of_element_located(self.LIGHTBOX)
        )

    def artwork_titles(self):
        gallery = self.wait_visible(self.GALLERY)
        return [heading.text for heading in gallery.find_elements(By.TAG_NAME, "h3")]
