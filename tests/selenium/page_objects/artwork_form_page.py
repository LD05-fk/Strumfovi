from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import BasePage


class ArtworkFormPage(BasePage):
    """Obrazac djela, njegova vidljivost i odjeljak za učitavanje slika."""

    path = "/portfolio/artworks/new"
    TITLE = (By.ID, "artwork-title")
    SAVE = (By.CSS_SELECTOR, 'form button[type="submit"]')
    PRIVATE = (By.CSS_SELECTOR, 'button[value="PRIVATE"]')
    SAVED = (By.XPATH, "//*[@role='status'][normalize-space()='Djelo spremljeno']")
    IMAGE_INPUT = (By.CSS_SELECTOR, 'input[type="file"]')
    ADD_IMAGE = (By.XPATH, "//button[normalize-space()='Odaberi sliku']")
    THUMBNAILS = (By.CSS_SELECTOR, 'img[src*="/api/v1/files/"]')
    REMOVE_IMAGE = (By.XPATH, "//button[normalize-space()='Ukloni']")

    def create_public(self, title):
        self.open()
        for name, value in {"title": title, "year": "2024", "technique": "Akril"}.items():
            self.wait_visible((By.ID, f"artwork-{name}")).send_keys(value)
        self.click((By.CSS_SELECTOR, 'button[value="PUBLIC"]'))
        self.save()
        # Spremanje novog djela mora otvoriti njegov obrazac, gdje se dodaju slike.
        WebDriverWait(self.driver, 10).until(
            EC.url_matches(r"/portfolio/artworks/[0-9a-f-]{36}$")
        )
        self.wait_visible(self.TITLE)
        return self.driver.current_url.rsplit("/", 1)[1]

    def upload_image(self, path):
        # Skriveni file input prima apsolutnu putanju; ne otvaramo dijalog sustava.
        file_input = WebDriverWait(self.driver, 10).until(
            EC.presence_of_element_located(self.IMAGE_INPUT)
        )
        file_input.send_keys(str(path.resolve()))

    def wait_for_images(self, count):
        self.wait_visible((By.XPATH, f"//*[normalize-space()='{count}/3 slike']"))
        # Broj oznaka <img> nije dovoljan: slika se mora i stvarno učitati s backenda.
        def images_loaded(driver):
            images = driver.find_elements(*self.THUMBNAILS)
            if len(images) != count:
                return False
            if any(image.get_property("naturalWidth") == 0 for image in images):
                return False
            return images

        return WebDriverWait(self.driver, 10).until(images_loaded)

    def remove_first_image(self):
        self.click(self.REMOVE_IMAGE)
        # Slika se briše tek nakon korisnikove potvrde u dijalogu.
        self.click((By.XPATH, "//*[@role='dialog']//button[normalize-space()='Ukloni']"))

    def wait_for_image_error(self, message):
        return self.wait_visible(
            (By.XPATH, f"//*[@role='alert'][contains(., '{message}')]")
        ).text

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
