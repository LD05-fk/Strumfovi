from urllib.parse import parse_qs, urlencode, urlparse

from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import DEFAULT_TIMEOUT, BasePage


class OpportunitySearchPage(BasePage):
    """Javni popis prilika /opportunities: filtri, prikaz popisa i karte."""

    path = "/opportunities"
    FILTERS = "//section[@aria-label='Filtri']"
    MAP = "//*[@role='region'][@aria-label='Karta prilika']"
    CARD_TITLES = (By.XPATH, "//main//li//h2")
    EMPTY_STATE = (By.XPATH, "//*[normalize-space()='Nema prilika za odabrane filtre']")

    def search(self, q):
        self.open_path(f"{self.path}?{urlencode({'q': q})}")
        return self

    def wait_for_result(self, title):
        return self.wait_visible((By.XPATH, f"//h2[normalize-space()='{title}']"))

    def wait_for_titles(self, expected):
        """Čeka da popis prikazuje točno zadane naslove, bez obzira na poredak."""
        WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
            lambda _: sorted(self.titles()) == sorted(expected),
            f"Očekivani naslovi: {sorted(expected)}",
        )

    def titles(self):
        return [
            element.text
            for element in self.driver.find_elements(*self.CARD_TITLES)
            if element.is_displayed()
        ]

    def filter_city(self, city):
        field = self.wait_visible((
            By.XPATH,
            f"{self.FILTERS}//input[@id=//label[normalize-space()='Grad']/@for]",
        ))
        field.clear()
        field.send_keys(city)

    def city_filter_value(self):
        return self.wait_visible((
            By.XPATH,
            f"{self.FILTERS}//input[@id=//label[normalize-space()='Grad']/@for]",
        )).get_attribute("value")

    def wait_for_query_param(self, name, value):
        WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
            lambda driver: parse_qs(urlparse(driver.current_url).query).get(name) == [value],
            f"URL nema {name}={value}.",
        )

    def show_map(self):
        self.click((By.XPATH, "//*[@aria-label='Prikaz']//button[normalize-space()='Karta']"))
        self.wait_visible((By.XPATH, self.MAP))
        return self

    def wait_for_markers(self, titles):
        for title in titles:
            WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
                EC.presence_of_element_located(self._marker(title))
            )
        return self.driver.find_elements(
            By.XPATH, f"{self.MAP}//*[contains(@class, 'leaflet-marker-icon')]"
        )

    def map_attribution(self):
        return self.wait_visible((By.XPATH, f"{self.MAP}//a[normalize-space()='OpenStreetMap']"))

    def missing_location_notice(self, count):
        return self.wait_visible((
            By.XPATH,
            f"//*[@role='alert'][contains(normalize-space(), 'Prilike bez lokacije: {count}.')]",
        ))

    def open_marker_details(self, title):
        self.click(self._marker(title))
        # Poveznica u skočnom prozoru oznake, a ne istoimeni gumb na kartici pokraj karte.
        self.click((
            By.XPATH,
            f"{self.MAP}//*[contains(@class, 'leaflet-popup')]"
            f"//a[@aria-label='Detalji prilike {title}']",
        ))

    def wait_for_empty_state(self):
        return self.wait_visible(self.EMPTY_STATE)

    def clear_filters(self):
        self.click((By.XPATH, f"{self.FILTERS}//button[normalize-space()='Očisti filtre']"))
        WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
            EC.invisibility_of_element_located(self.EMPTY_STATE)
        )

    def _marker(self, title):
        return (
            By.XPATH,
            f"{self.MAP}//*[contains(@class, 'leaflet-marker-icon')][@title='{title}']",
        )
