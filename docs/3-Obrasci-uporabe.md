# 3. Obrasci uporabe

## 3.1 Pregled obrazaca uporabe

Tablica povezuje ciljeve korisnika s akterima, sažetim funkcijskim zahtjevima i
projektnim issueima iz kojih su izvedeni. Oznake `UC-xx` koriste se i u
dijagramu obrazaca uporabe. Detaljni scenariji u nastavku postupno se razrađuju
prema fazama projekta; sažeti pregled obuhvaća i planirane funkcionalnosti.
Opcionalni obrasci uporabe označeni su s **(opcionalno)**.

| ID | Obrazac uporabe | Akter(i) | FR | Izvor |
| --- | --- | --- | --- | --- |
| UC-01 | Prijaviti se Google računom | Posjetitelj, Google OIDC | FR-01 | [AUTH-01](https://github.com/LD05-fk/Strumfovi/issues/33) |
| UC-02 | Odabrati ulogu | Novi prijavljeni korisnik | FR-01 | [AUTH-02](https://github.com/LD05-fk/Strumfovi/issues/34) |
| UC-03 | Upravljati vlastitim računom | Prijavljeni korisnik | FR-01 | [AUTH-01](https://github.com/LD05-fk/Strumfovi/issues/33), [AUTH-04](https://github.com/LD05-fk/Strumfovi/issues/59) |
| UC-04 | Urediti umjetnički profil | Umjetnik | FR-02 | [PORT-01](https://github.com/LD05-fk/Strumfovi/issues/36) |
| UC-05 | Urediti profil organizatora | Organizator | FR-02 | [AUTH-03](https://github.com/LD05-fk/Strumfovi/issues/35) |
| UC-06 | Upravljati portfolijem | Umjetnik | FR-02 | [PORT-02](https://github.com/LD05-fk/Strumfovi/issues/37), [PORT-03](https://github.com/LD05-fk/Strumfovi/issues/38), [PORT-04](https://github.com/LD05-fk/Strumfovi/issues/39) |
| UC-07 | Pregledati javni portfolio | Posjetitelj | FR-02, FR-07 | [PORT-05](https://github.com/LD05-fk/Strumfovi/issues/40), [RES-02](https://github.com/LD05-fk/Strumfovi/issues/55) |
| UC-08 | Stvoriti ili urediti priliku | Organizator, OpenStreetMap/Nominatim | FR-03 | [OPP-01](https://github.com/LD05-fk/Strumfovi/issues/41) |
| UC-09 | Pretražiti i pregledati prilike | Posjetitelj, OpenStreetMap | FR-04 | [OPP-02](https://github.com/LD05-fk/Strumfovi/issues/42) |
| UC-10 | Upravljati vlastitim prilikama | Organizator | FR-04 | [OPP-03](https://github.com/LD05-fk/Strumfovi/issues/43) |
| UC-11 | Automatski zatvoriti priliku nakon isteka roka | Planer | FR-04 | [OPP-04](https://github.com/LD05-fk/Strumfovi/issues/44) |
| UC-12 | Odrediti ili izmijeniti uvjete prijave | Organizator | FR-05 | [APP-01](https://github.com/LD05-fk/Strumfovi/issues/45), [APP-05](https://github.com/LD05-fk/Strumfovi/issues/49) |
| UC-13 | Predati prijavu na priliku | Umjetnik | FR-05 | [APP-02](https://github.com/LD05-fk/Strumfovi/issues/46) |
| UC-14 | Pregledati i upravljati vlastitim prijavama | Umjetnik | FR-05, FR-06 | [APP-03](https://github.com/LD05-fk/Strumfovi/issues/47) |
| UC-15 | Pregledati prijave i odlučiti o njima | Organizator | FR-06 | [APP-04](https://github.com/LD05-fk/Strumfovi/issues/48) |
| UC-16 | Objaviti ili ispraviti rezultate | Organizator | FR-07 | [RES-01](https://github.com/LD05-fk/Strumfovi/issues/54), [RES-03](https://github.com/LD05-fk/Strumfovi/issues/60) — plasmani su opcionalni |
| UC-17 | Dodati prihvaćeno događanje u Google kalendar | Umjetnik, Google Calendar | FR-07 | [CAL-01](https://github.com/LD05-fk/Strumfovi/issues/56), [CAL-02](https://github.com/LD05-fk/Strumfovi/issues/61) — izravno umetanje API-jem je opcionalno |
| UC-18 | Upravljati korisnicima i odobriti organizatora | Administrator | FR-09 | [ADM-01](https://github.com/LD05-fk/Strumfovi/issues/50), [ADM-02](https://github.com/LD05-fk/Strumfovi/issues/51) |
| UC-19 | Sakriti ili vratiti sadržaj | Administrator | FR-09 | [ADM-03](https://github.com/LD05-fk/Strumfovi/issues/52) |
| UC-20 | Upravljati šifrarnicima i postavkama | Administrator | FR-09 | [ADM-04](https://github.com/LD05-fk/Strumfovi/issues/53) |
| UC-21 | Pregledati statistiku prilika | Organizator | FR-08 | [STAT-01](https://github.com/LD05-fk/Strumfovi/issues/57), [STAT-02](https://github.com/LD05-fk/Strumfovi/issues/62) — grafikoni su opcionalni |
| UC-22 | Automatski izraditi mjesečni izvještaj | Planer | FR-08 | [REP-01](https://github.com/LD05-fk/Strumfovi/issues/58) |
| UC-23 | Pregledati izvještaje | Administrator | FR-08 | [REP-01](https://github.com/LD05-fk/Strumfovi/issues/58), [REP-02](https://github.com/LD05-fk/Strumfovi/issues/64) — povijest i ručno generiranje su opcionalni |
| UC-24 | Pregledati i označiti obavijesti | Prijavljeni korisnik | FR-10 | [NOTIF-01](https://github.com/LD05-fk/Strumfovi/issues/65), [NOTIF-02](https://github.com/LD05-fk/Strumfovi/issues/66) — opcionalno |
| UC-25 | Pregledati dnevnik administratorskih radnji **(opcionalno)** | Administrator | FR-09 | [ADM-05](https://github.com/LD05-fk/Strumfovi/issues/63) |

## UC-01 Prijaviti se Google računom

- **Primarni sudionik:** posjetitelj.
- **Vanjski sudionik:** Google OIDC.
- **Zahtjev:** FR-01 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
- **Preduvjet:** korisnik ima Google račun.
- **Rezultat:** korisnik je autentificiran i ima poslužiteljsku sesiju; novi korisnik nastavlja odabirom uloge.

Redoslijed poruka, provjere Google odgovora i alternativni ishodi prijave
prikazani su u [sekvencijskom dijagramu Google prijave](4-Arhitektura-i-oblikovanje.md#441-reprezentativni-uml-sekvencijski-dijagram).

## UC-02 Odabrati ulogu

- **Primarni sudionik:** novi korisnik.
- **Zahtjev:** FR-01 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
- **Preduvjet:** korisnik se prijavio Google računom i ima status `ONBOARDING`.
- **Rezultat:** korisnik ima ulogu `ARTIST` ili `ORGANIZER`, status `ACTIVE` i nova prava u istoj sesiji.

### Osnovni tijek

1. Sustav usmjerava novog korisnika na `/onboarding`.
2. Korisnik odabire karticu „Umjetnik” ili „Organizator” i potvrđuje odabir gumbom „Potvrdi”. Frontend šalje `POST /api/v1/me/role` uz sesiju i CSRF token.
3. Sustav jednom postavlja ulogu i aktivira račun. Organizatoru stvara prazan profil i automatski postavlja odobrenje `APPROVED`.
4. Sustav osvježava prava u postojećoj sesiji, a sučelje preusmjerava korisnika na `/profile` bez ponovne prijave.

### Alternativni tijekovi

- Nedostajuća ili nedopuštena uloga, uključujući `ADMIN`, vraća `400 VALIDATION_ERROR`.
- Ponovljeni odabir vraća `409 ROLE_ALREADY_CHOSEN`; kod istodobnih zahtjeva samo jedan uspijeva. Sučelje prikazuje obavijest „Uloga je već odabrana”, osvježava podatke korisnika (`['me']`) i preusmjerava ga na početnu stranicu (`/`).
- Bez prijave sustav vraća `401 UNAUTHENTICATED`; bez valjanog CSRF tokena vraća `403 FORBIDDEN`.
- Do odabira uloge ostale API rute vraćaju `403 ONBOARDING_REQUIRED`. Dostupni ostaju dohvat vlastitih podataka, izbor uloge, dohvat CSRF tokena i odjava; filtar dopušta i buduću rutu za brisanje računa.
- Pozvani korisnik aktivira unaprijed dodijeljenu ulogu prijavom putem Googlea i preskače ovaj odabir.

Detalji su u [API dokumentaciji prijave](api/auth.md#post-merole).

## UC-08 Stvoriti ili urediti priliku

### Stvaranje prilike

- **Primarni sudionik:** organizator, uključujući organizatora na čekanju odobrenja.
- **Zahtjev:** FR-03 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
- **Preduvjet:** aktivni račun s ulogom `ORGANIZER`, prijavljena sesija i valjan CSRF token.
- **Rezultat ovog koraka:** spremljen privatni nacrt u statusu `PREPARATION`.
  Javna objava i promjena statusa zasebni su koraci.

### Osnovni tijek

1. Organizator unosi naslov, opis, aktivnu vrstu prilike, jedno do deset različitih
   aktivnih područja umjetnosti i budući rok prijave.
2. Odabire fizičku lokaciju s adresom, gradom i koordinatama ili online lokaciju
   s HTTP/HTTPS poveznicom. Za fizičku lokaciju koordinate dobiva pretraživanjem
   adrese ili ručnim odabirom na karti.
3. Po želji dodaje kriterije, nagradu te početak i završetak događanja.
4. Sučelje šalje `POST /api/v1/opportunities` uz sesiju i CSRF token.
5. Sustav provjerava unos, uklanja okolne razmake tekstova i sprema priliku te
   veze s područjima u jednoj transakciji. Vlasnik je prijavljeni organizator.
6. Sustav vraća `201 Created`, puni objekt prilike i zaglavlje `Location`.
   Nova prilika ima status „U pripremi”, bez datuma objave.

### Alternativni tijekovi

- Nedostajuće polje, prekoračeno ograničenje, ponovljeno područje, neaktivan ili
  nepostojeći šifrarnik vraća `400 VALIDATION_ERROR` s nazivom problematičnog polja.
- Rok jednak sadašnjem trenutku ili u prošlosti odbija se. Završetak događanja
  bez početka ili prije početka također se odbija; oba datuma mogu izostati.
- Fizička lokacija bez adrese, grada ili koordinata i online lokacija bez valjane
  HTTP/HTTPS poveznice odbijaju se. Za online lokaciju fizička polja se zanemaruju,
  a za fizičku lokaciju zanemaruje se online poveznica.
- Pretraživanje adrese vraća do pet kandidata; prvi se odabire automatski, a
  organizator može odabrati drugi i ispraviti oznaku na karti. Prazan rezultat,
  ograničenje učestalosti (`429 RATE_LIMITED`) ili nedostupni Nominatim
  (`502 GEOCODING_UNAVAILABLE`) prikazuju odgovarajuću poruku, nakon čega
  organizator može ručno odabrati koordinate. Ako kartografske pločice ne rade,
  sučelje prikazuje upozorenje, a kontrole za odabir lokacije ostaju dostupne.
- Neispravan zahtjev ne sprema djelomičnu priliku.
- Bez prijave sustav vraća `401 UNAUTHENTICATED`; bez uloge organizatora ili
  valjanog CSRF tokena `403 FORBIDDEN`; prije odabira uloge `403 ONBOARDING_REQUIRED`.
  Blokirani organizator dobiva `403 USER_BLOCKED`.
- Organizator na čekanju smije spremiti nacrt. Odobravanje je potrebno za
  naknadnu javnu objavu, koja nije dio ovog endpointa.

Detalji i primjeri su u [API dokumentaciji prilika](api/opportunities.md#post-opportunities).

### Uređivanje prilike

- **Primarni sudionik:** organizator koji je vlasnik prilike.
- **Zahtjev:** FR-03 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
- **Preduvjet:** aktivni račun s ulogom `ORGANIZER`, prijavljena sesija i valjan CSRF token.
- **Rezultat:** sadržaj vlastite prilike ažuriran je bez promjene statusa.

### Osnovni tijek

1. Organizator mijenja podatke vlastite prilike u obrascu.
2. Sučelje šalje `PUT /api/v1/opportunities/{opportunityId}` sa svim unosivim
   poljima i cijelim odabranim skupom područja umjetnosti.
3. Sustav provjerava postoji li prilika i pripada li prijavljenom organizatoru.
4. Sustav provjerava podatke kao kod stvaranja, ali dopušta rok u prošlosti
   i zadržavanje već povezanih neaktivnih šifrarnika.
5. Sustav u jednoj transakciji zamjenjuje unosiva polja, ažurira veze s područjima
   i vrijeme izmjene. Vlasnik, status, arhiviranost i skrivenost ostaju isti.
6. Sustav vraća `200 OK` i ažurirani objekt prilike.

### Alternativni tijekovi

- Uređivanje je dopušteno u svakom statusu, i za arhivirane ili skrivene prilike.
- Nova neaktivna vrsta ili novo neaktivno područje, nepostojeći šifrarnici i
  ostali neispravni podaci vraćaju `400 VALIDATION_ERROR` bez spremanja promjena.
- Izostavljene opcionalne vrijednosti brišu prethodne vrijednosti.
  Promjena fizičke lokacije u online briše prethodnu adresu i koordinate.
- Prošli rok ne mijenja status u ovom zahtjevu; automatsko zatvaranje zaseban je zadatak.
- Nepostojeća prilika vraća `404 NOT_FOUND`, a tuđa prilika `403 FORBIDDEN`.
- Bez sesije sustav vraća `401 UNAUTHENTICATED`; bez uloge organizatora,
  s neaktivnim računom ili nevaljanim CSRF tokenom odgovarajući `403`.

Detalji su u [API dokumentaciji prilika](api/opportunities.md#put-opportunitiesopportunityid).
## UC-05 Urediti profil organizatora

- **Primarni sudionik:** organizator.
- **Zahtjev:** FR-02 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
- **Cilj:** pregledati profil i promijeniti naziv organizacije.
- **Preduvjet:** valjana sesija s ulogom `ORGANIZER`; za spremanje je potreban CSRF token.
- **Rezultat:** naziv organizacije je spremljen ili obrisan; ime i odobrenje ostaju nepromijenjeni.

### Osnovni tijek

1. Organizator otvara stranicu `/profile`.
2. Sučelje dohvaća `GET /api/v1/me/organizer-profile`.
3. Sustav vraća ime organizatora, naziv organizacije i status odobrenja. Ako profilni red ne postoji, naziv je `null`.
4. Organizator unosi ili mijenja naziv organizacije i sprema obrazac.
5. Sučelje šalje `PUT /api/v1/me/organizer-profile` uz sesiju i CSRF token.
6. Sustav obrezuje naziv, sprema ga (ili `null` za praznu vrijednost), osvježava `updated_at` i vraća spremljeni profil.

### Alternativni tijekovi

- Organizator koji čeka odobrenje može urediti profil; odobrenje se ovim zahtjevom ne mijenja.
- Ako profilni red ne postoji, uspješan PUT stvara ga.
- Izostavljeno polje `organizationName`, `null`, prazan niz ili niz samo od razmaka sprema se kao `null`.
- Naziv dulji od 200 znakova vraća `400 VALIDATION_ERROR`.
- Bez valjane sesije sustav vraća `401 UNAUTHENTICATED`; bez uloge organizatora, uz onboarding, blokiran račun ili nedostajući/nevaljan CSRF token vraća odgovarajući `403`.

## UC-06 Upravljati portfolijem

- **Primarni sudionik:** umjetnik.
- **Zahtjev:** FR-02 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
- **Cilj:** dodati djelo u vlastiti portfolio i odabrati njegovu vidljivost.
- **Preduvjet:** valjana prijavljena sesija s ulogom `ARTIST` i CSRF token.
- **Rezultat:** djelo je spremljeno u portfoliju bez slika; slike se mogu dodati
  zasebnim zahtjevom nakon što djelo dobije ID.

### Osnovni tijek

1. Umjetnik u obrascu unosi naslov, godinu i tehniku te po želji dimenzije i opis.
2. Umjetnik za djelo odabire vidljivost `PUBLIC` ili `PRIVATE`.
3. Sučelje šalje `POST /api/v1/artworks` uz sesiju i CSRF token.
4. Sustav provjerava obavezna polja, ograničenja tekstova i godinu od 1900 do
   tekuće godine.
5. Sustav sprema djelo vezano uz prijavljenog umjetnika i vraća `201 Created`,
   zaglavlje `Location` i djelo s praznim nizom slika.
6. Nakon stvaranja, sučelje može zasebno učitati slike za dobiveni ID djela.

### Alternativni tijekovi

- Nedostajući naslov, godina, tehnika ili vidljivost, nevaljana godina,
  prekoračeno ograničenje teksta ili nepoznata vidljivost vraća
  `400 VALIDATION_ERROR` s detaljima polja.
- Tekstualne vrijednosti se obrezuju; prazne neobavezne dimenzije i opis
  spremaju se kao `null`.
- Bez prijave sustav vraća `401 UNAUTHENTICATED`.
- Bez uloge `ARTIST`, bez valjanog CSRF tokena ili prije odabira uloge sustav
  vraća odgovarajući `403`.

Detalji zahtjeva i odgovora nalaze se u
[API dokumentaciji djela](api/artworks.md#post-artworks).
Ovaj scenarij detaljno opisuje dodavanje djela; uređivanje, brisanje, slike i
prethodne izložbe također pripadaju ovom obrascu uporabe.

## UC-04 Urediti umjetnički profil

- **Primarni sudionik:** umjetnik.
- **Zahtjev:** FR-02 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
- **Cilj:** urediti biografiju, opis umjetničkog rada, područja umjetnosti i
  poveznice na vlastitom profilu.
- **Preduvjet:** valjana prijavljena sesija s ulogom `ARTIST` i CSRF token.
- **Rezultat:** profil i njegove veze potpuno su ažurirani.

### Osnovni tijek

1. Umjetnik otvara `/profile`; sučelje dohvaća `GET /api/v1/me/artist-profile`
   i aktivna područja preko `GET /api/v1/art-fields?pageSize=100`.
   Zaglavlje omogućuje pregled javnog profila i kopiranje poveznice
   `/artists/{userId}`.
2. Ako profil još ne postoji, sučelje prikazuje prazan obrazac. Umjetnik unosi
   ili uređuje biografiju i opis umjetničkog rada.
3. Odabire do 10 područja iz aktivnog šifrarnika te unosi do 10 poveznica.
   Obrazac provjerava duljine, jedinstvenost područja i URL-ove prije slanja.
4. Sučelje šalje `PUT /api/v1/me/artist-profile` sa svim poljima obrasca;
   prazni tekstovi šalju se kao `null`, a redoslijed poveznica prati obrazac.
5. Sustav validira ograničenja tekstova, jedinstvenost i dostupnost područja te
   HTTP(S) format svake poveznice.
6. Sustav u jednoj transakciji stvara ili ažurira profil, potpuno zamjenjuje
   odabrana područja i poveznice te vraća spremljeni `ArtistProfile`.
7. Sučelje ažurira predmemoriju profila, invalidira javni profil umjetnika i
   prikazuje potvrdu „Profil spremljen”.

### Alternativni tijekovi

- Prazni nizovi područja i poveznica brišu postojeće veze.
- Postojeće neaktivno područje može ostati odabrano, ali se ne može dodati u
  profil koji ga prethodno nije imao; ako je već odabrano, ostaje prikazano i
  šalje se uz spremanje dok ga umjetnik ne ukloni.
- Tekstovi se obrezuju; prazna oznaka poveznice sprema se kao `null`.
- Nevaljana polja, duplikati, nedostupna područja ili neispravni URL-ovi vraćaju
  `400 VALIDATION_ERROR` s detaljima polja, koji se prikazuju uz odgovarajuće
  polje obrasca. Greška dohvata može se ponoviti; ostale greške spremanja
  prikazuju se globalnom obavijesti.
- Pri internoj navigaciji s nespremljenim izmjenama sučelje traži potvrdu; pri
  osvježavanju ili zatvaranju kartice preglednik prikazuje vlastito upozorenje.
- Neuspješno kopiranje javne poveznice prikazuje obavijest o pogrešci.
- Bez prijave sustav vraća `401 UNAUTHENTICATED`; bez uloge umjetnika, bez
  valjanog CSRF tokena ili prije odabira uloge vraća odgovarajući `403`.

Detalji zahtjeva, odgovora i ograničenja nalaze se u
[API dokumentaciji portfolija umjetnika](api/artists.md#put-meartist-profile).

## UC-18 Upravljati korisnicima i odobriti organizatora

- **Primarni sudionik:** administrator.
- **Zahtjev:** FR-09 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
- **Cilj:** pronaći korisnika i pregledati stanje računa ili pronaći organizatore koji čekaju odobrenje.
- **Preduvjet:** korisnik ima valjanu sesiju i ulogu `ADMIN`.
- **Rezultat:** dohvaćen je popis korisnika s ukupnim brojem rezultata; podaci računa se ne mijenjaju.

### Osnovni tijek

1. Administrator otvara pregled korisnika.
2. Sučelje poziva `GET /api/v1/admin/users`.
3. Sustav provjerava prijavu, administratorsku ulogu i ispravnost parametara.
4. Sustav vraća prvih 20 korisnika od najnovijeg prema najstarijem, uključujući obrisane račune.
5. Administrator može pretraživati dio imena ili e-maila, filtrirati po ulozi, statusu i odobrenju organizatora te promijeniti stranicu.
6. Sustav kombinira sve zadane filtre i vraća odgovarajuću stranicu s ukupnim brojem rezultata.

### Alternativni tijekovi

- Za popis organizatora na čekanju sučelje šalje `role=ORGANIZER&organizerApproval=PENDING`.
- Ako nema rezultata ili je tražena stranica iza kraja popisa, sustav vraća prazan popis uz metapodatke stranice.
- Ako su parametri neispravni, sustav vraća 400 `VALIDATION_ERROR` s opisom uz odgovarajuće polje.
- Ako nema valjane sesije, sustav vraća 401 `UNAUTHENTICATED`.
- Ako pozivatelj nema administratorsku ulogu, sustav vraća 403 `FORBIDDEN`.

Ovaj scenarij detaljno opisuje pregled korisnika; promjena uloga, blokiranje i
odobravanje organizatora također pripadaju ovom obrascu uporabe.
Detalji parametara i odgovora nalaze se u [API dokumentaciji administracije](api/admin.md).

## UC-20 Upravljati šifrarnicima i postavkama

- **Primarni sudionik:** administrator.
- **Zahtjev:** FR-09 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
- **Cilj:** održavati popise područja umjetnosti i vrsta prilika.
- **Preduvjet:** valjana administratorska sesija; za promjene valjan CSRF token.
- **Rezultat:** stavka je dodana, uređena, deaktivirana, ponovno aktivirana ili obrisana.

### Osnovni tijek

1. Administrator otvara upravljanje šifrarnicima.
2. Sučelje dohvaća aktivne i neaktivne stavke odgovarajućeg šifrarnika.
3. Administrator dodaje novu stavku ili uređuje šifru, naziv, aktivnost i poziciju postojeće.
4. Sustav provjerava obavezna polja, ograničenja i jedinstvenost šifre i naziva.
5. Sustav sprema izmjenu i vraća spremljenu stavku.
6. Aktivne stavke prikazuju se u javnim popisima za odabir.

### Alternativni tijekovi

- Administrator deaktivira stavku: ona nestaje iz javnog popisa, a postojeći profili i prilike zadržavaju veze.
- Administrator ponovno aktivira stavku: ona se vraća u javni popis.
- Administrator briše nekorištenu stavku: sustav je briše i vraća 204.
- Ako se stavka koristi u profilu ili prilici, brisanje vraća 409 `IN_USE`; administrator je može deaktivirati.
- Ako šifra ili naziv već postoji u istom šifrarniku, spremanje vraća 409 `DUPLICATE_CODE`.
- Neispravna polja vraćaju 400 `VALIDATION_ERROR` s detaljima po polju.
- Ako stavka više ne postoji, uređivanje i brisanje vraćaju 404 `NOT_FOUND`.
- Bez sesije sustav vraća 401; bez administratorske uloge ili valjanog CSRF tokena za promjenu vraća 403.

Backend dio pripada ADM-04-BE1 (#218), a administratorsko sučelje zasebnom
zadatku. Detalji ruta i ograničenja nalaze se u
[API dokumentaciji administracije](api/admin.md#administracija-šifrarnika).
Ovaj scenarij detaljno opisuje šifrarnike; upravljanje ograničenjima uploada
također pripada ovom obrascu uporabe.
