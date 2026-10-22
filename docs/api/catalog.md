# Šifrarnici i javne postavke

Tri javna GET endpointa za obrasce, filtre i provjeru datoteka prije slanja.
Sve putanje počinju s `/api/v1`. Prijava i CSRF token nisu potrebni.

Administratorsko uređivanje pripada zasebnim zadacima.

## GET /art-fields

Vraća samo aktivna područja umjetnosti iz `art_fields`.

| Parametar | Zadano | Dopušteno |
| --- | --- | --- |
| `page` | 1 | cijeli broj ≥ 1 |
| `pageSize` | 20 | cijeli broj od 1 do 100 |

Stavke su poredane uzlazno po `position`, zatim po `name` kada imaju istu poziciju.
Deaktivirane stavke izostavljaju se prije straničenja. `meta.total` broji samo
aktivne stavke. Stranica iza kraja popisa vraća prazan `data` uz isti ukupan broj.

Primjer zahtjeva:

```http
GET /api/v1/art-fields?page=1&pageSize=20
```

Odgovor `200 OK` (`CatalogItemPage`):

```json
{
  "data": [
    {
      "id": "a1f0c2d3-4b5e-4f60-8a71-9b2c3d4e5f01",
      "code": "slikarstvo",
      "name": "Slikarstvo",
      "active": true,
      "position": 1
    }
  ],
  "meta": { "page": 1, "pageSize": 20, "total": 1 }
}
```

### CatalogItem

Sva polja su obavezna.

| Polje | Tip | Opis |
| --- | --- | --- |
| `id` | UUID string | identifikator stavke |
| `code` | string | jedinstvena šifra, do 40 znakova, uzorak `^[a-z0-9-]+$` |
| `name` | string | jedinstveni hrvatski naziv, do 80 znakova |
| `active` | boolean | na javnim popisima uvijek `true` |
| `position` | integer | redoslijed prikaza, ≥ 0 |

### CatalogItemPage

| Polje | Tip | Opis |
| --- | --- | --- |
| `data` | CatalogItem[] | stavke tražene stranice |
| `meta.page` | integer | broj stranice, počinje od 1 |
| `meta.pageSize` | integer | tražena veličina stranice |
| `meta.total` | integer | ukupan broj aktivnih stavki |

## GET /opportunity-types

Vraća samo aktivne vrste prilika iz `opportunity_types`. Parametri, sortiranje,
straničenje, oblik odgovora i greške jednaki su kao za `/art-fields`.

Primjer zahtjeva:

```http
GET /api/v1/opportunity-types?page=1&pageSize=20
```

Odgovor `200 OK` (`CatalogItemPage`):

```json
{
  "data": [
    {
      "id": "b2e1d3c4-5a6f-4071-9b82-0c3d4e5f6a01",
      "code": "natjecaj",
      "name": "Natječaj",
      "active": true,
      "position": 1
    }
  ],
  "meta": { "page": 1, "pageSize": 20, "total": 1 }
}
```

### Neispravni parametri popisa

`page < 1`, `pageSize` izvan 1–100 ili vrijednost koja nije cijeli broj vraća
`400 Bad Request` s kodom `VALIDATION_ERROR`. Primjer za `pageSize=101`:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Neki podaci nisu ispravni.",
    "details": [
      { "field": "pageSize", "issue": "Veličina stranice smije biti najviše 100." }
    ]
  }
}
```

## GET /settings/public

Vraća pet ograničenja iz `system_settings`. Nema parametara ni straničenja.

```http
GET /api/v1/settings/public
```

Odgovor `200 OK` (`UploadLimits`):

```json
{
  "artworkMaxImageBytes": 5242880,
  "artworkMaxImages": 3,
  "applicationMaxImageBytes": 5242880,
  "applicationMaxPdfBytes": 10485760,
  "applicationMaxFiles": 10
}
```

Sva polja su obavezni cijeli brojevi. Veličine su izražene u bajtovima.

| Polje odgovora | Ključ u bazi | Zadano | Raspon |
| --- | --- | --- | --- |
| `artworkMaxImageBytes` | `upload.artwork.maxImageBytes` | 5242880 (5 MiB) | 102400–52428800 |
| `artworkMaxImages` | `upload.artwork.maxImagesPerArtwork` | 3 | 1–10 |
| `applicationMaxImageBytes` | `upload.application.maxImageBytes` | 5242880 (5 MiB) | 102400–52428800 |
| `applicationMaxPdfBytes` | `upload.application.maxPdfBytes` | 10485760 (10 MiB) | 102400–52428800 |
| `applicationMaxFiles` | `upload.application.maxFiles` | 10 | 1–30 |

`SettingsService.getUploadLimits()` pri svakom pozivu čita navedene ključeve iz
baze. Ostale postavke nisu javne. Ako neki ključ nedostaje, samo za njega koristi
zadanu vrijednost, jednaku početnim podacima Liquibasea.

Neispravna vrijednost u bazi (nije cijeli broj ili je izvan raspona) predstavlja
grešku konfiguracije: endpoint vraća `500 INTERNAL_ERROR` kroz zajedničku obradu
grešaka, bez izlaganja ključa i vrijednosti klijentu. Za nedostajuće ključeve
koriste se zadane vrijednosti.

Frontend koristi ove vrijednosti za raniju provjeru datoteka. Budući servisi za
upload i predaju prijave trebaju pozivati isti `SettingsService` radi provjere na
poslužitelju. Ovaj zadatak izlaže ograničenja; sam upload pripada drugim zadacima.

## Provjera

Iz mape `backend`, uz JDK 25 i pokrenut Docker:

```bash
./mvnw -B -Dtest=SettingsServiceTest,CatalogIntegrationTest test
./mvnw -B verify
```

Alternativno, iz korijena repozitorija u PowerShellu, s pokrenutim Docker Desktopom
u načinu Linux containers (lokalni JDK i Maven nisu potrebni):

```powershell
$projectPath = (Get-Location).Path
docker run --rm `
  --mount "type=bind,source=$projectPath,target=/workspace" `
  --mount type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock `
  --mount type=volume,source=artconnect-maven-cache,target=/root/.m2 `
  --env TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
  --workdir /workspace/backend `
  maven:3.9-eclipse-temurin-25 mvn -B -ntp verify
```

`SettingsServiceTest` provjerava parsiranje, zadane vrijednosti i odbijanje
neispravnih vrijednosti. `CatalogIntegrationTest` koristi PostgreSQL 17 kroz
Testcontainers i uključene sigurnosne filtre: provjerava anonimni pristup,
aktivne stavke, redoslijed, straničenje, validaciju i čitanje izmijenjenih postavki.
