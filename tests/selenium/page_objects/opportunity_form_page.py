import json

from selenium.webdriver import ActionChains
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import BasePage


class OpportunityFormPage(BasePage):
    """Obrazac za stvaranje i uređivanje prilike."""

    path = "/organizer/opportunities/new"
    SAVE = (By.CSS_SELECTOR, 'form button[type="submit"]')
    MAP = (By.CSS_SELECTOR, '[role="region"][aria-label="Karta lokacije prilike"]')
    FIND_ON_MAP = (By.XPATH, "//button[normalize-space()='Pronađi na karti']")

    def field(self, label):
        label_element = self.wait_visible(
            (By.XPATH, f"//label[normalize-space()='{label}']")
        )
        field_id = label_element.get_attribute("for")
        if not field_id:
            raise AssertionError(f"Polje '{label}' nema povezanu oznaku.")
        return self.driver.find_element(By.ID, field_id)

    def fill(self, label, value):
        field = self.field(label)
        field.click()
        field.send_keys(Keys.CONTROL, "a")
        field.send_keys(value)

    def select_first_option(self, label):
        field = self.field(label)
        field.click()
        option = WebDriverWait(self.driver, 10).until(
            EC.visibility_of_element_located((By.CSS_SELECTOR, '[role="option"]'))
        )
        option.click()
        field.send_keys(Keys.ESCAPE)
        WebDriverWait(self.driver, 10).until(
            EC.invisibility_of_element_located((By.CSS_SELECTOR, '[role="option"]'))
        )

    def select_location_type(self, value):
        self.click(
            (
                By.XPATH,
                f"//label[normalize-space()='{value}']",
            )
        )

    def set_deadline(self):
        label = self.wait_visible(
            (By.XPATH, "//label[normalize-space()='Rok prijave']")
        )
        group = self.driver.find_element(
            By.CSS_SELECTOR,
            f'div[role="group"][aria-labelledby="{label.get_attribute("id")}"]',
        )
        for section_name, value in (
            ("Dan", "12"),
            ("Mjesec", "12"),
            ("Godina", "2099"),
            ("Sati", "10"),
            ("Minute", "30"),
        ):
            section = group.find_element(
                By.CSS_SELECTOR,
                f'[role="spinbutton"][aria-label="{section_name}"]',
            )
            section.click()
            section.send_keys(value)
        self.field("Opis").click()

    def click_map_to_set_marker(self):
        map_region = self.wait_visible(self.MAP)
        WebDriverWait(self.driver, 10).until(
            lambda driver: map_region.find_elements(
                By.CSS_SELECTOR, ".leaflet-container"
            )
        )
        ActionChains(self.driver).move_to_element(map_region).click().perform()
        WebDriverWait(self.driver, 10).until(
            EC.presence_of_element_located(
                (
                    By.CSS_SELECTOR,
                    ".leaflet-marker-icon[title='Pomična oznaka lokacije']",
                )
            )
        )

    def search_address(self):
        self.click(self.FIND_ON_MAP)

    def save(self):
        self.click(self.SAVE)

    def field_error(self, label):
        field = self.field(label)
        WebDriverWait(self.driver, 10).until(
            lambda driver: field.get_attribute("aria-invalid") == "true"
        )
        error_id = field.get_attribute("aria-describedby")
        if not error_id:
            raise AssertionError(f"Polje '{label}' nema povezanu poruku greške.")
        return self.wait_visible((By.ID, error_id)).text

    def clear_performance_log(self):
        self.driver.get_log("performance")

    def opportunity_save_was_requested(self):
        for entry in self.driver.get_log("performance"):
            message = json.loads(entry["message"])["message"]
            if message["method"] != "Network.requestWillBeSent":
                continue
            request = message["params"]["request"]
            if (
                request["method"] == "POST"
                and "/api/v1/opportunities" in request["url"]
            ):
                return True
        return False

    def wait_for_details(self):
        WebDriverWait(self.driver, 10).until(
            lambda driver: driver.current_url.rstrip("/").rsplit("/", 1)[-1]
            != "new"
        )
