# 3. Obrasci uporabe

## 3.2 Prva prijava i odabir uloge

- **Primarni sudionik:** novi korisnik.
- **Zahtjevi:** FR-01 i FR-03 iz [analize zahtjeva](2-Analiza-zahtjeva.md#211-računi-i-prijava-1-ciklus-brisanje-računa-u-2).
- **Preduvjet:** korisnik ima Google račun.
- **Rezultat:** korisnik ima ulogu `ARTIST` ili `ORGANIZER`, status `ACTIVE` i nova prava u istoj sesiji.

### Osnovni tijek

1. Korisnik se prijavljuje Google računom.
2. Sustav stvara novi račun u statusu `ONBOARDING`, bez uloge, i usmjerava ga na `/onboarding`.
3. Korisnik odabire karticu „Umjetnik” ili „Organizator” i potvrđuje odabir gumbom „Potvrdi”. Frontend šalje `POST /api/v1/me/role` uz sesiju i CSRF token.
4. Sustav jednom postavlja ulogu i aktivira račun. Organizatoru stvara prazan profil i automatski postavlja odobrenje `APPROVED`.
5. Sustav osvježava prava u postojećoj sesiji, a sučelje preusmjerava korisnika na `/profile` bez ponovne prijave.
6.  Kada korisnik otvori početnu stranicu `/`, vidi pozdrav, poveznicu „Portfolio” (umjetnik) ili „Moje prilike” (organizator) i poveznicu „Prilike”.

### Alternativni tijekovi

- Nedostajuća ili nedopuštena uloga, uključujući `ADMIN`, vraća `400 VALIDATION_ERROR`.
- Ponovljeni odabir vraća `409 ROLE_ALREADY_CHOSEN`; kod istodobnih zahtjeva samo jedan uspijeva. Sučelje prikazuje obavijest „Uloga je već odabrana”, osvježava podatke korisnika (`['me']`) i preusmjerava ga na početnu stranicu (`/`).
- Bez prijave sustav vraća `401 UNAUTHENTICATED`; bez valjanog CSRF tokena vraća `403 FORBIDDEN`.
- Do odabira uloge ostale API rute vraćaju `403 ONBOARDING_REQUIRED`. Dostupni ostaju dohvat vlastitih podataka, izbor uloge, dohvat CSRF tokena i odjava; filtar dopušta i buduću rutu za brisanje računa.
- Pozvani korisnik aktivira unaprijed dodijeljenu ulogu prijavom putem Googlea i preskače ovaj odabir.
- Ako Google prijava ne uspije, početna stranica prikazuje poruku „Prijava nije uspjela. Pokušajte ponovno.”.

Detalji su u [API dokumentaciji prijave](api/auth.md#post-merole).

## Objava prilike

- **Primarni sudionik:** organizator, uključujući organizatora na čekanju odobrenja.
- **Zahtjevi:** FR-15 i FR-16 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
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
- Neispravan zahtjev ne sprema djelomičnu priliku.
- Bez prijave sustav vraća `401 UNAUTHENTICATED`; bez uloge organizatora ili
  valjanog CSRF tokena `403 FORBIDDEN`; prije odabira uloge `403 ONBOARDING_REQUIRED`.
  Blokirani organizator dobiva `403 USER_BLOCKED`.
- Organizator na čekanju smije spremiti nacrt. Odobravanje je potrebno za
  naknadnu javnu objavu, koja nije dio ovog endpointa.

Detalji i primjeri su u [API dokumentaciji prilika](api/opportunities.md#post-opportunities).

## Uređivanje prilike

- **Primarni sudionik:** organizator koji je vlasnik prilike.
- **Zahtjevi:** FR-15 i FR-16 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
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
## Brisanje prilike

- **Primarni sudionik:** organizator koji je vlasnik prilike.
- **Zahtjevi:** FR-20 iz [analize zahtjeva](2-Analiza-zahtjeva.md).
- **Preduvjet:** aktivni račun s ulogom `ORGANIZER`, prijavljena sesija i valjan CSRF token.
- **Rezultat:** vlastita prilika bez prijava trajno je uklonjena.

### Osnovni tijek

1. Organizator na popisu svojih prilika traži brisanje i potvrđuje radnju.
2. Sučelje šalje `DELETE /api/v1/opportunities/{opportunityId}` bez tijela.
3. Sustav provjerava postoji li prilika i pripada li prijavljenom organizatoru.
4. Sustav provjerava smije li se prilika obrisati, odnosno nema li prijava.
5. Sustav u jednoj transakciji briše priliku i njezine veze s područjima umjetnosti.
6. Sustav vraća `204 No Content` bez tijela.

### Alternativni tijekovi

- Brisanje je dopušteno u svakom statusu, i za arhivirane ili skrivene prilike.
- Prilika s prijavama ne briše se nego arhivira; sustav vraća `409 HAS_APPLICATIONS`,
  a sučelje nudi arhiviranje. Provjera je izdvojena u `OpportunityDeletionGuard` i
  u Fazi 1 uvijek dopušta brisanje jer tablica prijava još ne postoji.
- Nepostojeća prilika vraća `404 NOT_FOUND`, a tuđa prilika `403 FORBIDDEN`;
  vlasništvo se provjerava prije provjere prijava.
- Ponovljeni zahtjev za već obrisanom prilikom vraća `404 NOT_FOUND`.
- Bez sesije sustav vraća `401 UNAUTHENTICATED`; bez uloge organizatora,
  s neaktivnim računom ili nevaljanim CSRF tokenom odgovarajući `403`.

Detalji su u [API dokumentaciji prilika](api/opportunities.md#delete-opportunitiesopportunityid).

## Uređivanje profila organizatora

- **Primarni sudionik:** organizator.
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

## Dodavanje djela u portfolio

- **Primarni sudionik:** umjetnik.
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

## Uređivanje profila umjetnika

- **Primarni sudionik:** umjetnik.
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

## Pregled korisnika

- **Primarni sudionik:** administrator.
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

Ovdje je opisan obrazac uporabe. Backend dio implementiran je u ADM-01-BE2;
administratorsko sučelje, promjena uloga, blokiranje i odobravanje zasebni su zadaci.
Detalji parametara i odgovora nalaze se u [API dokumentaciji administracije](api/admin.md).

## Uređivanje šifrarnika

- **Primarni sudionik:** administrator.
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
