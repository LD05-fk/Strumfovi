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
pytest tests/selenium/test_portfolio.py -k 'not past_exhibition and not artwork' --html=report.html --self-contained-html
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
Za svih sedam E2E testova profila, izložbi i djela pokrenite gornju naredbu bez `-k` filtra.

Postojeći backend i E2E workflowi automatski uključuju ove scenarije i spremaju
izvještaje te PNG/HTML dokaze pada opisane iznad.

## Djela u portfoliju (PORT-03-TEST)

`ArtworkLifecycleIT` koristi stvarnu sesiju i CSRF token. Provjerava stvaranje
djela, promjenu u privatno i brisanje, drugu stranicu od 25 javnih djela
te zabrane uređivanja i brisanja tuđeg djela. Organizator i korisnik
`ONBOARDING` odbijaju se za dodavanje, uređivanje i brisanje uz pripadne kodove.
`PortfolioFixtures` priprema djela s različitim datumima i nakon testa briše
njegove podatke i sesije. Korijen pohrane je `@TempDir` za ovu testnu klasu.

Ciljano pokretanje iz `backend/`:

```bash
./mvnw '-Dtest=ArtworkLifecycleIT' test
```

Tri nova E2E testa provjeravaju obavezna polja, promjenu javnog djela u privatno
i brisanje uz potvrdu. Koriste `ArtworkFormPage`, `MyPortfolioPage` i
`PublicPortfolioPage`. Fixture `public_artwork` stvara jedno javno djelo kroz
API te ga nakon testa briše, čak i kod pada. Postojeća djela ostaju netaknuta.
Prije promjene potvrđuje se da posjetitelj vidi djelo; nakon promjene ponovno
se otvara javna stranica bez prijave i čeka učitavanje galerije.
Organizatorov 403 već pokriva `test_auth_01_wrong_role_sees_403` u `test_auth.py`.
Sva četiri scenarija iz issuea pokrenite iz korijena repozitorija:

```bash
pytest tests/selenium/test_portfolio.py tests/selenium/test_auth.py -k 'test_artwork or organizer-forbidden-from-portfolio' --html=report.html --self-contained-html --junitxml=test-results/pytest.xml
```

Postojeći CI izvodi novu `IT` klasu preko Failsafea i Selenium testove kroz
E2E posao. Izvještaji i dokazi pada spremaju se kao artefakti opisani iznad.
