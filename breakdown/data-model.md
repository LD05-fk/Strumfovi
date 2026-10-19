# Model podataka

Ovaj dokument opisuje relacijski model koji uvode Liquibase changesetovi. Enum-vrijednosti pohranjuju se kao `varchar` i ograničavaju se baznim `CHECK` pravilima.

## `opportunities`

| Stupac | Tip | Null | Zadano | Ograničenja / značenje |
| --- | --- | --- | --- | --- |
| `id` | `uuid` | ne | — | Primarni ključ. |
| `organizer_id` | `uuid` | ne | — | FK `fk_opp_organizer` na `users(id)` bez kaskadnog brisanja (`NO ACTION`); račun se anonimizira, a prilike ostaju. |
| `type_id` | `uuid` | ne | — | FK na `opportunity_types(id)`, `ON DELETE RESTRICT`. |
| `title` | `varchar(200)` | ne | — | Naslov. |
| `description` | `text` | ne | — | Opis; najviše 10.000 znakova provjerava aplikacija. |
| `deadline` | `timestamptz` | ne | — | Rok prijave. |
| `criteria` | `text` | da | — | Kriteriji. |
| `prize` | `text` | da | — | Nagrada. |
| `location_type` | `varchar(16)` | ne | — | `PHYSICAL` ili `ONLINE`, ograničeno s `ck_opp_location_type`. |
| `street_address` | `varchar(200)` | da | — | Adresa fizičke lokacije. |
| `city` | `varchar(100)` | da | — | Grad; fizička lokacija ga mora imati. |
| `postal_code` | `varchar(10)` | da | — | Poštanski broj. |
| `latitude` | `numeric(9,6)` | da | — | Geografska širina fizičke lokacije. |
| `longitude` | `numeric(9,6)` | da | — | Geografska dužina fizičke lokacije. |
| `online_url` | `varchar(1024)` | da | — | Poveznica; obavezna za online lokaciju. |
| `event_start` | `timestamptz` | da | — | Početak događanja. |
| `event_end` | `timestamptz` | da | — | Kraj ne može biti prije početka. |
| `status` | `varchar(16)` | ne | `PREPARATION` | Jedan od `PREPARATION`, `OPEN`, `CLOSED`, `SELECTION`, `FINISHED`, ograničeno s `ck_opp_status`. |
| `archived` | `boolean` | ne | `false` | Je li prilika arhivirana. |
| `hidden_by_admin` | `boolean` | ne | `false` | Je li administrator sakrio priliku. |
| `hidden_at` | `timestamptz` | da | — | Vrijeme skrivanja. |
| `published_at` | `timestamptz` | da | — | Vrijeme objave. |
| `created_at` | `timestamptz` | ne | `now()` | Vrijeme stvaranja. |
| `updated_at` | `timestamptz` | ne | `now()` | Vrijeme zadnje izmjene. |

CHECK ograničenja:

- `ck_opp_location_type`: `location_type IN ('PHYSICAL', 'ONLINE')`.
- `ck_opp_status`: `status IN ('PREPARATION', 'OPEN', 'CLOSED', 'SELECTION', 'FINISHED')`.
- `ck_opp_physical`: fizička lokacija zahtijeva adresu, grad i obje koordinate.
- `ck_opp_online`: online lokacija zahtijeva `online_url`.
- `ck_opp_event_end`: kraj je prazan ili postoji početak i kraj je jednak ili kasniji.

Indeksi:

| Indeks | Izraz | Svrha |
| --- | --- | --- |
| `ix_opp_public` | `(status, deadline) WHERE archived = false AND hidden_by_admin = false` | Javno pretraživanje i sortiranje po roku. |
| `ix_opp_organizer` | `(organizer_id, created_at DESC)` | Popis prilika organizatora. |
| `ix_opp_city_lower` | `(lower(city))` | Filtriranje grada bez obzira na velika/mala slova. |
| `ix_opp_open_deadline` | `(deadline) WHERE status = 'OPEN'` | Automatsko zatvaranje isteklih otvorenih prilika. |
| `ix_opp_fts` | `GIN (to_tsvector('simple', title || ' ' || description))` | Punotekstna pretraga; izraz u upitu mora biti jednak indeksnom izrazu. |
| `ix_opp_published_at` | `(published_at)` | Mjesečni izvještaj. |

## `opportunity_art_fields`

| Stupac | Tip | Null | Ograničenja / značenje |
| --- | --- | --- | --- |
| `opportunity_id` | `uuid` | ne | Dio primarnog ključa; FK na `opportunities(id) ON DELETE CASCADE`. |
| `art_field_id` | `uuid` | ne | Dio primarnog ključa; FK na `art_fields(id) ON DELETE RESTRICT`. |

Primarni ključ je `(opportunity_id, art_field_id)`. Indeks `ix_oaf_art_field` na `art_field_id` služi filtriranju po području umjetnosti.

Ove tablice nemaju seed podatke. Tablice prijava i `application_requirements` pripadaju zasebnom changesetu APP-01-DB.
