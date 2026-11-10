from urllib.parse import urlparse

from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

DEFAULT_TIMEOUT = 10


class BasePage:
    path = "/"

    def __init__(self, driver, base_url):
        self.driver = driver
        self.base_url = base_url

    def open(self):
        self.driver.get(self.base_url + self.path)
        return self

    def open_path(self, path):
        self.driver.get(self.base_url + path)
        return self

    def wait_for_path(self, path, timeout=DEFAULT_TIMEOUT):
        WebDriverWait(self.driver, timeout).until(
            lambda driver: urlparse(driver.current_url).path.rstrip("/")
            == path.rstrip("/")
        )

    def wait_visible(self, locator, timeout=DEFAULT_TIMEOUT):
        return WebDriverWait(self.driver, timeout).until(
            EC.visibility_of_element_located(locator)
        )

    def click(self, locator, timeout=DEFAULT_TIMEOUT):
        WebDriverWait(self.driver, timeout).until(
            EC.element_to_be_clickable(locator)
        ).click()
