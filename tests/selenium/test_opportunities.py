import re
from uuid import uuid4

import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.opportunity_detail_page import OpportunityDetailPage
from page_objects.opportunity_form_page import OpportunityFormPage


ORGANIZER = "organizator@artconnect.test"
ARTIST = "umjetnik@artconnect.test"


@pytest.fixture
def opportunity_organizer(driver, base_url, login_as):
    session = login_as(ORGANIZER)
    title = f"E2E prilika {uuid4()}"
    try:
        yield {"session": session, "title": title}
    finally:
        response = session.get(
            f"{base_url}/api/v1/me/opportunities?pageSize=100", timeout=10
        )
        response.raise_for_status()
        for opportunity in response.json()["data"]:
            if opportunity["title"] == title:
                deleted = session.delete(
                    f"{base_url}/api/v1/opportunities/{opportunity['id']}",
                    timeout=10,
                )
                if deleted.status_code != 404:
                    deleted.raise_for_status()


def fill_required_opportunity_fields(page, title):
    page.fill("Naslov", title)
    page.fill("Opis", "Opis testne prilike za umjetnike.")
    page.select_first_option("Vrsta prilike")
    page.select_first_option("Područja umjetnosti")
    page.set_deadline()


def test_opp_01_organizer_creates_physical_opportunity_with_manual_marker(
    driver, base_url, opportunity_organizer
):
    title = opportunity_organizer["title"]
    form = OpportunityFormPage(driver, base_url).open()
    fill_required_opportunity_fields(form, title)
    form.fill("Ulica i kućni broj", "Unska 3")
    form.fill("Grad", "Zagreb")
    form.fill("Poštanski broj", "10000")
    form.click_map_to_set_marker()
    form.save()
    form.wait_for_details()

    match = re.search(r"/opportunities/([0-9a-f-]{36})$", driver.current_url)
    assert match, f"Očekivan je detalj prilike, dobiven URL: {driver.current_url}"
    detail = OpportunityDetailPage(driver, base_url).wait_for_detail(match.group(1))

    assert detail.has_preparation_private_status()
    assert detail.wait_visible(
        (By.XPATH, "//*[normalize-space()='Unska 3, 10000 Zagreb']")
    ).is_displayed()
    detail.wait_for_location_marker(title)


def test_opp_01_online_opportunity_requires_url_without_sending_request(
    driver, base_url, opportunity_organizer
):
    form = OpportunityFormPage(driver, base_url).open()
    fill_required_opportunity_fields(form, opportunity_organizer["title"])
    form.select_location_type("Online")
    form.clear_performance_log()
    form.save()

    assert form.field_error("Poveznica na online lokaciju") == (
        "Unesite valjanu HTTP ili HTTPS poveznicu za online lokaciju."
    )
    assert not form.opportunity_save_was_requested()
    assert driver.current_url.endswith("/organizer/opportunities/new")


def test_opp_01_artist_is_forbidden_from_opportunity_form(
    driver, base_url, login_as
):
    login_as(ARTIST)
    driver.get(f"{base_url}/organizer/opportunities/new")

    forbidden = WebDriverWait(driver, 10).until(
        EC.visibility_of_element_located(
            (By.XPATH, "//*[self::p or self::h1][normalize-space()='403']")
        )
    )
    assert forbidden.is_displayed()


@pytest.mark.external
def test_opp_01_external_nominatim_search_finds_unska_3(
    driver, base_url, login_as
):
    login_as(ORGANIZER)
    form = OpportunityFormPage(driver, base_url).open()
    form.fill("Ulica i kućni broj", "Unska 3")
    form.fill("Grad", "Zagreb")
    form.search_address()

    candidates = WebDriverWait(driver, 20).until(
        EC.visibility_of_all_elements_located(
            (
                By.XPATH,
                "//label[normalize-space()='Pronađene adrese']/following-sibling::*[1]//label",
            )
        )
    )
    assert candidates
    form.wait_visible(
        (
            By.CSS_SELECTOR,
            ".leaflet-marker-icon[title='Pomična oznaka lokacije']",
        )
    )
