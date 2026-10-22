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

## Uređivanje šifrarnika

- **Primarni sudionik:** administrator.
- **Cilj:** održavati popise područja umjetnosti i vrsta prilika.
- **Preduvjet:** valjana administratorska sesija; za promjene valjan CSRF token.
- **Rezultat:** stavka je dodana, uređena, deaktivirana, ponovno aktivirana ili obrisana.

### Osnovni tijek

1. Administrator otvara upravljanje šifrarnicima.
2. Sučelje dohvaća aktivne i neaktivne stavke odgovarajućeg šifrarnika.
3. Administrator dodaje novu stavku ili uređuje šifru, naziv, aktivnost i poziciju postojeće.
4. Sustav provjerava obavezna polja, ograničenja i jedinstvenost šifre i naziva.
5. Sustav sprema izmjenu i vraća spremljenu stavku.
6. Aktivne stavke prikazuju se u javnim popisima za odabir.

### Alternativni tijekovi

- Administrator deaktivira stavku: ona nestaje iz javnog popisa, a postojeći profili i prilike zadržavaju veze.
- Administrator ponovno aktivira stavku: ona se vraća u javni popis.
- Administrator briše nekorištenu stavku: sustav je briše i vraća 204.
- Ako se stavka koristi u profilu ili prilici, brisanje vraća 409 `IN_USE`; administrator je može deaktivirati.
- Ako šifra ili naziv već postoji u istom šifrarniku, spremanje vraća 409 `DUPLICATE_CODE`.
- Neispravna polja vraćaju 400 `VALIDATION_ERROR` s detaljima po polju.
- Ako stavka više ne postoji, uređivanje i brisanje vraćaju 404 `NOT_FOUND`.
- Bez sesije sustav vraća 401; bez administratorske uloge ili valjanog CSRF tokena za promjenu vraća 403.

Backend dio pripada ADM-04-BE1 (#218), a administratorsko sučelje zasebnom
zadatku. Detalji ruta i ograničenja nalaze se u
[API dokumentaciji administracije](api/admin.md#administracija-šifrarnika).
