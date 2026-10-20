# 3. Obrasci uporabe

## Pregled korisnika

- **Primarni sudionik:** administrator.
- **Cilj:** pronaći korisnika i pregledati stanje računa ili pronaći organizatore koji čekaju odobrenje.
- **Preduvjet:** korisnik ima valjanu sesiju i ulogu `ADMIN`.
- **Rezultat:** dohvaćen je popis korisnika s ukupnim brojem rezultata; podaci računa se ne mijenjaju.

### Osnovni tijek

1. Administrator otvara pregled korisnika.
2. Sučelje poziva `GET /api/v1/admin/users`.
3. Sustav provjerava prijavu, administratorsku ulogu i ispravnost parametara.
4. Sustav vraća prvih 20 korisnika od najnovijeg prema najstarijem, uključujući obrisane račune.
5. Administrator može pretraživati dio imena ili e-maila, filtrirati po ulozi, statusu i odobrenju organizatora te promijeniti stranicu.
6. Sustav kombinira sve zadane filtre i vraća odgovarajuću stranicu s ukupnim brojem rezultata.

### Alternativni tijekovi

- Za popis organizatora na čekanju sučelje šalje `role=ORGANIZER&organizerApproval=PENDING`.
- Ako nema rezultata ili je tražena stranica iza kraja popisa, sustav vraća prazan popis uz metapodatke stranice.
- Ako su parametri neispravni, sustav vraća 400 `VALIDATION_ERROR` s opisom uz odgovarajuće polje.
- Ako nema valjane sesije, sustav vraća 401 `UNAUTHENTICATED`.
- Ako pozivatelj nema administratorsku ulogu, sustav vraća 403 `FORBIDDEN`.

Ovdje je opisan obrazac uporabe. Backend dio implementiran je u ADM-01-BE2;
administratorsko sučelje, promjena uloga, blokiranje i odobravanje zasebni su zadaci.
Detalji parametara i odgovora nalaze se u [API dokumentaciji administracije](api/admin.md).
