# Prijava, sesija i odjava

Sve putanje u ovom dokumentu počinju s `/api/v1`.
Preglednik šalje sesijski kolačić `SESSION`; frontend koristi zajednički `apiFetch`.

## GET /auth/csrf

Javno dostupan endpoint. Vraća `200 OK`:

```json
{
  "headerName": "X-XSRF-TOKEN",
  "token": "vrijednost-tokena"
}
```

Ako token još ne postoji, postavlja kolačić `XSRF-TOKEN` koji JavaScript smije
čitati. Frontend šalje njegovu vrijednost u zaglavlju `X-XSRF-TOKEN` kod zahtjeva
koji mijenjaju stanje. Postojeći token ponovno se koristi.

## GET /me

Zahtijeva valjanu sesiju. Vraća `200 OK` s podacima trenutno prijavljenog korisnika:

```json
{
  "id": "5e7d9c2b-1a4f-4b3e-8c6d-7f8a9b0c1d2e",
  "displayName": "Ana Horvat",
  "email": "ana@example.com",
  "avatarUrl": null,
  "role": "ARTIST",
  "status": "ACTIVE",
  "organizerApproval": null
}
```

| Polje | Vrijednost |
| --- | --- |
| `id` | UUID korisnika |
| `displayName` | prikazano ime |
| `email` | e-mail adresa |
| `avatarUrl` | URL slike ili `null` |
| `role` | `ARTIST`, `ORGANIZER`, `ADMIN` ili `null` |
| `status` | `INVITED`, `ONBOARDING`, `ACTIVE`, `BLOCKED` ili `DELETED` |
| `organizerApproval` | `PENDING`, `APPROVED` ili `null` |

Podaci se pri svakom pozivu čitaju iz baze, pa odgovor prikazuje i promjene nastale
nakon prijave. Time se ne mijenjaju ovlasti spremljene u samoj sesiji.
Endpoint je dostupan i korisniku u statusu `ONBOARDING`, kojemu je `role = null`.
`organizerApproval` odnosi se na organizatore; za ostale uloge je `null`.

Bez valjane prijave ili kada korisnik iz sesije više ne postoji u bazi, vraća
`401 Unauthorized`:

```json
{
  "error": {
    "code": "UNAUTHENTICATED",
    "message": "Potrebno je prijaviti se."
  }
}
```

## POST /dev/login-as

Javno dostupan samo u profilu `dev` (i ne uz profil `prod`). Ovaj endpoint služi
za razvojno i Selenium testiranje umjesto Google prijave. U produkciji ruta nije
registrirana.

Tijelo zahtjeva:

```json
{
  "email": "umjetnik@artconnect.test"
}
```

`email` mora biti valjana e-mail adresa duljine do 254 znaka. Uspjeh vraća
`200 OK` s objektom `CurrentUser` kao u `GET /me` te postavlja novi kolačić
sesije `SESSION`. Prethodna sesija, ako postoji, dobiva novi ID.

| Status | Kod | Značenje |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Nedostaje e-mail ili je neispravnog formata/duljine |
| 404 | `NOT_FOUND` | Korisnik ne postoji ili je u statusu `DELETED` |

Pretraga e-maila ne razlikuje velika i mala slova. Prijava ažurira
`last_login_at`; korisnik u statusu `INVITED` prelazi u `ACTIVE`. Ne postavlja se
`google_sub`. CSRF provjera za `/api/v1/dev/**` isključena je samo u profilu
`dev` bez `prod`.

## POST /auth/logout

Zahtjev nema tijelo. Preglednik šalje `SESSION`, kolačić `XSRF-TOKEN` i njegovu
vrijednost u zaglavlju `X-XSRF-TOKEN`.

Uspjeh vraća `204 No Content`, bez tijela i preusmjeravanja. Spring Security
poništava sesiju, briše njezin zapis iz baze, čisti sigurnosni kontekst i postavlja
istek kolačića `SESSION` (`Max-Age=0`). Briše se i CSRF kolačić.
Naknadni poziv `/me` sa starom sesijom vraća `401`.

Odjava je idempotentna: i bez aktivne sesije vraća `204` ako je CSRF token valjan.
Za ponovljeni poziv nakon odjave frontend prvo dohvaća novi token preko `/auth/csrf`.
Bez valjanog CSRF tokena u kolačiću i zaglavlju vraća `403 Forbidden` s kodom
`FORBIDDEN`, a postojeća sesija ostaje aktivna.

## Provjere

`AuthControllerTest` provjerava oblik odgovora, onboarding, aktualne podatke iz
baze i greške prijave. `CurrentUserDtoTest` provjerava mapiranje entiteta u DTO.
`AuthEndpointsIntegrationTest` koristi PostgreSQL i stvarni zapis sesije za
provjeru CSRF-a, promjene uloge, odjave, brisanja sesije i ponovljene odjave.
`SecurityConfigTest` dodatno provjerava podudaranje CSRF kolačića i zaglavlja.
