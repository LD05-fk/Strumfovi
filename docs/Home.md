# ArtConnect — Projektna dokumentacija

**Kolegij:** [Programsko inženjerstvo](https://www.fer.unizg.hr/predmet/proinz), Fakultet elektrotehnike i računarstva Sveučilišta u Zagrebu\
**Akademska godina:** 2026./2027.\
**Nastavnik:** Vlado Sruk\
**Asistentica:** Eda Jovičić

## Pregled projekta

ArtConnect je web aplikacija u kojoj mladi umjetnici vode portfolio i prijavljuju se na prilike (natječaje, izložbe, radionice) koje objavljuju organizatori.

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

## Kazalo dokumentacije

| Poglavlje | Sadržaj |
| --- | --- |
| [1. Opseg projekta](1-Projekt.md) | Problem, korisnici i granice projekta |
| [2. Analiza zahtjeva](2-Analiza-zahtjeva.md) | Funkcijski i nefunkcijski zahtjevi, ograničenja, sljedivost |
| [3. Obrasci uporabe](3-Obrasci-uporabe.md) | Dijagrami i specifikacije obrazaca uporabe |
| [4. Arhitektura i oblikovanje](4-Arhitektura-i-oblikovanje.md) | Strategija rješenja, model podataka, dijagrami |
| [5. Ispitivanje](5-Ispitivanje.md) | Ispitni slučajevi i rezultati |
| [6. Postavljanje, instalacija i konfiguracija](6-Postavljanje-instalacija-i-konfiguracija.md) | Lokalno pokretanje i javni poslužitelj |
| [A. Pregled aktivnosti grupe](A-Pregled-aktivnosti-grupe.md) | Sastanci, plan rada i doprinosi članova |
| [API dokumentacija](api/) | Rute po domenama |
| [Zapisnici sastanaka](meeting-notes/) | Detaljne bilješke sastanaka |
| [Predaje](deliverables/) | PDF dokumentacije i prezentacije |

## Ključne točke projekta

| Datum | Događaj |
| --- | --- |
| 20. 11. 2026. u 23:59 | Predaja 1. ciklusa |
| 22. 1. 2027. u 23:59 | Završna predaja |

**Javna adresa aplikacije:** <https://artconnect-fer.duckdns.org> (poslužitelj se pali na zahtjev, vidi [README](../README.md)).

## Organizacija i proces

Zadaci se prate kao [GitHub Issues](https://github.com/LD05-fk/Projekt-PROGI/issues), faze kao Milestones, a ploča je projekt „ArtConnect - razvoj".

**Pristup repozitoriju.** Nositelj kolegija (`VladoSruk`) i asistentica grupe Eda Jovičić (`edajovicic`) dodani su kao suradnici repozitorija (Settings → Collaborators, uloga Read/Triage) i imaju pristup ploči „ArtConnect - razvoj”. Pristup ostaje omogućen do kraja projekta.

Pravila grananja, commit poruka i pull requestova opisana su u [README-u, odjeljak „Doprinos"](../README.md#doprinos).

**Zadaci i faze.** Svaki zadatak je jedan Issue s oznakama uloge, složenosti i epika, a pripada Milestoneu svoje faze. Na ploči „ArtConnect - razvoj” prolazi stupce Backlog → Ready → In progress → In review → Done. Zadatak sam prelazi u Ready kad su zatvoreni svi zadaci o kojima ovisi (GitHub Action „Osvježi Ready”), a u Done kad se zatvori, odnosno kad se spoji pull request s `Closes #broj`. Novi zadatak otvara se predloškom „Zadatak”.

**Kraj faze.** Nedovršeni zadaci se ne brišu i ne zatvaraju, nego prelaze u sljedeću fazu: na kartici **Issues** filtrirajte `is:open milestone:"<faza koja završava>"`, označite zadatke i u izborniku **Milestone** odaberite sljedeću fazu (ili pojedinačno, u issueu desno pod *Milestone*). Tako se na ploči i u izvještaju vidi što je preneseno.

## Izjava o uporabi umjetne inteligencije

Kratak sažetak: [README – Uporaba umjetne inteligencije](../README.md#ai-usage). Ovaj odjeljak bilježi koji su alati korišteni, za koju dokumentaciju ili kod te kako je tim provjerio rezultat. Slijedi FER-ova [pravila o primjerenoj uporabi umjetne inteligencije](https://www.fer.unizg.hr/_download/repository/Policy%20on%20the%20appropriate%20use%20of%20artificial%20intelligence%20at%20the%20faculty%20of%20electrical%20engineering%20and%20computing%5B1%5D.pdf).

AI su koristili svi članovi tima, na različite načine: voditelj tima za razradu zadataka, infrastrukturu i pregled pull requestova, a ostali članovi kao pomoć pri pisanju koda i testova, za objašnjavanje koda i novih tehnologija te za dokumentaciju. Svaka promjena u `master` prolazi pregled drugog člana tima i zeleni CI, bez obzira na to je li u njoj korišten AI.

### Korišteni alati

| Alat | Inačica ili model | Koristio/la | Glavna svrha |
| --- | --- | --- | --- |
| Claude Code (Anthropic) | Claude Opus | Luka Dakić | Razrada zadataka iz projektnog zadatka, priprema CI/CD-a i poslužitelja te nezavisna provjera pull requestova uz pregled člana tima |
| GitHub Copilot (u IDE-u) | GPT-5.6 Luna, kasnije GPT-6 Luna | Vedran Novaković | Planiranje manjih koraka implementacije i strukture datoteka, predlošci testova i testni podaci, oblikovanje dokumentacije i README uputa |
| GitHub Copilot, Claude | nije navedeno | Bepa Tabulov-Štrelov | Pomoć pri implementaciji koda i pronalaženju grešaka |
| ChatGPT, GitHub Copilot | nije navedeno | Luka Stare | Učenje novih tehnologija i jezika, objašnjavanje tuđeg koda, planiranje rješenja, debugiranje |
| GPT-6 Luna | GPT-6 Luna | Bruno Kosmačin | Objašnjavanje dijelova koda i zadataka, ideje za sljedeći korak implementacije; kod nije generirao |
| GitHub Copilot (u IDE-u) | GPT-6 Luna | Nikola Mustač | Pomoć pri implementaciji funkcionalnosti, analiza grešaka, pisanje i prilagodba testova, tehnička dokumentacija |
| Claude Code (Anthropic) | Claude Opus 5 | Luka Dimjašević | Analiza i sažimanje postojećeg koda, objašnjavanje složenijih issueova, testovi prema zadanim uputama, početna struktura paketa i Liquibase changelogovi, provjera pokriva li kod zahtjeve issuea |

### Opseg doprinosa umjetne inteligencije

| Stranica, datoteka ili artefakt | Vrsta pomoći | Što je alat proizveo | Što je tim zadao i promijenio | Lokacija |
| --- | --- | --- | --- | --- |
| Zadaci projekta (epici, funkcionalnosti, podzadaci) | Izrada nacrta | Podjelu projektnog zadatka na 12 epika, 36 funkcionalnosti i 267 zadataka s opisom, specifikacijom, uputama za izradu i definicijom gotovosti; redoslijed i prioritete iz ovisnosti | Tim je zadao projektni zadatak, tehnologije i pravila kolegija; odredio opseg, opcionalne funkcionalnosti i odluke (npr. pali-gasi poslužitelj, bez praga pokrivenosti); pregledao i mijenjao zadatke prije uvoza | [Issues](https://github.com/LD05-fk/Projekt-PROGI/issues), ploča „ArtConnect - razvoj” |
| CI/CD i automatizacija | Izrada nacrta konfiguracije | Workflowe za CI, CD, E2E, SonarCloud, paljenje i gašenje poslužitelja i prebacivanje zadataka u Ready; Dockerfile za web image, izmjene produkcijskog Docker Composea | Tim je odabrao platformu i način rada (AWS EC2, DuckDNS, isporuka preko AWS SSM-a, imagei u GHCR-u); konfiguracija je pokrenuta i provjerena | `.github/workflows/`, `deploy/web.Dockerfile`, `docker-compose.prod.yml` |
| Postavljanje poslužitelja | Upute korak po korak | Postupak postavljanja EC2 instance, DuckDNS servisa, HTTPS-a, sigurnosnih kopija baze i IAM korisnika | Voditelj tima je postupak proveo na poslužitelju i provjerio svaki korak (health provjera, HTTPS, test gašenja i paljenja, vraćanje sigurnosne kopije) | poslužitelj `artconnect-fer.duckdns.org` |
| 6. Postavljanje, instalacija i konfiguracija (§6.3), README | Izrada nacrta | Tekst o javnom postavljanju, CI/CD-u, sigurnosnim kopijama i paljenju poslužitelja | Tekst uspoređen sa stvarnim stanjem poslužitelja i repozitorija; uklonjeni dijelovi koji opisuju nepostojeće stvari | [6-Postavljanje-instalacija-i-konfiguracija.md](6-Postavljanje-instalacija-i-konfiguracija.md), [README](../README.md) |
| Smjernice za tim, legenda zadataka, API dokumentacija, model podataka | Izrada nacrta | Dokumente za članove tima (pravila rada, smjernice po ulozi, objašnjenje oznaka) | Pregledani i usklađeni s odlukama tima | dokumenti razrade projekta, `docs/api/` |
| Pregled pull requestova | Pregled koda | Za svaki PR provjeru prema zahtjevima i definiciji gotovosti issuea, moguće greške u logici i sigurnosti, provjeru postoje li testovi | Voditelj tima ručno je provjerio svaki nalaz prije nego što ga je proslijedio autoru; pregled je dodatak odobrenju člana tima, ne zamjena | komentari na prvim [pull requestovima](https://github.com/LD05-fk/Projekt-PROGI/pulls); kasnije su nalazi slani autoru izravno na Discordu |
| Backend i frontend kod | Pomoć pri implementaciji | Prijedloge dijelova koda, početnu strukturu novih paketa i Liquibase changelogova, pomoć pri traženju grešaka | Član koji radi zadatak prilagodio je prijedloge strukturi projekta i zahtjevima issuea; kod je prošao code review drugog člana i CI (V. Novaković, B. Tabulov-Štrelov, L. Stare, N. Mustač, L. Dimjašević) | `backend/src/main/`, `frontend/src/`, `backend/src/main/resources/db/changelog/` |
| Automatski testovi | Izrada nacrta | Predloške jediničnih i integracijskih testova, testne podatke i prijedloge rubnih slučajeva | Član je zadao što treba ispitati, ručno dopunio rubne slučajeve, ispravio netočna očekivanja i uklonio suvišne testove; testovi su pokrenuti lokalno i u CI-ju (V. Novaković, N. Mustač, L. Dimjašević) | `backend/src/test/`, `frontend/src/**/*.test.tsx`, `tests/selenium/` |
| Tehnička dokumentacija uz zadatke | Oblikovanje i izrada nacrta | Formatiranje dokumentacije i README uputa | Nazivlje i sadržaj usklađeni su sa stvarnim kodom (V. Novaković, N. Mustač) | `docs/`, `docs/api/` |
| Razumijevanje koda i zadataka | Objašnjenje | Objašnjenja postojećeg koda, novih tehnologija i zahtjeva issueova; ideje za pristup implementaciji | Nema izravnog artefakta; implementaciju je napisao član (L. Stare, B. Kosmačin, L. Dimjašević) | — |

Članovi tima koji koriste umjetnu inteligenciju dodaju svoje retke uz zadatak na kojem su je koristili.

### Provjera generiranog sadržaja

| Provjera | Kako je provedena | Dokaz |
| --- | --- | --- |
| Činjenična točnost | Svaki korak postavljanja izveden je i provjeren na stvarnom poslužitelju i u GitHub Actionsu prije nego što je zapisan u dokumentaciju | uspješna pokretanja workflowa CI, CD i Server u [Actions](https://github.com/LD05-fk/Projekt-PROGI/actions); `https://artconnect-fer.duckdns.org/actuator/health` |
| Generirani kod | Svaki PR pregledava i odobrava član tima koji nije autor, a spaja se samo uz zelen CI (provjere `Backend` i `Frontend`) | zaštita grane `master`, odobrenja na [pull requestovima](https://github.com/LD05-fk/Projekt-PROGI/pulls?q=is%3Apr+is%3Amerged) |
| Generirani testovi | Testovi su pokrenuti lokalno i u CI-ju; autori su ručno provjerili očekivane vrijednosti i dodali rubne slučajeve | rezultati izvođenja u [poglavlju 5](5-Ispitivanje.md) i u Actionsu |
| Nalazi AI pregleda PR-ova | Voditelj tima ručno je provjerio svaki nalaz; prosljeđuju se samo greške koje stvaran korisnik može izazvati i stvarni sigurnosni problemi | komentari na prvim pull requestovima, kasnije poruke autoru na Discordu |
| Nema tvrdnji o neostvarenim funkcionalnostima | Dokumentacija opisuje samo postojeće stanje; planirano je označeno ciklusom ili zadatkom | poglavlja 2 i 3 (stupac „Ciklus”), README („Trenutno dostupno” i „Planirano”) |
| Dosljednost s drugim stranicama | ID-ovi zadataka, nazivi i poveznice provjereni su prema zadacima, README-u i `HOW_TO_RUN.md` | [README](../README.md), [HOW_TO_RUN.md](../HOW_TO_RUN.md) |
| Povjerljivi sadržaj | U alate nisu uneseni lozinke, tokeni ni ključevi: tajne su upisane izravno na poslužitelju i u GitHub Secrets; u repozitoriju je samo `.env.example` bez vrijednosti | GitHub Secrets, `.env.example` |

### Pogreške pronađene u izlazu umjetne inteligencije

| Problem | Kako je pronađen | Rješenje |
| --- | --- | --- |
| Prvi nacrt zadataka imao je preopširne zadatke i pretpostavke suprotne pravilima kolegija i odlukama tima (npr. prag pokrivenosti testovima) | Pregled zadataka prije uvoza u GitHub | Tim je sam odredio opseg, opcionalne funkcionalnosti i ključne odluke te uredio zadatke |
| U IAM Policy za paljenje poslužitelja bio je pogrešan ID AWS računa (preuzet s popisa uloga umjesto vlastitog), pa je pokretanje poslužitelja odbijeno | Workflow Server javio je `UnauthorizedOperation` | ID ispravljen u Policy-ju; kasnije politika koristi samo ID instance |
| Pomoćni komentar dodan na kraj workflowa zalijepio se na zadnji red skripte (`fi# …`) jer datoteka nije završavala novim redom, pa je PlantUML workflow pao | Pad workflowa Generate PlantUML diagrams (`syntax error: unexpected end of file`) | Komentar uklonjen iz svih workflowa |
| Uputa za provjeru `curl http://localhost/actuator/health` nakon uključenog HTTPS-a ne vraća ništa (Caddy preusmjerava na HTTPS) | Ručna provjera na poslužitelju | Uputa promijenjena u `curl https://artconnect-fer.duckdns.org/actuator/health` |
| AI pregled PR-ova prijavljivao je nerealne rubne slučajeve kao greške (npr. NUL znak u parametru ili datum +999999999-12-31 koji vraća 500 umjesto 400) | Ručna provjera nalaza | Nalazi su odbačeni, a pravila pregleda promijenjena: prijavljuje se samo ono što stvaran korisnik može izazvati običnim korištenjem ili stvarni sigurnosni problem |
| Generirani testovi pokrivali su uglavnom osnovne scenarije, a kod složenije poslovne logike imali su netočna očekivanja (V. Novaković, N. Mustač) | Pokretanje testova i pregled pokrivenosti koda | Testovi su ručno dopunjeni stvarnim rubnim slučajevima i ispravljeni prema očekivanom ponašanju |
| Generirani testovi ponekad su sadržavali nepotrebne slučajeve (L. Dimjašević) | Pregled generiranog koda | Suvišni testovi su uklonjeni |
| Prijedlozi koda nisu uvijek odgovarali postojećoj strukturi projekta ili zahtjevima (N. Mustač) | Provjera u kontekstu aplikacije | Prijedlozi su prilagođeni postojećem kodu i ispitani prije prihvaćanja |
| Nazivlje u generiranoj dokumentaciji bilo je ponekad pogrešno ili nelogično (V. Novaković) | Pregled teksta | Nazivlje je ispravljeno |
| Copilot je predlagao stvari koje su već napravljene i suvišan kod, a objašnjenja su mu bila slaba (L. Stare) | Code review generiranog koda | Detaljniji pregled svega što je generirao; za objašnjenja je korišten ChatGPT |
| ChatGPT je zaboravljao strukturu projekta, sadržaj datoteka i ono što je već napravljeno (L. Stare) | Odgovori koji se ne slažu s projektom | ChatGPT se koristi samo za pitanja manjeg opsega (objašnjenja, sintaksa) |
