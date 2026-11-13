from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import DEFAULT_TIMEOUT, BasePage


class MyOpportunitiesPage(BasePage):
    """Popis „Moje prilike” i radnje vlasnika kroz izbornik i dijalog potvrde."""

    path = "/my-opportunities"
    HEADING = (By.XPATH, "//h1[normalize-space()='Moje prilike']")
    DIALOG = (
        By.CSS_SELECTOR,
        '[role="dialog"][aria-labelledby="owner-action-confirm-title"]',
    )

    def open(self):
        super().open()
        self.wait_visible(self.HEADING)
        return self

    def _card(self, title):
        # Naslov je poveznica na detalj i razlikuje karticu od ostalih prilika organizatora.
        return (By.XPATH, f"//li[.//a[normalize-space()='{title}']]")

    def wait_for_opportunity(self, title):
        return self.wait_visible(self._card(title))

    def status_of(self, title):
        card = self.wait_for_opportunity(title)
        return card.find_element(By.CSS_SELECTOR, ".MuiChip-label").text

    def wait_for_status(self, title, status):
        WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
            lambda _: self.status_of(title) == status,
            f"Prilika „{title}” nije dobila status „{status}”.",
        )

    def run_action(self, title, action):
        """Odabere radnju iz izbornika kartice i potvrdi je u dijalogu."""
        card = self._card(title)
        self.click((By.XPATH, f"{card[1]}//button[@aria-label='Radnje']"))
        self.click(
            (By.XPATH, f"//*[@role='menuitem'][normalize-space()='{action}']")
        )
        self.wait_visible(self.DIALOG)
        # Gumb potvrde nosi isti naziv kao odabrana radnja.
        self.click(
            (By.XPATH, f"//*[@role='dialog']//button[normalize-space()='{action}']")
        )
        WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
            EC.invisibility_of_element_located(self.DIALOG)
        )

    def wait_for_opportunity_removed(self, title):
        WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
            EC.invisibility_of_element_located(self._card(title))
        )

    def open_archived_tab(self):
        self.click((By.XPATH, "//*[@role='tab'][normalize-space()='Arhivirane']"))
        self.wait_visible(
            (By.XPATH, "//*[@role='tab'][@aria-selected='true'][normalize-space()='Arhivirane']")
        )
        return self
