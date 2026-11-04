from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support import expected_conditions as EC
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
    EXHIBITIONS = (By.CSS_SELECTOR, 'section[aria-labelledby="past-exhibitions-heading"]')
    ADD_EXHIBITION = (By.CSS_SELECTOR, 'section[aria-labelledby="past-exhibitions-heading"] button')
    EXHIBITION_DIALOG = (By.CSS_SELECTOR, '[role="dialog"][aria-labelledby="past-exhibition-dialog-title"]')
    EXHIBITION_TITLE = (By.CSS_SELECTOR, '[role="dialog"] input[name="title"]')
    EXHIBITION_YEAR = (By.CSS_SELECTOR, '[role="dialog"] input[name="year"]')
    EXHIBITION_PLACE = (By.CSS_SELECTOR, '[role="dialog"] input[name="place"]')
    SAVE_EXHIBITION = (By.CSS_SELECTOR, '[role="dialog"] button[type="submit"]')
    DELETE_EXHIBITION_DIALOG = (By.CSS_SELECTOR, '[role="alertdialog"][aria-labelledby="delete-exhibition-title"]')
    CONFIRM_DELETE_EXHIBITION = (By.XPATH, "//*[@role='alertdialog']//button[normalize-space()='Obriši']")

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

    def add_exhibition(self, title, year, place):
        self.click(self.ADD_EXHIBITION)
        self.wait_visible(self.EXHIBITION_DIALOG)
        # Polja su u dijalogu izložbe, odvojena od glavnog obrasca profila.
        for locator, value in (
            (self.EXHIBITION_TITLE, title),
            (self.EXHIBITION_YEAR, str(year)),
            (self.EXHIBITION_PLACE, place),
        ):
            field = self.wait_visible(locator)
            field.send_keys(Keys.CONTROL, "a")
            field.send_keys(value)
        self.save_exhibition()

    def edit_exhibition_place(self, title, place):
        self.click((By.CSS_SELECTOR, f'button[aria-label="Uredi: {title}"]'))
        field = self.wait_visible(self.EXHIBITION_PLACE)
        field.send_keys(Keys.CONTROL, "a")
        field.send_keys(place)
        self.save_exhibition()

    def save_exhibition(self):
        self.click(self.SAVE_EXHIBITION)
        # Čekamo zatvaranje dijaloga, bez fiksnih pauza koje bi usporavale test.
        WebDriverWait(self.driver, 10).until(
            EC.invisibility_of_element_located(self.EXHIBITION_DIALOG)
        )

    def wait_for_exhibition(self, title, year, place):
        # Roditelj naslova i njegov roditelj čine karticu s podacima jedne izložbe.
        card = (By.XPATH, "//section[@aria-labelledby='past-exhibitions-heading']"
                f"//h3[normalize-space()='{title}']/../..")
        WebDriverWait(self.driver, 10).until(
            EC.text_to_be_present_in_element(card, f"{year}. · {place}")
        )
        return self.wait_visible(card).text

    def delete_exhibition(self, title):
        self.click((By.CSS_SELECTOR, f'button[aria-label="Obriši: {title}"]'))
        self.wait_visible(self.DELETE_EXHIBITION_DIALOG)
        self.click(self.CONFIRM_DELETE_EXHIBITION)
        WebDriverWait(self.driver, 10).until(
            EC.invisibility_of_element_located(self.DELETE_EXHIBITION_DIALOG)
        )

    def wait_for_exhibition_removed(self, title):
        heading = (By.XPATH, "//section[@aria-labelledby='past-exhibitions-heading']"
                   f"//h3[normalize-space()='{title}']")
        WebDriverWait(self.driver, 10).until(
            EC.invisibility_of_element_located(heading)
        )
