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

### Razlike u specifikaciji

Issue zahtijeva `400 VALIDATION_ERROR` s detaljem po polju za neispravne
parametre, iako OpenAPI odjeljak za ovu rutu u `API.md` ne navodi odgovor 400.
Primjenjuje se validacija tražena issueom. Predviđeni su i odgovori
`401 UNAUTHENTICATED` te `403 FORBIDDEN`. Provjere statusa pozivatelja
(`ONBOARDING_REQUIRED`, `USER_BLOCKED`) koje dokumentacija dodatno navodi
pripadaju zajedničkim sigurnosnim filtrima i zasebnim zadacima.
Trenutni `UserStatusFilter` još ne provodi te dvije provjere; ovaj endpoint
provodi prijavu i administratorsku ulogu, kako traži #200.

### Provjera

`AdminUserServiceTest` provjerava prosljeđivanje filtara i pretvorbu straničenja.
`AdminUserIntegrationTest` na PostgreSQL-u 17 provjerava pretragu, kombinacije
filtara, mapiranje, stabilan redoslijed, straničenje, dopuštene `null` vrijednosti,
uključivanje obrisanih računa i HTTP odgovore 200/400/401/403.

Pokretanje iz direktorija `backend` uz JDK 25 i dostupan Docker:

```sh
./mvnw -B verify
```
