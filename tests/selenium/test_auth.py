import pytest
from selenium.webdriver.support.ui import WebDriverWait
from page_objects.base_page import DEFAULT_TIMEOUT
from page_objects.home_page import HomePage
from page_objects.layout_page import LayoutPage
from page_objects.user_menu import UserMenu


ARTIST = "umjetnik@artconnect.test"
ORGANIZER = "organizator@artconnect.test"
ADMIN = "admin@artconnect.test"

EXPECTED_NAVIGATION = {
    None: ["Prilike"],
    ARTIST: ["Prilike", "Moj profil", "Moj portfolio", "Moje prijave"],
    ORGANIZER: ["Prilike", "Moje prilike", "Statistika"],
    ADMIN: ["Prilike", "Administracija"],
}


def test_auth_01_visitor_sees_google_login(driver, base_url):
    """Zadano neprijavljeni posjetitelj, Kada otvori `/`, Tada vidi gumb "Prijava s Googleom" s poveznicom za Google prijavu."""
    page = HomePage(driver, base_url).open()

    login = page.wait_visible(page.LOGIN_BUTTON)
    assert login.get_attribute("aria-label") == "Prijava s Googleom"
    assert login.get_attribute("href").endswith("/oauth2/authorization/google")


def test_auth_01_login_error_is_visible(driver, base_url):
    """Zadano neprijavljeni posjetitelj, Kada otvori `/?login=error`, Tada vidi poruku o neuspjeloj prijavi."""
    page = HomePage(driver, base_url).open_path("/?login=error")

    assert page.wait_visible(page.LOGIN_ERROR).is_displayed()


def test_auth_01_visitor_is_prompted_to_login_for_portfolio(driver, base_url):
    """Zadano neprijavljeni posjetitelj, Kada otvori `/portfolio`, Tada vidi panel "Prijava potrebna" s gumbom za prijavu."""
    page = HomePage(driver, base_url).open_path("/portfolio")

    panel = page.wait_visible(page.LOGIN_REQUIRED_PANEL)
    assert "Prijava potrebna" in panel.text
    assert page.login_button_is_displayed_in(panel)


@pytest.mark.parametrize(
    ("email", "path"),
    [(ARTIST, "/my-opportunities"), (ORGANIZER, "/portfolio")],
    ids=[
        "artist-forbidden-from-my-opportunities",
        "organizer-forbidden-from-portfolio",
    ],
)
def test_auth_01_wrong_role_sees_403(driver, base_url, login_as, email, path):
    """Zadano prijavljeni korisnik pogrešne uloge, Kada otvori zaštićenu rutu, Tada vidi stranicu 403."""
    login_as(email)
    page = HomePage(driver, base_url).open_path(path)

    assert page.wait_visible(page.FORBIDDEN).is_displayed()


def test_auth_01_artist_can_logout(driver, base_url, login_as):
    """Zadano prijavljeni umjetnik, Kada odabere "Odjava", Tada dolazi na `/`, vidi gumb za prijavu, a `/api/v1/me` vraća 401."""
    login_as(ARTIST)
    HomePage(driver, base_url).open_path("/opportunities")
    menu = UserMenu(driver, base_url)
    menu.open_menu()
    menu.logout()

    WebDriverWait(driver, DEFAULT_TIMEOUT).until(
        lambda current_driver: current_driver.current_url.rstrip("/")
        == base_url.rstrip("/")
    )
    page = HomePage(driver, base_url)
    assert page.wait_visible(page.LOGIN_BUTTON).is_displayed()
    assert page.me_status() == 401


def test_auth_01_admin_menu_has_role_without_profile(driver, base_url, login_as):
    """Zadano prijavljeni administrator, Kada otvori korisnički izbornik, Tada vidi oznaku "Administrator" i nema stavku "Profil"."""
    login_as(ADMIN)
    HomePage(driver, base_url).open()
    menu = UserMenu(driver, base_url)
    menu.open_menu()

    assert menu.administrator_is_displayed()
    assert not menu.has_profile_item()


@pytest.mark.parametrize(
    "email",
    [None, ARTIST, ORGANIZER, ADMIN],
    ids=["visitor", "artist", "organizer", "admin"],
)
def test_auth_01_header_navigation_is_keyboard_accessible(
    driver, base_url, login_as, email
):
    """Zadano neprijavljeni ili prijavljeni korisnik, Kada otvori `/` i tipkom Tab prolazi navigaciju zaglavlja, Tada svaka stavka dobije fokus i odgovara `:focus-visible`."""
    if email is not None:
        login_as(email)

    page = LayoutPage(driver, base_url).open()
    for link in page.wait_for_header_nav_labels(EXPECTED_NAVIGATION[email]):
        page.tab_to(link)
        assert page.focused_element_is_focus_visible()


def test_auth_01_html_language_is_croatian(driver, base_url):
    """Zadano neprijavljeni posjetitelj, Kada otvori `/`, Tada element `<html>` ima atribut `lang="hr"`."""
    page = HomePage(driver, base_url).open()

    assert page.html_language() == "hr"
