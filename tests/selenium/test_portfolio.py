import json
from urllib.parse import urlparse

from selenium.webdriver.common.by import By

from page_objects.profile_page import ProfilePage


def test_artist_adds_edits_and_deletes_past_exhibition(driver, base_url, past_exhibition_artist):
    """Cijeli tok izložbe kroz sučelje; nakon svake radnje provjerava se popis."""
    title = past_exhibition_artist
    page = ProfilePage(driver, base_url).open()

    page.add_exhibition(title, 2024, "Galerija Zagreb")
    assert title in page.wait_for_exhibition(title, 2024, "Galerija Zagreb")

    page.edit_exhibition_place(title, "Galerija Split")
    assert title in page.wait_for_exhibition(title, 2024, "Galerija Split")

    # Brisanje prolazi kroz dijalog potvrde, kao kod stvarnog korisnika.
    page.delete_exhibition(title)
    page.wait_for_exhibition_removed(title)
    assert title not in page.wait_visible(page.EXHIBITIONS).text


def test_artist_saves_profile_and_keeps_it_after_refresh(driver, base_url, portfolio_artist):
    """Umjetnik sprema biografiju, dva područja i poveznicu; osvježavanje ih zadržava."""
    assert len(portfolio_artist) >= 2, "Za test su potrebna dva aktivna područja."
    fields = portfolio_artist[:2]
    biography = "E2E biografija testnog umjetnika."
    url = "https://instagram.com/test"
    page = ProfilePage(driver, base_url).open()

    page.fill_biography(biography)
    for field in fields:
        page.select_art_field(field["name"])
    page.add_link(url)
    page.save()
    page.wait_visible(page.SAVED)

    driver.refresh()
    assert page.wait_visible(page.BIOGRAPHY).get_attribute("value") == biography
    assert set(page.selected_art_fields()) == {field["name"] for field in fields}
    assert page.wait_visible(page.LINK_URL).get_attribute("value") == url


def test_invalid_url_shows_field_error_without_sending_save(driver, base_url, portfolio_artist):
    """URL bez protokola daje grešku uz polje prije slanja zahtjeva backendu."""
    page = ProfilePage(driver, base_url).open()
    page.wait_visible(page.BIOGRAPHY)
    page.add_link("instagram.com/test")
    driver.get_log("performance")

    page.save()
    assert page.wait_for_url_error() == (
        "Unesite valjan URL koji počinje s http:// ili https://."
    )
    requests = [json.loads(entry["message"])["message"]
                for entry in driver.get_log("performance")]
    saves = [
        event["params"]["request"]
        for event in requests
        if event["method"] == "Network.requestWillBeSent"
        and event["params"]["request"]["method"] == "PUT"
        and urlparse(event["params"]["request"]["url"]).path
        == "/api/v1/me/artist-profile"
    ]
    assert saves == [], "Nevaljani obrazac ne smije poslati PUT profila."


def test_organizer_sees_own_profile_without_artist_options(driver, base_url, login_as):
    """Prema aktualnoj ruti organizator vidi svoj profil na istoj adresi /profile.

    Razlika prema #88: očekuje se organizatorski obrazac umjesto stranice 403.
    Umjetnički obrazac i njegove stavke izbornika ostaju nedostupni.
    """
    login_as("organizator@artconnect.test")
    page = ProfilePage(driver, base_url).open()
    page.wait_visible(page.ORGANIZATION)

    assert driver.find_elements(*page.BIOGRAPHY) == []
    assert driver.find_elements(*page.ART_FIELDS) == []
    assert driver.find_elements(By.CSS_SELECTOR, 'a[href="/portfolio"]') == []
    assert driver.find_elements(By.XPATH, "//a[normalize-space()='Moj profil']") == []

    page.click(page.USER_MENU)
    menu = page.wait_visible((By.CSS_SELECTOR, '[role="menu"]'))
    assert menu.find_element(By.CSS_SELECTOR, 'a[href="/profile"]').text == "Profil"
    assert menu.find_elements(By.CSS_SELECTOR, 'a[href="/portfolio"]') == []
    assert menu.find_elements(By.XPATH, ".//a[normalize-space()='Moj profil']") == []
