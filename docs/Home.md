# ArtConnect — Projektna dokumentacija

**Kolegij:** [Programsko inženjerstvo](https://www.fer.unizg.hr/predmet/proinz), Fakultet elektrotehnike i računarstva Sveučilišta u Zagrebu\
**Akademska godina:** 2026./2027.\
**Nastavnik:** Vlado Sruk\
**Asistentica:** Eda Jovičić

## Pregled projekta

ArtConnect je web aplikacija u kojoj mladi umjetnici vode portfolio i prijavljuju se na prilike (natječaje, izložbe, radionice) koje objavljuju organizatori.

**API dokumentacija:** [docs/api](api/README.md) (zajednička pravila i rute po domenama).

## Članovi tima i uloge

| Ime i prezime        | GitHub korisničko ime | Glavne uloge      | Sporedne uloge        |
| -------------------- | --------------------- | ----------------- | --------------------- |
| Luka Dakić           | LD05-fk               | Team Lead         | Documentation, DevOps |
| Vedran Novaković     | vnovakovic05          | Backend, Frontend | Documentation, DevOps |
| Nikola Mustač        | nmustac               | Backend, Frontend | -                     |
| Luka Stare           | Luka-Stare            | Frontend, Testing | -                     |
| Bruno Kosmačin       | bkosmacin             | Backend, Testing  | -                     |
| Luka Dimjašević      | lukadimjasevic        | Backend, Database | Frontend              |
| Bepa Tabulov-Štrelov | bstrelov              | Frontend, Testing | -                     |

## Organizacija i proces

Zadaci se prate kao [GitHub Issues](https://github.com/LD05-fk/Projekt-PROGI/issues), faze kao Milestones, a ploča je projekt „ArtConnect - razvoj".

**Pristup repozitoriju.** Nositelj kolegija (`VladoSruk`) i asistentica grupe Eda Jovičić (`edajovicic`) dodani su kao suradnici repozitorija (Settings → Collaborators, uloga Read/Triage) i imaju pristup ploči „ArtConnect - razvoj”. Pristup ostaje omogućen do kraja projekta.

Pravila grananja, commit poruka i pull requestova opisana su u [README-u, odjeljak „Doprinos"](../README.md#doprinos).

**Zadaci i faze.** Svaki zadatak je jedan Issue s oznakama uloge, složenosti i epika, a pripada Milestoneu svoje faze. Na ploči „ArtConnect - razvoj” prolazi stupce Backlog → Ready → In progress → In review → Done. Zadatak sam prelazi u Ready kad su zatvoreni svi zadaci o kojima ovisi (GitHub Action „Osvježi Ready”), a u Done kad se zatvori, odnosno kad se spoji pull request s `Closes #broj`. Novi zadatak otvara se predloškom „Zadatak”.

**Kraj faze.** Nedovršeni zadaci se ne brišu i ne zatvaraju, nego prelaze u sljedeću fazu: na kartici **Issues** filtrirajte `is:open milestone:"<faza koja završava>"`, označite zadatke i u izborniku **Milestone** odaberite sljedeću fazu (ili pojedinačno, u issueu desno pod *Milestone*). Tako se na ploči i u izvještaju vidi što je preneseno.

## Izjava o uporabi umjetne inteligencije

Kratak sažetak: [README – Uporaba umjetne inteligencije](../README.md#ai-usage). Ovaj odjeljak bilježi koji su alati korišteni, za koju dokumentaciju ili kod te kako je tim provjerio rezultat. Slijedi FER-ova [pravila o primjerenoj uporabi umjetne inteligencije](https://www.fer.unizg.hr/_download/repository/Policy%20on%20the%20appropriate%20use%20of%20artificial%20intelligence%20at%20the%20faculty%20of%20electrical%20engineering%20and%20computing%5B1%5D.pdf).

Budući da tim nema iskustva u vođenju projekta ovog opsega, umjetna inteligencija korištena je prije svega za organizaciju rada (razrada zadataka) i infrastrukturu (CI/CD, poslužitelj), a ne za pisanje funkcionalnosti aplikacije.

### Korišteni alati

| Alat | Inačica ili model | Koristio/la | Glavna svrha |
| --- | --- | --- | --- |
| Claude Code (Anthropic) | Claude Opus | Luka Dakić | razrada zadataka iz projektnog zadatka, priprema CI/CD pipelinea i smjernica za tim |

### Opseg doprinosa umjetne inteligencije

| Stranica, datoteka ili artefakt | Vrsta pomoći | Što je alat proizveo | Što je tim zadao i promijenio | Lokacija |
| --- | --- | --- | --- | --- |
| Zadaci projekta (epici, funkcionalnosti, podzadaci) | Izrada nacrta | Podjelu projektnog zadatka na 12 epika, 36 funkcionalnosti i 267 zadataka s opisom, specifikacijom i definicijom gotovosti; redoslijed i prioritete iz ovisnosti | Tim je zadao projektni zadatak, tehnologije i pravila kolegija; odredio opseg, opcionalne funkcionalnosti i odluke (npr. pali-gasi poslužitelj, bez praga pokrivenosti); pregledao i mijenjao zadatke prije uvoza | [Issues](https://github.com/LD05-fk/Projekt-PROGI/issues), ploča „ArtConnect - razvoj” |
| CI/CD i automatizacija | Izrada nacrta konfiguracije | Workflowe za paljenje i gašenje poslužitelja i za isporuku (CD), Dockerfile za web image, izmjene produkcijskog Docker Composea | Tim je odabrao platformu i način rada (AWS EC2, DuckDNS, isporuka preko AWS SSM-a, imagei u GHCR-u); konfiguracija je pokrenuta i provjerena | `.github/workflows/server.yml`, `.github/workflows/cd.yml`, `deploy/web.Dockerfile`, `docker-compose.prod.yml` |
| Postavljanje poslužitelja | Upute korak po korak | Postupak postavljanja EC2 instance, DuckDNS servisa, HTTPS-a, sigurnosnih kopija baze i IAM korisnika | Voditelj tima je postupak proveo na poslužitelju i provjerio svaki korak (health provjera, HTTPS, test gašenja i paljenja, vraćanje sigurnosne kopije) | poslužitelj `artconnect-fer.duckdns.org` |
| 6. Postavljanje, instalacija i konfiguracija (§6.3), README | Izrada nacrta | Tekst o javnom postavljanju, CI/CD-u, sigurnosnim kopijama i paljenju poslužitelja | Tekst uspoređen sa stvarnim stanjem poslužitelja i repozitorija; uklonjeni dijelovi koji opisuju nepostojeće stvari | [6-Postavljanje-instalacija-i-konfiguracija.md](6-Postavljanje-instalacija-i-konfiguracija.md), [README](../README.md) |
| Smjernice za tim, legenda zadataka, API dokumentacija, model podataka | Izrada nacrta | Dokumente za članove tima (pravila rada, smjernice po ulozi, objašnjenje oznaka) | Pregledani i usklađeni s odlukama tima |

Članovi tima koji koriste umjetnu inteligenciju dodaju svoje retke uz zadatak na kojem su je koristili.

### Provjera generiranog sadržaja

| Provjera | Kako je provedena | Dokaz |
| --- | --- | --- |
| Činjenična točnost | Svaki korak postavljanja izveden je i provjeren na stvarnom poslužitelju i u GitHub Actionsu prije nego što je zapisan u dokumentaciju | uspješna pokretanja workflowa CI, CD i Server u [Actions](https://github.com/LD05-fk/Projekt-PROGI/actions); `https://artconnect-fer.duckdns.org/actuator/health` |
| Nema tvrdnji o neostvarenim funkcionalnostima | Dokumentacija opisuje samo postojeće stanje; planirano je označeno zadatkom (npr. „uvodi AUTH-01-BE1”), a dijagram razmještaja izostavljen je dok se ne izradi | §6.1 i §6.3 |
| Dosljednost s drugim stranicama | ID-ovi zadataka, nazivi i poveznice provjereni su prema zadacima, README-u i `HOW_TO_RUN.md`; nakon selidbe repozitorija sve poveznice zamijenjene su novima | [README](../README.md), [HOW_TO_RUN.md](../HOW_TO_RUN.md) |
| Povjerljivi sadržaj | U alat nisu uneseni lozinke, tokeni ni ključevi: tajne je voditelj tima upisivao izravno na poslužitelju i u GitHub Secrets; u repozitoriju je samo `.env.example` bez vrijednosti | GitHub Secrets, `.env.example` |

### Pogreške pronađene u izlazu umjetne inteligencije

| Problem | Kako je pronađen | Rješenje |
| --- | --- | --- |
| U IAM Policy za paljenje poslužitelja bio je pogrešan ID AWS računa (preuzet s popisa uloga umjesto vlastitog), pa je pokretanje poslužitelja odbijeno | Workflow Server javio je `UnauthorizedOperation` | ID ispravljen u Policy-ju; kasnije politika koristi samo ID instance |
| Pomoćni komentar dodan na kraj workflowa zalijepio se na zadnji red skripte (`fi# …`) jer datoteka nije završavala novim redom, pa je PlantUML workflow pao | Pad workflowa Generate PlantUML diagrams (`syntax error: unexpected end of file`) | Komentar uklonjen iz svih workflowa |
| Uputa za provjeru `curl http://localhost/actuator/health` nakon uključenog HTTPS-a ne vraća ništa (Caddy preusmjerava na HTTPS) | Ručna provjera na poslužitelju | Uputa promijenjena u `curl https://artconnect-fer.duckdns.org/actuator/health` |
