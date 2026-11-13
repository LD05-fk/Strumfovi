from urllib.parse import urlencode

from selenium.webdriver.common.by import By

from page_objects.base_page import BasePage


class OpportunitySearchPage(BasePage):
    """Javni popis prilika /opportunities."""

    path = "/opportunities"

    def search(self, q):
        self.open_path(f"{self.path}?{urlencode({'q': q})}")
        return self

    def wait_for_result(self, title):
        return self.wait_visible((By.XPATH, f"//h2[normalize-space()='{title}']"))
