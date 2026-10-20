# Administracija

## GET /api/v1/admin/users

Ugovor za ADM-01-BE2 (#200), prema opisu issuea i dokumentu `API.md`.
Trenutno su pripremljeni DTO, zajedničko mapiranje i upit nad bazom.
Kontroler i servis slijede u idućim koracima; endpoint još nije dostupan.

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
