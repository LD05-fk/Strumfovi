from datetime import datetime, timedelta, timezone
import re
from uuid import uuid4

import pytest
import requests
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from page_objects.my_opportunities_page import MyOpportunitiesPage
from page_objects.opportunity_detail_page import OpportunityDetailPage
from page_objects.opportunity_form_page import OpportunityFormPage
from page_objects.opportunity_search_page import OpportunitySearchPage


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


# OPP-03: upravljanje vlastitim prilikama na stranici „Moje prilike”.
PUBLIC_STATUSES = ("OPEN", "CLOSED", "SELECTION", "FINISHED")


@pytest.fixture
def managed_opportunity(base_url, login_as):
    """Priprema jednu priliku organizatora kroz API i nakon testa briše samo nju."""
    session = login_as(ORGANIZER)
    created = []

    def _create(status="PREPARATION"):
        types = session.get(f"{base_url}/api/v1/opportunity-types", timeout=10)
        types.raise_for_status()
        fields = session.get(f"{base_url}/api/v1/art-fields?pageSize=100", timeout=10)
        fields.raise_for_status()
        # Jedna riječ bez crtica, da je javna punotekstna pretraga pronađe kao cjelinu.
        token = uuid4().hex
        deadline = datetime.now(timezone.utc) + timedelta(days=30)
        response = session.post(
            f"{base_url}/api/v1/opportunities",
            json={
                "title": f"E2E upravljanje {token}",
                "description": "Prilika za E2E test upravljanja statusom.",
                "typeId": _first(types.json())["id"],
                "artFieldIds": [_first(fields.json())["id"]],
                "deadline": deadline.strftime("%Y-%m-%dT%H:%M:%SZ"),
                "location": {"type": "ONLINE", "onlineUrl": "https://example.org/e2e"},
            },
            timeout=10,
        )
        response.raise_for_status()
        opportunity = response.json()
        created.append(opportunity["id"])
        if status == "OPEN":
            published = session.post(
                f"{base_url}/api/v1/opportunities/{opportunity['id']}/status",
                json={"status": "OPEN"},
                timeout=10,
            )
            published.raise_for_status()
        return {"id": opportunity["id"], "title": opportunity["title"], "token": token}

    yield _create
    for opportunity_id in created:
        deleted = session.delete(
            f"{base_url}/api/v1/opportunities/{opportunity_id}", timeout=10
        )
        # Test brisanja već je uklonio svoju priliku.
        if deleted.status_code != 404:
            deleted.raise_for_status()


def _first(payload):
    items = payload["data"] if isinstance(payload, dict) else payload
    return next(item for item in items if item.get("active", True))


def _public_search_ids(base_url, token):
    response = requests.get(
        f"{base_url}/api/v1/opportunities",
        params={"q": token, "statuses": PUBLIC_STATUSES},
        timeout=10,
    )
    response.raise_for_status()
    return [item["id"] for item in response.json()["data"]]


def test_opp_03_publish_makes_opportunity_visible_to_anonymous_visitor(
    driver, base_url, managed_opportunity
):
    opportunity = managed_opportunity()
    page = MyOpportunitiesPage(driver, base_url).open()
    assert page.status_of(opportunity["title"]) == "U pripremi"

    page.run_action(opportunity["title"], "Objavi")
    page.wait_for_status(opportunity["title"], "Otvorena")

    # Anonimni posjetitelj: isti preglednik bez kolačića sesije.
    driver.delete_all_cookies()
    search = OpportunitySearchPage(driver, base_url).search(opportunity["token"])
    assert search.wait_for_result(opportunity["title"]).is_displayed()


def test_opp_03_close_and_reopen_applications(
    driver, base_url, managed_opportunity
):
    opportunity = managed_opportunity(status="OPEN")
    page = MyOpportunitiesPage(driver, base_url).open()
    assert page.status_of(opportunity["title"]) == "Otvorena"

    page.run_action(opportunity["title"], "Zatvori prijave")
    page.wait_for_status(opportunity["title"], "Zatvorena")

    page.run_action(opportunity["title"], "Ponovno otvori prijave")
    page.wait_for_status(opportunity["title"], "Otvorena")


def test_opp_03_delete_opportunity_without_applications(
    driver, base_url, managed_opportunity
):
    opportunity = managed_opportunity()
    page = MyOpportunitiesPage(driver, base_url).open()
    page.wait_for_opportunity(opportunity["title"])

    page.run_action(opportunity["title"], "Obriši")
    page.wait_for_opportunity_removed(opportunity["title"])

    detail = OpportunityDetailPage(driver, base_url).open_path(
        f"/opportunities/{opportunity['id']}"
    )
    assert detail.wait_for_not_found().is_displayed()


def test_opp_03_archive_moves_opportunity_to_archived_tab(
    driver, base_url, managed_opportunity
):
    opportunity = managed_opportunity(status="OPEN")
    assert _public_search_ids(base_url, opportunity["token"]) == [opportunity["id"]]
    page = MyOpportunitiesPage(driver, base_url).open()

    page.run_action(opportunity["title"], "Arhiviraj")
    page.wait_for_opportunity_removed(opportunity["title"])

    page.open_archived_tab()
    page.wait_for_status(opportunity["title"], "Zatvorena")
    assert _public_search_ids(base_url, opportunity["token"]) == []
