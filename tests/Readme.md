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

Tri E2E scenarija profila (bez testa izložbi):

```bash
pytest tests/selenium/test_portfolio.py -k 'not past_exhibition' --html=report.html --self-contained-html
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

## Prethodne izložbe (PORT-02-TEST)

`ArtistProfileFlowIT` dodatno provjerava dodavanje, uređivanje i brisanje izložbe
uz ponovni dohvat javnog profila nakon svake promjene. Pokriva i buduću godinu
(400), uređivanje tuđe izložbe (404) te sve tri radnje za organizatora i
korisnika `ONBOARDING` (403 s odgovarajućim kodom). Koristi iste stvarne sesije,
CSRF tokene i automatsko čišćenje kroz `PortfolioFixtures`.

Jedan E2E test prolazi dodavanje, promjenu mjesta i brisanje uz potvrdu:

```bash
pytest tests/selenium/test_portfolio.py -k past_exhibition --html=report.html --self-contained-html --junitxml=test-results/pytest.xml
```

`ProfilePage` upravlja dijalozima i čeka promjene u popisu. Fixture
`past_exhibition_artist` prijavljuje dev umjetnika i priprema jedinstven naslov.
Ako test padne prije brisanja, preko API-ja briše samo tu testnu izložbu.
Postojeće izložbe ostaju netaknute. Prijava i odjava koriste postojeći `login_as`.
Za sve E2E testove portfolija pokrenite gornju naredbu bez `-k` filtra.

Postojeći backend i E2E workflowi automatski uključuju ove scenarije i spremaju
izvještaje te PNG/HTML dokaze pada opisane iznad.

## Javni portfolio (PORT-05)

`PublicPortfolioPage` i `test_portfolio.py` provjeravaju javni profil, prethodnu
izložbu, skrivanje privatnog djela, odsutnost praznog odjeljka sudjelovanja,
otvaranje/zatvaranje lightboxa i stranicu za nepostojećeg umjetnika. Fixture se
prijavljuje kroz `POST /api/v1/dev/login-as` samo radi stvaranja izložbe i
javnog/privatnog djela preko API-ja; preglednik ostaje neprijavljen. Nakon testa
brišu se samo zapisi stvoreni za taj test.

```bash
pytest tests/selenium/test_portfolio.py -k 'visitor or unknown_artist' --html=report.html --self-contained-html --junitxml=test-results/pytest.xml
```

Integracijski `PortfolioVisibilityIT` provjerava redoslijed područja i
poveznica javnog profila te nedostupnost profila blokiranog umjetnika. Matrica
privatnih i skrivenih djela/slika te blokiranog umjetnika dodatno je pokrivena
u `ArtworkReadIntegrationTest` i `FileDownloadIntegrationTest`; pohrana slika
u potonjem koristi privremeni `@TempDir`. Ciljane integracijske provjere izvode
se preko Maven Surefire/Failsafe ciljeva, a u CI-ju ih pokreće `mvn verify`.
