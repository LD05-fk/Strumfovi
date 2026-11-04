from selenium.webdriver.common.by import By
from page_objects.base_page import BasePage


class UserMenu(BasePage):
    MENU_BUTTON = (By.CSS_SELECTOR, "button[aria-label='Korisnički izbornik']")
    OPTIONS = (By.CSS_SELECTOR, "ul[aria-label='Korisničke opcije']")
    ADMINISTRATOR = (By.XPATH, "//*[normalize-space()='Administrator']")
    PROFILE_ITEM = (By.XPATH, "//li[normalize-space()='Profil']")
    LOGOUT = (By.XPATH, "//li[normalize-space()='Odjava']")

    def open_menu(self):
        self.click(self.MENU_BUTTON)
        return self.wait_visible(self.OPTIONS)

    def logout(self):
        self.click(self.LOGOUT)

    def administrator_is_displayed(self):
        return self.driver.find_element(*self.ADMINISTRATOR).is_displayed()

    def has_profile_item(self):
        return bool(self.driver.find_elements(*self.PROFILE_ITEM))
