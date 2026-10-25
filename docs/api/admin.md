# Administracija

## GET /api/v1/admin/users

Endpoint iz ADM-01-BE2 (#200) vraća stranični popis korisnika za
administratorsku stranicu i popis organizatora koji čekaju odobrenje.
Zahtijeva valjanu sesiju i ulogu `ADMIN`. Za ovaj GET nije potreban CSRF token.

- Pristup: samo `ADMIN`.
- `q`: neobavezan dio imena ili e-maila, najviše 100 znakova; pretraga ne razlikuje velika i mala slova. Znakovi `%` i `_` traže se doslovno.
- Izostavljen ili prazan `q` ne ograničava rezultate pretrage.
- `role`: `ARTIST`, `ORGANIZER` ili `ADMIN`.
- `status`: `INVITED`, `ONBOARDING`, `ACTIVE`, `BLOCKED` ili `DELETED`.
- `organizerApproval`: `PENDING` ili `APPROVED`.
- Svi zadani filtri vrijede istodobno. Bez filtra po statusu uključeni su i obrisani korisnici.
- `page`: najmanje 1, zadano 1. `pageSize`: 1–100, zadano 20.
- Redoslijed: `createdAt DESC`; kod jednakih datuma `id DESC` osigurava stabilan redoslijed stranica.
- `organizationName` dolazi iz `organizer_profiles`; korisnici bez profila ostaju u popisu s vrijednošću `null`.

Odgovor je postojeći `PageResponse<AdminUser>`: `{ data, meta: { page, pageSize, total } }`.
`AdminUser` sadrži `id`, `displayName`, `email`, `role`, `status`,
`organizerApproval`, `organizationName`, `createdAt` i `lastLoginAt`.
Vrijednosti `email`, `role`, `organizerApproval`, `organizationName` i
`lastLoginAt` mogu biti `null`, prema API ugovoru. Datumi koriste `OffsetDateTime`.

Primjer zahtjeva za organizatore koji čekaju odobrenje:

```http
GET /api/v1/admin/users?role=ORGANIZER&organizerApproval=PENDING&page=1&pageSize=20
```

Odgovor `200 OK` (`AdminUserPage`):

```json
{
  "data": [
    {
      "id": "8f1c2a4e-5b6d-4e7f-9a0b-1c2d3e4f5a6b",
      "displayName": "Marko Horvat",
      "email": "marko@example.test",
      "role": "ORGANIZER",
      "status": "ACTIVE",
      "organizerApproval": "PENDING",
      "organizationName": "Galerija",
      "createdAt": "2026-10-02T10:00:00Z",
      "lastLoginAt": null
    }
  ],
  "meta": { "page": 1, "pageSize": 20, "total": 1 }
}
```

`meta.total` broji sve rezultate koji odgovaraju filtrima, prije straničenja.
Ako nema rezultata ili je stranica iza kraja popisa, `data` je prazan niz.
Prazan rezultat je uspješan odgovor 200.

### Greške

| Status | Kod | Razlog |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `q` dulji od 100 znakova, nepoznata vrijednost enuma ili neispravno straničenje |
| 401 | `UNAUTHENTICATED` | nema valjane sesije |
| 403 | `FORBIDDEN` | prijavljeni korisnik nema ulogu `ADMIN` |

Greške koriste zajedničku omotnicu. Primjer za `pageSize=101`:

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

### Izvedba

`AdminUserController` provjerava ulogu preko `@PreAuthorize` i validira
`AdminUserQueryParams`, koji nasljeđuje zajedničko straničenje.
`AdminUserService` unutar transakcije za čitanje dohvaća podatke iz
`AdminUserRepository` i koristi zajednički `AdminUserMapper` za pretvorbu u DTO.
Entiteti se ne vraćaju u API odgovoru. Upit koristi `LEFT JOIN` na profil
organizatora i zaseban upit za ukupan broj rezultata.

Upit koristi postojeće tablice. Indeks `ix_users_role_status` već postoji;
hoće li ga upit koristiti, ovisi o filtrima i planu izvršavanja PostgreSQL-a.
Nove migracije nisu potrebne.

### Validacija i sigurnosne provjere

Za neispravne parametre endpoint vraća `400 VALIDATION_ERROR` s detaljem
po polju, bez valjane sesije `401 UNAUTHENTICATED`, a korisniku bez
administratorske uloge `403 FORBIDDEN`.

`OnboardingEnforcementFilter` korisniku u statusu `ONBOARDING` vraća
`403 ONBOARDING_REQUIRED` prije provjere administratorske uloge.
Provjera `USER_BLOCKED` pripada zasebnom zadatku EPIC-08.

### Provjera

`AdminUserServiceTest` provjerava prosljeđivanje filtara i pretvorbu straničenja.
`AdminUserIntegrationTest` na PostgreSQL-u 17 provjerava pretragu, kombinacije
filtara, mapiranje, stabilan redoslijed, straničenje, dopuštene `null` vrijednosti,
uključivanje obrisanih računa i HTTP odgovore 200/400/401/403.

Pokretanje iz direktorija `backend` uz JDK 25 i dostupan Docker:

```sh
./mvnw -B verify
```

## Administracija šifrarnika

ADM-04-BE1 (#218) dodaje upravljanje područjima umjetnosti i vrstama prilika.
Sve rute zahtijevaju sesiju i ulogu `ADMIN`. POST, PUT i DELETE dodatno
zahtijevaju CSRF token u zaglavlju `X-XSRF-TOKEN`.

Iste operacije dostupne su na obje osnovne putanje:

- `/api/v1/admin/art-fields`
- `/api/v1/admin/opportunity-types`

| Metoda | Putanja | Uspješan odgovor |
| --- | --- | --- |
| GET | osnovna putanja | 200, `CatalogItemPage` |
| POST | osnovna putanja | 201, `CatalogItem` i zaglavlje `Location` |
| PUT | osnovna putanja + `/{itemId}` | 200, `CatalogItem` |
| DELETE | osnovna putanja + `/{itemId}` | 204, bez tijela |

`itemId` je UUID. GET vraća aktivne i neaktivne stavke, poredane po
`position ASC`, zatim `name ASC`. Koristi parametre `page` (najmanje 1,
zadano 1) i `pageSize` (1–100, zadano 20).
Odgovor je `{ data, meta: { page, pageSize, total } }`; prazna stranica
vraća 200 s praznim nizom `data`.

### Dodavanje i uređivanje

POST i PUT primaju sva četiri obavezna polja:

| Polje | Tip | Ograničenje |
| --- | --- | --- |
| code | string | 2–40 znakova, izraz `^[a-z0-9-]+$`, jedinstven u tom šifrarniku |
| name | string | 2–80 znakova, ne smije biti prazan ili sadržavati samo razmake, jedinstven u tom šifrarniku |
| active | boolean | `true` ili `false` |
| position | integer | 0–999 |

Bjeline na početku i kraju naziva `name` uklanjaju se metodom `strip()` prije
validacije, provjere duplikata i spremanja. Primjerice, `"Keramika "`
i `"Keramika"` smatraju se istim nazivom.

Decimalni unos za `position` vraća 400 `VALIDATION_ERROR`. Zajednička
Jackson postavka `accept-float-as-int: false` sprječava skraćivanje decimalnih
JSON brojeva pri čitanju cjelobrojnih polja.

Primjer POST zahtjeva na `/api/v1/admin/art-fields`:

```json
{ "code": "keramika", "name": "Keramika", "active": true, "position": 6 }
```

Odgovor 201 sadrži ista polja i generirani `id`. `Location` je
`/api/v1/admin/art-fields/{id}`. PUT prima isti oblik i može promijeniti
i šifru. Pri provjeri jedinstvenosti izuzima se stavka koja se uređuje.
Ista šifra ili naziv u drugom šifrarniku nisu duplikati.

### Deaktivacija i brisanje

PUT s `active: false` deaktivira stavku. Ona nestaje iz javnih
`GET /api/v1/art-fields` i `GET /api/v1/opportunity-types`, dok
postojeće veze u profilima i prilikama ostaju sačuvane. Ponovnim slanjem
`active: true` stavka se vraća u javni popis.

DELETE briše samo nekorištene stavke, bez obzira na njihovu aktivnost:

- Područje je korišteno ako postoji veza u `artist_profile_art_fields`
  ili `opportunity_art_fields`.
- Vrsta prilike je korištena ako postoji red u `opportunities` s tim `type_id`.

Korištenu stavku administrator može deaktivirati. Strani ključevi dodatno
štite od brisanja ako se veza stvori nakon provjere korištenja.

### Greške šifrarnika

| Status | Kod | Razlog |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | neispravna polja, UUID ili straničenje; detalji po polju |
| 401 | `UNAUTHENTICATED` | nema valjane sesije |
| 403 | `FORBIDDEN` | nema administratorske uloge ili nedostaje valjan CSRF token za promjenu |
| 404 | `NOT_FOUND` | stavka za PUT ili DELETE ne postoji |
| 409 | `DUPLICATE_CODE` | šifra ili naziv već postoji, uključujući neaktivne stavke |
| 409 | `IN_USE` | pokušaj brisanja korištene stavke |

Greške koriste istu omotnicu `ErrorResponse` kao ostali endpointi.
Provjera duplikata prethodi spremanju, a UNIQUE greška pri istovremenom
spremanju također se prevodi u `DUPLICATE_CODE`.

### Izvedba i provjera

Dva kontrolera koriste dvije instance zajedničkog `CatalogAdminService<T>`.
`CatalogEntry` sadrži zajednička polja entiteta; tablice ostaju odvojene.
Repozitoriji provjeravaju korištenje SQL upitima. Nema novih migracija.

`CatalogAdminServiceTest` provjerava zajedničku logiku i obradu UNIQUE/FK
grešaka. `CatalogAdminIntegrationTest` koristi PostgreSQL 17 i provjerava
oba skupa ruta, dozvole, CSRF, validaciju, duplikate, strane ključeve,
straničenje i očuvanje veza nakon deaktivacije.

Servisi za unos profila umjetnika i prilika te dohvat prilike još nisu
implementirani. Pri njihovoj implementaciji treba odbiti neaktivne stavke
pri novom unosu i omogućiti prikaz stavki koje postojeći zapisi već koriste.
Test deaktivacije zasad provjerava čitljivost postojećih veza SQL spajanjem;
provjera kroz endpoint prilike ostaje za integraciju s tim servisom.
