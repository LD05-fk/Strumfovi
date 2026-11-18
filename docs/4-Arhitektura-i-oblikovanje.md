# 4. Arhitektura i oblikovanje

**Cilj:** Definirati arhitekturni stil, dekompoziciju programa na podsustave i module, organizaciju podataka te dinamičko ponašanje tijekom izvođenja. Arhitektura predstavlja temelj za implementaciju i ažurira se pri svakoj važnijoj promjeni projektnih odluka.

| Arhitekturni pogled      | Ključno inženjersko pitanje                           | Primijenjeni model ili dijagram    |
| :----------------------- | :---------------------------------------------------- | :--------------------------------- |
| **Strategija rješenja**  | Koji stil i temeljne odluke određuju program?         | Tablica odluka (odjeljak 4.1)      |
| **Struktura podsustava** | Koji su glavni dijelovi i kako komuniciraju?          | Dijagram komponenata               |
| **Model podataka**       | Kako su strukturirani trajni podaci programa?         | Relacijski ER dijagram             |
| **Struktura koda**       | Kako su dodijeljene odgovornosti i ključne klase?     | Reprezentativni dijagram razreda   |
| **Dinamičko ponašanje**  | Kako moduli surađuju pri obradi ključnih transakcija? | sekvencijski dijagram              |
| **Stanja sustava**       | Kako ključni domenski objekt mijenja svoja stanja?    | Dijagram stanja                    |
| **Fizički razmještaj**   | Na kojim se izvršnim čvorovima program izvodi?        | Dijagram razmještaja (poglavlje 6) |

---

Za svaki UML dijagram čuvajte čitljivu sliku i PlantUML izvor, kako je opisano na stranici [Home](./Home.md). U dovršenoj stranici koristite vlastite projektne odluke i modele.

> **Stanje:** poglavlje se puni postupno. Cjelovito ga piše DOC-13 do DOC-16 (Faza 1), a svaki zadatak ažurira svoj dio u istom PR-u kao kod. Odjeljci bez sadržaja još imaju upute iz predloška; primjeri (CrisisMap) ostavljeni su kao uzor, a njihove slike učitavaju se iz [predloška kolegija](https://github.com/VladoSruk/Programsko-inzenjerstvo).

## 4.1 Strategija rješenja

**Cilj:** Objasniti nekoliko arhitekturnih odluka koje najviše utječu na rješenje i navesti njihove projektno specifične razloge.

ArtConnect je web-aplikacija klijent–poslužitelj: React aplikacija u pregledniku poziva REST API jedne Spring Boot aplikacije, a podaci su u PostgreSQL-u. Sve se izvodi na jednom poslužitelju u Dockeru.

| Odluka | Razmatrana alternativa | Projektno specifično obrazloženje i kompromis |
| --- | --- | --- |
| Jedna poslužiteljska aplikacija podijeljena u module (`auth`, `opportunity`, `application`…) | Zasebne usluge (mikroservisi) | Sedmeročlani tim u jednom semestru lakše gradi, testira i postavlja jednu aplikaciju. Moduli imaju jasne odgovornosti, ali kvar aplikacije zaustavlja cijeli sustav. |
| Klijent i API na istoj adresi; Caddy poslužuje React aplikaciju i prosljeđuje `/api` backendu | Klijent i API na različitim domenama | Kolačić sesije radi bez CORS-a i dodatnih postavki. Klijent i poslužitelj ipak se uvijek isporučuju zajedno. |
| Poslužiteljska sesija u bazi ([ADR-01](#adr-01-poslužiteljska-sesija-umjesto-jwt-a)) | JWT tokeni | Sesiju je lako poništiti (odjava, blokiranje, promjena uloge). Za zahtjeve koji mijenjaju podatke potrebna je CSRF zaštita. |
| Relacijska baza PostgreSQL, shema se mijenja samo migracijama (Liquibase) | Dokumentna baza | Podaci su jako povezani (korisnik, prilika, prijava, rezultat), a migracije čuvaju povijest sheme. Svaka promjena sheme traži novu migraciju. |
| OpenStreetMap i Leaflet ([ADR-02](#adr-02-openstreetmap--leaflet-umjesto-google-mapsa)) | Google Maps | Nema API ključa ni računa za naplatu. Javna usluga nema jamstvo dostupnosti. |
| Geokodiranje preko backenda ([ADR-03](#adr-03-geokodiranje-preko-backenda-nominatim)) | Nominatim poziv izravno iz preglednika | Backend centralno poštuje ograničenje od jednog zahtjeva u sekundi. Kad usluga ne odgovori, organizator ručno postavlja lokaciju. |
| Datoteke na Docker volumenu, a podaci o njima u bazi ([ADR-04](#adr-04-pohrana-datoteka-na-docker-volumenu)) | Datoteke u bazi ili u vanjskoj pohrani | Baza ostaje mala, a datoteke prežive zamjenu kontejnera. Volumen treba zasebnu sigurnosnu kopiju. |
| Jedan poslužitelj AWS EC2 koji se pali po potrebi | Stalno upaljen poslužitelj ili upravljana platforma | Štedi ograničeni AWS kredit. Aplikacija nije uvijek dostupna (NF-09) i nema zasebnog testnog okruženja. |

## 4.2 Gradivni blokovi i UML dijagram komponenata

**Cilj:** Prikazati logičku podjelu programa na komponente, njihova sučelja i vanjske integracije. Dijagram komponenata prikazuje logičku organizaciju, dok dijagram razmještaja prikazuje fizičko izvođenje. Ovaj pogled ažurirajte kako sustav poprima konačan oblik.

**Dijagram komponenata:** [U izradi: DOC-13.]  
**PlantUML izvor:** [U izradi: DOC-13.]

| Komponenta | Odgovornost | Razlog za ovu granicu |
| --- | --- | --- |
| Poslužiteljska aplikacija (`backend/`, Spring Boot 4, Java 25) | REST API pod `/api/v1`, poslovna pravila, prijava i sesija, ovlaštenja, zakazani poslovi | Jedna aplikacija koja se postavlja kao cjelina; moduli su paketi unutar nje, a ne zasebne usluge. |
| Klijentski dio (`frontend/`, React + Vite + TypeScript) | Korisničko sučelje u pregledniku; poziva API preko `src/api/client.ts`; karte prikazuje pomoću Leafleta. | Prikaz i interakcija odvojeni su od poslovnih pravila; u produkciji ga Caddy servira kao statičke datoteke. |
| OpenStreetMap Tile Service (vanjski sustav) | Klijentski dio dohvaća pločice putem HTTPS-a s `https://tile.openstreetmap.org/{z}/{x}/{y}.png`; atribucija je uvijek vidljiva. | Karta koristi vanjski izvor kartografskih podataka; pločice se dohvaćaju izravno s klijenta, nisu dio baze podataka aplikacije, a pri neuspjelom učitavanju karta prikazuje upozorenje. |
| Nominatim (vanjski sustav) | Backend prima adresu preko `GET /api/v1/geocoding/search`, ograničava odlazne zahtjeve na jedan u sekundi i vraća koordinate organizatoru. | Adresa se geokodira na backendu kako klijent ne bi zaobilazio globalno ograničenje Nominatima; backend postavlja prepoznatljiv `User-Agent`, a neuspjeh vanjske usluge ne sprema se u bazu. |
| Baza podataka (PostgreSQL 17) | Trajni podaci aplikacije i sesije korisnika (Spring Session JDBC) | Podaci imaju jasne relacijske veze (korisnik, prilika, prijava); shemu verzionira Liquibase. |

### Backend: moduli i slojevi

Kod je organiziran **paket po modulu** (`hr.fer.artconnect.<modul>`). Moduli zasad postoje kao kostur (prazni paketi) i pune se uz zadatke svojih epika.

| Modul | Odgovornost |
| --- | --- |
| `auth` | Google prijava, sesija, odabir uloge, trenutni korisnik |
| `artist` | Profil umjetnika i javni portfolio |
| `artwork` | Djela u portfoliju i njihove slike |
| `opportunity` | Prilike (natječaji, izložbe, radionice): objava, pretraživanje, statusi |
| `opportunity.service.geocoding` | Geokodiranje adrese preko Nominatima; integracija je backend-only i bez trajne pohrane |
| `application` | Prijave umjetnika na prilike i odluke organizatora |
| `result` | Objava rezultata i sudjelovanja |
| `file` | Pohrana datoteka na volumenu `/data/uploads` i provjera vrste |
| `catalog` | Šifrarnici (područja umjetnosti, vrste prilika) |
| `admin` | Administracija korisnika, uloga, blokiranja i skrivanja sadržaja |
| `calendar` | Poveznica i događaj u Google kalendaru |
| `statistics` | Statistika organizatora |
| `report` | Automatski mjesečni izvještaj |
| `notification` | Obavijesti u aplikaciji (opcionalno) |
| `common` | Zajednički dijelovi: omotnica grešaka, straničenje, sigurnosna konfiguracija |

Unutar svakog modula kod je podijeljen u tri sloja:

| Sloj   | Paket     | Sadržaj                       | Pravilo                                                                                 |
| ------ | --------- | ----------------------------- | --------------------------------------------------------------------------------------- |
| Web    | `web`     | Kontroleri i DTO-ovi          | Prima i vraća DTO; nema poslovnih pravila; entiteti se nikad ne vraćaju iz API-ja.      |
| Servis | `service` | Poslovna logika i transakcije | Provjerava poslovna pravila i vlasništvo (`CurrentUserProvider`); ovdje su transakcije. |
| Domena | `domain`  | JPA entiteti i repozitoriji   | Pristup bazi; bez znanja o HTTP-u.                                                      |

Ovisnosti idu prema dolje: web → servis → domena.

### Frontend: struktura

React aplikacija u `frontend/src/`, organizirana po značajkama i zajedničkim komponentama. Router i globalni MUI provider nalaze se u korijenskoj aplikaciji; zajednički izgled, stanja i lokalizacija odvojeni su od funkcionalnih stranica.

| Mjesto                                  | Sadržaj                                                                                                                                                                                                                      |
| --------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `main.tsx`                              | Ulazna točka i globalni MUI provider (`ThemeProvider`, `CssBaseline`)                                                                                                                                                        |
| `App.tsx`                               | Korijenska aplikacija, React Router i definicije ruta                                                                                                                                                                        |
| `app/theme.ts`                          | MUI tema s dizajnerskim tokenima                                                                                                                                                                                             |
| `api/client.ts`                         | `apiFetch`: osnovna adresa `/api/v1`, kolačić sesije, CSRF zaglavlje `X-XSRF-TOKEN` na zahtjevima koji mijenjaju stanje, slanje JSON-a, pretvaranje svake greške (API, mreža, neočekivan odgovor) u `ApiError`               |
| `components/layout/`                    | `AppLayout`: zaglavlje, navigacija prema ulozi, mobilni Drawer, poveznica za preskok na sadržaj, `<Outlet />`, podnožje i naslov stranice                                                                                    |
| `components/state/`                     | Zajedničke komponente učitavanja, praznog i greška stanja; stranice 403/404; statusni chipovi i globalni Snackbar (`useNotify`)                                                                                              |
| `features/<značajka>/`                  | Stranice, komponente i hookovi jedne funkcionalnosti, npr. početna stranica u `features/home/` ili `features/opportunities/`                                                                                                 |
| `features/opportunities/components/map` | Zajednička **MapView** komponenta **(Leaflet + OpenStreetMap)** s podrškom za dinamičko učitavanje **(React.lazy)**, prikaz pojedinačnih/višestrukih oznaka te **DraggableMarkerMap** varijanta za odabir lokacije u obrascu |
| `i18n/hr.ts`                            | Zajednički tekstovi sučelja na hrvatskom; značajke mogu imati vlastite tekstove                                                                                                                                              |

Dok AUTH-01-FE1 ne isporuči hook za dohvat trenutnog korisnika, izgled prikazuje posjetiteljsku navigaciju. Statusi prilika i prijava definirani su zasebno.

Svi pozivi backenda idu kroz `apiFetch` iz `api/client.ts`, nikad kroz vlastiti `fetch`, kako bi CSRF i obrada grešaka bili na jednom mjestu. Putanje, polja i kodovi grešaka uzimaju se iz API dokumentacije tima.

<details>
<summary><strong>Granica sustava, odgovornosti i vanjske ovisnosti</strong></summary>

**Primjer — CrisisMap:** Predložena arhitektura podržava podnošenje prijava kriznih događaja, prikaz na karti i obavijesti o potvrđenim prijavama.

![Dijagram komponenata sustava CrisisMap](https://raw.githubusercontent.com/VladoSruk/Programsko-inzenjerstvo/main/docs/assets/diagrams/4-1-crisismap-system-CMP.svg)

**Slika 4.1. Dijagram komponenata sustava CrisisMap**

**PlantUML izvor:** [izvor](https://github.com/VladoSruk/Programsko-inzenjerstvo/blob/main/docs/puml/4-1-crisismap-system-CMP.puml)

| Dio                  | Odgovornost                                                                                     | Razlog za ovu granicu                                                          |
| -------------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Web-klijent          | Prikazuje prijave i prikuplja podatke za novu prijavu.                                          | Korisnička interakcija odvojena je od pravila prijave i trajne pohrane.        |
| API za prijave       | Prihvaća zahtjeve za rad s prijavama i vraća odgovore.                                          | Odgovornosti izložene putem HTTP-a odvojene su od obrade prijava.              |
| Usluga za prijave    | Primjenjuje pravila prijave i pristupa pohranjenim prijavama.                                   | Pravila provjere valjanosti i pohrane prijava imaju jasno mjesto odgovornosti. |
| Proces za obavijesti | Određuje korisnike koji trebaju primiti obavijest i traži od vanjskog pružatelja da je pošalje. | Obrada obavijesti odvojena je od odgovaranja na korisnikov zahtjev za prijavu. |

**Analiza primjera:** Vanjska granica odvaja predloženi sustav od pružatelja karte i pružatelja obavijesti. Unutarnja granica grupira API za prijave, uslugu za prijave i proces za obavijesti unutar jedne poslužiteljske aplikacije; njihovi zasebni simboli prikazuju predložene odgovornosti, a ne neovisno postavljene usluge. PostgreSQL pohranjuje podatke aplikacije, dok pružatelj karte osigurava kartografske pločice. Strelice sažimaju ovisnosti, a ne redoslijed transakcije; taj redoslijed prikazuje sekvencijski dijagram. Izostavite proces za obavijesti i Firebase Cloud Messaging ako obavijesti nisu dio odobrenog opsega. Pružatelja prijave u sustav uključite samo ako to zahtijeva odobreni zadatak. Kako implementacija napreduje, ispravite svaku predloženu granicu koja se pokaže netočnom; završni dijagram mora odgovarati onome što je tim izgradio. Jedan koherentan pogled na komponente korisniji je od nekoliko gotovo jednakih inačica s proturječnim vezama.

</details>

## 4.3 Podaci i detaljno oblikovanje

**Cilj:** Objasniti važne informacije koje aplikacija pohranjuje i jedno reprezentativno područje oblikovanja na razini razreda. Model podataka i UML dijagram razreda odgovaraju na različita pitanja; jedno ne zamjenjuje drugo.

### 4.3.1 ER dijagram i model podataka

**Cilj:** Prikazati važne pohranjene entitete, njihove identifikatore i veze te dovoljno atributa za razumijevanje podataka aplikacije. ER dijagram odvojen je od reprezentativnog UML dijagrama razreda u nastavku.

**ER dijagram:**

![ER dijagram podataka sustava ArtConnect](assets/diagrams/4-2-artconnect-data-ERD.svg)

**Slika 4.2. ER dijagram postojećih tablica**

**Izvor dijagrama:** [`docs/puml/4-2-artconnect-data-ERD.puml`](puml/4-2-artconnect-data-ERD.puml)  

**Implementacija:** Liquibase changesetovi `001-spring-session.yaml` – `009-artist-profiles.yaml` i `021-artworks-and-files.yaml` u
`backend/src/main/resources/db/changelog/changes/`; JPA entiteti `ArtField` i
`OpportunityType` u `hr.fer.artconnect.catalog.domain`, `SystemSetting` u `hr.fer.artconnect.common.domain`,
`User` i `OrganizerProfile` u `hr.fer.artconnect.auth.domain`, `ArtistProfile`, `ArtistLink` i
`PastExhibition` u `hr.fer.artconnect.artist.domain`, `StoredFile` u `hr.fer.artconnect.file.domain` te `Artwork` i `ArtworkImage` u `hr.fer.artconnect.artwork.domain`.
Tablice Spring Sessiona nemaju entitet - njima Spring Session JDBC pristupa izravno. Stavke šifrarnika se ne brišu nego isključuju (`active = false`).

Changeset `021-artworks-and-files.yaml` stvara:

| Tablica | Podaci i ograničenja |
| --- | --- |
| `stored_files` | Vlasnik (`users`, cascade), namjena `ARTWORK_IMAGE` ili `APPLICATION_ATTACHMENT`, izvorno ime, tip `image/jpeg`, `image/png` ili `application/pdf`, veličina veća od nule, jedinstveni relativni `storage_key` za volumen `/data/uploads` i vrijeme stvaranja. Datoteka se fizički briše servisom, ne bazom. |
| `artworks` | Umjetnik (`users`, cascade), naslov, godina uz CHECK `>= 1900`, tehnika, neobavezne dimenzije i opis do 5000 znakova, vidljivost `PUBLIC` ili zadano `PRIVATE`, `hidden_by_admin` i `hidden_at` te vremena stvaranja/ažuriranja. |
| `artwork_images` | Djelo i povezani redak datoteke (oba cascade), neobavezan alt tekst do 300 znakova (zadano naslov djela) i redoslijed; `file_id` je jedinstven. Najviše tri slike po djelu provodi aplikacija, ne baza. |

Indeksi: `ix_stored_files_owner (owner_id)`, `ix_artworks_artist_visibility (artist_id, visibility) WHERE hidden_by_admin = false`, `ix_artworks_artist_created (artist_id, created_at DESC)` za Moj portfolio i `ix_artwork_images_artwork (artwork_id, position)`. Dev kontekst dodaje testnom umjetniku jedno javno i jedno privatno djelo bez slika. Enum vrijednosti pohranjuju se kao varchar i provjeravaju CHECK ograničenjima.

**Indeksi javne pretrage prilika (`008-opportunities.yaml`).** Javna pretraga
(`GET /opportunities`) jedini je upit u sustavu pisan kao nativni SQL
([OpportunitySearchRepository](../backend/src/main/java/hr/fer/artconnect/opportunity/domain/OpportunitySearchRepository.java)),
jer djelomični i izrazni indeksi traže točan oblik izraza: planer ih koristi samo
ako se izraz upita slovo po slovo poklapa s izrazom indeksa. Zato uvjeti
`archived = false AND hidden_by_admin = false` stoje doslovno u upitu, a ne kao
parametri, i zato punotekstni izraz nije skriven u JPA kriterij.

| Filtar ili poredak | Indeks | Oblik koji indeks traži |
| --- | --- | --- |
| `statuses` + poredak po roku | `ix_opp_public (status, deadline) WHERE archived = false AND hidden_by_admin = false` | oba uvjeta vidljivosti doslovno u `WHERE` |
| zadana pretraga (`statuses=[OPEN]`) | `ix_opp_open_deadline (deadline) WHERE status = 'OPEN'` | uži djelomični indeks koji planer bira umjesto `ix_opp_public` kad je status samo `OPEN` |
| `q` | `ix_opp_fts` GIN nad `to_tsvector('simple', title \|\| ' ' \|\| description)` | identičan izraz, uključujući konfiguraciju `simple` i spajanje razmakom |
| `city` | `ix_opp_city_lower (lower(city))` | `lower(city) = lower(:city)`, ne `ILIKE` |
| `artFieldIds` | `ix_oaf_art_field (art_field_id)` | `EXISTS` nad `opportunity_art_fields` |
| `sort=-publishedAt` | `ix_opp_published_at (published_at)` | poredak po `published_at` |

Područja umjetnosti cijele stranice dohvaćaju se jednim dodatnim upitom
(`WHERE opportunity_id IN (:ids)`), pa je po zahtjevu točno troje upita —
`COUNT(*)`, stranica prilika i područja — neovisno o broju prilika na stranici.
To čuva test `podrucjaUmjetnostiStraniceDohvacaJedanUpit`, a korištenje indeksa
`upitiPretrageMoguKoristitiPredvideneIndekse` (oba u
[OpportunitySearchIntegrationTest](../backend/src/test/java/hr/fer/artconnect/opportunity/OpportunitySearchIntegrationTest.java)).

<details>
<summary><strong>Jedna prijava poplave i povezani podaci</strong></summary>

**Primjer — CrisisMap:** Ana putem svojeg korisničkog računa podnosi prijavu poplave `R-104`. Odabire lokaciju na karti i označava razinu ozbiljnosti kao visoku. Građanin može podnijeti više prijava; svaka prijava pripada jednoj kategoriji kriznog događaja, primjerice _Poplava_. Aplikacija pohranjuje prijavu i njezine koordinate. Kartografske pločice dolaze od vanjskog pružatelja i nisu entiteti u modelu podataka aplikacije.

![ER dijagram podataka sustava CrisisMap](https://raw.githubusercontent.com/VladoSruk/Programsko-inzenjerstvo/main/docs/assets/diagrams/4-2-crisismap-data-ERD.svg)

**Slika 4.2. ER dijagram podataka sustava CrisisMap**

**PlantUML izvor:** [izvor](https://github.com/VladoSruk/Programsko-inzenjerstvo/blob/main/docs/puml/4-2-crisismap-data-ERD.puml)

**Analiza primjera:** `R-104` ima jednog autora i jednu kategoriju; Ana i kategorija _Poplava_ mogu biti povezane s više prijava. Dijagram prikazuje veze bez prepisivanja svih stupaca baze podataka ili pisanja drugog tabličnog opisa za svaki entitet. Ako aplikacija prihvaća i **anonimne** prijave, tu mogućnost prikažite u vezi s entitetom `Citizen`. Ako su kategorije fiksne vrijednosti, a ne pohranjeni zapisi, nemojte izmišljati tablicu kategorija. Završni ER dijagram usporedite s modelom trajne pohrane i objasnite samo razlike važne za razumijevanje projekta.

</details>

### 4.3.2 Reprezentativni UML dijagram razreda

**Cilj:** Prije implementacije reprezentativnog dijela sustava modelirati i prikazati razrede ili sučelja potrebna za dodjelu odgovornosti, operacija i veza. Oblikovanje ažurirajte kada se te odluke promijene.

**Dijagram razreda:**

![Reprezentativni dijagram razreda prilika sustava ArtConnect](assets/diagrams/4-3-artconnect-opportunity-CD.svg)

**Slika 4.3. Reprezentativni dijagram razreda prilika**

**PlantUML izvor:** [`docs/puml/4-3-artconnect-opportunity-CD.puml`](puml/4-3-artconnect-opportunity-CD.puml)
**Referenca na kod:** [`opportunity`](../backend/src/main/java/hr/fer/artconnect/opportunity), [`User`](../backend/src/main/java/hr/fer/artconnect/auth/domain/User.java), [`OpportunityType`](../backend/src/main/java/hr/fer/artconnect/catalog/domain/OpportunityType.java) i [`ArtField`](../backend/src/main/java/hr/fer/artconnect/catalog/domain/ArtField.java).

Dijagram prikazuje priliku, njezinu lokaciju kao sastavni vrijednosni objekt,
organizatora i šifrarnike te ključne odgovornosti kontrolera, servisa,
repozitorija i mappera. `Opportunity` sadrži pravila i podatke domene; servis
koordinira stvaranje i uređivanje, a repozitorij i mapper podržavaju pohranu i
oblikovanje odgovora. Detalji polja zahtjeva i DTO-ova, podržavajući suradnici
servisa te vanjska geokoderska integracija namjerno su izostavljeni radi
čitljivosti i prikazani u API dokumentaciji odnosno sekvencijskom dijagramu.

<details>
<summary><strong>Oblikovanje odgovornosti prijave i dopuštenih promjena</strong></summary>

**Primjer — CrisisMap:** Prijava sadrži pravila za promjenu vlastitog statusa. Razred tijeka rada koordinira podnošenje i pregled, dok sučelje repozitorija opisuje potrebne operacije trajne pohrane. Dijagram predlaže te odgovornosti prije implementacije.

![Dijagram razreda za obradu prijava kriznih događaja](https://raw.githubusercontent.com/VladoSruk/Programsko-inzenjerstvo/main/docs/assets/diagrams/4-3-crisismap-domain-CD.svg)

**Slika 4.3. Dijagram razreda za obradu prijava kriznih događaja**

**PlantUML izvor:** [izvor](https://github.com/VladoSruk/Programsko-inzenjerstvo/blob/main/docs/puml/4-3-crisismap-domain-CD.puml)

**Analiza primjera:** `DisasterReport` sadrži dopuštena pravila promjene stanja pa tijek rada ne može prešutno odobriti već odbijenu prijavu. `ReportWorkflow` koordinira operaciju bez preuzimanja pravila prijelaza stanja same prijave; `ReportRepository` definira potrebne operacije pohrane. Kompozicija `Location` i odabrani tipovi prikazuju veze korisne za oblikovanje. Dijagram namjerno izostavlja stupce baze podataka i nepovezane razrede: ER model objašnjava pohranjene podatke, dok ovaj dijagram dodjeljuje programske odgovornosti i operacije. Ti su razredi odluka oblikovanja, a ne obavezni slojevi za svaki tim. Završnu implementaciju usporedite s početnim oblikovanjem i ažurirajte dijagram tamo gdje su se odluke promijenile; generiranje dijagrama iz koda tek na kraju ne može zamijeniti ovo početno modeliranje.

</details>

## 4.4 Ponašanje tijekom izvođenja

**Cilj:** Objasniti jednu važnu interakciju među dijelovima koji se izvode i životni ciklus jednog smislenog objekta. Odaberite primjere koji otkrivaju odluke ili neuspješne putove umjesto izrade dijagrama za svaku CRUD radnju.

### 4.4.1 Reprezentativni UML sekvencijski dijagram

**Cilj:** Prikazati redoslijed poruka za jedan reprezentativni scenarij i odgovore na važne alternative.

**Scenarij:** UC-01 — prijava posjetitelja putem Google OIDC-a (AUTH-01).

**Sekvencijski dijagram:**

![Sekvencijski dijagram Google prijave sustava ArtConnect](assets/diagrams/4-4-artconnect-google-login-SD.svg)

**Slika 4.4. Sekvencijski dijagram prijave Googleom i uspostave sesije**

**Izvor dijagrama:** [`docs/puml/4-4-artconnect-google-login-SD.puml`](puml/4-4-artconnect-google-login-SD.puml)

Posjetitelj pokreće prijavu putem Googlea. Spring Security obrađuje OIDC povratni poziv i provjerava ID token prije predaje korisničkih podataka komponenti `ArtConnectOidcUserService`. Servis zahtijeva dostupnu i potvrđenu e-mail adresu te u jednoj transakciji pronalazi ili stvara zapis u tablici `users`.

Postojeći korisnik pronalazi se prema Googleovu identifikatoru `sub`, a njegovi se profilni podaci i vrijeme posljednje prijave ažuriraju. Ako korisnik nije pronađen, pokušava se povezivanje pozvanog računa sa statusom `INVITED` prema e-mail adresi; to uključuje i unaprijed stvorenog administratora iz konfiguracije. Sukob s računom u drugom statusu odbija prijavu. Ako odgovarajući račun ne postoji, stvara se korisnik sa statusom `ONBOARDING` bez dodijeljene uloge. Obrisani korisnički računi ne pronalaze se prema `google_sub` ili e-mail adresi jer su te vrijednosti poništene.

Nakon uspješne obrade stvara se poslužiteljska sesija putem Spring Session JDBC. `ArtConnectPrincipal.getName()` vraća interni ID korisnika iz tablice `users`, što omogućuje indeksiranje sesija po korisniku. Korisnik sa statusom `ONBOARDING` preusmjerava se na `/onboarding`, a ostali dopušteni korisnici na `/`. Neuspjela autentikacija ili obrada korisnika vodi na `/?login=error`.

**Sigurnosno ponašanje:** API zahtjevi bez valjane sesije vraćaju `401 UNAUTHENTICATED`; ne pokreću OAuth preusmjeravanje. Sesija istječe nakon osam sati neaktivnosti, a kolačić `SESSION` koristi `HttpOnly`, `SameSite=Lax` i `Secure` u produkciji.

**Scenarij:** UC-04 — organizator stvara priliku i sprema je kao privatni nacrt.

![Sekvencijski dijagram stvaranja prilike](assets/diagrams/4-5-artconnect-opportunity-create-SD.svg)

**Slika 4.5. Sekvencijski dijagram stvaranja prilike**

**Izvor dijagrama:** [`docs/puml/4-5-artconnect-opportunity-create-SD.puml`](puml/4-5-artconnect-opportunity-create-SD.puml)

Dijagram prikazuje odabir fizičke ili online lokacije. Za fizičku lokaciju organizator može odabrati jedan od geokodiranih kandidata ili ručno postaviti odnosno ispraviti oznaku; prazan rezultat, ograničenje učestalosti i nedostupnost Nominatima vode u ručni odabir. Neuspjeh kartografskih pločica prikazuje upozorenje, bez ukidanja kontrola za odabir lokacije. Nakon provjere unosa API sprema priliku i područja umjetnosti u jednoj transakciji, u statusu `PREPARATION`; objava nije dio ovog scenarija.

**Scenarij:** UC-02 — umjetnik uređuje profil i portfolio učitavanjem slike djela (PORT-04-BE2).

![Sekvencijski dijagram UC-02: učitavanje slike djela](assets/diagrams/4-6-artconnect-artwork-image-upload-SD.svg)

**Slika 4.6. Sekvencijski dijagram UC-02 — uređivanje profila i portfolija: učitavanje slike djela**

**Izvor dijagrama:** [`docs/puml/4-6-artconnect-artwork-image-upload-SD.puml`](puml/4-6-artconnect-artwork-image-upload-SD.puml)

Zahtjev s jednom slikom i neobaveznim alternativnim tekstom prolazi autentikaciju, provjeru uloge umjetnika i CSRF zaštitu. `ArtworkService` u jednoj transakciji zaključava djelo, zatim provjerava vlasništvo, limit slika iz `system_settings`, veličinu i vrstu sadržaja tim redoslijedom. `FileStorageService` prepoznaje JPEG/PNG prema bajtovima, pohranjuje datoteku na volumenu i metapodatke; servis zatim dodaje sliku na sljedeću poziciju, zadani `altText` uzima iz naslova djela i ažurira `artworks.updated_at`. Zaključavanje reda sprječava da konkurentni zahtjevi prijeđu limit, a rollback transakcije uklanja već zapisanu datoteku. Ugovor i odgovori rute opisani su u [API dokumentaciji djela](api/artworks.md#post-artworksartworkidimages).

<details>
<summary><strong>Odabir lokacije, provjera valjanosti i podnošenje prijave</strong></summary>

**Primjer — CrisisMap:** Građanin podnosi prijavu kriznog događaja nakon što ručno odabere lokaciju ili zatraži položaj uređaja. Interakcija prikazuje i što se događa kada je pristup lokaciji odbijen, podaci prijave nisu valjani ili pohrana ne uspije.

![Sekvencijski dijagram podnošenja prijave kriznog događaja](https://raw.githubusercontent.com/VladoSruk/Programsko-inzenjerstvo/main/docs/assets/diagrams/4-4-crisismap-report-submission-SD.svg)

**Slika 4.4. Sekvencijski dijagram podnošenja prijave kriznog događaja**

**PlantUML izvor:** [izvor](https://github.com/VladoSruk/Programsko-inzenjerstvo/blob/main/docs/puml/4-4-crisismap-report-submission-SD.puml)

**Analiza primjera:** Dva načina odabira lokacije te alternative provjere i pohrane imaju različite posljedice za korisnika; usporedite ih s odgovarajućim obrascem uporabe i njegovim alternativnim tijekovima. API preglednika za lokaciju mogućnost je preglednika; web-klijent, API, usluga i pohrana odgovaraju prikazu komponenata. Odaberite grane koje pomažu objasniti scenarij vašeg projekta i završni dijagram uskladite s implementiranom interakcijom.

</details>

### 4.4.2 Reprezentativni UML dijagram stanja

**Cilj:** Prikazati važna stanja jednog domenskog objekta ili eksplicitno upravljanog načina rada sustava te događaje ili uvjete koji ih mijenjaju.

Prilika (`opportunities.status`) prolazi kroz sljedeća stanja:

![Dijagram stanja prilike](assets/diagrams/4-6-artconnect-opportunity-SM.svg)

**PlantUML izvor:** [stanja prilike](puml/4-6-artconnect-opportunity-SM.puml).
Ručne prijelaze provjerava čista Java klasa `OpportunityStatusMachine`.
`OpportunityService` provjerava organizatora i vlasništvo, poziva pravila te u
jednoj transakciji sprema status i `updatedAt`. Prvo otvaranje postavlja
`publishedAt`; ponovno otvaranje čuva taj datum. Arhivirana prilika, isti status
i nedopušten par daju `INVALID_STATUS_TRANSITION`; otvaranje na isteku roka ili
nakon njega daje `DEADLINE_PASSED`. Do ADM-02-BE svi organizatori smatraju se
odobrenima; klasa već ima ulaz za buduću provjeru odobrenja pri prvoj objavi.
`FINISHED` i automatsko zatvaranje prikazuju zasebne zadatke RES-01-BE1 i OPP-04-BE1.
Ugovor rute opisan je u [API dokumentaciji prilika](api/opportunities.md#post-opportunitiesopportunityidstatus).
Pravila pokriva [jedinični test](../backend/src/test/java/hr/fer/artconnect/opportunity/OpportunityStatusMachineTest.java),
a spremanje i HTTP odgovore [integracijski test](../backend/src/test/java/hr/fer/artconnect/opportunity/OpportunityStatusIntegrationTest.java).

Vlasnik stanja je korisnički račun (`users.status`). Nova Google prijava stvara
račun u stanju `ONBOARDING`, dok pozvani račun ima unaprijed zadanu ulogu.
Jednokratni odabir uloge aktivira novi račun i osvježava prava u istoj sesiji.

```mermaid
stateDiagram-v2
    [*] --> ONBOARDING: prva Google prijava
    [*] --> INVITED: unaprijed pozvani račun
    INVITED --> ACTIVE: Google prijava s pozvanom adresom
    ONBOARDING --> ACTIVE: odabir ARTIST ili ORGANIZER
    ACTIVE --> BLOCKED: administrator blokira (EPIC-08)
    BLOCKED --> ACTIVE: administrator deblokira (EPIC-08)
    ONBOARDING --> DELETED: brisanje računa (AUTH-04)
    ACTIVE --> DELETED: brisanje računa (AUTH-04)
    BLOCKED --> DELETED: brisanje računa (AUTH-04)
    DELETED --> [*]
```

**PlantUML izvor:** [stanja korisnika](puml/4-5-artconnect-user-state.puml).
Blokiranje i brisanje prikazuju planirane prijelaze; nisu implementirani u AUTH-02.
Ponovna prijava nakon brisanja stvara novi račun, a ne oživljava obrisani red.

<details>
<summary><strong>Načini rada i dostupnost usluge</strong></summary>

**Primjer — CrisisMap:** Način koordinacije kriznog događaja mijenja se kako se događaj procjenjuje i rješava. Istodobno se korisniku vidljiva dostupnost aplikacije može neovisno mijenjati kada pružatelj karte ili poslužiteljski sustav postanu nedostupni.

![Dijagram stanja prijave kriznog događaja](https://raw.githubusercontent.com/VladoSruk/Programsko-inzenjerstvo/main/docs/assets/diagrams/4-5-crisismap-report-SM.svg)

**Slika 4.5. Dijagram stanja prijave kriznog događaja**

**PlantUML izvor:** [izvor](https://github.com/VladoSruk/Programsko-inzenjerstvo/blob/main/docs/puml/4-5-crisismap-report-SM.puml)

**Analiza primjera:** Dvije regije opisuju neovisne aspekte modeliranog sustava: način koordinacije kriznog događaja i opaženu dostupnost usluge. Izvode se paralelno; aktivni krizni događaj može postojati uz normalno ili ograničeno pružanje usluge. Grane nakon oporavka poslužiteljskog sustava uzimaju u obzir da pružatelj karte možda i dalje nije dostupan. Prijelazi stanja imaju imenovane okidače ili uvjete umjesto da budu samo popis zaslona. Ovaj oblik koristite samo ako aplikacija stvarno održava ili pouzdano izvodi obje vrste stanja. Projekt čiji važan životni ciklus pripada prijavi, rezervaciji ili narudžbi treba modelirati taj objekt; složenost dijagrama nije kriterij vrednovanja. Pri završnom pregledu model stanja usporedite s ponašanjem koje pokrenuta aplikacija stvarno podržava.

</details>

## 4.5 Zapisi arhitekturnih odluka

**Cilj:** Sačuvati razloge i obrazložiti odabrane važne arhitekturne odluke kada tablica u odjelku 4.1 nije dovoljna za očuvanje konteksta, razmatranih alternativa i prihvaćenih kompromisa.

Napišite nekoliko ADR-a samo za važne odluke čije obrazloženje, alternative i kompromis treba sačuvati. Ostale arhitekturne odluke dovoljno je zabilježiti u tablici.

**Detaljni zapis arhitekturne odluke (engl. Architecture Decision Record, ADR), kada odluka zahtijeva dodatno objašnjenje:**

### ADR-[ID]: [Naslov odluke]

| Polje                  | Zapis odluke                                                                            |
| ---------------------- | --------------------------------------------------------------------------------------- |
| Kontekst i cilj        | [Koji je arhitekturni problem učinio ovu odluku potrebnom?]                             |
| Kriteriji odluke       | [Što je timu stvarno bilo važno?]                                                       |
| Razmatrane alternative | [Realne mogućnosti koje je tim razmotrio, ukratko.]                                     |
| Odluka i obrazloženje  | [Odabrana mogućnost, zašto je odabrana i čega se tim odrekao.]                          |
| Status                 | [Predloženo, prihvaćeno ili zamijenjeno; ako je primjenjivo, poveznica na novu odluku.] |

ADR pišite samo za odluku čije će obrazloženje ostati važno i nakon promjene implementacije. Značajna promjena odluke može se zabilježiti novim ADR-om koji zamjenjuje stari; rutinski odabiri alata ne trebaju detaljne zapise. Promjene dogovorenog opsega pripadaju zahtjevima i njihovoj povijesti promjena; povratna informacija o zadatku ostaje uz odgovarajući zadatak.

### ADR-01: Poslužiteljska sesija umjesto JWT-a

| Polje                  | Zapis odluke                                                                                                                                                                     |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Kontekst i cilj        | React aplikacija treba pamtiti prijavu, a backend treba moći poništiti sesije korisnika.                                                                                         |
| Razmatrane alternative | Poslužiteljska sesija u PostgreSQL-u ili JWT pristupni tokeni.                                                                                                                   |
| Odluka i obrazloženje  | Koristimo Spring Session JDBC i kolačić `SESSION`. Već imamo PostgreSQL, a sesije možemo dohvatiti po korisniku i obrisati. Ne trebamo uvoditi osvježavanje i opoziv JWT tokena. |
| Kompromis              | Backend pri radu sa sesijom pristupa bazi. Za zahtjeve koji mijenjaju podatke potrebna je CSRF zaštita.                                                                          |
| Status                 | Prihvaćeno u FND-04 (#9).                                                                                                                                                        |

<details>
<summary><strong>Odabir jednog poslužiteljskog sustava umjesto zasebno postavljenih usluga</strong></summary>

**Primjer — CrisisMap, ADR-01:**

| ID     | Datum          | Odluka                                     | Razmatrana alternativa                          | Obrazloženje i kompromis                                                                                                      | Status     |
| ------ | -------------- | ------------------------------------------ | ----------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- | ---------- |
| ADR-01 | [Datum odluke] | Jedan poslužiteljski API za obradu prijava | Zasebno postavljene usluge za prijave i resurse | Manji napor postavljanja i integracije za odobreni opseg; kvar poslužiteljskog sustava može utjecati na obje funkcionalnosti. | Prihvaćeno |

### ADR-01: Jedan poslužiteljski API za obradu prijava

| Polje                  | Zapis odluke                                                                                                                                                                                                   |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Kontekst i cilj        | Tim tijekom jednog semestra mora izraditi i javno postaviti prijavljivanje kriznih događaja i prikaz usmjeren na kartu.                                                                                        |
| Kriteriji odluke       | Tim može izraditi, integrirati, postaviti i objasniti rješenje; komponente i dalje imaju prepoznatljive odgovornosti.                                                                                          |
| Razmatrane alternative | **Jedan poslužiteljski sustav:** moduli dijele aplikaciju koja se postavlja kao cjelina. **Zasebne usluge:** svaka se može izvoditi neovisno, ali zahtijeva dodatni rad na integraciji i postavljanju.         |
| Odluka i obrazloženje  | Odabrati jedan poslužiteljski sustav s jasno odvojenim odgovornostima vezanim uz prijave. Time se smanjuje rad na postavljanju, uz prihvaćanje da kvar tog sustava može istodobno utjecati na rad s prijavama. |
| Status                 | Prihvaćeno.                                                                                                                                                                                                    |

**Analiza primjera:** Strategija rješenja navodi trenutačni pristup; ADR-01 čuva alternative i kriterije koji su do njega doveli. Dijagrami komponenata i razmještaja trebaju odražavati odabrani pristup kako se sustav razvija. Zaseban ADR koristan je za važnu odluku, a ne za svaki odabir alata.

</details>

### ADR-02: OpenStreetMap + Leaflet umjesto Google Mapsa

| Polje                  | Zapis odluke                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Kontekst i cilj        | Prikaz lokacija prilika i odabir lokacije zahtijevaju interaktivnu kartu u React klijentskoj aplikaciji. Za projektni opseg i očekivano opterećenje rješenje treba biti jednostavno za postavljanje bez uspostavljanja računa za naplatu kod kartografskog pružatelja.                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Kriteriji odluke       | Rješenje mora podržavati oznake i odabir lokacije, integrirati se s Reactom te prikazivati izvor podataka. Prednost ima rješenje koje ne zahtijeva API ključ ni račun s omogućenim obračunom po korištenju i čije postavljanje ne uvodi dodatnu kartografsku naplatu u projektnom opsegu.                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Razmatrane alternative | **Google Maps** kao kartografski servis te **OpenStreetMapove** pločice prikazane pomoću **Leafleta**.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Odluka i obrazloženje  | Odabrati **Leaflet** s javnim pločicama **OpenStreetMapa**. Leaflet je klijentska biblioteka bez kartografskog API ključa, a javni OSM Tile Service za ovaj opseg ne zahtijeva projektni račun za naplatu ni obračun po korištenju kao Google Maps Platform. Time se pojednostavljuje postavljanje i izbjegava konfiguracija Googleovih vjerodajnica i naplate; Leaflet se učitava lijeno kako ne bi ulazio u početni paket. Pločice se dohvaćaju s `https://tile.openstreetmap.org/{z}/{x}/{y}.png`, uz vidljivu atribuciju. Kompromis je ovisnost o javnom servisu bez jamstva dostupnosti, uz obvezu poštivanja njegove politike korištenja pločica; za veće opterećenje treba ponovno procijeniti pružatelja. |
| Status                 | Prihvaćeno.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |

### ADR-03: Geokodiranje preko backenda (Nominatim)

| Polje | Zapis odluke |
| --- | --- |
| Kontekst i cilj | Organizatoru treba pretvorba adrese prilike u koordinate. Nominatim zahtijeva poštovanje ograničenja od jednog zahtjeva u sekundi za cijelu aplikaciju. |
| Kriteriji odluke | Zahtjev mora biti ograničen globalno, koristiti prepoznatljiv `User-Agent`, ne izlagati vanjski servis pregledniku i omogućiti determinističko testiranje bez mrežnog poziva javnoj usluzi. |
| Razmatrane alternative | Izravni poziv iz preglednika ili poziv iz backenda s centralnim ograničivačem i konfigurabilnim Nominatim URL-om. |
| Odluka i obrazloženje | Odabrati backend poziv `GET /search` preko Spring `RestClient`. Jedan `ReentrantLock` ograničava pozive cijelog procesa, a `Clock` i konfiguracija omogućuju testiranje i razvoj preko lažnog poslužitelja. Organizatori, uključujući one na čekanju, mogu geokodirati prilike u pripremi; odobrenje se zasebno provjerava pri objavi prilike. |
| Kompromis | Svi geokodirajući zahtjevi čekaju zajednički raspoloživi termin; ako termin nije dostupan unutar dvije sekunde ili Nominatim ne odgovori, korisnik dobiva odgovarajuću grešku i može ručno odabrati lokaciju. Javni Nominatim nema jamstvo dostupnosti. |
| Status | Prihvaćeno u OPP-01-BE3. |

### ADR-04: Pohrana datoteka na Docker volumenu

| Polje | Zapis odluke |
| --- | --- |
| Kontekst i cilj | Slike djela i budući prilozi prijava moraju biti pohranjeni odvojeno od poslovne logike i opstati pri zamjeni backend kontejnera. |
| Kriteriji odluke | Pohrana treba imati jedinstvene ključeve, metapodatke u `stored_files`, sigurne putanje, očuvanje nakon ponovnog pokretanja i provjeru stvarnog tipa datoteke bez povjerenja u klijentska zaglavlja. |
| Razmatrane alternative | Datoteke kao binarni podaci u PostgreSQL-u, lokalna putanja unutar kontejnera ili zajednički direktorij na trajnom Docker volumenu. |
| Odluka i obrazloženje | Koristiti `FileStorageService` s korijenom `artconnect.storage.root`, UUID nazivima i metapodacima u `stored_files`; u produkciji montirati imenovani volumen `uploads` na `/data/uploads`. Binarne podatke ne spremati u PostgreSQL jer bi povećavali bazu i sigurnosne kopije te opteretili bazu pohranom i dohvatom sadržaja datoteka. Lokalna putanja unutar kontejnera nije dovoljna jer bi se datoteke izgubile pri zamjeni kontejnera. Imenovani Docker volumen opstaje neovisno o životnom vijeku kontejnera. Lokalni backend izvan Dockera koristi `./data/uploads`. Tip se određuje po JPEG, PNG ili PDF magic bytes. |
| Kompromis | Baza i datotečni sustav ne dijele jednu atomsku transakciju; privremene datoteke, atomsko premještanje i callbackovi transakcije ograničavaju rizik nekonzistentnosti. Docker volumen opstaje nakon zamjene kontejnera, ali zahtijeva zasebnu sigurnosnu kopiju i oporavak. |
| Status | Prihvaćeno u PORT-04-BE1 (#104). |

## 4.6 Koncepti koji prožimaju sustav

Ovdje su pravila koja vrijede u cijelom sustavu. Zajednički kod backenda je u paketu `hr.fer.artconnect.common`, a detalji pojedinih ruta u [API dokumentaciji](api/).

### Prijava, sesija i ovlasti

- Korisnik se prijavljuje Google računom. Backend zatim otvara sesiju koja se čuva u bazi i istječe nakon 8 sati neaktivnosti ([ADR-01](#adr-01-poslužiteljska-sesija-umjesto-jwt-a)). Kolačić sesije nije dostupan JavaScriptu (`HttpOnly`, `SameSite=Lax`, u produkciji i `Secure`).
- Uloge su `ARTIST`, `ORGANIZER` i `ADMIN` i međusobno su odvojene: administrator ne dobiva automatski prava umjetnika ili organizatora. Korisnik koji još nije odabrao ulogu dobiva `403 ONBOARDING_REQUIRED` na svim rutama osim onih potrebnih za odabir uloge ([API](api/auth.md)).
- Pravo na radnju provjerava se na backendu u dva koraka: uloga (`@PreAuthorize`) i vlasništvo (npr. organizator smije mijenjati samo svoju priliku). Frontend dodatno skriva stranice i radnje koje uloga ne smije koristiti, ali to nije zaštita.
- Zahtjevi koji mijenjaju podatke (POST, PUT, DELETE) moraju imati CSRF token u zaglavlju `X-XSRF-TOKEN`; klijent ga dobiva preko `GET /api/v1/auth/csrf`.

Provjera da sesija preživi ponovno pokretanje backenda opisana je u [poglavlju 5](5-Ispitivanje.md#provjera-fnd-04--18-10-2026).

### Vidljivost prilika

Jedno pravilo (`OpportunityVisibilityPolicy`) određuje tko vidi priliku. Vlasnik i administrator vide je uvijek. Ostali, uključujući neprijavljene, vide samo objavljene prilike (`OPEN`, `CLOSED`, `SELECTION`, `FINISHED`) koje nisu arhivirane ni skrivene i čiji organizator nije blokiran. Za priliku koju korisnik ne smije vidjeti API vraća `404`, isto kao za nepostojeću.

### Greške

Sve greške imaju isti JSON oblik, a poruke su na hrvatskom:

```json
{ "error": { "code": "VALIDATION_ERROR", "message": "Neki podaci nisu ispravni.", "details": [ { "field": "year", "issue": "Godina ne smije biti u budućnosti." } ] } }
```

Neispravan unos vraća `400` s popisom polja u `details`, neprijavljeni korisnik `401`, a korisnik bez prava `403`. Neočekivana greška vraća `500` s općom porukom, a detalji ostaju samo u zapisima poslužitelja. Frontend sve pozive šalje kroz `apiFetch` (`frontend/src/api/client.ts`), pa su CSRF i obrada grešaka na jednom mjestu.

### Straničenje i sortiranje

Popisi su uvijek straničeni: `page` počinje od 1, `pageSize` je zadano 20, a najviše 100. Sortira se parametrom `sort` (`deadline` uzlazno, `-deadline` silazno), a svaka ruta određuje dopuštena polja. Odgovor ima oblik `{ "data": [...], "meta": { "page": 1, "pageSize": 20, "total": 134 } }`.

### Datumi i vremenska zona

API šalje datume i vremena u obliku ISO-8601 s vremenskim pomakom (npr. `2026-11-20T23:59:00+01:00`). Sučelje ih prikazuje u zoni `Europe/Zagreb`, koja vrijedi i za pravila poput „godina ne smije biti u budućnosti”.

### Jezik

Sučelje je samo na hrvatskom. Tekstovi sučelja su u `frontend/src/i18n/hr.ts`, a poruke grešaka backenda u `messages_hr.properties`.

### Datoteke

Slike i ostale datoteke spremaju se na Docker volumen ([ADR-04](#adr-04-pohrana-datoteka-na-docker-volumenu)). Vrsta datoteke provjerava se po sadržaju, a ne po nazivu ili zaglavlju koje šalje preglednik. Ograničenja veličine i broja datoteka čitaju se iz postavki sustava koje uređuje administrator.

### Zapisi i nadzor

Zapisi svih kontejnera dostupni su timu u pregledniku na `/logs` (Dozzle, iza lozinke). Ispravnost backenda provjerava se na `/actuator/health`. Greške poslužitelja (5xx) bilježe se u zapise, a greške klijenta ne.

### Korisničko sučelje i pristupačnost

Sučelje je izrađeno u MUI-ju i koristi zajedničke dizajnerske tokene (boje, tipografija, razmaci, prijelomne točke) opisane u [`design-system/tokens.md`](design-system/tokens.md). Svaka stranica ima poveznicu „Preskoči na sadržaj” i može se koristiti tipkovnicom uz vidljiv fokus. Statusi se prikazuju tekstom, a ne samo bojom (NF-04, NF-05).

## 4.7 Arhitekturni rizici i tehnički dug

**Cilj:** Navesti stvarne tehničke ili domenske rizike i svjesno prihvaćene kompromise oblikovanja, njihove posljedice i način na koji ih tim obrađuje.

**Tehnički rizici i dug**

| Rizik ili kompromis | Posljedica | Trenutačni pristup |
| ------------------- | ---------- | ------------------ |
|                     |            |                    |

**Poslovni ili domenski rizici, ako su značajni**

| Rizik | Posljedica | Trenutačni pristup |
| ----- | ---------- | ------------------ |
|       |            |                    |

<details>
<summary><strong>Prepoznavanje ograničenja bez izmišljanja produkcijske infrastrukture</strong></summary>

**Primjer — CrisisMap, tehnički rizici:**

| Rizik ili kompromis                                    | Posljedica                                                                     | Trenutačni pristup                                                                                         |
| ------------------------------------------------------ | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------- |
| Jedna instanca aplikacije obrađuje podnošenje prijava. | Kvar smještaja aplikacije privremeno onemogućuje nove prijave.                 | Prihvaćeno u okviru projekta kolegija; tim ne tvrdi da postoji automatsko prebacivanje na pričuvni sustav. |
| Vanjske kartografske pločice nisu dostupne.            | Prikaz karte može prestati raditi iako su pohranjene prijave i dalje dostupne. | Objasnite korisniku vidljivu pogrešku ili alternativu koju implementirana aplikacija stvarno pruža.        |

**Primjer — CrisisMap, domenski rizik:** Čitatelji mogu neprovjerenu prijavu građanina zamijeniti za službeno upozorenje. Tim može u sučelju jasno razlikovati status provjere prijave i uputiti korisnike na službene izvore.

**Analiza primjera:** Prvi tehnički rizik prihvaća realistično ograničenje projekta kolegija; domenski se rizik odnosi na način na koji čitatelj tumači prijavu. Zabilježite ograničenje i realan odgovor koji pripadaju vašem projektu. Neostvarena obavezna funkcionalnost nije tehnički dug nego nedovršen opseg.

</details>

**Završna provjera:** Usporedite dijagrame komponenata, podataka, razreda, sekvencijski dijagram i dijagram stanja s implementiranim kodom, ponašanjem aplikacije i stvarnim razmještajem. Ažurirajte zastarjele opise oblikovanja i objasnite svaku važnu razliku koja ostaje; poveznice na odgovarajući kod ili ispitivanja korisne su kada odgovaraju na konkretno pitanje.
