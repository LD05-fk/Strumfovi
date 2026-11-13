from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import BasePage


class OpportunityDetailPage(BasePage):
    """Detalj prilike i njezina lokacija."""

    def wait_for_detail(self, opportunity_id):
        self.wait_for_path(f"/opportunities/{opportunity_id}")
        return self

    def has_preparation_private_status(self):
        return self.wait_visible(
            (By.XPATH, "//*[normalize-space()='U pripremi – vidljivo samo vama']")
        ).is_displayed()

    def wait_for_not_found(self):
        message = "Prilika ne postoji ili nije dostupna"
        return self.wait_visible(
            (
                By.XPATH,
                f"//*[contains(normalize-space(text()), '{message}')]",
            )
        )

    def wait_for_location_marker(self, title):
        WebDriverWait(self.driver, 10).until(
            EC.visibility_of_element_located(
                (By.CSS_SELECTOR, '[role="region"][aria-label="Karta lokacije prilike"]')
            )
        )
        return WebDriverWait(self.driver, 10).until(
            EC.presence_of_element_located(
                (By.CSS_SELECTOR, f".leaflet-marker-icon[title='{title}']")
            )
        )
