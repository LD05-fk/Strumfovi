# ArtConnect (Projekt PROGI)

[![CI](https://github.com/LD05-fk/Projekt-PROGI/actions/workflows/ci.yml/badge.svg)](https://github.com/LD05-fk/Projekt-PROGI/actions/workflows/ci.yml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=LD05-fk_Projekt-PROGI&metric=coverage)](https://sonarcloud.io/summary/new_code?id=LD05-fk_Projekt-PROGI)

Projekt iz kolegija Programsko inženjerstvo (FER, 2026./2027.): web aplikacija u kojoj mladi umjetnici vode portfolio i prijavljuju se na prilike (natječaje, izložbe, radionice) koje objavljuju organizatori.

- **Zadaci:** [Issues](https://github.com/LD05-fk/Projekt-PROGI/issues), faze kao [Milestones](https://github.com/LD05-fk/Projekt-PROGI/milestones)
- **Ploča:** [ArtConnect - razvoj](https://github.com/users/LD05-fk/projects/1); pogled „Za preuzimanje” prikazuje slobodne zadatke redom rada
- **Kvaliteta koda:** [SonarCloud](https://sonarcloud.io/project/overview?id=LD05-fk_Projekt-PROGI), pregled u pozadini (jednom tjedno), ne utječe na pull requestove

## Opis aplikacije

ArtConnect povezuje mlade umjetnike i organizatore: umjetnici vode javni portfolio i prijavljuju se na prilike, a organizatori objavljuju natječaje, izložbe i radionice te biraju prijave. Aplikacija je u razvoju u dva ciklusa (predaje 20. 11. 2026. i 22. 1. 2027.).

**Planirane ključne funkcionalnosti**

- Prijava Googleom i odabir uloge (umjetnik, organizator; administrator je unaprijed određen).
- Portfolio umjetnika: profil, djela sa slikama i javna stranica koju umjetnik dijeli poveznicom.
- Prilike: objava s lokacijom na karti (OpenStreetMap), javno pretraživanje i filtri.
- Prijave na prilike, pregled prijava, status i objava rezultata (2. ciklus).

Za potpuni opseg, zahtjeve, oblikovanje i ispitivanje pogledajte [docs](docs/Home.md).

## Server

**<https://artconnect-fer.duckdns.org>**

Poslužitelj (AWS EC2) nije stalno upaljen, da se štede AWS krediti. Pali ga i gasi bilo koji član tima, bez AWS računa:

1. [Actions → Server](https://github.com/LD05-fk/Projekt-PROGI/actions/workflows/server.yml) → **Run workflow**
2. odaberite `start`, `stop` ili `status` → **Run workflow**
3. nakon `start` stranica radi za 1–2 minute; stanje i adresa su u sažetku pokretanja

Kad vam više ne treba, ugasite ga (`stop`). Svaku noć u 1:00 UTC poslužitelj se ionako sam ugasi; u razdobljima ocjenjivanja to je isključeno (varijabla repozitorija `ALWAYS_ON=true`). Detalji postavljanja: [poglavlje 6](docs/6-Postavljanje-instalacija-i-konfiguracija.md#63-javno-postavljanje-i-administracija).

**Isprobajte:** nakon paljenja otvorite adresu iznad; stanje poslužitelja pokazuje `/actuator/health`.

**Demonstracija:** za pregled javnih stranica korisnički račun nije potreban. Prijava Googleom još nije dostupna u javnoj inačici.

**Ograničenja:** poslužitelj nije stalno upaljen (štednja AWS kredita), a javno su dostupne samo već dovršene funkcionalnosti. Poznata ograničenja vode se u [poglavlju 6](docs/6-Postavljanje-instalacija-i-konfiguracija.md).

## Brzi početak

**Preduvjeti:** Docker s Docker Composeom, JDK 25, Node.js (LTS) i npm.

```bash
git clone https://github.com/LD05-fk/Projekt-PROGI.git
cd Projekt-PROGI
docker compose up -d                       # baza (PostgreSQL)
cd backend && ./mvnw spring-boot:run       # backend na :8080 (drugi terminal)
cd frontend && npm install && npm run dev  # frontend na :5173 (treći terminal)
```

**Adresa:** `http://localhost:5173`. **Konfiguracija:** zadane vrijednosti rade bez `.env`; ako ih želite promijeniti, kopirajte `.env.example` u `.env` (ne stavljajte stvarne tajne u repozitorij). Detalji: [HOW_TO_RUN.md](HOW_TO_RUN.md).

## Kako pokrenuti

Upute za lokalno pokretanje i deploy na EC2: [HOW_TO_RUN.md](HOW_TO_RUN.md). Rad s bazom i Liquibaseom: [db/README.md](db/README.md).

## Tehnologije

| Tehnologija | Što radi u aplikaciji | Razlog ili ograničenje |
| --- | --- | --- |
| React 19, Vite, TypeScript | Korisničko sučelje u pregledniku | Brz razvoj i provjera tipova |
| MUI | Komponente i tema sučelja | Zajednički dizajnski sustav tima |
| TanStack Query, React Hook Form, Zod | Dohvat podataka, obrasci i validacija | Manje ručnog koda u obrascima |
| Leaflet, OpenStreetMap, Nominatim | Prikaz karte i pretvorba adrese u položaj | Besplatno; backend zove Nominatim najviše 1 put u sekundi |
| Java 25, Spring Boot 4.1 | Backend i REST API (`/api/v1`) | Java 25 je trenutni LTS |
| Spring Security (Google OAuth 2.0 / OIDC) | Prijava i sesije | Bez lozinki; sesija na poslužitelju |
| PostgreSQL 17, Spring Data JPA, Liquibase | Podaci i migracije sheme | Shema se mijenja samo migracijama |
| JUnit, Testcontainers, Vitest, Selenium | Backend, integracijski, frontend i E2E testovi | Osnovni testovi, bez praga pokrivenosti |
| Docker, Caddy | Pokretanje na poslužitelju, HTTPS i proxy | Caddy sam uzima HTTPS certifikat |
| GitHub Actions, GHCR | CI i automatska isporuka | Zeleni CI uvjet je za spajanje |
| AWS EC2, DuckDNS | Javni poslužitelj i domena | Bez stalne IP adrese; poslužitelj se pali na zahtjev |
| SonarCloud | Tjedni pregled kvalitete koda | Ne utječe na pull requestove |

## Kako čitati ID zadatka

`EPIC-01` je epik (područje sustava). `AUTH-02` je funkcionalnost, a `AUTH-02-BE`, `AUTH-02-DES`, `AUTH-02-FE`, `AUTH-02-TEST` su njezini podzadaci po ulogama. Zadatak samo jedne uloge nema kod na kraju (npr. `FND-01`). Kodovi uloga: `DB` Database, `BE` Backend, `DES` UI Designer, `FE` Frontend, `TEST` Testing, `OPS` DevOps, `DOC` Documentation.

## Automatizacija

Workflow [Osvježi Ready](.github/workflows/refresh-ready.yml) nakon zatvaranja ili ponovnog otvaranja issuea prebacuje zadatke iz Backlog u Ready kad su zatvoreni svi zadaci iz njihovog retka „Ovisi o” (i obrnuto). Treba tajnu repozitorija `PROJECT_TOKEN`: classic token s pravom `project`.

Workflow [Server](.github/workflows/server.yml) pali i gasi produkcijski poslužitelj (vidi [Server](#server)). Treba tajne `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (IAM korisnik koji smije samo paliti i gasiti ovu instancu) i `EC2_INSTANCE_ID`.

## Članovi tima

| Ime i prezime | Uloga |
| --- | --- |
| Luka Dakić | Voditelj tima, dokumentacija, DevOps |
| Vedran Novaković | Backend, Frontend |
| Nikola Mustač | Backend, Frontend |
| Luka Stare | Frontend, Testing |
| Bruno Kosmačin | Backend, Testing |
| Luka Dimjašević | Backend, Database |
| Bepa Tabulov-Štrelov | Frontend, Testing |

## Doprinos

`master` je stabilna grana i ocjenjuje se njezino stanje. Promjene ulaze isključivo kroz pull requestove.

- **Grane:** `prefiks/KOD-ZADATKA`, uz broj podzadatka ako ga ima, npr. `feature/AUTH-01-BE1`. Prefiks je obično `feature`, a prema vrsti promjene može biti i `fix`, `docs`, `chore`…
- **Commit poruke:** `tip: poruka` na hrvatskom, npr. `fix: ispravi preusmjeravanje nakon prijave`. Tipovi: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`.
- **Pull request:** jedan zadatak po PR-u; u opisu `Closes #broj` (broj issuea).
- **Uvjeti za spajanje:** zelen CI (provjere `Backend` i `Frontend`) i 1 odobrenje od osobe koja nije autor. Recenzent stvarno pročita i isproba promjenu; autor odredi konkretnog recenzenta.
- **Testovi:** osnovni testovi za poslovnu logiku i glavne tokove; ne lovi se postotak pokrivenosti. [SonarCloud](https://sonarcloud.io/project/overview?id=LD05-fk_Projekt-PROGI) analizira `master` u pozadini (jednom tjedno ili ručno: Actions → SonarCloud → Run workflow) i ne pojavljuje se na pull requestovima.
- **Tajne** (`.env`, ključevi, tokeni) nikad ne idu u repozitorij.

<a id="ai-usage"></a>
## Uporaba umjetne inteligencije

Projekt slijedi FER-ova [pravila o primjerenoj uporabi umjetne inteligencije](https://www.fer.unizg.hr/_download/repository/Policy%20on%20the%20appropriate%20use%20of%20artificial%20intelligence%20at%20the%20faculty%20of%20electrical%20engineering%20and%20computing%5B1%5D.pdf).

**Izjava:** Tim nema prethodnog iskustva u vođenju projekta ovog opsega, pa je voditelj tima koristio Claude Code (Anthropic) kao pomoć u organizaciji i infrastrukturi: za razradu zahtjeva iz projektnog zadatka u epike, funkcionalnosti i zadatke (GitHub Issues) te za pripremu CI/CD pipelinea (GitHub Actions, Docker, isporuka na AWS EC2) i pripadne dokumentacije. Opseg, prioritete i odluke donio je tim; generirani zadaci, konfiguracija i tekst pregledani su i ispravljeni prije objave, a infrastruktura je provjerena u radu (CI, isporuka, javna adresa). U alate nisu uneseni pristupni podaci ni osobni podaci. Detaljan zapis: [Home – Izjava o uporabi umjetne inteligencije](docs/Home.md#izjava-o-uporabi-umjetne-inteligencije).

## Licenca i predložak

Za ovu aplikaciju nije dodijeljena licenca koja dopušta javnu ponovnu uporabu; sva prava zadržavaju autori. Kod i sadržaj trećih strana zadržavaju vlastite licence.

Struktura dokumentacije i workflow za PlantUML temelje se na [predlošku kolegija Programsko inženjerstvo](https://github.com/VladoSruk/Programsko-inzenjerstvo) (autor Vlado Sruk, CC BY-NC-SA 4.0).
