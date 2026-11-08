from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import BasePage


class MyPortfolioPage(BasePage):
    """Popis vlastitih djela i brisanje preko stvarnog dijaloga potvrde."""

    path = "/portfolio"
    HEADING = (By.XPATH, "//h1[normalize-space()='Moj portfolio']")
    DELETE_DIALOG = (By.CSS_SELECTOR, '[role="dialog"][aria-labelledby="portfolio-confirm-title"]')
    CONFIRM_DELETE = (By.XPATH, "//*[@role='dialog']//button[normalize-space()='Obriši']")
    DELETED = (By.XPATH, "//*[@role='status'][normalize-space()='Djelo obrisano']")

    def wait_for_artwork(self, title):
        return self.wait_visible((By.XPATH, f"//article//h2[normalize-space()='{title}']"))

    def delete_artwork(self, title):
        # Naslov u pristupačnom nazivu gumba razlikuje djelo od ostalih kartica.
        self.click((By.CSS_SELECTOR, f'button[aria-label="Obriši: {title}"]'))
        self.wait_visible(self.DELETE_DIALOG)
        self.click(self.CONFIRM_DELETE)
        self.wait_visible(self.DELETED)
        WebDriverWait(self.driver, 10).until(
            EC.invisibility_of_element_located(self.DELETE_DIALOG)
        )

    def wait_for_artwork_removed(self, title):
        self.wait_visible(self.HEADING)
        heading = (By.XPATH, f"//article//h2[normalize-space()='{title}']")
        WebDriverWait(self.driver, 10).until(
            EC.invisibility_of_element_located(heading)
        )
