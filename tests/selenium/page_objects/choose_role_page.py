from selenium.webdriver.common.by import By

from page_objects.base_page import BasePage


class ChooseRolePage(BasePage):
    path = "/onboarding"
    HEADING = (By.CSS_SELECTOR, "h1")
    CONFIRM = (By.XPATH, "//button[normalize-space()='Potvrdi']")

    def role_choice(self, role):
        return (
            By.XPATH,
            f"//*[@role='radio'][.//*[normalize-space()='{role}']]",
        )

    def open(self):
        super().open()
        self.wait_for_path(self.path)
        self.wait_visible(self.HEADING)
        return self

    def select_role(self, role):
        self.click(self.role_choice(role))

    def confirm_is_disabled(self):
        return not self.wait_visible(self.CONFIRM).is_enabled()

    def confirm(self):
        self.click(self.CONFIRM)
