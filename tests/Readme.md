# Testovi (E2E)

E2E testovi koriste Selenium (Python, pytest).
U CI-ju E2E pokreće posao e2e na PR-u koji mijenja backend, frontend, testove,
`docker-compose.yml` ili E2E workflow, te ručno i noćno.
Izvještaji su artefakti workflowa u kartici Actions.

## Preduvjeti

Python 3.12+, Google Chrome i Docker s Docker Compose.
pip install -r tests/requirements.txt
Sustav mora raditi prema `HOW_TO_RUN.md`
(baza, backend s profilom `dev`, frontend na http://localhost:5173).

## Pokretanje

pytest tests/selenium --html=report.html --self-contained-html --junitxml=test-results/pytest.xml
Iz korijena repozitorija.
Izvještaj u report.html.

Kod pada testa tijekom pripreme ili izvođenja, ako je preglednik dostupan,
`test-results/` sadrži PNG snimku zaslona i HTML stranice. Naziv datoteke
odgovara testu i fazi (`setup` ili `call`). Oba dokaza dostupna su i kroz
HTML izvještaj. Čišćenje podataka i odjava izvode se nakon snimanja pada.

CI i kod pada sprema `e2e-report` (`report.html`, `test-results/`, logovi
backenda i frontenda). Backend posao zasebno sprema `backend-test-reports`
sa Surefire i Failsafe izvještajima iz `backend/target/`.

## Struktura

- `tests/selenium/conftest.py` – preglednik, `login_as(email)`, `cleanup`
- `tests/selenium/page_objects/base_page.py` - temeljna klasa za sve page objekte u Selenium E2E testovima
- `tests/selenium/page_objects/` – jedan page objekt po stranici
- `tests/selenium` – testovi

## Prijava

Google prijava se ne automatizira.
`login_as(email)` poziva `POST /api/v1/dev/login-as`.
Korisnici: organizator@artconnect.test, umjetnik@artconnect.test, admin@artconnect.test, novi@artconnect.test(bez uloge)

## Profil umjetnika (PORT-01-TEST)

Samo tri E2E scenarija profila:

```bash
pytest tests/selenium/test_portfolio.py --html=report.html --self-contained-html
```

`ProfilePage` sadrži unos biografije, odabir područja i dodavanje poveznice.
Testovi provjeravaju spremanje i osvježavanje, grešku nevaljanog URL-a bez slanja
PUT zahtjeva te odvajanje umjetničkog i organizatorskog profila.
Prema aktualnom kodu organizator na `/profile` vidi vlastiti obrazac, a ne 403
naveden u izvornom issueu #88. Ne vidi umjetnički obrazac ni stavke „Moj profil”
i „Moj portfolio”; njegova stavka „Profil” vodi na organizatorski obrazac.

`portfolio_artist` prije testa sprema izvorni sadržaj dev profila i priprema
prazan obrazac. Nakon testa, čak i ako test padne, uklanja testne unose vraćanjem
izvornog sadržaja. Dev korisnici ostaju u bazi, a testne sesije se odjavljuju.
Testovi se izvode redom, bez paralelnog korištenja istog dev umjetnika.

Integracijski `ArtistProfileFlowIT` koristi `IntegrationTestBase`, stvarnu
sesiju iz `dev/login-as` i CSRF token iz `/auth/csrf`. `PortfolioFixtures`
stvara korisnike i područja samo za taj test te ih zajedno sa sesijama briše
nakon svakog testa. Ciljano pokretanje iz `backend/`:

```bash
./mvnw '-Dtest=ArtistProfileFlowIT' test
```

Klasa završava s `IT`, pa je redoviti `mvn verify` pokreće preko Failsafea.
