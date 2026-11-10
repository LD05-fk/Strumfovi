import base64
import os
from pathlib import Path
import re
from uuid import uuid4
import pytest
import requests
from selenium import webdriver
from selenium.webdriver.chrome.options import Options

BASE_URL = os.environ.get("BASE_URL", "http://localhost:5173")
DRIVER_KEY = pytest.StashKey()


@pytest.fixture(scope="session")
def base_url():
    return BASE_URL


@pytest.fixture
def driver(request):
    options = Options()
    options.add_argument("--headless=new")
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.set_capability("goog:loggingPrefs", {"performance": "ALL"})
    browser = webdriver.Chrome(options=options)
    # Dostupno i ako drugi fixture padne prije ulaska u tijelo testa.
    request.node.stash[DRIVER_KEY] = browser
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
def reset_dev_user(base_url):
    def _reset_dev_user():
        response = requests.post(
            f"{base_url}/api/v1/dev/reset-user",
            timeout=10,
        )
        if response.status_code == 404:
            raise RuntimeError("dev/reset-user ne postoji.")
        response.raise_for_status()
        return response.json()

    return _reset_dev_user


@pytest.fixture
def onboarding_user(reset_dev_user, login_as):
    reset_dev_user()
    session = login_as("novi@artconnect.test")
    try:
        yield session
    finally:
        reset_dev_user()


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
def past_exhibition_artist(login_as, base_url):
    """Prijava umjetnika i jedinstven naslov za izložbu koju ovaj test smije obrisati."""
    session = login_as("umjetnik@artconnect.test")
    response = session.get(f"{base_url}/api/v1/me/artist-profile", timeout=10)
    response.raise_for_status()
    artist_id = response.json()["userId"]
    title = f"E2E izložba {uuid4()}"
    try:
        yield title
    finally:
        # Ako test padne prije brisanja, uklanjamo samo njegov unos; postojeće izložbe ostaju.
        response = session.get(f"{base_url}/api/v1/artists/{artist_id}", timeout=10)
        response.raise_for_status()
        for exhibition in response.json()["pastExhibitions"]:
            if exhibition["title"] == title:
                deleted = session.delete(
                    f"{base_url}/api/v1/past-exhibitions/{exhibition['id']}", timeout=10
                )
                deleted.raise_for_status()


@pytest.fixture
def public_artwork(login_as, base_url):
    """Priprema jedno javno djelo; postojeća djela dev umjetnika ne mijenja."""
    session = login_as("umjetnik@artconnect.test")
    path = f"{base_url}/api/v1/artworks"
    artwork = None
    try:
        # Priprema ide kroz API, a radnju koju ispitujemo test obavlja kroz sučelje.
        response = session.post(path, json={
            "title": f"E2E djelo {uuid4()}",
            "year": 2024,
            "technique": "Akril",
            "dimensions": None,
            "description": None,
            "visibility": "PUBLIC",
        }, timeout=10)
        response.raise_for_status()
        artwork = response.json()
        yield artwork
    finally:
        # API sesija ostaje dostupna i kad preglednik prijeđe u neprijavljeni prikaz.
        # Ako je sam test već obrisao djelo, očekivani 404 nije greška čišćenja.
        if artwork is not None:
            response = session.delete(f"{path}/{artwork['id']}", timeout=10)
            if response.status_code != 404:
                response.raise_for_status()


@pytest.fixture
def public_portfolio_data(base_url):
    """Stvara jedinstvene javne podatke, a nakon testa briše samo svoje zapise."""
    session = requests.Session()
    artwork_ids = []
    exhibition_id = None
    logged_in = False
    try:
        login = session.post(
            f"{base_url}/api/v1/dev/login-as",
            json={"email": "umjetnik@artconnect.test"},
            timeout=10,
        )
        if login.status_code == 404:
            raise RuntimeError("dev/login-as ne postoji.")
        login.raise_for_status()
        logged_in = True

        csrf = session.get(f"{base_url}/api/v1/auth/csrf", timeout=10)
        csrf.raise_for_status()
        session.headers["X-XSRF-TOKEN"] = csrf.json()["token"]

        profile = session.get(f"{base_url}/api/v1/me/artist-profile", timeout=10)
        profile.raise_for_status()
        artist_id = profile.json()["userId"]
        exhibition = session.post(
            f"{base_url}/api/v1/past-exhibitions",
            json={
                "title": f"E2E javna izložba {uuid4()}",
                "year": 2024,
                "place": "Galerija test",
            },
            timeout=10,
        )
        exhibition.raise_for_status()
        exhibition_id = exhibition.json()["id"]

        artworks = {}
        for visibility in ("PUBLIC", "PRIVATE"):
            title = f"E2E {visibility} djelo {uuid4()}"
            response = session.post(
                f"{base_url}/api/v1/artworks",
                json={
                    "title": title,
                    "year": 2024,
                    "technique": "Ulje",
                    "dimensions": None,
                    "description": None,
                    "visibility": visibility,
                },
                timeout=10,
            )
            response.raise_for_status()
            artwork_id = response.json()["id"]
            artwork_ids.append(artwork_id)
            artworks[visibility] = {"id": artwork_id, "title": title}

        yield {
            "artist_id": artist_id,
            "exhibition_title": exhibition.json()["title"],
            "artworks": artworks,
        }
    finally:
        try:
            for artwork_id in reversed(artwork_ids):
                deleted = session.delete(
                    f"{base_url}/api/v1/artworks/{artwork_id}", timeout=10
                )
                deleted.raise_for_status()
            if exhibition_id:
                deleted = session.delete(
                    f"{base_url}/api/v1/past-exhibitions/{exhibition_id}", timeout=10
                )
                deleted.raise_for_status()
        finally:
            try:
                if logged_in:
                    logout = session.post(f"{base_url}/api/v1/auth/logout", timeout=10)
                    logout.raise_for_status()
            finally:
                session.close()


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

    driver = item.stash.get(DRIVER_KEY, None)
    if driver is None:
        return

    try:
        output_dir = Path.cwd() / "test-results"
        output_dir.mkdir(parents=True, exist_ok=True)
        name = re.sub(
            r"[^A-Za-z0-9_.-]+", "_",
            f"{item.path.stem}-{item.name}-{report.when}",
        )
        screenshot = driver.get_screenshot_as_base64()
        source = driver.page_source
        (output_dir / f"{name}.png").write_bytes(base64.b64decode(screenshot))
        (output_dir / f"{name}.html").write_text(source, encoding="utf-8")

        from pytest_html import extras

        report.extras = getattr(report, "extras", [])
        report.extras.append(extras.url(driver.current_url, name="Stranica"))
        report.extras.append(
            extras.png(screenshot, name="Snimka zaslona")
        )
        report.extras.append(extras.text(source, name="HTML stranice"))
    except Exception as error:
        print(f"Spremanje dokaza pada testa nije uspjelo: {error}")
