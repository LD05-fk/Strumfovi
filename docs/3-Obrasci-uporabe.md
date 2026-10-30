# 3. Obrasci uporabe

## 3.2 Prva prijava i odabir uloge

- **Primarni sudionik:** novi korisnik.
- **Zahtjevi:** FR-01 i FR-03 iz [analize zahtjeva](2-Analiza-zahtjeva.md#211-računi-i-prijava-1-ciklus-brisanje-računa-u-2).
- **Preduvjet:** korisnik ima Google račun.
- **Rezultat:** korisnik ima ulogu `ARTIST` ili `ORGANIZER`, status `ACTIVE` i nova prava u istoj sesiji.

### Osnovni tijek

1. Korisnik se prijavljuje Google računom.
2. Sustav stvara novi račun u statusu `ONBOARDING`, bez uloge, i usmjerava ga na `/onboarding`.
3. Korisnik bira umjetnika ili organizatora. Frontend šalje `POST /api/v1/me/role` uz sesiju i CSRF token.
4. Sustav jednom postavlja ulogu i aktivira račun. Organizatoru stvara prazan profil i automatski postavlja odobrenje `APPROVED`.
5. Sustav osvježava prava u postojećoj sesiji. Korisnik nastavlja rad bez ponovne prijave.
6. Korisnik dolazi na početnu stranicu `/`, gdje vidi pozdrav, poveznicu „Portfolio” (umjetnik) ili „Moje prilike” (organizator) i poveznicu „Prilike”.

### Alternativni tijekovi

- Nedostajuća ili nedopuštena uloga, uključujući `ADMIN`, vraća `400 VALIDATION_ERROR`.
- Ponovljeni odabir vraća `409 ROLE_ALREADY_CHOSEN`; kod istodobnih zahtjeva samo jedan uspijeva.
- Bez prijave sustav vraća `401 UNAUTHENTICATED`; bez valjanog CSRF tokena vraća `403 FORBIDDEN`.
- Do odabira uloge ostale API rute vraćaju `403 ONBOARDING_REQUIRED`. Dostupni ostaju dohvat vlastitih podataka, izbor uloge, dohvat CSRF tokena i odjava; filtar dopušta i buduću rutu za brisanje računa.
- Pozvani korisnik aktivira unaprijed dodijeljenu ulogu prijavom putem Googlea i preskače ovaj odabir.
- Ako Google prijava ne uspije, početna stranica prikazuje poruku „Prijava nije uspjela. Pokušajte ponovno.”.

Detalji su u [API dokumentaciji prijave](api/auth.md#post-merole).

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

1. Umjetnik u obrascu unosi ili uređuje biografiju i opis umjetničkog rada.
2. Odabire do 10 područja iz aktivnog šifrarnika te unosi do 10 poveznica.
3. Sučelje šalje `PUT /api/v1/me/artist-profile` sa svim poljima obrasca.
4. Sustav validira ograničenja tekstova, jedinstvenost i dostupnost područja te
   HTTP(S) format svake poveznice.
5. Sustav u jednoj transakciji stvara ili ažurira profil, potpuno zamjenjuje
   odabrana područja i poveznice te vraća spremljeni `ArtistProfile`.

### Alternativni tijekovi

- Prazni nizovi područja i poveznica brišu postojeće veze.
- Postojeće neaktivno područje može ostati odabrano, ali se ne može dodati u
  profil koji ga prethodno nije imao.
- Tekstovi se obrezuju; prazna oznaka poveznice sprema se kao `null`.
- Nevaljana polja, duplikati, nedostupna područja ili neispravni URL-ovi vraćaju
  `400 VALIDATION_ERROR` s detaljima polja.
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
