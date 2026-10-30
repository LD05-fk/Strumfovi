# Portfolio umjetnika

Endpointi za javni profil umjetnika i portfolio umjetnika. Sve putanje počinju s `/api/v1`.

Javni profil ne traži prijavu ni CSRF token. Za upravljanje vlastitim portfoliom
potrebna je prijava i uloga `ARTIST`, a nesigurne metode traže i CSRF zaglavlje
`X-XSRF-TOKEN` iz kolačića `XSRF-TOKEN`. Vlasnik zapisa uvijek je prijavljeni
korisnik — `artist_id` se ne prima iz tijela zahtjeva i ne može se proturiti.

Zapisi se vežu na profil umjetnika. Red u `artist_profiles` ne nastaje pri odabiru
uloge, pa ga prvi unos u portfolio stvara kao prazan profil — umjetnik ne mora
prvo spremiti biografiju. `403 ONBOARDING_REQUIRED` dobiva korisnik koji je još u
statusu `ONBOARDING`, odnosno nije odabrao ulogu.

## GET /artists/{artistId}

Javno vraća profil umjetnika i njegove ručno unesene prethodne izložbe. Odgovor
`200 OK` koristi ista polja kao `ArtistProfile`, uz `pastExhibitions` i
`participations`:

```json
{
  "userId": "0b6a5c1e-3f2d-4e8b-9a7c-1d2e3f4a5b6c",
  "displayName": "Ana Horvat",
  "avatarUrl": "https://lh3.googleusercontent.com/a/ana-horvat",
  "biography": "Akademska slikarica iz Splita.",
  "artistStatement": "U radu istražujem odnos mora, svjetla i sjećanja.",
  "artFields": [],
  "links": [],
  "pastExhibitions": [
    {
      "id": "c3d4e5f6-7a8b-4c90-9d12-3e4f5a6b7c08",
      "title": "Gradski motivi",
      "year": 2024,
      "place": "Galerija Nova, Zagreb"
    }
  ],
  "participations": []
}
```

`pastExhibitions` sadrži `id`, `title`, `year` i `place`, poredane po godini
silazno, pa po vremenu stvaranja silazno. Umjetnik bez retka u
`artist_profiles` vraća se s praznim profilom, kao kod `GET /me/artist-profile`.
`participations` je u Fazi 1 uvijek prazan niz; stvarni podaci pune se od
EPIC-05. Endpoint se koristi i za prikaz prethodnih izložbi vlasnika na
`/profile`.

Odgovor `404 NOT_FOUND` vraća se ako korisnik ne postoji, nema ulogu `ARTIST`
ili je u statusu `BLOCKED`, `DELETED`, `ONBOARDING` ili `INVITED`. Vlasnik i
administrator mogu vidjeti blokiran profil; obrisani profil nije javan ni njima.
Neispravan UUID također vraća `404 NOT_FOUND`.

## GET /me/artist-profile

Vraća profil prijavljenog umjetnika za obrazac na stranici `/profile`.
`displayName` i `avatarUrl` dolaze iz korisnikova Google profila i ne uređuju se
kroz ovaj API. Ostala polja dolaze iz profila umjetnika i njegovih povezanih
područja umjetnosti i poveznica.

Odgovor `200 OK` (`ArtistProfile`):

```json
{
  "userId": "0b6a5c1e-3f2d-4e8b-9a7c-1d2e3f4a5b6c",
  "displayName": "Ana Horvat",
  "avatarUrl": "https://lh3.googleusercontent.com/a/ana-horvat",
  "biography": "Akademska slikarica iz Splita.",
  "artistStatement": "U radu istražujem odnos mora, svjetla i sjećanja.",
  "artFields": [
    { "id": "a1f0c2d3-4b5e-4f60-8a71-9b2c3d4e5f01", "code": "slikarstvo", "name": "Slikarstvo" }
  ],
  "links": [
    { "label": "Instagram", "url": "https://www.instagram.com/ana.horvat.art" }
  ]
}
```

Ako profilni red još ne postoji, odgovor sadrži `biography` i `artistStatement`
kao `null`, a `artFields` i `links` kao prazne nizove. Dohvat ne stvara red.
Vrijednosti `avatarUrl` i `label` mogu biti `null`. Područja se vraćaju
poredana po `art_fields.position`, a poveznice po `artist_links.position`;
jednake pozicije razrješavaju se uzlazno po ID-u. Vraćaju se i neaktivna
područja koja su već povezana s profilom.

### Greške

| Status | Kod | Kada |
| --- | --- | --- |
| 401 | `UNAUTHENTICATED` | zahtjev bez valjane sesije |
| 403 | `FORBIDDEN` | prijavljeni korisnik nema ulogu `ARTIST` |
| 403 | `ONBOARDING_REQUIRED` | korisnik je u statusu `ONBOARDING` i nije odabrao ulogu |
| 403 | `USER_BLOCKED` | prijavljeni korisnik ima status `BLOCKED` |

## PUT /me/artist-profile

Potpuno zamjenjuje uređivački dio profila prijavljenog umjetnika. Zahtjev
zahtijeva ulogu `ARTIST`, valjanu sesiju i CSRF zaglavlje `X-XSRF-TOKEN`.
`userId`, `displayName` i `avatarUrl` ne mogu se mijenjati ovim endpointom.

Tijelo zahtjeva (`ArtistProfileUpdate`):

| Polje | Tip | Ograničenja |
| --- | --- | --- |
| `biography` | string ili `null` | neobavezno, najviše 5000 znakova |
| `artistStatement` | string ili `null` | neobavezno, najviše 5000 znakova |
| `artFieldIds` | UUID niz | obavezno, najviše 10 jedinstvenih identifikatora |
| `links` | niz poveznica | obavezno, najviše 10 poveznica |
| `links[].label` | string ili `null` | neobavezno, najviše 60 znakova |
| `links[].url` | string | obavezno, najviše 1024 znaka; valjani HTTP(S) URI s hostom |

Primjer zahtjeva:

```http
PUT /api/v1/me/artist-profile
Content-Type: application/json
X-XSRF-TOKEN: <token iz kolačića XSRF-TOKEN>

{
  "biography": "Akademska slikarica iz Splita.",
  "artistStatement": "U radu istražujem odnos mora, svjetla i sjećanja.",
  "artFieldIds": ["a1f0c2d3-4b5e-4f60-8a71-9b2c3d4e5f01"],
  "links": [
    { "label": "Instagram", "url": "https://www.instagram.com/ana.horvat.art" }
  ]
}
```

Odgovor je `200 OK` sa spremljenim `ArtistProfile` prikazanim u primjeru za
`GET /me/artist-profile`.

### Pravila spremanja

- Profil se stvara ako još ne postoji ili se postojeći ažurira. `updated_at` se
  postavlja pri svakom uspješnom spremanju.
- `artFieldIds` i `links` potpuno zamjenjuju postojeće veze u istoj transakciji
  kao profil. Prazni nizovi brišu sve veze. Poveznice dobivaju `position` prema
  indeksu u zahtjevu, počevši od nule.
- Svaki identifikator područja mora postojati u šifrarniku. Neaktivno područje
  prihvaća se samo ako je već povezano s profilom; time se zadržavaju postojeće
  veze, ali se ne mogu odabrati u novom unosu. Duplikati nisu dopušteni.
- Vodeće i prateće praznine se uklanjaju iz tekstova. Prazna ili samo
  bjelinama ispunjena oznaka poveznice sprema se kao `null`.
- URL mora imati shemu `http` ili `https`, biti sintaktički valjan URI i imati
  neprazan host.

### Greške

| Status | Kod | Kada |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | neispravne vrijednosti ili ograničenja zahtjeva; `error.details[].field` navodi polje, npr. `links[2].url` ili `artFieldIds` |
| 401 | `UNAUTHENTICATED` | zahtjev bez valjane sesije |
| 403 | `FORBIDDEN` | korisnik nije umjetnik ili nedostaje/neispravan je CSRF token |
| 403 | `ONBOARDING_REQUIRED` | korisnik nije odabrao ulogu |
| 403 | `USER_BLOCKED` | korisnički račun je blokiran |

## GET /me/organizer-profile

Vraća profil prijavljenog organizatora. `displayName` i `organizerApproval`
čitaju se iz korisnika, a `organizationName` iz `organizer_profiles`.
Organizacija je samo naziv; profil ne sadrži zasebne podatke o organizaciji.

Odgovor `200 OK` (`OrganizerProfile`):

```json
{
  "userId": "5e7d9c2b-1a4f-4b3e-8c6d-7f8a9b0c1d2e",
  "displayName": "Marko Kovačević",
  "organizationName": "Galerija Kontrast",
  "organizerApproval": "APPROVED"
}
```

Ako red u `organizer_profiles` ne postoji, `organizationName` je `null`; GET
ne stvara profilni red. Organizator koji čeka odobrenje može čitati profil.

| Status | Kod | Kada |
| --- | --- | --- |
| 401 | `UNAUTHENTICATED` | zahtjev bez valjane sesije |
| 403 | `FORBIDDEN` | prijavljeni korisnik nema ulogu `ORGANIZER` |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | prijavljeni korisnik ima status `BLOCKED` |

## PUT /me/organizer-profile

Uređuje naziv organizacije profila prijavljenog organizatora. Zahtjev šalje
CSRF token iz kolačića `XSRF-TOKEN` u zaglavlju `X-XSRF-TOKEN`.

Tijelo zahtjeva (`OrganizerProfileUpdate`):

| Polje | Tip | Opis |
| --- | --- | --- |
| `organizationName` | string ili `null` | neobavezno, najviše 200 znakova |

Primjer:

```http
PUT /api/v1/me/organizer-profile
Content-Type: application/json
X-XSRF-TOKEN: <token iz kolačića XSRF-TOKEN>

{ "organizationName": "Galerija Kontrast" }
```

Odgovor `200 OK` je `OrganizerProfile`, istog oblika kao GET. Naziv se obrezuje
prije pohrane. `null`, izostavljeno polje, prazan niz i niz sastavljen samo od
praznina spremaju se kao `null`. Ako profilni red ne postoji, PUT ga stvara.
Svaki uspješan PUT osvježava `updated_at`; `organizerApproval` se ne mijenja.
Organizator koji čeka odobrenje smije urediti profil.

| Status | Kod | Kada |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `organizationName` dulji je od 200 znakova ili tijelo nije ispravno |
| 401 | `UNAUTHENTICATED` | zahtjev bez valjane sesije |
| 403 | `FORBIDDEN` | korisnik nema ulogu `ORGANIZER` ili nedostaje/nevaljan je CSRF token |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | prijavljeni korisnik ima status `BLOCKED` |

## POST /past-exhibitions

Dodaje prethodnu izložbu ili projekt u portfolio prijavljenog umjetnika.
Podatke upisuje umjetnik ručno; ništa se ne provjerava u vanjskim izvorima.

Tijelo zahtjeva (`PastExhibitionInput`), sva polja su obavezna:

| Polje | Tip | Opis |
| --- | --- | --- |
| `title` | string | naslov izložbe ili projekta, 1–200 znakova nakon obrezivanja |
| `year` | integer | godina, od 1900 do tekuće godine u zoni `Europe/Zagreb` |
| `place` | string | mjesto ili ustanova, 1–200 znakova nakon obrezivanja |

Primjer zahtjeva:

```http
POST /api/v1/past-exhibitions
Content-Type: application/json
X-XSRF-TOKEN: <token iz kolačića XSRF-TOKEN>

{
  "title": "Gradski motivi",
  "year": 2024,
  "place": "Galerija Nova, Zagreb"
}
```

Odgovor `201 Created` (`PastExhibition`), uz zaglavlje
`Location: /api/v1/past-exhibitions/{id}`:

```json
{
  "id": "c3d4e5f6-7a8b-4c90-9d12-3e4f5a6b7c08",
  "title": "Gradski motivi",
  "year": 2024,
  "place": "Galerija Nova, Zagreb"
}
```

### PastExhibition

| Polje | Tip | Opis |
| --- | --- | --- |
| `id` | UUID string | identifikator zapisa, dodjeljuje ga poslužitelj |
| `title` | string | obrezani naslov |
| `year` | integer | godina |
| `place` | string | obrezano mjesto |

Nuspojava uspješnog zahtjeva je novi red u tablici `past_exhibitions`,
vezan na profil prijavljenog umjetnika. Ako umjetnik još nema red u
`artist_profiles`, stvara se prazan (`biography` i `artistStatement` su `null`) u
istoj transakciji; postojeći profil se ne mijenja.

### Poslovna pravila

- `title` i `place` se obrezuju prije pohrane, pa vodeće i prateće praznine ne
  ulaze u bazu. Tekst sastavljen samo od praznina jednak je praznom i vraća `400`.
- Donja granica godine je 1900. Gornja granica je **tekuća godina u zoni
  `Europe/Zagreb`**, pa se nova godina počinje prihvaćati u ponoć po lokalnom
  vremenu, a ne po UTC-u.
- Zapis se nakon stvaranja ne mijenja ovim endpointom; za uređivanje služi
  `PUT /past-exhibitions/{pastExhibitionId}`.

### Greške

| Status | Kod | Kada |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | prazan ili predug `title`/`place`, `year` izvan 1900–tekuća, neispravan JSON |
| 401 | `UNAUTHENTICATED` | zahtjev bez prijave |
| 403 | `FORBIDDEN` | prijavljeni korisnik nije umjetnik, ili CSRF token nedostaje |
| 403 | `ONBOARDING_REQUIRED` | korisnik je u statusu `ONBOARDING` i nije odabrao ulogu |

Neispravna polja navedena su u `error.details`, svako sa svojim imenom. Primjer
za godinu u budućnosti:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Neki podaci nisu ispravni.",
    "details": [
      { "field": "year", "issue": "Godina ne smije biti u budućnosti." }
    ]
  }
}
```

### Provjera

Jedinični testovi pokrivaju granicu godine s fiksnim satom
(`NotInFutureValidatorTest`, uključujući prijelaz 31. 12. u 1. 1. u zagrebačkoj
zoni) i pravila servisa (`PastExhibitionServiceTest`). Integracijski testovi
(`PastExhibitionIntegrationTest`) pokrivaju svaki dokumentirani status: 201 sa
zaglavljem `Location` i obrezanim vrijednostima u bazi, 400 za svako neispravno
polje, 401, 403 za organizatora i 403 bez CSRF tokena. Umjetnik bez reda u
`artist_profiles` dobiva 201, a profil mu se stvori prazan.

## PUT /past-exhibitions/{pastExhibitionId}

Uređuje prethodnu izložbu prijavljenog umjetnika. Tijelo (`PastExhibitionInput`),
ograničenja i oblik odgovora isti su kao kod `POST /past-exhibitions`; šalju se
sva tri polja.

Odgovor `200 OK` (`PastExhibition`) s ažuriranim vrijednostima; `id` se ne mijenja.
Nuspojava je ažuriran red u `past_exhibitions`.

### Greške

| Status | Kod | Kada |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | kao kod `POST`, i neispravan `pastExhibitionId` |
| 401 | `UNAUTHENTICATED` | zahtjev bez prijave |
| 403 | `FORBIDDEN` | prijavljeni korisnik nije umjetnik, ili CSRF token nedostaje |
| 403 | `ONBOARDING_REQUIRED` | korisnik je u statusu `ONBOARDING` i nije odabrao ulogu |
| 404 | `NOT_FOUND` | izložba ne postoji ili pripada drugom umjetniku |

Tuđa izložba vraća isti odgovor kao nepostojeća, pa se postojanje tuđih zapisa
ne otkriva.

### Provjera

`PastExhibitionServiceTest` pokriva provjeru vlasništva, a
`PastExhibitionIntegrationTest` svaki dokumentirani status: 200 s obrezanim
vrijednostima u bazi, 400, 401, 403 za organizatora te 404 za tuđu i
nepostojeću izložbu.

## DELETE /past-exhibitions/{pastExhibitionId}

Briše izložbu prijavljenog umjetnika. Zahtjev nema tijelo.

Odgovor `204 No Content` bez tijela. Rezultat je obrisan red u
`past_exhibitions`, ostali zapisi portfolija ostaju nepromijenjeni.

### Greške

| Status | Kod | Kada |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | neispravan `pastExhibitionId` |
| 401 | `UNAUTHENTICATED` | zahtjev bez prijave |
| 403 | `FORBIDDEN` | prijavljeni korisnik nije umjetnik, ili CSRF token nedostaje |
| 403 | `ONBOARDING_REQUIRED` | korisnik je u statusu `ONBOARDING` i nije odabrao ulogu |
| 404 | `NOT_FOUND` | izložba ne postoji ili pripada drugom umjetniku |

Kao i kod `PUT`, tuđa izložba vraća isti odgovor kao nepostojeća. Ponovno
brisanje istog zapisa zato vraća `404`.

### Provjera

`PastExhibitionServiceTest` pokriva brisanje vlastite izložbe i odbijanje tuđe, a
`PastExhibitionIntegrationTest` svaki dokumentirani status: 204 s obrisanim redom,
401, 403 za organizatora i bez CSRF tokena te 404 za tuđu i nepostojeću izložbu.
