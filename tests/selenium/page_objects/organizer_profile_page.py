import json
from urllib.parse import urlparse

from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.base_page import BasePage, DEFAULT_TIMEOUT


class OrganizerProfilePage(BasePage):
    path = "/profile"
    ORGANIZATION_NAME = (By.ID, "organizationName")
    APPROVED = (By.XPATH, "//*[normalize-space()='Odobren']")
    SAVE = (By.XPATH, "//button[normalize-space()='Spremi']")
    SAVED = (By.XPATH, "//*[@role='status'][normalize-space()='Profil spremljen']")
    ORGANIZATION_NAME_ERROR = (By.ID, "organizationName-helper-text")

    def open(self):
        super().open()
        self.wait_for_path(self.path)
        WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
            lambda driver: driver.find_element(
                *self.ORGANIZATION_NAME
            ).get_attribute("value")
        )
        return self

    def set_organization_name(self, name):
        field = self.wait_visible(self.ORGANIZATION_NAME)
        field.clear()
        field.send_keys(name)

    def save(self):
        self.click(self.SAVE)

    def clear_network_log(self):
        self.driver.get_log("performance")

    def approved_badge_is_displayed(self):
        return self.wait_visible(self.APPROVED).is_displayed()

    def wait_for_organization_name(self, name):
        WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
            lambda driver: driver.find_element(
                *self.ORGANIZATION_NAME
            ).get_attribute("value") == name
        )

    def wait_for_validation_error(self):
        WebDriverWait(self.driver, DEFAULT_TIMEOUT).until(
            lambda driver: driver.find_element(
                *self.ORGANIZATION_NAME
            ).get_attribute("aria-invalid")
            == "true"
        )
        return self.wait_visible(self.ORGANIZATION_NAME_ERROR).text

    def organizer_profile_saves_sent(self):
        events = [
            json.loads(entry["message"])["message"]
            for entry in self.driver.get_log("performance")
        ]
        return [
            event["params"]["request"]
            for event in events
            if event["method"] == "Network.requestWillBeSent"
            and event["params"]["request"]["method"] == "PUT"
            and urlparse(event["params"]["request"]["url"]).path
            == "/api/v1/me/organizer-profile"
        ]
