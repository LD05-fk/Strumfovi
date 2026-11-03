# Djela u portfoliju

Sve putanje počinju s `/api/v1`. Za stvaranje djela potrebna je prijava s
ulogom `ARTIST` i CSRF token u zaglavlju `X-XSRF-TOKEN` iz kolačića
`XSRF-TOKEN`. Vlasnik djela uvijek je prijavljeni korisnik; `artistId` se ne
prima u zahtjevu. Djelo se stvara bez slika; slike se dodaju zasebnim pozivom
nakon što djelo dobije ID.

## GET /artists/{artistId}/artworks

Vraća stranicu djela umjetnika, sortiranu po `createdAt` silazno. Putanja je
javna; ako postoji sesija, prijavljeni korisnik uzima se u obzir pri odluci o
vidljivosti.

| Parametar | Tip | Zadano | Pravila |
| --- | --- | --- | --- |
| `artistId` | UUID | — | ID korisnika s ulogom `ARTIST` |
| `page` | integer | `1` | najmanje `1` |
| `pageSize` | integer | `20` | od `1` do `100` |

Odgovor `200 OK` (`ArtworkPage`) sadrži `data: Artwork[]` i `meta` s poljima
`page`, `pageSize` i `total`. Umjetnik bez djela vraća `data: []` i `total: 0`.

## GET /artworks/{artworkId}

Vraća detalj djela (`Artwork`) s pripadajućim slikama. Putanja je javna; ako
postoji sesija, prijavljeni korisnik uzima se u obzir pri odluci o vidljivosti.

### Pravila vidljivosti za oba GET endpointa

| Pozivatelj | Umjetnik | Djela koja može vidjeti |
| --- | --- | --- |
| Posjetitelj ili drugi korisnik | aktivan umjetnik | samo `PUBLIC` i `hiddenByAdmin: false` |
| Vlasnik portfolija | umjetnik s bilo kojim statusom | sva djela, uključujući `PRIVATE` i skrivena |
| Administrator | umjetnik s bilo kojim statusom | sva djela, uključujući `PRIVATE` i skrivena |
| Posjetitelj ili drugi korisnik | blokiran ili obrisan umjetnik | ništa; popis i detalj vraćaju `404 NOT_FOUND` |

Za popis, nepostojeći korisnik ili korisnik koji nije umjetnik također vraća
`404 NOT_FOUND`. Detalj djela vraća `404 NOT_FOUND` ako djelo ne postoji ili ga
pozivatelj ne smije vidjeti; time se ne otkriva postojanje privatnih ili
skrivenih djela. Neispravni parametri straničenja vraćaju
`400 VALIDATION_ERROR`.

## POST /artworks

Stvara djelo u portfoliju prijavljenog umjetnika.

Tijelo zahtjeva (`ArtworkInput`):

| Polje | Tip | Opis |
| --- | --- | --- |
| `title` | string | obavezno, 1–200 znakova nakon obrezivanja |
| `year` | integer | obavezno, od 1900 do tekuće godine u zoni `Europe/Zagreb` |
| `technique` | string | obavezno, 1–200 znakova nakon obrezivanja |
| `dimensions` | string ili `null` | neobavezno, najviše 100 znakova |
| `description` | string ili `null` | neobavezno, najviše 5000 znakova |
| `visibility` | `PUBLIC` ili `PRIVATE` | obavezno; zadana vrijednost baze ne zamjenjuje izostavljeno polje |

Primjer:

```http
POST /api/v1/artworks
Content-Type: application/json
X-XSRF-TOKEN: <token iz kolačića XSRF-TOKEN>

{
  "title": "Morska tišina",
  "year": 2024,
  "technique": "Ulje na platnu",
  "dimensions": "40 x 50 cm",
  "description": "Pogled na more u sumrak.",
  "visibility": "PUBLIC"
}
```

Odgovor `201 Created` (`Artwork`), uz zaglavlje
`Location: /api/v1/artworks/{id}`:

```json
{
  "id": "c3d4e5f6-7a8b-4c90-9d12-3e4f5a6b7c08",
  "artistId": "0b6a5c1e-3f2d-4e8b-9a7c-1d2e3f4a5b6c",
  "title": "Morska tišina",
  "year": 2024,
  "technique": "Ulje na platnu",
  "dimensions": "40 x 50 cm",
  "description": "Pogled na more u sumrak.",
  "visibility": "PUBLIC",
  "hiddenByAdmin": false,
  "images": [],
  "createdAt": "2026-01-02T03:04:05Z"
}
```

`Artwork` sadrži `id`, `artistId`, `title`, `year`, `technique`, `dimensions`,
`description`, `visibility`, `hiddenByAdmin`, `images` i `createdAt`.
Neobavezna tekstualna polja izostavljena iz zahtjeva, `null` ili sastavljena
samo od praznina spremaju se kao `null`; svi tekstovi se obrezuju. Odgovor
stvaranja uvijek ima `hiddenByAdmin: false` i prazan niz `images`.

Slika djela u odgovoru (`ArtworkImage`) ima oblik:

```json
{
  "id": "a1f0c2d3-4b5e-4f60-8a71-9b2c3d4e5f01",
  "file": {
    "id": "b1f0c2d3-4b5e-4f60-8a71-9b2c3d4e5f02",
    "originalName": "morska.jpg",
    "contentType": "image/jpeg",
    "sizeBytes": 1234,
    "url": "/api/v1/files/b1f0c2d3-4b5e-4f60-8a71-9b2c3d4e5f02"
  },
  "altText": "Pogled na more"
}
```

### Poslovna pravila

- `artistId` je ID prijavljenog korisnika; tijelo ne može zadati drugog vlasnika.
- `title`, `year`, `technique` i `visibility` su obavezni. `visibility` mora biti
  izričito poslan unatoč zadanoj vrijednosti `PRIVATE` u bazi.
- Godina se provjerava postojećom validacijom od 1900 do tekuće godine u zoni
  `Europe/Zagreb`.
- Zahtjev stvara samo red u `artworks`; slike se učitavaju zasebnim zahtjevom
  nakon stvaranja djela.

### Greške

| Status | Kod | Kada |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | nedostajuće/neispravno polje, predug tekst, godina izvan dopuštenog raspona ili nepoznata vidljivost |
| 401 | `UNAUTHENTICATED` | zahtjev bez valjane sesije |
| 403 | `FORBIDDEN` | korisnik nema ulogu `ARTIST` ili nedostaje/nevaljan je CSRF token |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | prijavljeni korisnik je blokiran |

## PUT /artworks/{artworkId}

Ažurira podatke i vidljivost vlastitog djela prijavljenog umjetnika. Potrebna je
uloga `ARTIST`, valjana sesija i CSRF token u zaglavlju `X-XSRF-TOKEN` iz
kolačića `XSRF-TOKEN`. Zahtjev prima isti `ArtworkInput` i primjenjuje ista
pravila validacije kao `POST /artworks`; sva polja iz zahtjeva zamjenjuju
postojeće vrijednosti.

Slike se ovim pozivom ne mijenjaju. U odgovoru `200 OK` vraća se ažurirani
`Artwork` s postojećim slikama. Ako je administrator prethodno sakrio djelo,
uređivanje ne mijenja `hiddenByAdmin`; djelo ostaje skriveno dok ga administrator
ponovno ne učini vidljivim.

Nepostojeće djelo i djelo drugog umjetnika vraćaju isti `404 NOT_FOUND`.
Promjena `visibility` u `PRIVATE` odmah uklanja djelo iz javnog popisa.
Uspješno ažuriranje postavlja `updated_at` na trenutno vrijeme.

| Status | Kod | Kada |
| --- | --- | --- |
| 200 | — | djelo je ažurirano |
| 400 | `VALIDATION_ERROR` | nedostajuće/neispravno polje, predug tekst, godina izvan dopuštenog raspona ili nepoznata vidljivost |
| 401 | `UNAUTHENTICATED` | zahtjev bez valjane sesije |
| 403 | `FORBIDDEN` | korisnik nema ulogu `ARTIST` ili nedostaje/nevaljan je CSRF token |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | prijavljeni korisnik je blokiran |
| 404 | `NOT_FOUND` | djelo ne postoji ili pripada drugom umjetniku |

## DELETE /artworks/{artworkId}

Briše vlastito djelo prijavljenog umjetnika i sve njegove slike. Potrebna je
uloga `ARTIST`, valjana sesija i CSRF token u zaglavlju `X-XSRF-TOKEN` iz
kolačića `XSRF-TOKEN`. Uspješan odgovor je `204 No Content` bez tijela.

Redovi slika i pripadajući metapodaci pohranjenih datoteka brišu se u istoj
transakciji kao i djelo. Datoteke s volumena uklanja `FileStorageService` nakon
uspješnog commita. Buduća veza prijave na djelo (`application_items`) ovim se
endpointom ne mijenja.

Nepostojeće djelo i djelo drugog umjetnika vraćaju isti `404 NOT_FOUND`.

| Status | Kod | Kada |
| --- | --- | --- |
| 204 | — | djelo i njegove slike su obrisani; odgovor nema tijelo |
| 401 | `UNAUTHENTICATED` | zahtjev bez valjane sesije |
| 403 | `FORBIDDEN` | korisnik nema ulogu `ARTIST` ili nedostaje/nevaljan je CSRF token |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | prijavljeni korisnik je blokiran |
| 404 | `NOT_FOUND` | djelo ne postoji ili pripada drugom umjetniku |

## POST /artworks/{artworkId}/images

Učitava jednu sliku u portfolio prijavljenog umjetnika. Potrebni su aktivna
sesija s ulogom `ARTIST` i CSRF token u zaglavlju `X-XSRF-TOKEN` iz kolačića
`XSRF-TOKEN`.

Zahtjev je `multipart/form-data`:

| Polje | Tip | Pravila |
| --- | --- | --- |
| `file` | binary | obavezno, neprazno; podržani su JPEG i PNG, prepoznati prema sadržaju |
| `altText` | string | neobavezno, najviše 300 znakova; ako nedostaje ili je prazan, koristi se naslov djela |

Primjer:

```bash
curl -X POST \
  -H "X-XSRF-TOKEN: <token iz kolačića XSRF-TOKEN>" \
  -b "SESSION=<sesija>; XSRF-TOKEN=<token>" \
  -F "file=@morska.jpg;type=image/jpeg" \
  -F "altText=Pogled na more" \
  https://primjer.test/api/v1/artworks/{artworkId}/images
```

Odgovor `201 Created` sadrži `ArtworkImage`:

```json
{
  "id": "a1f0c2d3-4b5e-4f60-8a71-9b2c3d4e5f01",
  "file": {
    "id": "b1f0c2d3-4b5e-4f60-8a71-9b2c3d4e5f02",
    "originalName": "morska.jpg",
    "contentType": "image/jpeg",
    "sizeBytes": 1234,
    "url": "/api/v1/files/b1f0c2d3-4b5e-4f60-8a71-9b2c3d4e5f02"
  },
  "altText": "Pogled na more"
}
```

### Poslovna pravila

- Djelo mora postojati i pripadati prijavljenom umjetniku; nepostojeće i tuđe
  djelo jednako vraćaju `404 NOT_FOUND`.
- Provjere se provode ovim redoslijedom: vlasništvo, dopušteni broj slika,
  veličina datoteke pa vrsta sadržaja. Red djela zaključava se u transakciji
  (`PESSIMISTIC_WRITE`) prije brojanja i spremanja, pa paralelni zahtjevi ne
  mogu prijeći limit.
- Granice se dohvaćaju iz `system_settings` preko servisa postavki:
  `upload.artwork.maxImageBytes` (zadano `5242880` bajtova) i
  `upload.artwork.maxImagesPerArtwork` (zadano `3`). Učitavanje iznad limita
  veličine ili broja slika odbija se kodom navedenim u tablici grešaka.
- Samo JPEG i PNG sadržaj se prihvaća; naziv datoteke i MIME tip zahtjeva ne
  određuju vrstu. Prazna datoteka nije valjana.
- Nova slika dobiva `position` jedan veći od najveće postojeće vrijednosti;
  prva slika ima poziciju `0`. `stored_files.purpose` je `ARTWORK_IMAGE`, a
  `owner_id` prijavljeni korisnik. Uspjeh sprema datoteku na volumen, retke u
  `stored_files` i `artwork_images` te osvježava `artworks.updated_at`.
  Pohrana datoteke usklađena je s transakcijom: neuspjela transakcija uklanja
  već zapisanu datoteku.

### Greške

| Status | Kod | Kada |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | nedostaje dio `file`, datoteka je prazna ili `altText` ima više od 300 znakova |
| 401 | `UNAUTHENTICATED` | zahtjev bez valjane sesije |
| 403 | `FORBIDDEN` | korisnik nema ulogu `ARTIST` ili nedostaje/nevaljan je CSRF token |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 403 | `USER_BLOCKED` | prijavljeni korisnik je blokiran |
| 404 | `NOT_FOUND` | djelo ne postoji ili pripada drugom korisniku |
| 409 | `IMAGE_LIMIT_REACHED` | djelo već ima `upload.artwork.maxImagesPerArtwork` slika |
| 413 | `FILE_TOO_LARGE` | datoteka je veća od `upload.artwork.maxImageBytes` |
| 415 | `UNSUPPORTED_FILE_TYPE` | sadržaj nije JPEG ili PNG, uključujući PDF |

## DELETE /artworks/{artworkId}/images/{imageId}

Briše sliku iz djela prijavljenog umjetnika. Potrebni su aktivna sesija s
ulogom `ARTIST` i CSRF token u zaglavlju `X-XSRF-TOKEN` iz kolačića
`XSRF-TOKEN`.

Uspješan odgovor je `204 No Content` bez tijela.

Slika mora pripadati navedenom djelu, a djelo prijavljenom umjetniku.
Nepostojeće djelo, tuđe djelo ili slika koja ne pripada tom djelu vraćaju isti
odgovor `404 NOT_FOUND`. Brisanje uklanja retke iz `artwork_images` i
`stored_files`, a datoteku s volumena nakon commita transakcije. Ažurira se i
`artworks.updated_at`. Pozicije preostalih slika ne mijenjaju se; razmaci u
redoslijedu su dopušteni.

### Greške

| Status | Kod | Kada |
| --- | --- | --- |
| 401 | `UNAUTHENTICATED` | zahtjev bez valjane sesije |
| 403 | `FORBIDDEN` | korisnik nema ulogu `ARTIST` ili nedostaje/nevaljan je CSRF token |
| 403 | `ONBOARDING_REQUIRED` | korisnik još nije odabrao ulogu |
| 404 | `NOT_FOUND` | djelo ne postoji ili pripada drugom korisniku, ili slika ne pripada navedenom djelu |
