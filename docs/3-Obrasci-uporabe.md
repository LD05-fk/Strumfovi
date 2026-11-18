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
| UC-04 | Upravljati prilikama | Organizator, OpenStreetMap/Nominatim, Planer (automatsko zatvaranje) |
| UC-05 | Pregledati prilike | Posjetitelj, umjetnik, OpenStreetMap/Nominatim |
| UC-06 | Postaviti uvjete i prijaviti se na priliku | Organizator, umjetnik |
| UC-07 | Odlučivati o prijavama | Organizator, umjetnik |
| UC-08 | Objaviti rezultate i dodati u kalendar | Organizator, umjetnik, Google Calendar |
| UC-09 | Pregledati statistiku i izvještaje | Organizator, administrator, Planer |
| UC-10 | Upravljati platformom | Administrator |
| UC-11 | Pregledati obavijesti **(opcionalno)** | Prijavljeni korisnik |
| UC-12 | Pregledati dnevnik administratorskih radnji **(opcionalno)** | Administrator |

Veze između obrazaca i zahtjeva bit će u [matrici sljedivosti](2-Analiza-zahtjeva.md#26-sljedivost-zahtjeva) kad ona bude ispunjena.

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
| **Rezultat** | Korisnik ima ulogu i aktivan račun. Brisanje računa planirano je u zadatku AUTH-04. |

Detalji su u [API dokumentaciji prijave](api/auth.md).

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

#### Uređivanje profila umjetnika i prethodnih izložbi

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Umjetnik |
| **Povezani zahtjevi** | FR-02; PORT-01, PORT-02 |
| **Preduvjeti** | Korisnik je prijavljen s ulogom `ARTIST`. |
| **Glavni uspješni tok** | 1. Umjetnik otvara `/profile`.<br>2. Uređuje biografiju, umjetničku izjavu, područja umjetnosti (najviše 10) i poveznice (najviše 10) te sprema (`PUT /api/v1/me/artist-profile`).<br>3. Dodaje prethodnu izložbu s naslovom, godinom i mjestom (`POST /api/v1/past-exhibitions`).<br>4. Po potrebi uređuje ili uz potvrdu briše postojeću izložbu (`PUT` ili `DELETE /api/v1/past-exhibitions/{id}`).<br>5. Promjene se prikazuju na javnom portfoliju. |
| **Alternativni tokovi** | **2a — Nevaljan unos** (npr. predug tekst ili poveznica bez `http(s)`): `400 VALIDATION_ERROR`, greška se prikazuje uz polje.<br>**3a — Godina izložbe u budućnosti ili prije 1900.:** `400 VALIDATION_ERROR` uz polje godine.<br>**4a — Tuđa izložba:** `404 NOT_FOUND`, ništa se ne mijenja. |
| **Rezultat** | Profil i popis prethodnih izložbi su ažurirani i vidljivi na javnom portfoliju. |

#### Uređivanje profila organizatora

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator |
| **Povezani zahtjevi** | FR-02; AUTH-03 |
| **Preduvjeti** | Korisnik je prijavljen s ulogom `ORGANIZER`; može i čekati odobrenje. |
| **Glavni uspješni tok** | 1. Organizator otvara `/profile`.<br>2. Vidi svoje ime s Google računa (samo za čitanje) i status odobrenja („Odobren” ili „Čeka odobrenje”).<br>3. Unosi naziv organizacije i sprema (`PUT /api/v1/me/organizer-profile`). |
| **Alternativni tokovi** | **3a — Naziv dulji od 200 znakova:** `400 VALIDATION_ERROR`, greška se prikazuje uz polje.<br>**3b — Prazan naziv:** sprema se kao prazan (naziv nije obavezan). |
| **Rezultat** | Naziv organizacije je spremljen; status odobrenja ne mijenja se. |

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
| **Vanjski akter** | OpenStreetMap/Nominatim (geokodiranje adrese), Planer (automatsko zatvaranje prijava nakon roka) |
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
| **Povezani zahtjevi** | FR-03; OPP-03 |
| **Preduvjeti** | Organizator je prijavljen s ulogom `ORGANIZER`, a prilika pripada njegovu računu. Stranici „Moje prilike” pristupa na `/my-opportunities`; za promjenu statusa prilika može biti otvorena i iz popisa ili iz vlastitog detalja. |
| **Glavni uspješni tok** | 1. Sustav dohvaća organizatorove prilike (`GET /api/v1/me/opportunities`) i prikazuje kartice „Aktivne” i „Arhivirane”, filtar statusa te straničenje.<br>2. Organizator odabire dopuštenu statusnu radnju u traci radnji. Za `PREPARATION` nudi se „Objavi” (`OPEN`), za `OPEN` „Zatvori prijave” (`CLOSED`), a za `CLOSED` „Započni odabir” (`SELECTION`). „Ponovno otvori prijave” (`OPEN`) nudi se za `CLOSED` samo dok je rok u budućnosti. Za `SELECTION` i `FINISHED` ne nude se statusne radnje. Arhivirana prilika nema statusnih radnji.<br>3. Sustav traži potvrdu radnje. Organizator je potvrđuje, a sustav šalje `POST /api/v1/opportunities/{id}/status` s ciljnim statusom.<br>4. Sustav sprema promjenu, osvježava popis i detalj prilike te prikazuje potvrdu uspjeha. |
| **Alternativni tokovi** | **1a — Nema prilika:** sustav prikazuje prazno stanje i poveznicu za stvaranje nove prilike (`/organizer/opportunities/new`).<br>**1b — Dohvat traje ili ne uspije:** sustav prikazuje stanje učitavanja odnosno grešku s mogućnošću ponovnog pokušaja.<br>**2a — Rok je istekao:** ponovno otvaranje nije ponuđeno; ako je rok istekao prije slanja zahtjeva, API vraća `409 DEADLINE_PASSED` i sustav upućuje organizatora da produlji rok uređivanjem prilike.<br>**3a — Organizator odustane:** dijalog se zatvara bez slanja zahtjeva i prilika ostaje nepromijenjena.<br>**4a — Prijelaz više nije dopušten ili je prilika arhivirana:** API vraća `409 INVALID_STATUS_TRANSITION`; sustav prikazuje poruku i osvježava podatke.<br>**4b — Organizator nema odobrenje za objavu:** kada se provjera odobrenja aktivira u ADM-02-BE, API vraća `403 ORGANIZER_NOT_APPROVED`, a sustav prikazuje poruku poslužitelja.<br>**4c — Ostala greška poslužitelja:** sustav prikazuje obavijest o grešci, a podaci se ponovno dohvaćaju prema potrebi. |
| **Rezultat** | Status prilike promijenjen je dopuštenim prijelazom; vlasnik prilike ostaje isti. |

#### Brisanje ili arhiviranje prilike

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (vlasnik prilike) |
| **Povezani zahtjevi** | FR-03; OPP-03 |
| **Preduvjeti** | Organizator je prijavljen s ulogom `ORGANIZER`, a prilika pripada njegovu računu. Radnja je dostupna iz trake radnji na popisu „Moje prilike” (`/my-opportunities`) ili na detalju prilike. |
| **Glavni uspješni tok** | 1. Organizator odabire „Obriši”.<br>2. Sustav prikazuje dijalog za potvrdu brisanja i upozorava da se radnja ne može poništiti.<br>3. Organizator potvrđuje brisanje; sustav šalje `DELETE /api/v1/opportunities/{id}`.<br>4. API potvrđuje brisanje (`204 No Content`), sustav osvježava popis i uklanja priliku iz prikaza. Ako je radnja pokrenuta s detalja, sustav vodi organizatora na `/my-opportunities`. |
| **Alternativni tokovi** | **1a — Prilika je već arhivirana:** „Obriši” ostaje dostupno, ali se ne nude statusne radnje ni dodatna radnja „Arhiviraj”.<br>**2a — Organizator odustane:** dijalog se zatvara, bez slanja zahtjeva i promjene prilike.<br>**3a — Prilika ima prijave:** API vraća `409 HAS_APPLICATIONS`; sustav ne briše priliku i u dijalogu nudi arhiviranje. Ako organizator odabere „Arhiviraj”, sustav šalje `POST /api/v1/opportunities/{id}/archive`.<br>**3b — Brisanje ili arhiviranje ne uspije:** sustav prikazuje obavijest o grešci i zadržava priliku u prikazu.<br>**3c — Prilika ne postoji ili ne pripada organizatoru:** API vraća `404 NOT_FOUND` odnosno `403 FORBIDDEN`; sustav prikazuje odgovarajuću poruku i osvježava prikaz. |
| **Rezultat** | Prilika je obrisana ili arhivirana. Arhiviranje je ne vraća u popis aktivnih prilika; status se ne mijenja osim što se prilika u statusu `OPEN` pri arhiviranju zatvara (`CLOSED`). |

### UC-05 Pregledati prilike

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Posjetitelj, umjetnik |
| **Vanjski akter** | OpenStreetMap/Nominatim (karta i pretraga lokacija) |
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
| **Preduvjeti** | Prilika postoji i organizator je njezin vlasnik; za prijavu prijave su otvorene. |
| **Glavni uspješni tok** | 1. Organizator postavlja uvjete prijave (zadaci APP-01, APP-05).<br>2. Umjetnik pregledava uvjete i predaje prijavu (APP-02).<br>3. Umjetnik može prijavu povući ili dopuniti dok su prijave otvorene. |
| **Alternativni tokovi** | **2a — Prijave zatvorene:** predaja i dopuna nisu moguće. |
| **Rezultat** | Uvjeti su spremljeni; prijava je predana i vidljiva umjetniku. |

Detalji rute i odgovora bit će u API dokumentaciji prijava.

### UC-07 Odlučivati o prijavama

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator |
| **Sudionik** | Umjetnik (prati odluku) |
| **Povezani zahtjevi** | FR-06 |
| **Preduvjeti** | Umjetnik je predao prijavu na priliku organizatora. |
| **Glavni uspješni tok** | 1. Organizator otvara popis prijava na svoju priliku (APP-04).<br>2. Pregledava prijavu i prihvaća je ili odbija.<br>3. Sustav sprema odluku.<br>4. Umjetnik vidi odluku među svojim prijavama (APP-03). |
| **Alternativni tokovi** | **2a — Prijava nije pronađena ili pripada drugom organizatoru:** odluka se ne sprema. |
| **Rezultat** | Prijava je prihvaćena ili odbijena, a umjetnik vidi odluku. |

### UC-08 Objaviti rezultate i dodati u kalendar

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (objava), umjetnik (pregled i kalendar) |
| **Vanjski akter** | Google Calendar |
| **Povezani zahtjevi** | FR-07 |
| **Preduvjeti** | Organizator ima prihvaćene prijave na priliku. |
| **Glavni uspješni tok** | 1. Organizator objavljuje rezultate prilike (RES-01).<br>2. Sustav prikazuje rezultate sudionicima i dodaje sudjelovanje na javni portfolio umjetnika (RES-02).<br>3. Umjetnik odabire dodavanje prihvaćenog događanja u Google kalendar (CAL-01). |
| **Alternativni tokovi** | **1a — Ispravak rezultata:** mijenja prikaz i na portfoliju (RES-03).<br>**3a — Prijava nije prihvaćena:** događanje se ne može dodati.<br>**3b — Izravno umetanje API-jem:** opcionalno (CAL-02). |
| **Rezultat** | Rezultati su vidljivi, a umjetnik može otvoriti kalendar s podacima događanja. |

### UC-09 Pregledati statistiku i izvještaje

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Organizator (statistika), administrator (izvještaji) |
| **Vanjski akter** | Planer (mjesečni izvještaj) |
| **Povezani zahtjevi** | FR-08 |
| **Preduvjeti** | Organizator ima prilike; administrator je prijavljen s ulogom `ADMIN`. |
| **Glavni uspješni tok** | 1. Organizator otvara statistiku svojih prilika (STAT-01).<br>2. Planer jednom mjesečno generira izvještaj o korištenju platforme (REP-01).<br>3. Administrator pregledava izvještaje (REP-02). |
| **Alternativni tokovi** | **1a — Grafikoni:** opcionalni (STAT-02).<br>**3a — Povijest i ručno generiranje:** opcionalni. |
| **Rezultat** | Organizator vidi statistiku svojih prilika, a administrator mjesečne izvještaje. |

### UC-10 Upravljati platformom

| Element specifikacije | Opis |
| :--- | :--- |
| **Primarni akter** | Administrator |
| **Povezani zahtjevi** | FR-09 |
| **Preduvjeti** | Administrator je prijavljen s ulogom `ADMIN`; za promjene je potreban CSRF token. |
| **Glavni uspješni tok** | 1. Administrator pregledava korisnike (`GET /api/v1/admin/users`) i filtrira ih po ulozi, statusu i odobrenju organizatora.<br>2. Mijenja uloge korisnika i odobrava organizatore (ADM-01, ADM-02).<br>3. Sakriva neprimjereni sadržaj (ADM-03).<br>4. Uređuje šifrarnike i postavke (ADM-04). |
| **Alternativni tokovi** | **2a — Neispravni parametri:** `400 VALIDATION_ERROR`.<br>**4a — Stavka se koristi:** brisanje vraća `409 IN_USE`; administrator je može deaktivirati.<br>**4b — Duplikat šifre ili naziva:** `409 DUPLICATE_CODE`. |
| **Rezultat** | Korisnici, odobrenja, sadržaj i šifrarnici su ažurirani. |

Detalji su u [API dokumentaciji administracije](api/admin.md).

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

| Provjera | Pitanje za tim |
| --- | --- |
| Dijagram i tablica | Imaju li svi obrasci na dijagramu ID u tablici u 3.1 i odjeljak u 3.2? |
| Zahtjevi | Je li svaki obrazac povezan s odobrenim funkcijskim zahtjevom? Veze će biti u matrici sljedivosti (§2.6) kad bude ispunjena. |
| Akteri | Odgovaraju li akteri ulogama koje će biti opisane u §2.3 i dopuštenim radnjama? |
| Aplikacija | Može li tim demonstrirati glavni tok i važnu alternativu? |
