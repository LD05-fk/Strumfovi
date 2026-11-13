# ArtConnect (Projekt PROGI)

[![CI](https://github.com/LD05-fk/Projekt-PROGI/actions/workflows/ci.yml/badge.svg)](https://github.com/LD05-fk/Projekt-PROGI/actions/workflows/ci.yml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=LD05-fk_Projekt-PROGI&metric=coverage)](https://sonarcloud.io/summary/new_code?id=LD05-fk_Projekt-PROGI)

Projekt iz kolegija [Programsko inženjerstvo](https://www.fer.unizg.hr/predmet/proinz) (FER, 2026./2027.): web aplikacija u kojoj mladi umjetnici vode portfolio i prijavljuju se na prilike (natječaje, izložbe, radionice) koje objavljuju organizatori.

- **Zadaci:** [Issues](https://github.com/LD05-fk/Projekt-PROGI/issues), faze kao [Milestones](https://github.com/LD05-fk/Projekt-PROGI/milestones)
- **Ploča:** [ArtConnect - razvoj](https://github.com/users/LD05-fk/projects/1); pogled „Za preuzimanje” prikazuje slobodne zadatke redom rada
- **Kvaliteta koda:** [SonarCloud](https://sonarcloud.io/project/overview?id=LD05-fk_Projekt-PROGI), pregled u pozadini (jednom tjedno), ne utječe na pull requestove

## Opis aplikacije

ArtConnect povezuje mlade umjetnike i organizatore: umjetnici vode javni portfolio i prijavljuju se na prilike, a organizatori objavljuju natječaje, izložbe i radionice te biraju prijave. Aplikacija je u razvoju u dva ciklusa (predaje 20. 11. 2026. i 22. 1. 2027.).

**Trenutno dostupno (1. ciklus)**

- Prijava Googleom i odabir uloge (umjetnik ili organizator; administrator je unaprijed određen).
- Portfolio umjetnika: profil, prethodne izložbe, djela sa slikama i vidljivošću te javna stranica koju umjetnik dijeli poveznicom.
- Profil organizatora s nazivom organizacije.
- Prilike: objava i uređivanje s lokacijom na karti (OpenStreetMap), promjena statusa, arhiviranje i brisanje.
- Javni popis prilika s pretragom, filtrima i prikazom na karti.

**Planirano (2. ciklus)**

- Prijave na prilike prema uvjetima organizatora, pregled prijava i odluke.
- Objava rezultata i dodavanje događanja u Google kalendar.
- Statistika za organizatora, mjesečni izvještaj i administracija platforme.
- Brisanje vlastitog računa.

Za potpuni opseg, zahtjeve, oblikovanje i ispitivanje pogledajte [docs](docs/Home.md).

<a id="deployment"></a>
## Postavljanje

**<https://artconnect-fer.duckdns.org>**

Poslužitelj (AWS EC2) nije stalno upaljen, da se štede AWS krediti. Pali ga i gasi bilo koji član tima, bez AWS računa:

1. [Actions → Server](https://github.com/LD05-fk/Projekt-PROGI/actions/workflows/server.yml) → **Run workflow**
2. odaberite `start`, `stop` ili `status` → **Run workflow**
3. nakon `start` stranica radi za 1–2 minute; stanje i adresa su u sažetku pokretanja

Kad vam više ne treba, ugasite ga (`stop`). Svaku noć u 1:00 UTC poslužitelj se ionako sam ugasi; u razdobljima ocjenjivanja to je isključeno (varijabla repozitorija `ALWAYS_ON=true`). Detalji postavljanja: [poglavlje 6](docs/6-Postavljanje-instalacija-i-konfiguracija.md#63-javno-postavljanje-i-administracija).

**Isprobajte:** otvorite „Prilike”, suzite popis filtrima i prebacite prikaz na kartu. Zatim se prijavite Googleom kao umjetnik i dodajte djelo u portfolio. Stanje poslužitelja pokazuje `/actuator/health`.

**Demonstracija:** za pregled javnih stranica korisnički račun nije potreban. Prijava radi s bilo kojim Google računom.

**Ograničenja:** poslužitelj nije stalno upaljen (štednja AWS kredita), a javno su dostupne samo već dovršene funkcionalnosti.

<a id="quick-start"></a>
## Brzi početak

**Preduvjeti:** Docker s Docker Composeom, JDK 25, Node.js (LTS) i npm.

```bash
git clone https://github.com/LD05-fk/Projekt-PROGI.git
cd Projekt-PROGI
docker compose up -d                       # baza (PostgreSQL)
cd backend && ./mvnw spring-boot:run       # backend na :8080 (drugi terminal)
cd frontend && npm install && npm run dev  # frontend na :5173 (treći terminal)
```

**Adresa:** `http://localhost:5173`; backend radi ako `http://localhost:8080/actuator/health` vraća `UP`. **Konfiguracija:** zadane vrijednosti rade bez `.env`; ako ih želite promijeniti, kopirajte `.env.example` u `.env` (ne stavljajte stvarne tajne u repozitorij). Detaljne upute (i Google prijava lokalno): [HOW_TO_RUN.md](HOW_TO_RUN.md). Rad s bazom i Liquibaseom: [db/README.md](db/README.md).

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

Workflow [Server](.github/workflows/server.yml) pali i gasi produkcijski poslužitelj (vidi [Postavljanje](#deployment)). Treba tajne `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (IAM korisnik koji smije samo paliti i gasiti ovu instancu) i `EC2_INSTANCE_ID`.

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
## Uporaba umjetne inteligencije [![AI Usage: Disclosed](https://img.shields.io/badge/AI%20Usage-Disclosed-blue.svg)](https://www.fer.unizg.hr/_download/repository/Policy%20on%20the%20appropriate%20use%20of%20artificial%20intelligence%20at%20the%20faculty%20of%20electrical%20engineering%20and%20computing%5B1%5D.pdf)

Projekt slijedi FER-ova [pravila o primjerenoj uporabi umjetne inteligencije](https://www.fer.unizg.hr/_download/repository/Policy%20on%20the%20appropriate%20use%20of%20artificial%20intelligence%20at%20the%20faculty%20of%20electrical%20engineering%20and%20computing%5B1%5D.pdf).

**Izjava:** Tim nema prethodnog iskustva u vođenju projekta ovog opsega, pa je voditelj tima koristio Claude Code (Anthropic) kao pomoć u organizaciji i infrastrukturi: za razradu zahtjeva iz projektnog zadatka u epike, funkcionalnosti i zadatke (GitHub Issues) te za pripremu CI/CD pipelinea (GitHub Actions, Docker, isporuka na AWS EC2) i pripadne dokumentacije. Opseg, prioritete i odluke donio je tim; generirani zadaci, konfiguracija i tekst pregledani su i ispravljeni prije objave, a infrastruktura je provjerena u radu (CI, isporuka, javna adresa). U alate nisu uneseni pristupni podaci ni osobni podaci. Detaljan zapis: [Home – Izjava o uporabi umjetne inteligencije](docs/Home.md#izjava-o-uporabi-umjetne-inteligencije).

<a id="code-of-conduct-and-support"></a>
## Kodeks ponašanja i podrška [![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

Od studenata se očekuje pridržavanje minimalnih standarda prihvatljivog ponašanja definiranih [Kodeksom ponašanja studenata Fakulteta elektrotehnike i računarstva Sveučilišta u Zagrebu](https://www.fer.hr/_download/repository/Kodeks_ponasanja_studenata_FER-a_procisceni_tekst_2016%5B1%5D.pdf). Također se očekuje pridržavanje [Etičkog kodeksa IEEE-a](https://www.ieee.org/about/corporate/governance/p7-8.html), koji postavlja visoke standarde integriteta, odgovornog i etičnog profesionalnog ponašanja. Repozitorij koristi i [Kodeks ponašanja](CODE_OF_CONDUCT.md) (Contributor Covenant 2.1).

**Očekivanja od tima:**

- jasno definirati kako će se rad raspodijeliti među članovima tima i kako će tim komunicirati;
- primjenjivati dogovorena pravila i standarde pri rješavanju nesuglasica;
- svi članovi tima dužni su pridržavati se kodeksa ponašanja.

Probleme u timu najbolje je prvo riješiti izravnim razgovorom; ako to ne uspije, javite se voditelju tima ili asistentici.

## Licenca i predložak

Za ovu aplikaciju nije dodijeljena licenca koja dopušta javnu ponovnu uporabu; sva prava zadržavaju autori. Kod i sadržaj trećih strana zadržavaju vlastite licence.

Struktura dokumentacije i workflow za PlantUML temelje se na [predlošku kolegija Programsko inženjerstvo](https://github.com/VladoSruk/Programsko-inzenjerstvo) (autor Vlado Sruk, CC BY-NC-SA 4.0).
