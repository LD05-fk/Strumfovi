# 3. Obrasci uporabe

**Cilj:** Prikazati interakcije između aktera i sustava ArtConnect. Svaki obrazac opisuje kako akter ostvaruje svoj cilj i što se događa kad važan korak ne uspije. Obrasci su izvedeni iz odobrenih funkcijskih zahtjeva ([analiza zahtjeva](2-Analiza-zahtjeva.md#21-funkcijski-zahtjevi)).

## 3.1 Dijagram obrazaca uporabe

![Pregledni dijagram obrazaca uporabe](assets/diagrams/3-1-artconnect-system-UC.svg)

**Slika 3.1. Dijagram obrazaca uporabe sustava ArtConnect** · **PlantUML izvor:** [izvor](puml/3-1-artconnect-system-UC.puml)

Detaljni dijagrami po skupinama obrazaca:

- [Računi i portfolio](puml/3-1-artconnect-racuni-UC.puml) (`assets/diagrams/3-1-artconnect-racuni-UC.svg`)
- [Prilike](puml/3-1-artconnect-prilike-UC.puml) (`assets/diagrams/3-1-artconnect-prilike-UC.svg`)
- [Prijave i rezultati](puml/3-1-artconnect-prijave-UC.puml) (`assets/diagrams/3-1-artconnect-prijave-UC.svg`)
- [Izvještaji i administracija](puml/3-1-artconnect-izvjestaji-UC.puml) (`assets/diagrams/3-1-artconnect-izvjestaji-UC.svg`)

| ID | Obrazac uporabe | Akter(i) |
| --- | --- | --- |
| UC-01 | Prijaviti se i upravljati računom | Posjetitelj, prijavljeni korisnik, Google OAuth 2.0 |
| UC-02 | Urediti profil i portfolio | Umjetnik, organizator |
| UC-03 | Pregledati javne profile i portfolije | Posjetitelj |
| UC-04 | Upravljati prilikama | Organizator, OpenStreetMap / Nominatim, Planer (automatsko zatvaranje) |
| UC-05 | Pregledati prilike | Posjetitelj, umjetnik, OpenStreetMap / Nominatim |
| UC-06 | Postaviti uvjete i prijaviti se na priliku | Organizator, umjetnik |
| UC-07 | Odlučivati o prijavama | Organizator, umjetnik |
| UC-08 | Objaviti rezultate i dodati u kalendar | Organizator, umjetnik, Google Calendar |
| UC-09 | Pregledati statistiku i izvještaje | Organizator, administrator, Planer |
| UC-10 | Upravljati platformom | Administrator |
| UC-11 | Pregledati obavijesti **(opcionalno)** | Prijavljeni korisnik |
| UC-12 | Pregledati dnevnik administratorskih radnji **(opcionalno)** | Administrator |

Veze obrazaca sa zahtjevima su u §3.3 i u [sljedivosti zahtjeva](2-Analiza-zahtjeva.md#26-sljedivost-zahtjeva).

Obrasci UC-06 do UC-10 isporučuju se u 2. ciklusu. Opisani su sažeto, kao dogovoreno ponašanje; detalji će se dopuniti uz implementaciju.

## 3.2 Specifikacije obrazaca uporabe

### UC-01 Prijaviti se i upravljati računom

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Posjetitelj (prijava); prijavljeni korisnik (upravljanje računom) |
| **Vanjski akter** | Google OAuth 2.0 |
| **Povezani zahtjevi** | FR-01 |
| **Preduvjeti** | Korisnik ima Google račun. |
| **Glavni uspješni tok** | 1. Posjetitelj se prijavi Google računom.<br>2. Sustav stvara ili prepoznaje korisnika i uspostavlja sesiju.<br>3. Novi korisnik odabire ulogu umjetnika ili organizatora (`POST /api/v1/me/role`).<br>4. Sustav aktivira račun i preusmjerava korisnika na profil. |
| **Alternativni tokovi** | **3a — Uloga je već odabrana:** sustav vraća `409 ROLE_ALREADY_CHOSEN`.<br>**3b — Prije odabira uloge:** ostale rute vraćaju `403 ONBOARDING_REQUIRED`.<br>**3c — Pozvani korisnik:** unaprijed dodijeljena uloga aktivira se prijavom, a odabir se preskače. |
| **Rezultat** | Korisnik ima ulogu i aktivan račun. |

Detalji su u [API dokumentaciji prijave](api/auth.md).

#### Brisanje vlastitog računa (2. ciklus)

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Prijavljeni korisnik |
| **Povezani zahtjevi** | FR-01; AUTH-04 |
| **Preduvjeti** | Korisnik je prijavljen. |
| **Glavni uspješni tok** | 1. Korisnik odabire „Obriši račun” i potvrđuje radnju upisom riječi „OBRISI”.<br>2. Sustav (`DELETE /api/v1/me`) anonimizira osobne podatke: ime postaje „Obrisani korisnik”, a e-adresa i Google identitet brišu se.<br>3. Sustav poništava sesiju i vraća korisnika na početnu stranicu. |
| **Alternativni tokovi** | **1a — Korisnik odustane:** račun ostaje nepromijenjen.<br>**2a — Organizator ima otvorene prilike:** prilike se zatvaraju, ali ostaju vidljive uz autora „Obrisani korisnik”.<br>**2b — Umjetnik ima prijave:** prijave i rezultati ostaju, ali bez osobnih podataka i priloga. |
| **Rezultat** | Račun je anonimiziran; podaci drugih korisnika (prijave, rezultati) ostaju sačuvani. |


### UC-02 Urediti profil i portfolio

| Element specifikacije | Opis                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| :--- |-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Primarni akter** | Umjetnik, organizator                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Povezani zahtjevi** | FR-02                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Preduvjeti** | Korisnik je prijavljen s ulogom `ARTIST` ili `ORGANIZER`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| **Glavni uspješni tok** | 1. Umjetnik uređuje umjetnički profil (`PUT /api/v1/me/artist-profile`) i otvara popis vlastitih djela (`GET /api/v1/artists/{artistId}/artworks`).<br>2. Umjetnik odabire dodavanje djela, unosi naslov, godinu, tehniku, dimenzije, opis i vidljivost te sprema (`POST /api/v1/artworks`). Nakon stvaranja ostaje na obrascu spremljenog djela.<br>3. Umjetnik po želji dodaje JPG ili PNG slike spremljenom djelu (`POST /api/v1/artworks/{artworkId}/images`); postavlja alternativni tekst za svaku učitanu sliku. Može ukloniti postojeću sliku (`DELETE /api/v1/artworks/{artworkId}/images/{imageId}`).<br>4. Umjetnik uređuje postojeće djelo (`PUT /api/v1/artworks/{artworkId}`) ili ga odabire za brisanje s popisa ili obrasca.<br>5. Sustav sprema promjene i prikazuje ih na javnom profilu.<br>6. Organizator uređuje naziv organizacije (`PUT /api/v1/me/organizer-profile`). |
| **Alternativni tokovi** | **1a — Umjetnik pregledava vlastiti popis:** prikazuju se sva njegova djela, uključujući `PRIVATE` i skrivena od administratora; prikazuju se najviše dva reda, a ostala djela dostupna su na sljedećim stranicama; straničenje se zadržava u URL-u.<br>**2a — Nevaljani unos:** klijentska validacija ili odgovor `400 VALIDATION_ERROR` prikazuje grešku uz odgovarajuće polje i ništa ne sprema.<br>**2b — Djelo s vidljivošću `PRIVATE`:** ne prikazuje se javno.<br>**2c — Prazno polje dimenzija ili opisa:** sprema se kao `null` i briše prethodnu vrijednost.<br>**2d — Djelo nije pronađeno:** sustav prikazuje obavijest „Djelo nije pronađeno” i povratak na portfolio. Tuđe javno djelo ne tretira se kao nepostojeće.<br>**2e — Nevaljana ili prevelika slika:** klijentska provjera prikazuje poruku prije slanja; odgovor poslužitelja za nepodržani tip (`415`), preveliku datoteku (`413`) ili dosegnuti limit slika (`409`) prikazuje odgovarajuću poruku, bez prekoračenja limita.<br>**2f — Učitavanje više slika:** slike se šalju jedna po jedna i postupak staje na prvoj pogrešci.<br>**3a — Nespremljene promjene:** pri napuštanju obrasca sustav traži potvrdu prije odbacivanja unosa.<br>**3b — Brisanje djela:** sustav traži potvrdu; nakon potvrde briše djelo i slike. Ako je uklonjena posljednja stavka na stranici, sustav prikazuje prethodnu stranicu.<br>**3c — Brisanje ne uspije:** djelo ostaje prikazano, a sustav obavještava umjetnika o pogrešci. |
| **Rezultat** | Profil i portfolio su ažurirani; umjetnik može pregledavati, dodavati, uređivati i brisati vlastita djela i njihove slike. |

Detalji su u [API dokumentaciji umjetnika](api/artists.md) i [djela](api/artworks.md).

### UC-03 Pregledati javne profile i portfolije

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Posjetitelj |
| **Povezani zahtjevi** | FR-02, FR-07 |
| **Preduvjeti** | Nema; pregled ne traži prijavu. |
| **Glavni uspješni tok** | 1. Posjetitelj otvara javnu poveznicu `/artists/{userId}`; prijava nije potrebna.<br>2. Sustav prikazuje profil umjetnika, javna djela i prethodne izložbe, uz paginaciju galerije.<br>3. Posjetitelj otvara djelo u dijalogu i pregledava njegove slike. |
| **Alternativni tokovi** | **2a — Djelo nije javno ili ga je administrator sakrio:** djelo se ne prikazuje; vlasniku se u javnom prikazu također prikazuju samo javna i neskrivena djela.<br>**2b — Profil nema javnih djela:** prikazuje se poruka da umjetnik još nema javnih djela.<br>**2c — Zatražena stranica galerije izvan je raspona:** URL se ne preusmjerava; prikazuje se prazno stanje s mogućnošću povratka na dostupnu stranicu.<br>**2d — Profil ne postoji ili više nije dostupan:** sustav prikazuje „Umjetnik nije pronađen”.<br>**2e — Greška poslužitelja:** sustav prikazuje grešku s mogućnošću ponovnog pokušaja.<br>**3a — Dijalog djela:** zatvara se tipkom Escape, a slike se mijenjaju strelicama; zatvaranjem se vraća fokus na otvoreno djelo. |
| **Rezultat** | Posjetitelj vidi javni portfolio umjetnika, a vlasnik može otvoriti uređivanje profila ili kopirati javnu poveznicu. |

### UC-04 Upravljati prilikama

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (vlasnik prilike) |
| **Vanjski akter** | OpenStreetMap / Nominatim (geokodiranje adrese), Planer (automatsko zatvaranje prijava nakon roka) |
| **Povezani zahtjevi** | FR-03, FR-04 |
| **Preduvjeti** | Organizator ima aktivan račun s ulogom `ORGANIZER` i prijavljenu sesiju. |
| **Glavni uspješni tok** | 1. Organizator unosi naslov, opis, vrstu, područja umjetnosti, rok i lokaciju (fizička s koordinatama ili online s poveznicom); sustav sprema nacrt u statusu `PREPARATION`.<br>2. Organizator uređuje priliku (`PUT /api/v1/opportunities/{id}`); status ostaje isti.<br>3. Organizator otvara „Moje prilike” na `/my-opportunities`; sustav dohvaća njegove prilike (`GET /api/v1/me/opportunities`) i omogućuje filtriranje po statusu i odabir aktivnih ili arhiviranih prilika.<br>4. Organizator mijenja status prilike (`POST /api/v1/opportunities/{id}/status`): `PREPARATION` → `OPEN`, `OPEN` → `CLOSED` ili `CLOSED` → `SELECTION`.<br>5. Organizator briše priliku ili je arhivira. |
| **Alternativni tokovi** | **1a — Nevaljani unos ili rok u prošlosti:** sustav odbija spremanje (`400`).<br>**1b — Adresa nije pronađena:** organizator ručno odabire koordinate na karti.<br>**3a — Pregled bez prilika:** prazan popis uz podatke o stranici.<br>**4a — Nedopušten prijelaz** (npr. isti status ili `FINISHED`): `409 INVALID_STATUS_TRANSITION`, a `FINISHED` postiže se samo objavom rezultata (UC-08).<br>**4b — Otvaranje s rokom u prošlosti:** `409 DEADLINE_PASSED`; prijelaz `PREPARATION` → `OPEN` i `CLOSED` → `OPEN` traže rok u budućnosti.<br>**4c — Objava bez odobrenja:** dok ADM-02-BE nije gotov, svi organizatori smatraju se odobrenima; nakon toga prvo otvaranje bez odobrenja vraća `403 ORGANIZER_NOT_APPROVED`.<br>**4d — Arhivirana prilika:** svaka promjena statusa odbijena je s `409 INVALID_STATUS_TRANSITION`.<br>**5a — Prilika ima prijave:** brisanje je odbijeno (`409 HAS_APPLICATIONS`), a organizator može samo arhivirati.<br>**5b — Automatsko zatvaranje:** Planer zatvara prijave kad rok istekne (zadatak OPP-04, 2. ciklus). |
| **Rezultat** | Prilika je spremljena, uređena, arhivirana ili obrisana. |

Detalji su u [API dokumentaciji prilika](api/opportunities.md), za [pregled organizatorovih prilika](api/opportunities.md#get-apiv1meopportunities), [promjenu statusa](api/opportunities.md#post-opportunitiesopportunityidstatus), [arhiviranje](api/opportunities.md#post-opportunitiesopportunityidarchive) i [brisanje](api/opportunities.md#delete-opportunitiesopportunityid).

#### Objava prilike

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator |
| **Povezani zahtjevi** | FR-03; OPP-01-FE2; OPP-01-FE3 |
| **Preduvjeti** | Korisnik je prijavljen s ulogom `ORGANIZER`. Ruta `/organizer/opportunities/new` zaštićena je provjerom uloge. |
| **Glavni uspješni tok** | 1. Organizator otvara obrazac za novu priliku.<br>2. Sustav dohvaća aktivne vrste prilika i područja umjetnosti (`GET /api/v1/opportunity-types`, `GET /api/v1/art-fields`).<br>3. Organizator unosi naslov, opis, vrstu, jedno do deset područja, rok prijave te po želji kriterije, nagradu i datume događanja.<br>4. Organizator odabire fizičku ili online lokaciju. Za fizičku unosi ulicu, poštanski broj i grad, zatim po želji pokreće geokodiranje gumbom „Pronađi na karti” (`GET /api/v1/geocoding/search`) i odabire rezultat ili ručno postavlja oznaku na karti. Za online lokaciju unosi HTTP(S) poveznicu.<br>5. Sustav provjerava unos prema pravilima `OpportunityInput`. Rok mora biti u budućnosti; kraj događanja može se unijeti samo uz početak i ne može mu prethoditi.<br>6. Datumi i vremena prikazuju se u vremenskoj zoni `Europe/Zagreb` i šalju kao ISO-8601 vrijednosti s pomakom.<br>7. Organizator sprema obrazac (`POST /api/v1/opportunities`). Sustav stvara priliku u statusu `PREPARATION`, osvježava relevantne podatke, prikazuje poruku „Prilika je spremljena” i vodi na `/opportunities/{id}`. |
| **Alternativni tokovi** | **2a — Dohvat nije uspio:** sustav prikazuje stanje pogreške i omogućuje ponovni pokušaj.<br>**4a — Adresa nije pronađena:** sustav prikazuje poruku, a organizator ručno postavlja oznaku klikom na kartu ili gumbom „Postavi oznaku u središte karte”; oznaku može povući radi ispravka točke.<br>**4b — Geokodiranje vraća `429 RATE_LIMITED`:** sustav obavještava da je previše upita i predlaže ponovni pokušaj za trenutak.<br>**4c — Geokodiranje vraća `502 GEOCODING_UNAVAILABLE`:** sustav obavještava da usluga karte nije dostupna; organizator može ručno postaviti oznaku.<br>**4d — Promjena adrese nakon geokodiranja:** oznaka ostaje postavljena, a sustav savjetuje ponovno traženje na karti.<br>**5a — Nevaljan unos:** poruke validacije prikazuju se uz pripadajuća polja, a zahtjev se ne šalje.<br>**7a — API vraća `400 VALIDATION_ERROR`:** detalji pogreške mapiraju se na polja obrasca.<br>**7b — API vraća `403`:** sustav obavještava organizatora da nema pristup.<br>**7c — Druga pogreška poslužitelja:** prikazuje se obavijest o pogrešci; unesene vrijednosti ostaju u obrascu.<br>**8a — Pokušaj napuštanja s nespremljenim promjenama:** sustav traži potvrdu prije odbacivanja unosa. |
| **Rezultat** | Nova prilika spremljena je kao nacrt. |

#### Uređivanje prilike

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (vlasnik prilike) |
| **Povezani zahtjevi** | FR-03; OPP-01-FE2; OPP-01-FE3 |
| **Preduvjeti** | Organizator je prijavljen s ulogom `ORGANIZER`, a prilika postoji i pripada mu. |
| **Glavni uspješni tok** | 1. Organizator otvara `/organizer/opportunities/{id}/edit`.<br>2. Sustav dohvaća priliku (`GET /api/v1/opportunities/{id}`) i aktivne šifrarnike te popunjava obrazac postojećim podacima.<br>3. Ako trenutačna vrsta ili područje umjetnosti nije među aktivnim opcijama, i dalje se prikazuje kao odabrano s oznakom „(neaktivno)” i ostaje u obrascu.<br>4. Postojeća fizička lokacija prikazuje se s oznakom na spremljenim koordinatama; organizator može urediti adresu, ponovno geokodirati ili ručno ispraviti položaj. Za online lokaciju uređuje HTTP(S) poveznicu.<br>5. Organizator uređuje osnovna polja i datume. Pri uređivanju rok može biti u prošlosti; ostala pravila polja i datuma događanja vrijede kao pri stvaranju.<br>6. Organizator sprema promjene (`PUT /api/v1/opportunities/{id}`). Sustav osvježava relevantne podatke, prikazuje poruku „Prilika je spremljena” i vodi na `/opportunities/{id}`. |
| **Alternativni tokovi** | **2a — Dohvat je u tijeku:** sustav prikazuje stanje učitavanja.<br>**2b — API vraća `403` ili `404`:** sustav prikazuje odgovarajuću poruku i poveznicu na „Moje prilike”.<br>**2c — Druga pogreška poslužitelja:** sustav prikazuje stanje pogreške s mogućnošću ponovnog pokušaja.<br>**4a — Adresa nije pronađena:** organizator ručno postavlja ili ispravlja oznaku na karti.<br>**4b — Geokodiranje vraća `429 RATE_LIMITED` ili `502 GEOCODING_UNAVAILABLE`:** sustav prikazuje odgovarajuću poruku; fizičku lokaciju moguće je postaviti ručno.<br>**5a — Nevaljan unos ili API `400 VALIDATION_ERROR`:** pogreške se prikazuju uz pripadajuća polja, uključujući polja lokacije.<br>**6a — Pokušaj napuštanja s nespremljenim promjenama:** sustav traži potvrdu prije odbacivanja unosa. |
| **Rezultat** | Postojeća prilika ažurirana je; njezin status i vlasnik ostaju nepromijenjeni. |

**Opseg OPP-01-FE2 i OPP-01-FE3:** zajednički obrazac obuhvaća osnovna polja, datume, spremanje i lokaciju. Lokacija podržava fizički ili online odabir, eksplicitno pokretano geokodiranje, izbor rezultata te ručno postavljanje i pomicanje oznake na karti. „Zahtjevi prijave” ostaju mjesto za uključivanje funkcionalnosti APP-01-FE1.

#### Promjena statusa prilike

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (vlasnik prilike) |
| **Povezani zahtjevi** | FR-04; OPP-03 |
| **Preduvjeti** | Organizator je prijavljen s ulogom `ORGANIZER`, a prilika pripada njegovu računu. Stranici „Moje prilike” pristupa na `/my-opportunities`; za promjenu statusa prilika može biti otvorena i iz popisa ili iz vlastitog detalja. |
| **Glavni uspješni tok** | 1. Sustav dohvaća organizatorove prilike (`GET /api/v1/me/opportunities`) i prikazuje kartice „Aktivne” i „Arhivirane”, filtar statusa te straničenje.<br>2. Organizator odabire dopuštenu statusnu radnju u traci radnji. Za `PREPARATION` nudi se „Objavi” (`OPEN`), za `OPEN` „Zatvori prijave” (`CLOSED`), a za `CLOSED` „Započni odabir” (`SELECTION`). „Ponovno otvori prijave” (`OPEN`) nudi se za `CLOSED` samo dok je rok u budućnosti. Za `SELECTION` i `FINISHED` ne nude se statusne radnje. Arhivirana prilika nema statusnih radnji.<br>3. Sustav traži potvrdu radnje. Organizator je potvrđuje, a sustav šalje `POST /api/v1/opportunities/{id}/status` s ciljnim statusom.<br>4. Sustav sprema promjenu, osvježava popis i detalj prilike te prikazuje potvrdu uspjeha. |
| **Alternativni tokovi** | **1a — Nema prilika:** sustav prikazuje prazno stanje i poveznicu za stvaranje nove prilike (`/organizer/opportunities/new`).<br>**1b — Dohvat traje ili ne uspije:** sustav prikazuje stanje učitavanja odnosno grešku s mogućnošću ponovnog pokušaja.<br>**2a — Rok je istekao:** ponovno otvaranje nije ponuđeno; ako je rok istekao prije slanja zahtjeva, API vraća `409 DEADLINE_PASSED` i sustav upućuje organizatora da produlji rok uređivanjem prilike.<br>**3a — Organizator odustane:** dijalog se zatvara bez slanja zahtjeva i prilika ostaje nepromijenjena.<br>**4a — Prijelaz više nije dopušten ili je prilika arhivirana:** API vraća `409 INVALID_STATUS_TRANSITION`; sustav prikazuje poruku i osvježava podatke.<br>**4b — Organizator nema odobrenje za objavu:** kada se provjera odobrenja aktivira u ADM-02-BE, API vraća `403 ORGANIZER_NOT_APPROVED`, a sustav prikazuje poruku poslužitelja.<br>**4c — Ostala greška poslužitelja:** sustav prikazuje obavijest o grešci, a podaci se ponovno dohvaćaju prema potrebi. |
| **Rezultat** | Status prilike promijenjen je dopuštenim prijelazom; vlasnik prilike ostaje isti. |

#### Brisanje ili arhiviranje prilike

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (vlasnik prilike) |
| **Povezani zahtjevi** | FR-04; OPP-03 |
| **Preduvjeti** | Organizator je prijavljen s ulogom `ORGANIZER`, a prilika pripada njegovu računu. Radnja je dostupna iz trake radnji na popisu „Moje prilike” (`/my-opportunities`) ili na detalju prilike. |
| **Glavni uspješni tok** | 1. Organizator odabire „Obriši”.<br>2. Sustav prikazuje dijalog za potvrdu brisanja i upozorava da se radnja ne može poništiti.<br>3. Organizator potvrđuje brisanje; sustav šalje `DELETE /api/v1/opportunities/{id}`.<br>4. API potvrđuje brisanje (`204 No Content`), sustav osvježava popis i uklanja priliku iz prikaza. Ako je radnja pokrenuta s detalja, sustav vodi organizatora na `/my-opportunities`. |
| **Alternativni tokovi** | **1a — Prilika je već arhivirana:** „Obriši” ostaje dostupno, ali se ne nude statusne radnje ni dodatna radnja „Arhiviraj”.<br>**2a — Organizator odustane:** dijalog se zatvara, bez slanja zahtjeva i promjene prilike.<br>**3a — Prilika ima prijave:** API vraća `409 HAS_APPLICATIONS`; sustav ne briše priliku i u dijalogu nudi arhiviranje. Ako organizator odabere „Arhiviraj”, sustav šalje `POST /api/v1/opportunities/{id}/archive`.<br>**3b — Brisanje ili arhiviranje ne uspije:** sustav prikazuje obavijest o grešci i zadržava priliku u prikazu.<br>**3c — Prilika ne postoji ili ne pripada organizatoru:** API vraća `404 NOT_FOUND` odnosno `403 FORBIDDEN`; sustav prikazuje odgovarajuću poruku i osvježava prikaz. |
| **Rezultat** | Prilika je obrisana ili arhivirana. Arhiviranje je ne vraća u popis aktivnih prilika; status se ne mijenja osim što se prilika u statusu `OPEN` pri arhiviranju zatvara (`CLOSED`). |

### UC-05 Pregledati prilike

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Posjetitelj, umjetnik |
| **Vanjski akter** | OpenStreetMap / Nominatim (karta i pretraga lokacija) |
| **Povezani zahtjevi** | FR-04 |
| **Preduvjeti** | Nema; pregled ne traži prijavu. |
| **Glavni uspješni tok** | 1. Korisnik otvara `/opportunities`; prikazuju se otvorene prilike (zadani status `OPEN`), poredane po najbližem roku, 20 po stranici.<br>2. Sužava pretragu filtrima: ključna riječ, područja umjetnosti, vrste prilike, grad, vrsta lokacije, status i raspon roka prijave. Ključna riječ i grad šalju se tek kad korisnik prestane tipkati, a svaka promjena filtra vraća pretragu na prvu stranicu.<br>3. Mijenja redoslijed (rok prijave ili vrijeme objave) i prelazi među stranicama rezultata.<br>4. Po potrebi prebacuje prikaz s popisa na kartu, koja prikazuje do 100 rezultata s koordinatama.<br>5. Otvara pojedinu priliku s kartice ili iz skočnog opisa na karti.<br>6. Adresu pretrage može podijeliti ili spremiti: filtri, sortiranje, stranica i prikaz zapisani su u URL upitu, pa i gumbi natrag i naprijed vraćaju prethodne pretrage. |
| **Alternativni tokovi** | **2a — Arhivirana ili neobjavljena prilika:** ne prikazuje se u javnoj pretrazi.<br>**2b — Nevaljan raspon roka:** poruka se prikazuje uz polje "Rok od" i zahtjev se ne šalje. Ako takav raspon stigne iz dijeljene adrese, API vraća `400 VALIDATION_ERROR`, a poruka se prikazuje uz filtre.<br>**2c — Nema rezultata:** prazno stanje. Uz postavljene filtre nudi se "Očisti filtre", a bez njih poruka da još nema objavljenih prilika.<br>**3a — Stranica izvan raspona:** prazno stanje s povratkom na prvu stranicu.<br>**4a — Prilika bez koordinata (online):** ne prikazuje se na karti. Obavijest javlja koliko je takvih prilika i je li prikazan samo dio rezultata.<br>**4b — Kartografske pločice nisu dostupne:** sustav prikazuje upozorenje.<br>**5a — Dohvaćanje nije uspjelo:** prikazuje se poruka API-ja i "Pokušaj ponovno". |
| **Rezultat** | Korisnik vidi javno objavljene prilike. Rokovi i datumi prikazani su u zoni `Europe/Zagreb`. |

#### Pregled detalja prilike

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Posjetitelj, umjetnik, organizator (vlasnik prilike) |
| **Vanjski akter** | OpenStreetMap (karta i poveznica na lokaciju) |
| **Povezani zahtjevi** | FR-04; OPP-02-BE1; OPP-02-DES; OPP-01-FE1 |
| **Preduvjeti** | Prilika postoji i vidljiva je korisniku; prijava nije obavezna za javno vidljive prilike. |
| **Glavni uspješni tok** | 1. Korisnik otvara `/opportunities/{id}`.<br>2. Sustav dohvaća detalj (`GET /api/v1/opportunities/{id}`) i, ako postoji prijavljena sesija, trenutnog korisnika (`GET /api/v1/me`).<br>3. Sustav prikazuje naslov, organizatora, status, vrstu, područja umjetnosti, opis, kriterije, nagradu, rok i datume događanja. Datumi i vremena prikazuju se u vremenskoj zoni `Europe/Zagreb`; opis, kriteriji i nagrada prikazuju se kao običan tekst.<br>4. Za fizičku lokaciju sustav prikazuje adresu, kartu s nepomičnom oznakom, atribuciju OpenStreetMap i poveznicu za otvaranje lokacije na OpenStreetMapu. Za online lokaciju prikazuje vanjsku poveznicu u novoj kartici.<br>5. Ako je korisnik vlasnik prilike, sustav prikazuje vlasničku traku i oznake statusa; za priliku u pripremi prikazuje „U pripremi - vidljivo samo vama”. Akcije vlasnika, gumb „Prijavi se” i rezultati prikazuju se kao mjesta za uključivanje pripadajućih funkcionalnosti. |
| **Alternativni tokovi** | **2a - Dohvat je u tijeku:** sustav prikazuje stanje učitavanja.<br>**2b - Prilika ne postoji ili nije vidljiva korisniku (`404 NOT_FOUND`):** sustav prikazuje poruku „Prilika ne postoji ili nije dostupna” i poveznicu na popis prilika.<br>**2c - Druga pogreška poslužitelja:** sustav prikazuje poruku pogreške i omogućuje ponovni pokušaj.<br>**4a - Kartografske pločice nisu dostupne:** sustav prikazuje upozorenje, a poveznica na OpenStreetMap ostaje dostupna ako su koordinate poznate. |
| **Rezultat** | Korisnik vidi dostupne detalje prilike i njezinu lokaciju; vlasnik vidi dodatne oznake vlasništva. |

### UC-06 Postaviti uvjete i prijaviti se na priliku

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (uvjeti), umjetnik (prijava) |
| **Povezani zahtjevi** | FR-05 |
| **Preduvjeti** | Prilika postoji; za predaju prijave prilika je u statusu `OPEN`. |
| **Glavni uspješni tok** | 1. Organizator određuje što prijava mora sadržavati.<br>2. Umjetnik predaje prijavu prema tim uvjetima.<br>3. Umjetnik prati prijavu, dopunjuje je ili povlači. |
| **Alternativni tokovi** | **2a — Prilika nije otvorena:** predaja nije moguća. |
| **Rezultat** | Uvjeti su spremljeni, a prijava predana i vidljiva umjetniku i organizatoru. |

**Stanja prijave:** „Predana” (`SUBMITTED`), „U pregledu” (`UNDER_REVIEW`), „Potrebna dopuna” (`NEEDS_INFO`), „Prihvaćena” (`ACCEPTED`), „Odbijena” (`REJECTED`) i „Povučena” (`WITHDRAWN`). Prve tri su neodlučene, a zadnje tri završne.

#### Postavljanje uvjeta prijave

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (vlasnik prilike) |
| **Povezani zahtjevi** | FR-05; APP-01, APP-05 |
| **Preduvjeti** | Organizator je prijavljen i prilika mu pripada. |
| **Glavni uspješni tok** | 1. Organizator otvara uvjete prijave svoje prilike.<br>2. Dodaje stavke: naziv, vrstu (tekst, poveznica, datoteka ili djelo iz portfolija), je li obavezna i najveći broj priloga.<br>3. Sprema uvjete (`PUT /api/v1/opportunities/{id}/requirements`). |
| **Alternativni tokovi** | **2a — Previše datoteka:** zbroj datotečnih stavki veći od 10 odbija se (`400 VALIDATION_ERROR`).<br>**3a — Izmjena nakon prvih prijava:** uvjeti se smiju mijenjati; neodlučene prijave koje ih više ne zadovoljavaju automatski prelaze u „Potrebna dopuna” s porukom što nedostaje. Prihvaćene i odbijene prijave ne mijenjaju se. |
| **Rezultat** | Uvjeti su spremljeni i prikazuju se na detalju prilike. |

#### Predaja prijave

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Umjetnik |
| **Povezani zahtjevi** | FR-05; APP-02 |
| **Preduvjeti** | Umjetnik je prijavljen, a prilika je u statusu `OPEN`. |
| **Glavni uspješni tok** | 1. Umjetnik na detalju prilike odabire „Prijavi se”.<br>2. Popunjava stavke: tekst, poveznice, datoteke ili vlastita djela (i privatna, koja tada vidi samo organizator te prilike).<br>3. Predaje prijavu (`POST /api/v1/opportunities/{id}/applications`); status je „Predana”.<br>4. Prijava se prikazuje u „Mojim prijavama”. |
| **Alternativni tokovi** | **2a — Nedostaje obavezna stavka ili je priloga previše:** `400 VALIDATION_ERROR`, greška se prikazuje uz stavku.<br>**2b — Djelo je sakrio administrator:** ne može se priložiti.<br>**3a — Prilika u međuvremenu nije otvorena:** predaja je odbijena (`409`). |
| **Rezultat** | Prijava je predana i organizator je vidi. |

#### Dopuna ili povlačenje prijave

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Umjetnik |
| **Povezani zahtjevi** | FR-05; APP-03 |
| **Preduvjeti** | Umjetnik ima neodlučenu prijavu. |
| **Glavni uspješni tok** | 1. Umjetnik otvara prijavu i vidi status, povijest i poruku organizatora.<br>2. Ako je status „Potrebna dopuna”, dopunjuje stavke i ponovno predaje (`PUT /api/v1/applications/{id}`); status je opet „Predana”.<br>3. Umjesto toga može povući prijavu uz potvrdu (`POST /api/v1/applications/{id}/withdraw`); status je „Povučena”. |
| **Alternativni tokovi** | **2a — Prijava nije u statusu „Potrebna dopuna”:** dopuna nije ponuđena.<br>**3a — Prijava je već odlučena:** povlačenje nije moguće (`409`).<br>**3b — Nova prijava nakon povlačenja:** dopuštena dok je prilika otvorena; stara ostaje kao „Povučena”. |
| **Rezultat** | Prijava je dopunjena i ponovno predana ili povučena. |

Detalji ruta i odgovora bit će u API dokumentaciji prijava.

### UC-07 Odlučivati o prijavama

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (vlasnik prilike) |
| **Sudionik** | Umjetnik (prati odluku) |
| **Povezani zahtjevi** | FR-06; APP-04 |
| **Preduvjeti** | Na priliku organizatora predana je barem jedna prijava. |
| **Glavni uspješni tok** | 1. Organizator otvara popis prijava na svoju priliku (`GET /api/v1/opportunities/{id}/applications`) i po želji filtrira po statusu.<br>2. Otvara prijavu i pregledava priloge.<br>3. Mijenja status (`POST /api/v1/applications/{id}/status`) u „U pregledu”, „Potrebna dopuna” (uz poruku), „Prihvaćena” ili „Odbijena” (uz razlog). Status smije mijenjati i dok je prilika otvorena.<br>4. Sustav bilježi promjenu u povijesti prijave, a umjetnik vidi novi status i poruku. |
| **Alternativni tokovi** | **1a — Nema prijava:** prikazuje se prazno stanje.<br>**3a — Nedostaje poruka ili razlog:** za „Potrebna dopuna” i „Odbijena” promjena se ne sprema (`400 VALIDATION_ERROR`).<br>**3b — Nedopušten prijelaz** (npr. prijava je već odlučena ili povučena): `409 INVALID_STATUS_TRANSITION`.<br>**3c — Tuđa prilika:** `403 FORBIDDEN`. |
| **Rezultat** | Prijava ima novi status, a umjetnik vidi odluku. |

### UC-08 Objaviti rezultate i dodati u kalendar

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (objava), umjetnik (pregled i kalendar) |
| **Vanjski akter** | Google Calendar |
| **Povezani zahtjevi** | FR-07 |
| **Preduvjeti** | Prilika je u statusu `SELECTION` i ima prihvaćene prijave. |
| **Glavni uspješni tok** | 1. Organizator objavljuje rezultate.<br>2. Sustav prikazuje rezultate i dodaje sudjelovanje na portfolio umjetnika.<br>3. Prihvaćeni umjetnik dodaje događanje u svoj Google kalendar. |
| **Alternativni tokovi** | **1a — Ispravak rezultata:** promjena se vidi i na portfoliju. |
| **Rezultat** | Rezultati su vidljivi, a umjetnik može dodati događanje u kalendar. |

#### Objava rezultata

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (vlasnik prilike) |
| **Povezani zahtjevi** | FR-07; RES-01, RES-02, RES-03 |
| **Preduvjeti** | Prilika je u statusu `SELECTION`. |
| **Glavni uspješni tok** | 1. Organizator odabire „Objavi rezultate”.<br>2. Upisuje tekst objave i odabire sudionike među umjetnicima s prihvaćenom prijavom; plasman (1.–3. mjesto) je opcionalan.<br>3. Potvrđuje objavu (`PUT /api/v1/opportunities/{id}/results`).<br>4. Sustav sprema rezultate, prebacuje priliku u `FINISHED` i dodaje sudjelovanje na portfolio svakog sudionika. |
| **Alternativni tokovi** | **2a — Odabran umjetnik bez prihvaćene prijave:** `409 PARTICIPANT_NOT_ACCEPTED`.<br>**3a — Prilika nije u `SELECTION`:** `409 INVALID_STATUS_TRANSITION`.<br>**4a — Ispravak:** organizator kasnije mijenja rezultate istim obrascem, a promjena se prikazuje i na portfoliju. |
| **Rezultat** | Rezultati su objavljeni na detalju prilike, a prilika je završena. |

#### Dodavanje događanja u Google kalendar

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Umjetnik |
| **Vanjski akter** | Google Calendar |
| **Povezani zahtjevi** | FR-07; CAL-01, CAL-02 |
| **Preduvjeti** | Umjetnikova prijava je prihvaćena, a prilika ima datum početka događanja. |
| **Glavni uspješni tok** | 1. Umjetnik na prihvaćenoj prijavi odabire „Dodaj u Google kalendar”.<br>2. Sustav otvara Google kalendar s popunjenim naslovom, datumom i lokacijom.<br>3. Umjetnik sprema događaj u svoj kalendar. |
| **Alternativni tokovi** | **1a — Prilika nema datum početka:** gumb se ne prikazuje.<br>**2a — Nema datuma završetka:** događaj traje jedan sat.<br>**2b — Izravno umetanje (opcionalno):** uz odobrenje korisnika sustav sam dodaje događaj putem Google Calendar API-ja. |
| **Rezultat** | Događanje je u umjetnikovu kalendaru. |

### UC-09 Pregledati statistiku i izvještaje

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (statistika), administrator (izvještaji) |
| **Vanjski akter** | Planer (mjesečni izvještaj) |
| **Povezani zahtjevi** | FR-08; STAT-01, REP-01, REP-02 |
| **Preduvjeti** | Korisnik je prijavljen s ulogom `ORGANIZER` ili `ADMIN`. |
| **Glavni uspješni tok** | 1. Organizator otvara „Statistika” (`GET /api/v1/me/statistics`) i vidi broj prijava po prilici i po području umjetnosti te broj prihvaćenih umjetnika.<br>2. Planer jednom mjesečno izrađuje izvještaj o korištenju: umjetnici, objavljene prilike i predane prijave, za svaku brojku novi u mjesecu i ukupno.<br>3. Administrator pregledava izvještaje (`GET /api/v1/admin/reports`). |
| **Alternativni tokovi** | **1a — Organizator nema prilika ni prijava:** prikazuju se nule.<br>**3a — Izvještaj još ne postoji:** prikazuje se prazno stanje.<br>**3b — Grafikoni, povijest i ručno generiranje izvještaja:** opcionalni. |
| **Rezultat** | Organizator vidi statistiku svojih prilika, a administrator mjesečne izvještaje. |

### UC-10 Upravljati platformom

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Administrator |
| **Povezani zahtjevi** | FR-09 |
| **Preduvjeti** | Administrator je prijavljen s ulogom `ADMIN`. |
| **Glavni uspješni tok** | 1. Administrator pregledava korisnike (`GET /api/v1/admin/users`) i filtrira ih po ulozi, statusu i odobrenju organizatora.<br>2. Odobrava organizatore i mijenja uloge.<br>3. Blokira i odblokira korisnike.<br>4. Sakriva i vraća neprimjeren sadržaj.<br>5. Uređuje šifrarnike i postavke. |
| **Alternativni tokovi** | **1a — Neispravni parametri filtra:** `400 VALIDATION_ERROR`. |
| **Rezultat** | Korisnici, sadržaj, šifrarnici i postavke su ažurirani. |

Detalji su u [API dokumentaciji administracije](api/admin.md).

#### Odobravanje organizatora i promjena uloge

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Administrator |
| **Povezani zahtjevi** | FR-09; ADM-01, ADM-02 |
| **Preduvjeti** | Administrator je prijavljen; korisnik postoji. |
| **Glavni uspješni tok** | 1. Administrator filtrira organizatore koji čekaju odobrenje.<br>2. Odobrava organizatora (`POST /api/v1/admin/users/{id}/approve-organizer`); organizator od tada smije objavljivati prilike.<br>3. Po potrebi mijenja ulogu korisnika (`POST /api/v1/admin/users/{id}/role`). |
| **Alternativni tokovi** | **2a — Korisnik ne čeka odobrenje:** `409 NOT_PENDING_ORGANIZER`.<br>**3a — Korisnik već ima tu ulogu:** `409 SAME_ROLE`.<br>**3b — Umjetnik postaje organizator:** njegove neodlučene prijave automatski se povlače; prihvaćene i odbijene ostaju. |
| **Rezultat** | Organizator je odobren ili korisnik ima novu ulogu. |

#### Blokiranje korisnika

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Administrator |
| **Povezani zahtjevi** | FR-09; ADM-01 |
| **Preduvjeti** | Administrator je prijavljen; korisnik je aktivan. |
| **Glavni uspješni tok** | 1. Administrator odabire „Blokiraj” uz potvrdu (`POST /api/v1/admin/users/{id}/block`).<br>2. Sustav prekida sesije korisnika i odbija njegovu prijavu, skriva njegov portfolio i prilike iz javnih prikaza, a otvorene prilike zatvara.<br>3. „Odblokiraj” (`POST /api/v1/admin/users/{id}/unblock`) vraća vidljivost; zatvorene prilike ostaju zatvorene. |
| **Alternativni tokovi** | **1a — Administrator pokuša blokirati sebe:** `409 CANNOT_BLOCK_SELF`.<br>**1b — Korisnik je već blokiran ili obrisan:** `409 INVALID_STATUS_TRANSITION`. |
| **Rezultat** | Korisnik je blokiran ili ponovno aktivan. |

#### Skrivanje i vraćanje sadržaja

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Administrator |
| **Povezani zahtjevi** | FR-09; ADM-03 |
| **Preduvjeti** | Administrator je prijavljen; prilika ili djelo postoji. |
| **Glavni uspješni tok** | 1. Administrator na prilici ili djelu odabire „Sakrij” (`POST /api/v1/admin/opportunities/{id}/hide`, odnosno `/artworks/{id}/hide`).<br>2. Sadržaj nestaje iz javnih prikaza; vlasnik ga i dalje vidi s oznakom da je skriven.<br>3. Na popisu skrivenog sadržaja (`GET /api/v1/admin/hidden-content`) administrator ga može vratiti (`.../restore`). |
| **Alternativni tokovi** | **1a — Sadržaj je već skriven:** radnja „Sakrij” nije ponuđena, nudi se „Vrati”.<br>**2a — Skriveno djelo:** ne može se priložiti novoj prijavi. |
| **Rezultat** | Sadržaj je skriven ili vraćen u javni prikaz. |

#### Uređivanje šifrarnika i postavki

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Administrator |
| **Povezani zahtjevi** | FR-09; ADM-04 |
| **Preduvjeti** | Administrator je prijavljen. |
| **Glavni uspješni tok** | 1. Administrator otvara područja umjetnosti ili vrste prilika.<br>2. Dodaje, preimenuje, deaktivira ili briše stavku.<br>3. U postavkama mijenja ograničenja učitavanja datoteka (najveća veličina i broj) (`PUT /api/v1/admin/settings`). |
| **Alternativni tokovi** | **2a — Stavka se koristi:** brisanje vraća `409 IN_USE`; stavku je moguće deaktivirati.<br>**2b — Šifra ili naziv već postoji:** `409 DUPLICATE_CODE`.<br>**3a — Nevaljana vrijednost postavke:** `400 VALIDATION_ERROR`. |
| **Rezultat** | Šifrarnici i postavke su ažurirani; deaktivirane stavke više se ne nude u obrascima. |

### UC-11 Pregledati obavijesti (opcionalno)

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Prijavljeni korisnik |
| **Povezani zahtjevi** | FR-10 |
| **Preduvjeti** | Korisnik je prijavljen. |
| **Glavni uspješni tok** | 1. Korisnik otvara popis obavijesti.<br>2. Sustav prikazuje obavijesti vezane uz njegov račun i aktivnosti.<br>3. Korisnik označava obavijest pročitanom (NOTIF-01, NOTIF-02). |
| **Alternativni tokovi** | **2a — Tuđe obavijesti:** nisu vidljive. |
| **Rezultat** | Korisnik vidi svoje obavijesti. |

### UC-12 Pregledati dnevnik administratorskih radnji (opcionalno)

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Administrator |
| **Povezani zahtjevi** | FR-09 (dodatna mogućnost niskog prioriteta) |
| **Proširuje** | UC-10 Upravljati platformom |
| **Preduvjeti** | Administrator je prijavljen s ulogom `ADMIN`. |
| **Glavni uspješni tok** | 1. Administrator otvara zapisnik administratorskih radnji (ADM-05).<br>2. Sustav prikazuje zapise radnji. |
| **Alternativni tokovi** | **2a — Ostali korisnici:** ne mogu pregledavati zapisnik. |
| **Rezultat** | Administrator vidi zapisnik radnji. |

## 3.3 Provjera dosljednosti obrazaca uporabe

**Obrasci i zahtjevi.** Svaki obrazac povezan je s barem jednim zahtjevom, a svaki zahtjev visokog prioriteta (FR-01–FR-06, FR-09) ima obrazac.

| Obrazac uporabe | Funkcijski zahtjevi | Ciklus |
| --- | --- | --- |
| UC-01 Prijaviti se i upravljati računom | FR-01 | 1.; brisanje računa u 2. |
| UC-02 Urediti profil i portfolio | FR-02 | 1. |
| UC-03 Pregledati javne profile i portfolije | FR-02, FR-07 | 1.; sudjelovanja u 2. |
| UC-04 Upravljati prilikama | FR-03, FR-04 | 1.; automatsko zatvaranje u 2. |
| UC-05 Pregledati prilike | FR-04 | 1. |
| UC-06 Postaviti uvjete i prijaviti se na priliku | FR-05 | 2. |
| UC-07 Odlučivati o prijavama | FR-06 | 2. |
| UC-08 Objaviti rezultate i dodati u kalendar | FR-07 | 2. |
| UC-09 Pregledati statistiku i izvještaje | FR-08 | 2. |
| UC-10 Upravljati platformom | FR-09 | 2. |
| UC-11 Pregledati obavijesti (opcionalno) | FR-10 | 2. |
| UC-12 Pregledati dnevnik administratorskih radnji (opcionalno) | FR-09 | 2. |

**Akteri i nazivi.** Akteri i nazivi obrazaca u tablici §3.1, na dijagramima i u specifikacijama su isti, a odgovaraju akterima iz [§2.3](2-Analiza-zahtjeva.md#23-dionici-i-akteri).

**Pronađene nedosljednosti i ispravci:**

| Nedosljednost | Ispravak |
| --- | --- |
| Promjena statusa i brisanje ili arhiviranje prilike bili su vezani uz FR-03, a upravljanje statusima pripada FR-04. | Podobrasci su povezani s FR-04. |
| Naziv vanjskog aktera bio je „OpenStreetMap/Nominatim” u tablicama, a „OpenStreetMap / Nominatim” na dijagramima. | Svugdje je „OpenStreetMap / Nominatim”. |
| UC-08 je kao preduvjet imao samo prihvaćene prijave, a rezultati se objavljuju tek u statusu prilike `SELECTION`. | Preduvjet uključuje status `SELECTION`. |
| UC-07 nije spominjao „Potrebna dopuna” ni obaveznu poruku ili razlog. | Dodani su svi statusi i pravilo o poruci. |
| Brisanje računa (FR-01) nije imalo opis toka. | Dodan je podobrazac „Brisanje vlastitog računa”. |
