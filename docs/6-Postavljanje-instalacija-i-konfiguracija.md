# 6. Postavljanje, instalacija i konfiguracija

**Cilj:** Detaljno dokumentirati topologiju izvođenja, postupak lokalne instalacije i konfiguracije iz izvornog koda te način javnog postavljanja i administracije programa. [README](../README.md) navodi javnu adresu i sažeti postupak lokalnog pokretanja; ova stranica donosi potpun inženjerski postupak koji osigurava točnu ponovljivost na drugom računalu.

> **Stanje:** opisano je stvarno stanje u Fazi 1. Poglavlje dopunjuju DOC-18 i FND zadaci kad se uvedu kontinuirana isporuka (FND-10), nadzor (FND-11) i Google prijava (AUTH-01-BE1).

---

## 6.1 Prikaz razmještaja

**Cilj:** Prikazati okruženja izvođenja, raspodijeljene dijelove programa, spremište podataka i relevantne vanjske usluge. Njihovu komunikaciju i mrežne protokole prikažite jednim UML dijagramom razmještaja. Dok dijagram komponenata iz Poglavlja 4 prikazuje logičke odgovornosti, dijagram razmještaja pokazuje gdje se izvršni artefakti fizički ili virtualno izvode.

Aplikacija se izvodi na jednoj virtualnoj mašini u AWS-u. Sva tri dijela (Caddy s izgrađenim frontendom, backend i baza) su Docker kontejneri pokrenuti iz `docker-compose.prod.yml`, pa je cijela aplikacija na jednoj domeni i preglednik ne treba CORS.

**Dijagram razmještaja:** u izradi (DOC-18).
**Javno dostupna aplikacija:** <https://artconnect-fer.duckdns.org> (isto kao u [README-u](../README.md))

- **Korisnički uređaj:** preglednik preuzima React SPA i sve zahtjeve šalje istoj domeni preko HTTPS-a. HTTP zahtjeve Caddy preusmjerava na HTTPS (308).
- **EC2 instanca** (`artconnect-server`, c7i-flex.large: 2 vCPU, 4 GB RAM, Ubuntu Server 24.04 LTS, regija `eu-north-1`, disk 25 GB gp3). Iz interneta su otvoreni samo portovi 80 i 443 (te 22 za SSH s adrese održavatelja). Backend (8080) i baza (5432) dostupni su samo unutar Docker mreže.
- **Caddy** poslužuje statičke datoteke frontenda (`frontend/dist`) i prosljeđuje `/api`, `/oauth2`, `/login/oauth2` i `/actuator` backendu, isto kao Vite proxy u razvoju. Certifikat sam dobiva i obnavlja od Let's Encrypta i čuva ga na volumenu `caddy-data`.
- **Backend** (Spring Boot, `app.jar`) spaja se na PostgreSQL JDBC-om unutar Docker mreže; podaci baze su na volumenu `db-data` i preživljavaju ponovno pokretanje kontejnera i instance.
- **DuckDNS:** instanca nema stalnu IP adresu (bez Elastic IP-a, jer se poslužitelj pali i gasi). Servis `duckdns.service` pri svakom pokretanju javi novu javnu IP, pa `artconnect-fer.duckdns.org` uvijek vodi na poslužitelj.
- **Google** (OAuth 2.0 / OpenID Connect) je jedini način prijave na javnoj adresi.
- **GitHub Actions** (workflow _Server_) pali i gasi instancu preko AWS EC2 API-ja; vidi §6.3.

---

## 6.2 Lokalna instalacija i konfiguracija

**Cilj:** Omogućiti novom članu tima ili ocjenjivaču da iz čiste radne kopije repozitorija instalira i lokalno pokrene cijeli programski sustav, uključujući bazu podataka i početne podatke.

Korak po korak postupak s naredbama je u [HOW_TO_RUN.md](../HOW_TO_RUN.md#lokalno-pokretanje). Ovdje je sažetak i ono što taj postupak pretpostavlja. Lokalno se samo baza vrti u Dockeru; backend i frontend pokreću se izravno (brži restart, debugger).

**1. Preduvjeti**

| Alat | Verzija | Za što |
| Docker + Docker Compose | najnovija | lokalna baza i Testcontainers testovi |
| JDK | 25 (Eclipse Temurin) | backend; Maven se ne instalira, koristi se `./mvnw` |
| Node.js + npm | 22 | frontend |

Slobodni portovi: 5432 (baza), 8080 (backend), 5173 (frontend).

```bash
git clone https://github.com/LD05-fk/Projekt-PROGI.git
cd Projekt-PROGI
```

**2. Konfiguracija**

Backend čita vezu s bazom iz varijabli `DB_URL`, `DB_USER` i `DB_PASSWORD` (`backend/src/main/resources/application.yml`). Zadane vrijednosti (baza, korisnik i lozinka `artconnect` na `localhost:5432`) odgovaraju `docker-compose.yml`, pa za lokalni rad `.env` nije potreban. Za druge vrijednosti: `cp .env.example .env`, izmijenite i backend pokrenite s istim `DB_*` varijablama. Tajne nikad ne idu u repozitorij.

**Google OAuth.** Lokalno ga treba samo tko radi na samoj prijavi; za sav ostali rad prijava u profilu `dev` ide preko `POST /api/v1/dev/login-as` s testnim korisnicima. Tko radi na Google prijavi, od voditelja tima dobiva `GOOGLE_CLIENT_ID` i `GOOGLE_CLIENT_SECRET` jednokratnim linkom i upisuje ih u svoj `.env`, a svoj Google e-mail mora imati na popisu _Test users_ (aplikacija je u načinu _Testing_). Redirect URI za lokalni rad je `http://localhost:5173/login/oauth2/code/google`. Postavke klijenta i postupak: [HOW_TO_RUN.md, Google OAuth](../HOW_TO_RUN.md#google-oauth).

**Dev korisnici.** Lokalno Liquibase (kontekst `dev`) stvara testne korisnike `umjetnik@`, `umjetnik2@`, `organizator@`, `admin@` i `novi@artconnect.test` za `POST /api/v1/dev/login-as`. Na poslužitelju se taj changeset preskače (`SPRING_LIQUIBASE_CONTEXTS=prod`).

**3. Baza i migracije**

```bash
docker compose up -d       # iz korijena repozitorija; PostgreSQL 17
docker compose ps          # db mora biti "healthy"
```

Shemu stvara **Liquibase pri pokretanju backenda** iz `backend/src/main/resources/db/changelog/db.changelog-master.yaml`; ručno se ne uređuje. Liquibase pamti izvedene changesete, pa se nijedan ne izvodi dvaput. Tablice sesija stvara Spring Session. Kako dodati promjenu sheme: [db/README.md](../db/README.md).

**4. Pokretanje backenda i provjera**

```bash
cd backend
./mvnw spring-boot:run                                    # ili s profilom dev:
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Provjera: <http://localhost:8080/actuator/health> vraća `{"status":"UP"}`, što znači da je backend pokrenut i spojen na bazu. Testovi: `./mvnw verify` (Docker mora raditi jer testovi podižu vlastitu PostgreSQL bazu kroz Testcontainers).

**5. Pokretanje frontenda i provjera**

```bash
cd frontend
npm install
npm run dev               # http://localhost:5173
```

Vite u razvoju prosljeđuje `/api`, `/oauth2`, `/login/oauth2` i `/actuator` na backend (`frontend/vite.config.ts`), pa preglednik vidi jednu domenu kao i u produkciji; backend mora biti pokrenut. Provjera veze s backendom: <http://localhost:5173/actuator/health> (adresa frontenda) vraća `{"status":"UP"}`, što znači da proxy prosljeđuje zahtjeve backendu.

Ostale naredbe: `npm test` (Vitest), `npm run lint`, `npm run typecheck`, `npm run build`.

**Ako nešto ne radi:** greške backenda su u terminalu u kojem je pokrenut; stanje baze `docker compose ps` i `docker compose logs db`. Česti problemi: [HOW_TO_RUN.md, Mogući problemi](../HOW_TO_RUN.md#mogući-problemi).

---

## 6.3 Javno postavljanje i administracija

**Cilj:** Dokumentirati točne korake za ponovljivo postavljanje cjelovitog programskog sustava na produkcijsku infrastrukturu u oblaku, uključujući smještenu bazu podataka i veze među postavljenim dijelovima. Kada je jasnije, povežite konfiguraciju koja već postoji u Gitu umjesto njezina prepisivanja.

| **Postavljeni podsustav** | **Infrastruktura i izvor koda**                                                                                                                                                                  | **Naredbe izgradnje i pokretanja**                                                                                                                                                                                                    | **Konfiguracija i varijable okruženja**                                                                                                                                                                                                                                                                                                           |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Klijentski dio**        | EC2 instanca, kontejner `caddy` iz imagea `ghcr.io/ld05-fk/artconnect-web` (Caddy 2 + izgrađeni frontend; [`deploy/web.Dockerfile`](../deploy/web.Dockerfile)); grana `master`, mapa `frontend/` | Image gradi CD: `npm ci && npm run build` → `frontend/dist`, kopiran u `/srv` imagea; poslužitelj ga povlači (`docker compose … pull`)                                                                                                | Nema varijabli vidljivih pregledniku: API je na istoj domeni (`/api/v1`). `SITE_ADDRESS` (domena za koju Caddy poslužuje i traži certifikat); `LOGS_USER`, `LOGS_PASSWORD_HASH` (prijava na `/logs`)                                                                                                                                              |
| **Poslužiteljski dio**    | EC2 instanca, kontejner `backend` iz imagea `ghcr.io/ld05-fk/artconnect-backend` ([`backend/Dockerfile`](../backend/Dockerfile)); grana `master`                                                 | Image gradi CD (Maven gradi `app.jar`, kontejner pokreće `java -jar app.jar`); poslužitelj ga povlači i pokreće `docker compose -f docker-compose.prod.yml up -d`                                                                     | `DB_URL`, `DB_USER`, `DB_PASSWORD`; `SPRING_LIQUIBASE_CONTEXTS=prod` (bez demo podataka); `SPRING_PROFILES_ACTIVE=prod` (JSON zapisi); `SERVER_FORWARD_HEADERS_STRATEGY=native` (iza Caddyja); `ARTCONNECT_ADMIN_EMAIL` (e-mail prvog administratora); `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (vidi niže). Port 8080 samo unutar Docker mreže |
| **Baza podataka**         | EC2 instanca, kontejner `db` (slika `postgres:17`), podaci na volumenu `db-data`                                                                                                                 | Shemu stvara **Liquibase pri pokretanju backenda**; changeseti s kontekstom `dev` se ne primjenjuju                                                                                                                                   | `DB_NAME`, `DB_USER`, `DB_PASSWORD`; port 5432 nije izložen                                                                                                                                                                                                                                                                                       |
| **Sesija i kolačić**      | Poslužiteljska sesija putem Spring Session JDBC; JWT se ne koristi                                                                                                                               | `server.servlet.session.timeout=8h`; `server.servlet.session.cookie.same-site=lax`; `server.servlet.session.cookie.secure=true` u produkciji, a `false` u profilu `dev` zbog lokalnog HTTP-a. Kolačić `SESSION` mora imati `HttpOnly` |

**Konfiguracija sesije:** Autentifikacija koristi poslužiteljsku sesiju putem Spring Session JDBC, bez JWT-a. Sesija istječe nakon osam sati neaktivnosti, a identifikator se sprema u kolačić SESSION s oznakama HttpOnly i SameSite=Lax. U osnovnoj konfiguraciji application.yml oznaka Secure isključena je zbog lokalnog razvoja preko HTTP-a, dok je u produkcijskom profilu application-prod.yml uključena jer se aplikaciji pristupa preko HTTPS-a. Shemu tablica sesija stvara Liquibase.

Oba imagea su javna u GitHub Container Registryju, s oznakama `latest` i sha commita; koju inačicu poslužitelj pokreće određuje `IMAGE_TAG` u `.env` (zadano `latest`). Sve vrijednosti su u datoteci `.env` na poslužitelju (predložak [`.env.example`](../.env.example), prava `600`), a Docker Compose ih predaje kontejnerima. Vrijednosti ima voditelj tima; ne nalaze se u repozitoriju ni u dokumentaciji. Token za DuckDNS je u `/etc/duckdns.env` (čita ga samo root).

**CI i CD:** kontinuirana integracija postoji: workflow [CI](../.github/workflows/ci.yml) na svakom pull requestu i pushu na `master` gradi i testira backend i frontend (provjere `Backend` i `Frontend`), a pull request se spaja tek kad su zelene i uz jedno odobrenje. Uz to postoji **kontinuirano postavljanje** (workflow [CD](../.github/workflows/cd.yml)): nakon što CI prođe na `master`u, CD izgradi i objavi imagee, a ako poslužitelj radi, preko AWS Systems Managera (Run Command) mu naredi da povuče novu inačicu i provjeri `/actuator/health`; neuspjela provjera označava workflow crvenim. Postavlja se samo ono što je prošlo pull request, CI i pregled. CD se u AWS prijavljuje ključem IAM korisnika koji smije samo pročitati stanje instance i poslati joj naredbu (tajne `AWS_CD_ACCESS_KEY_ID`, `AWS_CD_SECRET_ACCESS_KEY`).

### Postupak postavljanja

1. **Infrastruktura:** EC2 instanca s parametrima iz §6.1, _Auto-assign public IP_, bez Elastic IP-a, _Shutdown behavior: Stop_ i zaštitom od brisanja. Sigurnosna grupa dopušta dolazni promet samo na 80 i 443 (svima) te 22 (SSH, samo s adrese održavatelja).
2. **Priprema poslužitelja:** Docker Engine s Compose dodatkom; IAM uloga instance s politikom `AmazonSSMManagedInstanceCore` (za CD); kloniranje repozitorija; `cp .env.example .env`, upis jake lozinke baze, Google vrijednosti i `SITE_ADDRESS=:80`, zatim `chmod 600 .env`.
3. **Domena:** poddomena `artconnect-fer` na DuckDNS-u i systemd servis koji pri svakom pokretanju instance javi javnu IP adresu (postupak i servis: [HOW_TO_RUN.md, Domena i HTTPS](../HOW_TO_RUN.md#domena-i-https-duckdns)). Provjera: `getent hosts artconnect-fer.duckdns.org` vraća javnu IP instance.
4. **Izgradnja i pokretanje:** imagee izgradi CD (ili ručno `docker compose -f docker-compose.prod.yml up -d --build`), poslužitelj ih pokreće s `docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d`; systemd servis `artconnect-update` to ponavlja pri svakom paljenju. Pri prvom pokretanju backend preko Liquibasea stvara shemu u praznoj bazi. Provjera: `docker compose -f docker-compose.prod.yml ps` (sva tri kontejnera _Up_, `db` _healthy_).
5. **HTTPS i završna provjera:** u `.env` `SITE_ADDRESS=artconnect-fer.duckdns.org`, pa `docker compose -f docker-compose.prod.yml up -d`; Caddy dobiva certifikat od Let's Encrypta (portovi 80 i 443 moraju biti otvoreni, a DNS već pokazivati na instancu). Provjera: `https://artconnect-fer.duckdns.org/actuator/health` vraća `"status":"UP"`, a `http://` adresa preusmjerava na `https://`. CORS se ne postavlja jer su frontend i API na istoj domeni.

### Pristup i administracija

- **Paljenje i gašenje:** poslužitelj nije stalno upaljen (štedi AWS kredite), a stalno radi u razdobljima ocjenjivanja i demonstracije. Pali ga i gasi bilo koji član tima u GitHubu: _Actions → Server → Run workflow_ i odabir `start`, `stop` ili `status` ([server.yml](../.github/workflows/server.yml)); AWS račun nije potreban. Svaku noć u 1:00 UTC workflow sam gasi upaljen poslužitelj; u razdobljima kad mora stalno raditi to se isključuje varijablom repozitorija `ALWAYS_ON=true`. Workflow se u AWS prijavljuje ključem IAM korisnika koji smije samo paliti, gasiti i čitati stanje ove instance (tajne `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `EC2_INSTANCE_ID` u GitHub Secrets). Nakon paljenja aplikacija radi za 1–2 minute: kontejneri se sami podignu (`restart: unless-stopped`), a DuckDNS dobije novu IP adresu.
- **Nadzor rada:** javni `https://artconnect-fer.duckdns.org/actuator/health` vraća samo `"status":"UP"` ili `"DOWN"` (uključuje vezu s bazom i prostor na disku, bez detalja); ostali actuator endpointi nisu izloženi. UptimeRobot (besplatni plan) provjerava ga svakih 5 minuta i e-mailom javlja voditelju tima pad i oporavak. Monitor je uključen samo kad poslužitelj mora raditi (20. 11. – 6. 12. i od 22. 1. do demonstracije, uz `ALWAYS_ON=true`) i za vrijeme labosa; inače je pauziran, da ugašen poslužitelj ne javlja lažne padove. Tko upali poslužitelj ručno za demonstraciju, javlja voditelju da uključi monitor.
- **Zapisi:** backend u profilu `prod` piše zapise u JSON obliku (razina `INFO`, `com.zaxxer.hikari` `WARN`). Docker čuva zapise svakog kontejnera u najviše 5 datoteka po 10 MB. Članovi tima zapise backenda, baze i Caddyja čitaju u pregledniku na **`https://artconnect-fer.duckdns.org/logs`** (Dozzle; korisničko ime i lozinku daje voditelj tima). Dozzle samo prikazuje zapise: kontejneri se iz njega ne mogu zaustaviti ni mijenjati, a njegov port nije izložen. Lozinka je u `.env` kao hash (`LOGS_USER`, `LOGS_PASSWORD_HASH`; hash: `docker run --rm caddy:2 caddy hash-password --plaintext '<lozinka>'`, u `.env` u jednostrukim navodnicima). Terminal na poslužitelju (SSH) ima samo voditelj tima.

- **Upravljanje administratorskim ovlastima:** administratorsko sučelje je pod `/admin`. Prvog administratora stvara Liquibase pri pokretanju iz varijable `ARTCONNECT_ADMIN_EMAIL` u `.env` poslužitelja: jedan red s ulogom `ADMIN` i statusom `INVITED`, koji se aktivira njegovom prvom Google prijavom. Prazna varijabla znači da se ne stvara nitko. Lozinke u aplikaciji ne postoje.
- **Postupak ažuriranja:** promjena ulazi u `master` samo kroz pull request sa zelenim CI-jem i jednim odobrenjem, a dalje je automatska (CD, vidi gore). Ako je poslužitelj tada ugašen, CD samo objavi imagee, a servis `artconnect-update` na poslužitelju pri sljedećem paljenju povuče i pokrene najnoviju inačicu. **Povratak na stariju inačicu:** _Actions → CD → Run workflow_ i upisati sha commita; sljedeća normalna isporuka vraća `latest`. Rezervni ručni postupak (bez registryja): `docker compose -f docker-compose.prod.yml up -d --build` na poslužitelju. Nikad se ne koristi `docker compose down -v`, jer briše bazu i certifikat.
- **Sigurnosne kopije i oporavak:** systemd timer na poslužitelju jednom dnevno (18:00 UTC, a ako je poslužitelj tada ugašen, odmah nakon sljedećeg paljenja) izvodi `pg_dump` baze iz kontejnera `db` u komprimiranu datoteku na disku instance; čuva se zadnjih 7 kopija. Kopije štite od pogrešne migracije ili brisanja podataka, ne i od gubitka same instance, pa ih održavatelj povremeno preuzima na svoje računalo. Vraćanje: kopija se učita u bazu s `psql` (`gunzip -c <kopija> | docker compose -f docker-compose.prod.yml exec -T db psql -U <DB_USER> -d <baza>`); postupak je provjeren vraćanjem u privremenu bazu.

### Runbook

| Situacija                                  | Što napraviti                                                                                                                                                                                                                                                                                             |
| ------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Treba pogledati zapise                     | Preglednik: `/logs`. Na poslužitelju: `docker compose -f docker-compose.prod.yml logs -f backend` (analogno `caddy`, `db`, `dozzle`); DuckDNS: `journalctl -u duckdns.service -b`                                                                                                                         |
| Aplikacija ne radi, poslužitelj je upaljen | U `/logs` pogledati zadnje greške backenda. Ponovno pokretanje: `docker compose -f docker-compose.prod.yml restart backend` (ili bez imena servisa za sve); ako je kontejner pao, `docker compose -f docker-compose.prod.yml up -d`                                                                       |
| Treba upaliti ili ugasiti poslužitelj      | _Actions → Server → Run workflow_ (`start`, `stop`, `status`). Rezervno, s AWS pristupom: EC2 konzola (_Instance state_) ili `aws ec2 start-instances --instance-ids <id> --region eu-north-1` (`stop-instances` za gašenje)                                                                              |
| Upaljen, ali domena ne odgovara            | Provjeriti `getent hosts artconnect-fer.duckdns.org` prema javnoj IP iz workflowa _Server_ (`status`). Ako se razlikuju, DuckDNS nije osvježen: `sudo systemctl restart duckdns.service` i `journalctl -u duckdns.service -n 5` (treba pisati `OK`); ako piše `KO`, provjeriti token u `/etc/duckdns.env` |
| Nova inačica ne radi                       | Povratak na stariju inačicu: _Actions → CD → Run workflow_ sa sha prethodnog commita (vidi _Postupak ažuriranja_)                                                                                                                                                                                         |

### Google OAuth na javnoj adresi

Na poslužitelju (profil `prod`) prijava ide isključivo Googleom; zamjenska prijava `dev/login-as` ne postoji.

| Postavka | Vrijednost | Gdje se postavlja |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | iz OAuth klijenta u Google Cloud projektu „ArtConnect” | `.env` na poslužitelju, uz ostale tajne (nikad u repozitorij); vrijednosti ima voditelj tima. Backendu ih prosljeđuje `docker-compose.prod.yml` (`environment` servisa `backend`, dodaje se uz AUTH-01-BE1) |
| Authorized redirect URI | `https://artconnect-fer.duckdns.org/login/oauth2/code/google` | Google Cloud konzola → Google Auth Platform → Clients (već postavljeno) |
| Authorized JavaScript origin | `https://artconnect-fer.duckdns.org` | isto (već postavljeno) |
| Tko se može prijaviti | samo korisnici na popisu _Test users_ (aplikacija je u načinu _Testing_, najviše 100 korisnika) | Google Auth Platform → Audience; asistentici i nastavniku za ocjenjivanje treba dodati njihove Google e-mailove |

Backend iza Caddyja koristi zaglavlja `X-Forwarded-*` (`SERVER_FORWARD_HEADERS_STRATEGY: native` u `docker-compose.prod.yml`), pa Googleu šalje HTTPS redirect URI javne domene. Ako se domena promijeni, novi redirect URI i origin dodaju se u Google konzolu prije prebacivanja. Lokalna konfiguracija: §6.2 i [HOW_TO_RUN.md, Google OAuth](../HOW_TO_RUN.md#google-oauth).
