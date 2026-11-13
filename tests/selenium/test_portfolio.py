import json
import pytest
from urllib.parse import urlparse
from uuid import uuid4

from selenium.webdriver.common.by import By

from page_objects.profile_page import ProfilePage
from page_objects.artwork_form_page import ArtworkFormPage
from page_objects.my_portfolio_page import MyPortfolioPage
from page_objects.public_portfolio_page import PublicPortfolioPage


def test_artwork_images_upload_limit_and_removal(
    driver, base_url, artwork_image_artist, artwork_image_files
):
    """JPG i PNG se prikazuju, treća slika zatvara dodavanje, brisanje ga otvara."""
    page = ArtworkFormPage(driver, base_url)
    artwork_id = page.create_public(artwork_image_artist)
    page.upload_image(artwork_image_files["art.jpg"])
    page.wait_for_images(1)
    page.upload_image(artwork_image_files["art.png"])
    assert len(page.wait_for_images(2)) == 2
    page.wait_for_path(f"/portfolio/artworks/{artwork_id}")

    # Osvježavanje provjerava da sličice dolaze iz pohrane, a ne samo iz lokalnog pregleda.
    driver.refresh()
    page.wait_for_images(2)
    page.upload_image(artwork_image_files["art.jpg"])
    page.wait_for_images(3)
    assert not page.wait_visible(page.ADD_IMAGE).is_enabled()

    page.remove_first_image()
    page.wait_for_images(2)
    assert page.wait_visible(page.ADD_IMAGE).is_enabled()
    page.upload_image(artwork_image_files["art.png"])
    page.wait_for_images(3)
    # Fixture na kraju briše djelo i sve njegove datoteke, čak i kod pada testa.


@pytest.mark.parametrize("name,message", [
    ("document.pdf", "Dopuštene su samo JPG i PNG slike"),
    ("big.jpg", "Slika je veća od 5 MB"),
])
def test_artwork_images_reject_invalid_files(
    driver, base_url, public_artwork, artwork_image_files, name, message
):
    """Pogrešan tip i prekoračenje veličine daju jasnu poruku bez dodavanja slike."""
    page = ArtworkFormPage(driver, base_url).open_artwork(public_artwork["id"])
    page.upload_image(artwork_image_files[name])
    assert message in page.wait_for_image_error(message)
    page.wait_visible((By.XPATH, "//*[normalize-space()='0/3 slike']"))
    assert driver.find_elements(*page.THUMBNAILS) == []


def test_artwork_empty_form_shows_three_required_errors(driver, base_url, login_as):
    """Prazan obrazac mora objasniti da nedostaju naslov, godina i tehnika."""
    login_as("umjetnik@artconnect.test")
    page = ArtworkFormPage(driver, base_url).open()
    page.save()

    assert page.field_error("title") == "Ovo polje je obavezno."
    assert page.field_error("year") == "Godina je obavezna."
    assert page.field_error("technique") == "Ovo polje je obavezno."


def test_artwork_made_private_disappears_from_public_portfolio(
    driver, base_url, login_as, public_artwork
):
    """Javno djelo vidljivo je posjetitelju, a nakon promjene u privatno više nije."""
    artwork = public_artwork
    public_page = PublicPortfolioPage(driver, base_url)

    # Brisanjem kolačića preglednik postaje posjetitelj; API fixture zadržava sesiju.
    driver.delete_all_cookies()
    public_page.open_artist(artwork["artistId"])
    assert public_page.wait_for_artwork(artwork["title"], artwork["year"]).is_displayed()

    login_as("umjetnik@artconnect.test")
    ArtworkFormPage(driver, base_url).open_artwork(artwork["id"]).make_private_and_save()
    # Djelo se skriva od posjetitelja, ali mora ostati na vlasnikovom popisu.
    portfolio_page = MyPortfolioPage(driver, base_url).open()
    assert portfolio_page.wait_for_artwork(artwork["title"]).is_displayed()

    driver.delete_all_cookies()
    public_page.open_artist(artwork["artistId"])
    assert artwork["title"] not in public_page.artwork_titles()


def test_artwork_deleted_with_confirmation_disappears_from_both_portfolios(
    driver, base_url, login_as, public_artwork
):
    """Brisanje javnog djela kroz karticu uklanja ga i iz javne galerije."""
    artwork = public_artwork
    public_page = PublicPortfolioPage(driver, base_url)
    driver.delete_all_cookies()
    public_page.open_artist(artwork["artistId"])
    assert public_page.wait_for_artwork(artwork["title"], artwork["year"]).is_displayed()

    login_as("umjetnik@artconnect.test")
    page = MyPortfolioPage(driver, base_url).open()
    page.wait_for_artwork(artwork["title"])
    page.delete_artwork(artwork["title"])
    page.wait_for_artwork_removed(artwork["title"])

    driver.delete_all_cookies()
    public_page.open_artist(artwork["artistId"])
    assert artwork["title"] not in public_page.artwork_titles()


# Organizatorov 403 na /portfolio već pokriva test_auth_01_wrong_role_sees_403
# u test_auth.py. Koristimo postojeći test kako ne bismo ponavljali istu provjeru.


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


def test_visitor_sees_only_public_portfolio_and_can_open_lightbox(
    driver, base_url, public_portfolio_data
):
    data = public_portfolio_data
    public_artwork = data["artworks"]["PUBLIC"]
    private_artwork = data["artworks"]["PRIVATE"]
    page = PublicPortfolioPage(driver, base_url).open_artist(data["artist_id"])

    assert page.wait_for_artist().text
    assert data["exhibition_title"] in page.wait_visible(page.PAST_EXHIBITIONS).text
    assert page.wait_for_artwork(public_artwork["title"], 2024)
    assert driver.find_elements(
        By.XPATH, f"//h3[normalize-space()='{private_artwork['title']}']"
    ) == []
    assert driver.find_elements(*page.PARTICIPATIONS) == []

    lightbox = page.open_artwork(public_artwork["title"], 2024)
    assert public_artwork["title"] in lightbox.text
    page.close_lightbox_with_escape()


def test_unknown_artist_shows_not_found_page(driver, base_url):
    page = PublicPortfolioPage(driver, base_url).open_artist(uuid4())

    assert page.wait_visible(
        (By.XPATH, "//h1[normalize-space()='Umjetnik nije pronađen']")
    ).is_displayed()
