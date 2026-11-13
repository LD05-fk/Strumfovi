import pytest
from page_objects.choose_role_page import ChooseRolePage
from page_objects.home_page import HomePage
from page_objects.layout_page import LayoutPage
from page_objects.organizer_profile_page import OrganizerProfilePage
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
    page = HomePage(driver, base_url).open_path("/opportunities")
    menu = UserMenu(driver, base_url)
    menu.open_menu()
    menu.logout()

    page.wait_for_path("/")
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


def test_auth_02_onboarding_user_is_redirected_from_profile(
    driver, base_url, onboarding_user
):
    """Zadano prijavljen novi korisnik, Kada otvori `/profile`, Tada je preusmjeren na `/onboarding`."""
    page = ChooseRolePage(driver, base_url)
    page.open_path("/profile")
    page.wait_for_path("/onboarding")


def test_auth_02_confirm_is_disabled_without_role(driver, base_url, onboarding_user):
    """Zadano novi korisnik na `/onboarding`, Kada ne odabere ulogu, Tada je gumb "Potvrdi" onemogućen."""
    page = ChooseRolePage(driver, base_url).open()

    assert page.confirm_is_disabled()


def test_auth_02_artist_role_redirects_and_is_visible_in_menu(
    driver, base_url, onboarding_user
):
    """Zadano novi korisnik na `/onboarding`, Kada odabere "Umjetnik" i potvrdi, Tada dolazi na `/profile`, izbornik prikazuje "Umjetnik", a `/onboarding` preusmjerava na `/profile`."""
    page = ChooseRolePage(driver, base_url).open()
    page.select_role("Umjetnik")
    page.confirm()
    page.wait_for_path("/profile")

    menu = UserMenu(driver, base_url)
    menu.open_menu()
    assert menu.role_is_displayed("Umjetnik")

    page.open_path("/onboarding")
    page.wait_for_path("/profile")


def test_auth_03_onboarding_user_can_choose_organizer_and_see_approved_profile(
    driver, base_url, onboarding_user
):
    """Zadano novi@artconnect.test, Kada odabere "Organizator", Tada na /profile vidi profil organizatora s oznakom „Odobren”"""
    page = ChooseRolePage(driver, base_url).open()
    page.select_role("Organizator")
    page.confirm()
    page.wait_for_path("/profile")

    profile = OrganizerProfilePage(driver, base_url)
    assert profile.approved_badge_is_displayed()


def test_auth_03_organizer_can_save_organization_name_and_keep_it_after_refresh(
    driver, base_url, organizer_profile
):
    """Zadano prijavljen organizator@artconnect.test, Kada promijeni naziv organizacije u „Galerija E2E” i spremi te osvježi stranicu, Tada polje prikazuje „Galerija E2E”"""
    page = OrganizerProfilePage(driver, base_url).open()
    page.set_organization_name("Galerija E2E")
    page.save()
    page.wait_visible(page.SAVED)

    driver.refresh()
    page.wait_for_organization_name("Galerija E2E")


def test_auth_03_organization_name_over_200_characters_is_rejected(
    driver, base_url, organizer_profile
):
    """Zadano organizator@artconnect.test, Kada unese 201 znak, Tada vidi validacijsku grešku i spremanje se ne šalje."""
    page = OrganizerProfilePage(driver, base_url).open()
    page.set_organization_name("x" * 201)
    page.clear_network_log()

    page.save()

    assert page.wait_for_validation_error() == (
        "Naziv organizacije smije imati najviše 200 znakova."
    )
    assert page.organizer_profile_saves_sent() == []
