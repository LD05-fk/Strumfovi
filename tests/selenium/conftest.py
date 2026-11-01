import os
import pytest
import requests
from selenium import webdriver
from selenium.webdriver.chrome.options import Options

BASE_URL = os.environ.get("BASE_URL", "http://localhost:5173")


@pytest.fixture(scope="session")
def base_url():
    return BASE_URL


@pytest.fixture
def driver():
    options = Options()
    options.add_argument("--headless=new")
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.set_capability("goog:loggingPrefs", {"performance": "ALL"})
    browser = webdriver.Chrome(options=options)
    browser.implicitly_wait(0)

    yield browser
    browser.quit()


@pytest.fixture
def login_as(driver, base_url):
    sessions = []

    def _login_as(email):
        session = requests.Session()
        sessions.append(session)
        response = session.post(
            f"{base_url}/api/v1/dev/login-as",
            json={"email": email},
            timeout=10,
        )
        if response.status_code == 404:
            raise RuntimeError("dev/login-as ne postoji.")
        response.raise_for_status()

        csrf = session.get(f"{base_url}/api/v1/auth/csrf", timeout=10)
        csrf.raise_for_status()
        session.headers["X-XSRF-TOKEN"] = csrf.json()["token"]

        driver.get(base_url)
        for cookie in session.cookies:
            driver.add_cookie(
                {"name": cookie.name, "value": cookie.value, "path": "/"}
            )
        driver.refresh()
        return session

    yield _login_as
    for session in sessions:
        try:
            response = session.post(f"{base_url}/api/v1/auth/logout", timeout=10)
            response.raise_for_status()
        finally:
            session.close()


@pytest.fixture
def portfolio_artist(login_as, base_url):
    """Svaki test počinje praznim obrascem, a zatim vraća izvorni dev profil."""
    session = login_as("umjetnik@artconnect.test")
    path = f"{base_url}/api/v1/me/artist-profile"
    response = session.get(path, timeout=10)
    response.raise_for_status()
    original = response.json()
    original_update = {
        "biography": original["biography"],
        "artistStatement": original["artistStatement"],
        "artFieldIds": [field["id"] for field in original["artFields"]],
        "links": [
            {"label": link["label"], "url": link["url"]}
            for link in original["links"]
        ],
    }
    try:
        response = session.put(path, json={
            "biography": None,
            "artistStatement": None,
            "artFieldIds": [],
            "links": [],
        }, timeout=10)
        response.raise_for_status()
        fields = session.get(f"{base_url}/api/v1/art-fields?pageSize=100", timeout=10)
        fields.raise_for_status()
        yield fields.json()["data"]
    finally:
        # I kod pada testa uklanjamo njegove unose i vraćamo spremljeni sadržaj.
        restored = session.put(path, json=original_update, timeout=10)
        restored.raise_for_status()


@pytest.fixture
def cleanup():
    undo_actions = []
    yield undo_actions.append
    for undo in reversed(undo_actions):
        try:
            undo()
        except Exception as error:
            print(f"Čišćenje nije uspjelo: {error}")


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    report = outcome.get_result()

    if report.when not in ("setup", "call") or not report.failed:
        return

    driver = item.funcargs.get("driver")
    if driver is None:
        return

    try:
        from pytest_html import extras

        report.extras = getattr(report, "extras", [])
        report.extras.append(extras.url(driver.current_url, name="Stranica"))
        report.extras.append(
            extras.png(driver.get_screenshot_as_base64(),
                       name="Snimka zaslona")
        )
    except Exception as error:
        print(f"Snimka zaslona nije uspjela: {error}")
