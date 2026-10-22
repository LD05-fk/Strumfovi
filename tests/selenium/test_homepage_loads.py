from selenium.webdriver.common.by import By
from page_objects.base_page import BasePage


def test_homepage_loads(driver, base_url):
    """Zadano: aplikacija radi. Kada: otvorim početnu stranicu. Tada: stranica se učita."""
    page = BasePage(driver, base_url).open()

    assert page.wait_visible((By.CSS_SELECTOR, "#root")).is_displayed()
    assert driver.title != ""
