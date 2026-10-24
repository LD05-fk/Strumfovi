from urllib.parse import urlparse

from page_objects.layout_page import LayoutPage


def test_fnd06_header_footer(driver, base_url):
    """Given posjetitelj, When otvori `/`, Then vidi zaglavlje s logom, stavkom "Prilike" i gumbom "Prijava s Googleom" te podnožje."""
    page = LayoutPage(driver, base_url).open()

    assert page.wait_visible(page.HEADER).is_displayed()
    assert page.wait_visible(page.LOGO).is_displayed()
    assert page.wait_visible(page.MAIN_NAV_ITEM).is_displayed()
    assert page.wait_visible(page.LOGIN_BUTTON).is_displayed()
    assert page.wait_visible(page.FOOTER).is_displayed()


def test_fnd06_does_not_exist(driver, base_url):
    """Given posjetitelj, When otvori `/does-not-exist`, Then vidi stranicu 404 s poveznicom na početnu, a `document.title` odgovara ruti."""
    page = LayoutPage(driver, base_url).open_path("/does-not-exist")

    assert page.wait_visible(page.NOT_FOUND_TITLE).is_displayed()
    home_link = page.wait_visible(page.HOME_LINK)
    assert urlparse(home_link.get_attribute("href")).path == "/"
    page.wait_until_title(f"404 | {page.APP_NAME}")


def test_fnd06_mobile_viewport_menu(driver, base_url):
    """Given prozor širine 375 px, When posjetitelj otvori izbornik i pritisne Esc, Then se drawer zatvara."""
    page = LayoutPage(driver, base_url).use_mobile_viewport().open()

    assert page.open_menu().is_displayed()
    page.press_escape()
    page.wait_until_gone(page.DRAWER_NAV)


def test_fnd06_first_tab_focus_skip_link(driver, base_url):
    """Given posjetitelj na `/`, When pritisne Tab, Then je prvi fokus poveznica "Preskoči na sadržaj"."""
    page = LayoutPage(driver, base_url).open()

    page.wait_present(page.SKIP_LINK)
    page.press_tab()
    assert page.focused_text() == page.SKIP_LINK_TEXT
