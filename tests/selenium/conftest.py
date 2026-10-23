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
    browser = webdriver.Chrome(options=options)
    browser.implicitly_wait(0)

    yield browser
    browser.quit()


@pytest.fixture
def login_as(driver, base_url):
    def _login_as(email):
        session = requests.Session()
        response = session.post(
            f"{base_url}/api/v1/dev/login-as",
            json={"email": email},
            timeout=10,
        )
        if response.status_code == 404:
            raise RuntimeError("dev/login-as ne postoji.")
        response.raise_for_status()

        driver.get(base_url)
        for cookie in session.cookies:
            driver.add_cookie(
                {"name": cookie.name, "value": cookie.value, "path": "/"}
            )
        driver.refresh()

    return _login_as


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
