# Testovi (E2E)

E2E testovi koriste Selenium (Python, pytest).
U CI-ju E2E pokreće posao e2e (ručno i noćno), HTML izvještaj je artefakt workflowa u kartici Actions.

## Preduvjeti

Python 3.12+, Google Chrome i Docker s Docker Compose.
pip install -r tests/requirements.txt
Sustav mora raditi prema `HOW_TO_RUN.md`
(baza, backend s profilom `dev`, frontend na http://localhost:5173).

## Pokretanje

pytest tests/selenium --html=report.html
Iz korijena repozitorija.
Izvještaj u report.html.

## Struktura

- `tests/selenium/conftest.py` – preglednik, `login_as(email)`, `cleanup`
- `tests/selenium/page_objects/base_page.py` - temeljna klasa za sve page objekte u Selenium E2E testovima
- `tests/selenium/page_objects/` – jedan page objekt po stranici
- `tests/selenium` – testovi

## Prijava

Google prijava se ne automatizira.
`login_as(email)` poziva `POST /api/v1/dev/login-as`.
Korisnici: organizator@artconnect.test, umjetnik@artconnect.test, admin@artconnect.test, novi@artconnect.test(bez uloge)
