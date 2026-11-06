from selenium.webdriver.common.by import By
from page_objects.base_page import BasePage


class HomePage(BasePage):
    path = "/"

    LOGIN_BUTTON = (By.CSS_SELECTOR, "a[aria-label='Prijava s Googleom']")
    LOGIN_ERROR = (
        By.XPATH, "//*[@role='alert' and contains(., 'Prijava nije uspjela')]")
    LOGIN_REQUIRED_PANEL = (
        By.CSS_SELECTOR, "section[aria-labelledby='login-required-title']"
    )
    FORBIDDEN = (By.XPATH, "//p[normalize-space()='403']")

    def html_language(self):
        return self.driver.find_element(By.TAG_NAME, "html").get_attribute("lang")

    def me_status(self):
        return self.driver.execute_async_script(
            """
            const done = arguments[arguments.length - 1];
            fetch('/api/v1/me', {credentials: 'include'})
              .then(response => done(response.status))
              .catch(() => done(0));
            """
        )

    def login_button_is_displayed_in(self, element):
        return element.find_element(*self.LOGIN_BUTTON).is_displayed()
