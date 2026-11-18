# API dokumentacija

ArtConnect ima jedan REST API. Ovdje su zajednička pravila, a pojedine rute opisane su u datoteci svoje domene. Dokumentacija opisuje samo ono što postoji u kodu; svaki PR koji mijenja API ažurira i pripadnu datoteku.

**Osnovna adresa:** `/api/v1` (javno `https://artconnect-fer.duckdns.org/api/v1`, lokalno `http://localhost:8080/api/v1`). Podaci se šalju i primaju kao JSON.

## Domene

| Domena | Datoteka | Što pokriva |
| --- | --- | --- |
| Prijava i sesija | [auth.md](auth.md) | CSRF token, trenutni korisnik, odabir uloge, odjava, razvojna prijava |
| Profili | [artists.md](artists.md) | Javni profil umjetnika, profil umjetnika i organizatora, prethodne izložbe |
| Djela | [artworks.md](artworks.md) | Djela u portfoliju i njihove slike |
| Datoteke | [files.md](files.md) | Dohvat učitanih datoteka |
| Šifrarnici i postavke | [catalog.md](catalog.md) | Područja umjetnosti, vrste prilika, javne postavke |
| Prilike | [opportunities.md](opportunities.md) | Objava, uređivanje, pretraga, statusi, arhiviranje, brisanje, geokodiranje |
| Administracija | [admin.md](admin.md) | Popis korisnika, uređivanje šifrarnika |

**2. ciklus:** prijave na prilike, rezultati, Google kalendar, statistika, mjesečni izvještaji, ostatak administracije i obavijesti (opcionalno). Te domene dokumentiraju se uz implementaciju.

**OpenAPI:** [openapi.yaml](openapi.yaml) je strojno čitljiva snimka API-ja generirana iz koda (springdoc) za predaju 1. ciklusa. Sadrži putanje, parametre i oblike podataka, ali ne i poslovna pravila ni kodove grešaka, koji su u datotekama domena. Ne održava se uz svaki PR, nego se ponovno generira pred predaju.

## Prijava, sesija i CSRF

- Prijava je samo Google računom (`/oauth2/authorization/google`). Nakon prijave preglednik ima kolačić `SESSION`; sesija istječe nakon 8 sati neaktivnosti.
- Korisnik koji još nije odabrao ulogu dobiva `403 ONBOARDING_REQUIRED` na svim zaštićenim rutama osim onih za odabir uloge ([auth.md](auth.md)).
- Zahtjevi `POST`, `PUT`, `PATCH` i `DELETE` moraju u zaglavlju `X-XSRF-TOKEN` poslati vrijednost kolačića `XSRF-TOKEN`. Token se dobiva preko `GET /auth/csrf`.
- Frontend i API su na istoj adresi, pa CORS nije potreban. Frontend sve pozive šalje kroz `apiFetch` (`frontend/src/api/client.ts`).

## Greške

Sve greške imaju isti oblik, a `message` je na hrvatskom i može se prikazati korisniku:

```json
{ "error": { "code": "VALIDATION_ERROR", "message": "Neki podaci nisu ispravni.", "details": [ { "field": "year", "issue": "Godina ne smije biti u budućnosti." } ] } }
```

`details` postoji samo kad se greška odnosi na pojedina polja. Zajednički kodovi:

| HTTP | Kod | Kada |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Neispravni podaci ili parametri |
| 401 | `UNAUTHENTICATED` | Nema valjane sesije |
| 403 | `FORBIDDEN` | Uloga ili vlasništvo ne dopuštaju radnju, ili nedostaje CSRF token |
| 403 | `ONBOARDING_REQUIRED` | Korisnik još nije odabrao ulogu |
| 404 | `NOT_FOUND` | Zapis ne postoji ili ga korisnik ne smije vidjeti |
| 500 | `INTERNAL_ERROR` | Neočekivana greška; detalji su samo u zapisima poslužitelja |

Kodovi specifični za pojedinu radnju (npr. `409 INVALID_STATUS_TRANSITION`) navedeni su uz rutu u datoteci domene. Svi kodovi su u `backend/src/main/java/hr/fer/artconnect/common/ErrorCode.java`.

## Straničenje i sortiranje

- Parametri `page` (od 1) i `pageSize` (zadano 20, najviše 100).
- `sort` prima naziv polja: `deadline` je uzlazno, `-deadline` silazno. Svaka ruta navodi koja polja dopušta.
- Odgovor: `{ "data": [...], "meta": { "page": 1, "pageSize": 20, "total": 134 } }`.

## Datumi

Datumi i vremena šalju se kao ISO-8601 s vremenskim pomakom, npr. `2026-11-20T23:59:00+01:00`. Pravila poput „ne smije biti u budućnosti” računaju se u zoni `Europe/Zagreb`.
