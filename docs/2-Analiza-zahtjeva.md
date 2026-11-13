# 2. Analiza zahtjeva

**Cilj:** Iz odobrenog opsega projekta izvesti prepoznatljive, prioritetizirane i provjerljive zahtjeve. Zahtjevi se mogu mijenjati; pri završnoj predaji moraju odražavati dogovorenu aplikaciju.

## 2.1 Funkcijski zahtjevi

**Cilj:** Navesti što aplikacija mora omogućiti korisnicima, odakle pojedina mogućnost proizlazi i koji se opažljivi rezultat smatra prihvaćanjem.

Zahtjevi su grupirani u deset povezanih korisničkih ciljeva. Svaki ima stabilan ID `FR-xx` koji se koristi u obrascima uporabe (poglavlje 3), ispitivanju (poglavlje 5) i Issueima. ID se ne mijenja ni ako se zahtjev ukloni.

**Prioritet.** *Visok* označava osnovnu funkcionalnost bez koje se ne ostvaruje glavni cilj projekta; *Srednji* označava važnu funkcionalnost koja se može pojednostaviti; *Nizak* označava dodatnu mogućnost. Prioritet izražava relativnu važnost, a ne sam po sebi odluku o tome ulazi li funkcionalnost u pojedini ciklus.

**Isporuka.** Uz svaki zahtjev naveden je ciklus. 1. ciklus: predaja 20. 11. 2026.; 2. ciklus: predaja 22. 1. 2027. Niskoprioritetne dodatne mogućnosti navedene su zasebno kako se njihov status ne bi zamijenio s prioritetom osnovne funkcionalnosti.

**Kriteriji prihvaćanja** opisuju opažljivo ponašanje kojim se potvrđuje da je zahtjev ispunjen. Detaljni koraci, rubni slučajevi i ograničenja ostaju u obrascima uporabe, API dokumentaciji i ispitivanju.

**Izvor.** `G06` je odobreni projektni zadatak (ArtConnect). „Odluka tima” označava pojašnjenje ili odluku tima pri razradi zadatka.

**Uloge:** posjetitelj je neprijavljeni korisnik; umjetnik, organizator i administrator opisani su u §1.2.

| ID | Funkcijski zahtjev | Prioritet | Ciklus | Izvor | Kriterij prihvaćanja |
| --- | --- | --- | --- | --- | --- |
| FR-01 | Korisnik se može prijaviti Google računom i upravljati svojim računom (odabir uloge, odjava i brisanje računa). | Visok | 1. ciklus; upravljanje i brisanje računa u 2. | G06; odluka tima | Prihvaćen je kada se korisnik može prijaviti Google računom, odabrati ulogu pri prvoj prijavi, odjaviti se i podnijeti zahtjev za brisanje vlastitog računa. |
| FR-02 | Umjetnik može uređivati svoj profil i portfolio, uključujući djela, slike i prethodne izložbe, a organizator svoj profil. | Visok | 1. ciklus; sudjelovanja na portfoliju u 2. | G06; odluka tima | Prihvaćen je kada se spremljene promjene profila prikazuju, umjetnik može odabrati vidljivost svojih djela, a njegov javni portfolio dostupan je putem poveznice. |
| FR-03 | Organizator može stvarati i uređivati prilike. | Visok | 1. ciklus | G06; odluka tima | Prihvaćen je kada organizator može spremiti novu priliku ili promjene svoje prilike te odrediti fizičku lokaciju na karti ili online lokaciju. |
| FR-04 | Posjetitelj može pregledavati i pretraživati prilike, a organizator upravljati njima i voditi ih kroz statuse. | Visok | 1. ciklus; automatsko zatvaranje i arhiviranje u 2. | G06; odluka tima | Prihvaćen je kada posjetitelj može pronaći javno dostupne objavljene prilike, a organizator pregledati svoje prilike i promijeniti njihov status. |
| FR-05 | Organizator može odrediti uvjete prijave, a umjetnik predati i upravljati svojom prijavom. | Visok | 2. ciklus | G06; odluka tima | Prihvaćen je kada organizator može postaviti uvjete prijave, a umjetnik predati, povući ili dopuniti prijavu i vidjeti njezino stanje. |
| FR-06 | Organizator može pregledati prijave i odlučivati o njima, a umjetnik pratiti odluke. | Visok | 2. ciklus | G06; odluka tima | Prihvaćen je kada organizator može prihvatiti ili odbiti prijavu, a umjetnik vidi odluku. |
| FR-07 | Organizator može objaviti rezultate, a umjetnik pregledati ishode, sudjelovanja na portfoliju i dodati prihvaćeno događanje u kalendar. | Srednji | 2. ciklus | G06; odluka tima | Prihvaćen je kada su objavljeni rezultati vidljivi sudionicima, ostvareno sudjelovanje prikazano na umjetnikovu portfoliju, a umjetnik može otvoriti Google kalendar s podacima prihvaćenog događanja. |
| FR-08 | Organizator može pregledati statistiku svojih prilika, a administrator izvještaje o korištenju platforme. | Srednji | 2. ciklus | G06; odluka tima | Prihvaćen je kada organizator vidi statistiku svojih prilika, a administrator mjesečni izvještaj. |
| FR-09 | Administrator može upravljati korisnicima, odobrenjem organizatora, sadržajem i postavkama platforme; pregled zapisnika administratorskih radnji dodatna je mogućnost niskog prioriteta. | Visok | 2. ciklus | G06; odluka tima | Prihvaćen je kada samo administrator može upravljati korisnicima i odobrenjem organizatora, sakriti neprimjeren sadržaj te urediti šifrarnike i postavke; ako se dodatna mogućnost uključi, samo administrator može pregledavati zapisnik. |
| FR-10 | Sustav može prikazivati korisniku obavijesti o promjenama vezanim uz njegov račun i aktivnosti. | Nizak | 2. ciklus; opcionalno | Odluka tima | Prihvaćen je kada se korisniku prikazuju samo njegove obavijesti. |

**Niskoprioritetne dodatne mogućnosti:** plasman sudionika (1., 2. ili 3. mjesto), izravno umetanje događanja putem Google Calendar API-ja, grafikoni statistike, povijest i ručno generiranje izvještaja, zapisnik administratorskih radnji te obavijesti unutar aplikacije. Njihovo uključivanje ovisi o raspoloživom vremenu i kapacitetu tima.

## 2.2 Nefunkcijski zahtjevi

**Cilj:** Odrediti važna svojstva kvalitete i uvjete rada odobrene aplikacije na način koji tim može razumno provjeriti.

| ID | Opis | Prioritet | Izvor | Provjera |
| --- | --- | --- | --- | --- |
| NF-01 | **Sigurnost pristupa:** prijava je isključivo putem Googlea (bez lozinki). Sesija je poslužiteljska i istječe nakon 8 sati neaktivnosti, kolačić sesije nije dostupan JavaScriptu, a zahtjevi koji mijenjaju podatke zaštićeni su od CSRF napada. Pristup bez prijave vraća 401, a pristup bez ovlasti 403. | Visok | G06; odluka tima | Integracijski testovi autorizacije i sesije. |
| NF-02 | **Privatnost podataka:** privatna djela i prilozi dostupni su samo ovlaštenim osobama, brisanje računa anonimizira osobne podatke, tajne (lozinke, ključevi, tokeni) nikad nisu u repozitoriju, a javna adresa dostupna je putem HTTPS-a. | Visok | Odluka tima | Testovi vlasništva i pristupa datotekama. `.env.example` bez vrijednosti. Pregled HTTPS-a na javnoj adresi. |
| NF-03 | **Ograničenja unosa:** slike su samo JPG ili PNG, najviše 5 MB, do tri po djelu. Unos se uvijek provjerava na poslužitelju, a poruke grešaka su na hrvatskom u jedinstvenom obliku. | Srednji | Odluka tima | Testovi učitavanja datoteka i validacije. |
| NF-04 | **Responzivnost:** sučelje je upotrebljivo na mobitelu i na računalu u aktualnim inačicama preglednika Chrome, Firefox i Edge. | Srednji | Odluka tima | Ručna provjera glavnih tokova na oba oblika zaslona. |
| NF-05 | **Pristupačnost:** sve stranice mogu se koristiti tipkovnicom uz vidljiv fokus, postoji poveznica „Preskoči na sadržaj”, slike imaju alternativni tekst, a status se prikazuje i tekstom, ne samo bojom. | Srednji | Odluka tima | Pregled sučelja tipkovnicom. Provjera alatom Lighthouse na glavnim stranicama. |
| NF-06 | **Odziv:** uz do 100 istodobnih korisnika stranice se učitavaju za manje od 2 sekunde. Popisi su uvijek straničeni (zadano 20, najviše 100 po stranici), a pretraga prilika koristi indekse u bazi. | Srednji | Odluka tima | Test opterećenja u Fazi 5 ([DOC-41](https://github.com/LD05-fk/Strumfovi/issues/299)). |
| NF-07 | **Održivost:** u `master` ulazi samo promjena sa zelenim CI-jem (provjere Backend i Frontend) i jednim odobrenjem druge osobe. Osnovni testovi pokrivaju poslovna pravila i glavne tokove, shema baze mijenja se samo Liquibase migracijama, a SonarCloud tjedno pregledava kvalitetu koda. | Srednji | Pravila kolegija; odluka tima | Zaštita grane `master`, rezultati GitHub Actionsa, SonarCloud. |
| NF-08 | **Isporuka i rad:** spajanje u `master` automatski isporučuje novu inačicu na AWS EC2 (Docker). Poslužitelj pri paljenju sam povuče zadnju inačicu. Zapisi su dostupni u pregledniku, a baza se jednom dnevno sigurnosno kopira (čuva se zadnjih 7 kopija). | Srednji | Pravila kolegija; odluka tima | Pokretanja workflowa CD, `/actuator/health`, pregled zapisa na `/logs`. |
| NF-09 | **Dostupnost:** aplikacija nije stalno upaljena (zbog štednje AWS kredita), nego je dostupna na zahtjev i u terminima ocjenjivanja i demonstracija. | Nizak | Odluka tima | Workflow Server (`status`), varijabla `ALWAYS_ON` u razdobljima ocjenjivanja. |
| NF-10 | **Vanjske usluge:** poštuju se uvjeti korištenja OpenStreetMapa (najviše jedan zahtjev u sekundi prema Nominatimu, vlastito zaglavlje `User-Agent`, atribucija na karti). Ako usluga karte nije dostupna, aplikacija prikazuje poruku i omogućuje ručni unos položaja. | Srednji | Odluka tima (uvjeti korištenja OSM-a) | Test geokodiranja i pregled karte. |

## 2.3 Dionici i akteri

**Cilj:** Utvrditi uloge koje utječu na zahtjeve i razlikovati dionike od aktera koji komuniciraju s aplikacijom.

Akteri su isti kao na dijagramu obrazaca uporabe ([§3.1](3-Obrasci-uporabe.md#31-dijagram-obrazaca-uporabe)).

| ID uloge ili aktera | Odnos prema sustavu | Relevantni zahtjevi |
| --- | --- | --- |
| Posjetitelj | Akter. Neprijavljeni korisnik; pregledava prilike i javne portfolije, može se prijaviti. | FR-01, FR-02, FR-04 |
| Umjetnik | Akter. Uređuje portfolio, prijavljuje se na prilike i prati ishode. | FR-02, FR-05–FR-07, FR-10 |
| Organizator | Akter. Objavljuje prilike, odlučuje o prijavama, objavljuje rezultate i vidi statistiku. | FR-02–FR-08, FR-10 |
| Administrator | Akter. Upravlja korisnicima, odobrenjem organizatora, sadržajem i postavkama; vidi mjesečni izvještaj. | FR-08, FR-09 |
| Google OAuth 2.0 | Vanjski sustav. Potvrđuje identitet korisnika pri prijavi. | FR-01, NF-01 |
| OpenStreetMap / Nominatim | Vanjski sustav. Karta i pretvaranje adrese u koordinate. | FR-03, FR-04, NF-10 |
| Google Calendar | Vanjski sustav. Prima prihvaćeno događanje umjetnika. | FR-07 |
| Planer | Sistemski akter. Zakazani zadaci: automatsko zatvaranje prijava i mjesečni izvještaj. | FR-04, FR-08 |
| Nositelj kolegija i asistentica | Dionici. Ne koriste aplikaciju, ali njihova pravila određuju ograničenja (§2.4). | C-01–C-04 |

---

## 2.4 Ograničenja i pretpostavke

**Cilj:** Zabilježiti projektno specifične granice i pretpostavke koje utječu na zahtjeve ili oblikovanje, zajedno s njihovim izvorom i praktičnom posljedicom. Ovo je njihovo mjerodavno mjesto u dokumentaciji.

Otvorena pitanja iz početka projekta (C-02, C-03, C-05–C-07, AS-01) potvrđena su s asistenticom.

| ID | Tip | Opis ograničenja ili pretpostavke | Izvor | Inženjerska posljedica / Provjera |
| --- | --- | --- | --- | --- |
| C-01 | Ograničenje | Prijava je samo putem Googlea; aplikacija ne traži ni ne pohranjuje lozinke. | Pravila kolegija | OAuth 2.0 prijava i poslužiteljska sesija (NF-01). |
| C-02 | Ograničenje | Obavezna vanjska integracija s kartom ostvaruje se OpenStreetMapom umjesto Google Mapsa, jer Google traži obračunski račun. | Odluka tima, potvrđeno s asistenticom | Leaflet i Nominatim ([ADR-02](4-Arhitektura-i-oblikovanje.md#adr-02-openstreetmap--leaflet-umjesto-google-mapsa), [ADR-03](4-Arhitektura-i-oblikovanje.md#adr-03-geokodiranje-preko-backenda-nominatim)); NF-10. |
| C-03 | Ograničenje | Aplikacija je javno dostupna od prvog ciklusa, na AWS EC2 u Dockeru, uz ograničen AWS kredit. | Pravila kolegija; potvrđeno s asistenticom | Automatska isporuka i rad na zahtjev (NF-08, NF-09). |
| C-04 | Ograničenje | Rokovi predaje su 20. 11. 2026. i 22. 1. 2027.; rad svih članova mora biti vidljiv na GitHubu (Issues, commitovi, PR-ovi). | Pravila kolegija | Podjela zahtjeva po ciklusima (§2.1); rad kroz Issue i PR. |
| C-05 | Ograničenje | Datum rođenja umjetnika ne prikuplja se. | Odluka tima, potvrđeno s asistenticom | Dobni kriteriji prilike samo su informativni tekst, bez automatske provjere. |
| C-06 | Ograničenje | Brisanje računa anonimizira osobne podatke, a prijave i rezultati ostaju sačuvani. | Odluka tima, potvrđeno s asistenticom | FR-01, NF-02. |
| C-07 | Ograničenje | Google aplikacija radi u testnom načinu: prijavu i Google Calendar mogu koristiti samo dodani testni korisnici. | Odluka tima, potvrđeno s asistenticom | Tim dodaje račune za testiranje i demonstraciju u Google Cloud. |
| AS-01 | Pretpostavka | Obavijesti unutar aplikacije nisu nužne; status prijave vidi se na stranici prijave. | Odluka tima, potvrđeno s asistenticom | FR-10 je opcionalan (Nizak prioritet). |
| AS-02 | Pretpostavka | Besplatni OpenStreetMap i Nominatim dovoljni su za opterećenje projekta. | Procjena tima | Test geokodiranja; ručni unos položaja ako usluga ne radi (NF-10). |
| AS-03 | Pretpostavka | Korisnici koriste aktualne inačice preglednika Chrome, Firefox ili Edge. | Procjena tima | Ručna provjera glavnih tokova (NF-04). |

---

## 2.5 Značajne promjene zahtjeva

**Cilj:** Sačuvati razlog promjena koje mijenjaju dogovoreni opseg, prioritet ili kriterije prihvaćanja, uz istodobno održavanje prethodnih zahtjeva ažurnima.

| Datum | ID zahtjeva | Promjena i razlog | Dogovoreno s / dokaz |
| --- | --- | --- | --- |
| 1. 11. 2026. | FR-01–FR-10 | 52 detaljna funkcijska zahtjeva sažeta su u 10 korisničkih ciljeva s kratkim kriterijima prihvaćanja, a prioriteti MoSCoW zamijenjeni su s Visok/Srednji/Nizak. Razlog: zahtjevi trebaju biti kratki i razumljivi; detalji ostaju u obrascima uporabe, API dokumentaciji i ispitivanju. | Povratna informacija asistentice na vježbi 26. 10. 2026.; [PR #357](https://github.com/LD05-fk/Strumfovi/pull/357) (DOC-10) |

---

## 2.6 Sljedivost zahtjeva

**Cilj:** Osigurati dvosmjernu sljedivost — povezati zahtjeve s opisanim ponašanjem, dijelovima sustava koji ih ostvaruju te ispitivanjem ili drugom provjerom. 

Sljedivost se vodi vezama, bez zasebne matrice: zahtjev `FR-xx` → obrazac uporabe `UC-xx` (poglavlje 3) → Issue i PR koji ga ostvaruju → ispitni slučaj (poglavlje 5).

**Primjer:** FR-03 (organizator stvara i uređuje prilike) opisan je u [UC-04](3-Obrasci-uporabe.md#uc-04-upravljati-prilikama). Ostvaren je zadacima OPP-01 (backend modul `opportunity`), a ispitan PR-om [#379](https://github.com/LD05-fk/Strumfovi/pull/379). Ispitni slučaj `OPP-01-IT-02` ([§5.2](5-Ispitivanje.md#opp-01--objava-i-uređivanje-prilike)) potvrđuje da vlasnik može spremiti promjene svoje prilike.

| ID zahtjeva | Obrazac uporabe / modul | Arhitekturna realizacija | Ispitivanje ili provjera |
| --- | --- | --- | --- |
| FR-01 | UC-01 | `auth`; zadaci AUTH-01, AUTH-02 | [AUTH-01-TEST](5-Ispitivanje.md#auth-01-test---3112026), [AUTH-02-TEST](5-Ispitivanje.md#auth-02-test--odabir-uloge-pri-prvoj-prijavi) |
| FR-02 | UC-02, UC-03 | `artist`, `artwork`, `file`; zadaci PORT-01–PORT-05, AUTH-03 | [PORT-01](5-Ispitivanje.md#port-01--profil-umjetnika), [PORT-03](5-Ispitivanje.md#port-03--djela-u-portfoliju), [PORT-05](5-Ispitivanje.md#port-05--javni-portfolio-i-matrica-vidljivosti), [AUTH-03-TEST](5-Ispitivanje.md#auth-03-test--profil-organizatora) |
| FR-03 | UC-04 | `opportunity`, `catalog`; zadaci OPP-01 | [OPP-01](5-Ispitivanje.md#opp-01--objava-i-uređivanje-prilike) |
| FR-04 | UC-04, UC-05 | `opportunity`; zadaci OPP-02, OPP-03 | [OPP-02](5-Ispitivanje.md#opp-02--javna-pretraga-prilika), [OPP-03](5-Ispitivanje.md#opp-03--moje-prilike-i-upravljanje-statusom) |
| FR-05 | UC-06 | `application` | Planirano (2. ciklus) |
| FR-06 | UC-07 | `application` | Planirano (2. ciklus) |
| FR-07 | UC-08 | `result`, `calendar` | Planirano (2. ciklus) |
| FR-08 | UC-09 | `statistics`, `report` | Planirano (2. ciklus) |
| FR-09 | UC-10, UC-12 | `admin` | Planirano (2. ciklus) |
| FR-10 | UC-11 | `notification` | Planirano (2. ciklus; opcionalno) |

---

## 2.7 Pregled zahtjeva

**Cilj:** Zabilježiti važne povratne informacije o zahtjevima i odluke koje iz njih proizlaze na dvije formalne kontrolne točke projekta, u 7. i 14. tjednu.

| Kontrolna točka | Povratna informacija ili nalaz | Odluka o zahtjevu ili opsegu |
| --- | --- | --- |
| Vježba, 26. 10. 2026. | Funkcijskih zahtjeva ima previše i predetaljni su; nefunkcijske zahtjeve treba pojednostaviti. Sigurnost se ostvaruje Google prijavom bez lozinki. | Zahtjevi sažeti u FR-01–FR-10 i pojednostavljeni NF-01–NF-10; vidi §2.5 (1. 11. 2026.). |
| Kontrolna točka 1 (7. tjedan) | Još nije održano. | — |
| Demo 2 (11. tjedan) | Još nije održano. | — |
| Kontrolna točka 2 (14. tjedan) | Još nije održano. | — |

**Broj funkcijskih zahtjeva po prioritetu i ciklusu:**

| Prioritet | 1. ciklus | 2. ciklus | Ukupno |
| --- | --- | --- | --- |
| Visok | 4 (FR-01–FR-04, dijelom) | 3 (FR-05, FR-06, FR-09) | 7 |
| Srednji | — | 2 (FR-07, FR-08) | 2 |
| Nizak | — | 1 (FR-10, opcionalno) | 1 |

---

>**Završna provjera:** Zahtjevi su aktualni, provjerljivi i povezani. Svaki važan zahtjev moguće je pratiti od njegova opisa i ponašanja preko mjesta realizacije u sustavu do odgovarajućeg ispitivanja ili druge provjere. Poveznice koriste stvarne identifikatore i nazive iz projektne dokumentacije.
