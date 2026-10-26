# Djela u portfoliju

Sve putanje počinju s `/api/v1`. Za stvaranje djela potrebna je prijava s
ulogom `ARTIST` i CSRF token u zaglavlju `X-XSRF-TOKEN` iz kolačića
`XSRF-TOKEN`. Vlasnik djela uvijek je prijavljeni korisnik; `artistId` se ne
prima u zahtjevu. Djelo se stvara bez slika; slike se dodaju zasebnim pozivom
nakon što djelo dobije ID.

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
