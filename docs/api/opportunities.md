# Prilike

Ovaj dokument opisuje stvaranje, uređivanje i promjenu statusa prilike te geokodiranje adrese.

## POST /opportunities

`POST /api/v1/opportunities` stvara privatni nacrt. Pristup ima prijavljeni,
aktivni korisnik s ulogom `ORGANIZER`, uključujući organizatora na čekanju
(`organizerApproval = PENDING`). Potrebni su sesijski kolačić i CSRF token:
vrijednost kolačića `XSRF-TOKEN` šalje se u zaglavlju `X-XSRF-TOKEN`.

### Podaci zahtjeva

| Polje | Obavezno | Ograničenje |
| --- | --- | --- |
| `title` | da | 3–200 znakova |
| `description` | da | 1–10000 znakova |
| `typeId` | da | UUID postojeće aktivne vrste prilike |
| `artFieldIds` | da | 1–10 UUID-ova postojećih aktivnih područja, bez duplikata |
| `deadline` | da | ISO-8601 datum i vrijeme s pomakom, strogo nakon sadašnjeg trenutka |
| `criteria` | ne | tekst do 5000 znakova ili `null` |
| `prize` | ne | tekst do 2000 znakova ili `null` |
| `location` | da | objekt opisan u nastavku |
| `eventStart` | ne | datum i vrijeme s pomakom ili `null` |
| `eventEnd` | ne | samo uz `eventStart`, ne prije njega; jednak trenutak je dopušten |

Datumi događanja nisu obavezni ni za jednu vrstu prilike. Datumi s različitim
pomacima uspoređuju se kao stvarni trenuci. Tekstovi se obrezuju prije provjera;
prazni `criteria` i `prize` spremaju se kao `null`.

| Polje lokacije | Ograničenje |
| --- | --- |
| `type` | obavezno: `PHYSICAL` ili `ONLINE` |
| `streetAddress` | do 200 znakova; obavezna neprazna adresa za `PHYSICAL` |
| `city` | do 100 znakova; obavezan neprazan grad za `PHYSICAL` |
| `postalCode` | neobavezno, do 10 znakova |
| `latitude` | broj od −90 do 90, obavezan za `PHYSICAL` |
| `longitude` | broj od −180 do 180, obavezan za `PHYSICAL` |
| `onlineUrl` | do 1024 znaka; za `ONLINE` obavezna valjana HTTP/HTTPS poveznica s nazivom poslužitelja |

Za `PHYSICAL` se `onlineUrl` ignorira i sprema kao `null`. Koordinate se prvo
provjeravaju pa zaokružuju na šest decimala; polovice se zaokružuju od nule.
Za `ONLINE` se adresa, grad, poštanski broj i koordinate ignoriraju i spremaju
kao `null`. Stvaranje prilike ne poziva vanjski servis za geokodiranje:
koordinate dolaze iz prethodnog pretraživanja adrese ili ručnog odabira na karti.

### Primjer fizičke prilike

UUID-ove treba zamijeniti stvarnim vrijednostima iz aktivnih šifrarnika, a rok
postaviti u budućnost u odnosu na trenutak slanja.

```json
{
  "title": "Izložba mladih umjetnika",
  "description": "Poziv za izlaganje radova u galeriji.",
  "typeId": "a1b2c3d4-0001-4e5f-8a9b-000000000001",
  "artFieldIds": ["3f1c2a8e-6b1d-4c7e-9a52-1d0e7b4c9f10"],
  "deadline": "2027-01-10T23:00:00+01:00",
  "criteria": "Prijaviti najviše tri rada.",
  "prize": null,
  "location": {
    "type": "PHYSICAL",
    "streetAddress": "Ilica 1",
    "city": "Zagreb",
    "postalCode": "10000",
    "latitude": 45.8131234,
    "longitude": 15.9771234
  },
  "eventStart": "2027-02-01T18:00:00+01:00",
  "eventEnd": "2027-02-10T20:00:00+01:00"
}
```

### Primjer online prilike

```json
{
  "title": "Online radionica ilustracije",
  "description": "Zajednička radionica za mlade ilustratore.",
  "typeId": "a1b2c3d4-0001-4e5f-8a9b-000000000003",
  "artFieldIds": ["3f1c2a8e-6b1d-4c7e-9a52-1d0e7b4c9f10"],
  "deadline": "2027-01-10T23:00:00+01:00",
  "location": {
    "type": "ONLINE",
    "onlineUrl": "https://example.org/radionica"
  }
}
```

### Odgovor

Uspjeh vraća `201 Created`, zaglavlje
`Location: /api/v1/opportunities/{id}` i puni objekt prilike:

- `id`, `title`, `description`, `deadline`, `criteria`, `prize`;
- `type` kao `{ id, code, name }` i `artFields` kao niz takvih objekata;
- `location`, `eventStart`, `eventEnd`;
- `status = PREPARATION`, `archived = false`, `hiddenByAdmin = false`, `publishedAt = null`;
- `organizer` kao `{ id, displayName, organizationName }`; naziv organizacije može biti `null`;
- `createdAt` i `updatedAt` kao datum i vrijeme s pomakom;
- `hasResults = false`, `myApplicationId = null`.

Vlasnik je uvijek trenutni korisnik. Klijent ne određuje vlasnika, status ni
vrijeme stvaranja. Spremanje prilike i veza s umjetničkim područjima obavlja
se zajedno; odbijeni zahtjev ne stvara priliku.

Ova ruta sprema nacrt. Uređivanje je opisano u nastavku; javni dohvat, objava i
definiranje zahtjeva prijave zasebni su zadaci. Zaglavlje `Location` određuje
adresu resursa; stvaranje ne implementira `GET` na toj adresi.

### Greške

| HTTP | Kod | Razlog |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | obavezna polja, ograničenja, duplikati područja, lokacija, rok, datumi događanja ili nepostojeći/neaktivni šifrarnici |
| 401 | `UNAUTHENTICATED` | nema valjane sesije |
| 403 | `FORBIDDEN` | korisnik nema ulogu organizatora, račun nije aktivan ili nedostaje valjan CSRF token |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | organizator je blokiran |

Greške koriste zajedničku omotnicu. Primjer za nedostajuću koordinatu:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Podaci prilike nisu ispravni.",
    "details": [{
      "field": "location.latitude",
      "issue": "Zemljopisna širina je obavezna za fizičku lokaciju."
    }]
  }
}
```

Provjere se izvršavaju prije spremanja. Odgovor može sadržavati jednu ili više
grešaka; nakon ispravka prikazanih polja mogu se pokazati ostale greške.
Neispravan JSON ili vrijednosti koje se ne mogu pretvoriti u očekivani tip
vraćaju zajedničku grešku validacije tijela zahtjeva.

## PUT /opportunities/{opportunityId}

`PUT /api/v1/opportunities/{opportunityId}` uređuje vlastitu priliku. Pristup ima
prijavljeni, aktivni organizator, uključujući organizatora na čekanju odobrenja.
Potrebni su sesijski kolačić i valjan CSRF token kao za stvaranje prilike.

### Podaci zahtjeva i pravila

Tijelo je isti `OpportunityInput` kao za [stvaranje](#post-opportunities).
Zamjenjuje sva unosiva polja i cijeli skup `artFieldIds`. Izostavljene opcionalne
vrijednosti brišu prethodne kriterije, nagradu ili datume događanja.
Prazan skup područja nije dopušten: i dalje treba odabrati 1–10 različitih područja.

Vrijede ista ograničenja tekstova, lokacije i datuma događanja uz dvije razlike:

- `deadline` je obavezan, ali smije biti jednak sadašnjem trenutku ili u prošlosti.
- Neaktivna vrsta smije ostati samo ako je trenutačna vrsta te prilike.
  Neaktivno područje smije ostati samo ako je već povezano s tom prilikom.
  Dodavanje druge neaktivne vrste ili novog neaktivnog područja vraća `400`.
  Nepostojeći šifrarnici također se odbijaju.

Uređivanje je dopušteno u svakom statusu, uključujući arhivirane i skrivene
prilike. Zahtjev ne mijenja vlasnika, status, arhiviranost, skrivenost,
`hiddenAt`, `publishedAt` ni `createdAt`. Postavlja `updatedAt` na trenutačno vrijeme.
Prošli rok ne zatvara priliku u ovom zahtjevu; automatsko zatvaranje zaseban je zadatak.

Lokacija se u cijelosti zamjenjuje. Promjena `PHYSICAL` u `ONLINE` briše adresu,
grad, poštanski broj i koordinate; promjena u `PHYSICAL` briše online poveznicu.
Sve provjere prethode promjenama, a prilika i razlika veza s područjima spremaju
se u jednoj transakciji. Odbijeni zahtjev ne mijenja postojeću priliku.

### Odgovor i greške

Uspjeh vraća `200 OK` s punim objektom prilike u istom obliku kao kod stvaranja,
uz ažurirane vrijednosti i očuvane podatke o statusu i vlasniku.

| HTTP | Kod | Razlog |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | neispravan UUID, JSON ili unos, nepostojeći šifrarnici ili nove neaktivne stavke |
| 401 | `UNAUTHENTICATED` | nema valjane sesije |
| 403 | `FORBIDDEN` | korisnik nije organizator ili vlasnik, račun nije aktivan ili CSRF token nije valjan |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | organizator je blokiran |
| 404 | `NOT_FOUND` | prilika s navedenim ID-om ne postoji |

Greške koriste istu zajedničku omotnicu kao kod stvaranja.

## POST /opportunities/{opportunityId}/status

`POST /api/v1/opportunities/{opportunityId}/status` mijenja status vlastite prilike.
Pristup ima prijavljeni, aktivni organizator uz sesijski kolačić i valjan CSRF
token, kao kod uređivanja. Tijelo je `{ "status": "OPEN" }`; obavezni `status`
prihvaća samo `OPEN`, `CLOSED` i `SELECTION`.

| Trenutni status | Traženi status | Uvjet |
| --- | --- | --- |
| `PREPARATION` | `OPEN` | `deadline` mora biti strogo u budućnosti |
| `OPEN` | `CLOSED` | neovisno o roku |
| `CLOSED` | `OPEN` | `deadline` mora biti strogo u budućnosti |
| `CLOSED` | `SELECTION` | neovisno o roku |

Svaki drugi par, uključujući isti status, vraća `409 INVALID_STATUS_TRANSITION`.
Arhivirana prilika odbija svaku promjenu tim kodom, neovisno o roku.
Rok jednak trenutačnom vremenu već je istekao. Datumi se uspoređuju kao stvarni
trenuci, neovisno o vremenskom pomaku. `FINISHED` se postiže isključivo objavom
rezultata kroz zasebnu rutu; `PREPARATION` se postavlja stvaranjem nacrta.

Uspjeh vraća `200 OK` s punim objektom prilike kao kod stvaranja. Mijenjaju se
samo `status`, `updatedAt` i, pri prvom otvaranju, `publishedAt`. Ponovno otvaranje
čuva vrijeme prve objave. Provjere prethode izmjenama; sve se sprema u jednoj
transakciji i odbijeni zahtjev ne mijenja podatke.

| HTTP | Kod | Razlog |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | neispravan UUID ili JSON, nedostajući ili nepoznat status, uključujući `PREPARATION` i `FINISHED` |
| 401 | `UNAUTHENTICATED` | nema valjane sesije |
| 403 | `FORBIDDEN` | korisnik nije aktivni organizator ili vlasnik, ili CSRF token nije valjan |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | organizator je blokiran |
| 404 | `NOT_FOUND` | prilika ne postoji |
| 409 | `INVALID_STATUS_TRANSITION` | nedopušten prijelaz ili arhivirana prilika |
| 409 | `DEADLINE_PASSED` | pokušaj prvog ili ponovnog otvaranja na isteku roka ili nakon njega |

Prema dogovoru u [issueu #131](https://github.com/LD05-fk/Strumfovi/issues/131),
do implementacije ADM-02-BE svi organizatori smatraju se odobrenima za objavu,
uključujući postojeći `PENDING` račun. `OpportunityStatusMachine` već prima
`organizerApproved`; ADM-02-BE treba proslijediti stvarno odobrenje. Tada prva
objava bez odobrenja vraća `403 ORGANIZER_NOT_APPROVED`.

`OpportunityStatusMachine` sadrži pravila bez Springa i pristupa bazi. Posao
automatskog zatvaranja (OPP-04-BE1) može koristiti istu provjeru za `OPEN → CLOSED`;
taj posao nije dio ove rute. [Dijagram stanja](../puml/4-6-artconnect-opportunity-SM.puml)
prikazuje životni ciklus i zasebnu objavu rezultata.

## POST /opportunities/{opportunityId}/archive

`POST /api/v1/opportunities/{opportunityId}/archive` arhivira vlastitu priliku.
Pristup ima prijavljeni, aktivni organizator uz sesijski kolačić i valjan CSRF
token, kao kod promjene statusa. Zahtjev nema tijelo.

Arhiviranje je dopušteno u svakom statusu, neovisno o roku i o tome ima li prilika
prijave. Prilika u statusu `OPEN` istovremeno prelazi u `CLOSED`; ostali statusi
ostaju nepromijenjeni. Zahtjev je idempotentan: arhiviranje već arhivirane prilike
vraća `200 OK` i ne mijenja ni status ni `updatedAt`.

Uspjeh vraća `200 OK` s punim objektom prilike kao kod stvaranja, uz `archived = true`
i ažurirani `updatedAt`. Vraćanja iz arhive nema, a arhivirana prilika odbija svaku
daljnju promjenu statusa s `409 INVALID_STATUS_TRANSITION`. Arhivirana prilika nije
u javnoj pretrazi, ali ostaje dostupna organizatoru.

| HTTP | Kod | Razlog |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | neispravan UUID |
| 401 | `UNAUTHENTICATED` | nema valjane sesije |
| 403 | `FORBIDDEN` | korisnik nije organizator ili vlasnik, račun nije aktivan ili CSRF token nije valjan |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | organizator je blokiran |
| 404 | `NOT_FOUND` | prilika s navedenim ID-om ne postoji |

Greške koriste istu zajedničku omotnicu kao kod stvaranja.

## DELETE /opportunities/{opportunityId}

`DELETE /api/v1/opportunities/{opportunityId}` briše vlastitu priliku. Pristup ima
prijavljeni, aktivni organizator uz sesijski kolačić i valjan CSRF token, kao kod
uređivanja. Zahtjev nema tijelo.

Brisanje je dopušteno u svakom statusu, uključujući arhivirane i skrivene prilike.
Uspjeh vraća `204 No Content` bez tijela. Uz red u `opportunities` kaskadno nestaju
i veze u `opportunity_art_fields`. Ponovljeni zahtjev za istim ID-om vraća `404`.

Prilika koja ima prijave ne briše se nego arhivira. Provjera je izdvojena u sučelje
`OpportunityDeletionGuard`; u Fazi 1 tablica `applications` još ne postoji pa
implementacija uvijek dopušta brisanje, a OPP-04-BE2 je zamjenjuje stvarnom
provjerom i tek tada je `409 HAS_APPLICATIONS` moguć. Vlasništvo se provjerava
prije te provjere, pa tuđa prilika uvijek vraća `403`, neovisno o prijavama.

| HTTP | Kod | Razlog |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | neispravan UUID |
| 401 | `UNAUTHENTICATED` | nema valjane sesije |
| 403 | `FORBIDDEN` | korisnik nije organizator ili vlasnik, račun nije aktivan ili CSRF token nije valjan |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | organizator je blokiran |
| 404 | `NOT_FOUND` | prilika s navedenim ID-om ne postoji |
| 409 | `HAS_APPLICATIONS` | prilika ima prijave (aktivno od OPP-04-BE2) |

Greške koriste istu zajedničku omotnicu kao kod stvaranja.

## `GET /api/v1/geocoding/search`

Geokodira adresu preko OSM Nominatima. Frontend poziva samo ArtConnect API; javni Nominatim poziva isključivo backend. Rezultati se ne spremaju u bazu.

**Pristup:** prijavljeni korisnik s ulogom `ORGANIZER`, neovisno o tome je li organizator odobren. Organizator može geokodirati dok priprema priliku; odobrenje je potrebno zasebno za objavu prilike.

| Parametar | Obavezno | Ograničenje |
| --- | --- | --- |
| `street` | da | 3–200 znakova |
| `city` | da | 2–100 znakova |
| `postalCode` | ne | najviše 10 znakova |

Uspješan odgovor sadrži najviše pet rezultata:

```json
{
  "data": [{
    "displayName": "Unska 3, Zagreb",
    "latitude": 45.801235,
    "longitude": 15.971235,
    "city": "Zagreb",
    "postalCode": "10000"
  }]
}
```

Ako Nominatim ne pronađe adresu, odgovor je `200` s `"data": []`; organizator tada ručno postavlja oznaku na karti.

Backend traži Nominatimov `/search` s `format=jsonv2`, `addressdetails=1`, `limit=5` i `accept-language=hr`. Grad se bira redoslijedom `city`, `town`, `village`, `municipality`; koordinate se pretvaraju u brojeve i zaokružuju na šest decimala. Svaki zahtjev šalje `User-Agent: ArtConnect/1.0 (+https://artconnect-fer.duckdns.org)`.

Zahtjevi prema Nominatimu globalno su ograničeni na najviše jedan u sekundi. Zahtjev čeka slobodan termin najviše dvije sekunde; nakon toga API vraća `429 RATE_LIMITED`. Timeout, mrežna greška, HTTP 4xx/5xx ili neispravan odgovor vanjskog servisa vraća `502 GEOCODING_UNAVAILABLE`. Detalji vanjskog odgovora ne šalju se klijentu.

| HTTP | Kod greške | Značenje |
| --- | --- | --- |
| 200 | — | Rezultati su u `data`; nema rezultata znači prazan niz. |
| 400 | `VALIDATION_ERROR` | Nedostaje parametar ili ne zadovoljava ograničenje duljine. |
| 401 | `UNAUTHENTICATED` | Nema valjane sesije. |
| 403 | `FORBIDDEN` | Prijavljeni korisnik nema ulogu organizatora. |
| 429 | `RATE_LIMITED` | Globalni limit Nominatima nije dostupan unutar dvije sekunde. |
| 502 | `GEOCODING_UNAVAILABLE` | Nominatim nije dostupan ili je vratio neispravan odgovor. |

U testovima se `artconnect.geocoding.base-url` usmjerava na lokalni lažni servis; testovi nikad ne pozivaju `nominatim.openstreetmap.org`.
