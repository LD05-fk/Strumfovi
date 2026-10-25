# Portfolio umjetnika

Endpointi kojima umjetnik vodi vlastiti portfolio. Sve putanje počinju s `/api/v1`.

Zahtjevi traže prijavu i ulogu `ARTIST`, a nesigurne metode i CSRF zaglavlje
`X-XSRF-TOKEN` iz kolačića `XSRF-TOKEN`. Vlasnik zapisa uvijek je prijavljeni
korisnik — `artist_id` se ne prima iz tijela zahtjeva i ne može se proturiti.

Zapisi se vežu na profil umjetnika. Red u `artist_profiles` ne nastaje pri odabiru
uloge, pa ga prvi unos u portfolio stvara kao prazan profil — umjetnik ne mora
prvo spremiti biografiju. `403 ONBOARDING_REQUIRED` dobiva korisnik koji je još u
statusu `ONBOARDING`, odnosno nije odabrao ulogu.

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
- Zapis se nakon stvaranja ne mijenja ovim endpointom; uređivanje i brisanje
  pripadaju zasebnim zadacima.

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
