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
| FR-01 | Korisnik se može prijaviti Google računom i upravljati svojim računom (odabir uloge, odjava). | Visok | 1. ciklus; upravljanje i brisanje računa u 2. | G06; odluka tima | Prihvaćen je kada se korisnik može prijaviti Google računom, odabrati ulogu pri prvoj prijavi, odjaviti se i podnijeti zahtjev za brisanje vlastitog računa. |
| FR-02 | Umjetnik može uređivati svoj profil i portfolio, uključujući djela, slike i prethodne izložbe, a organizator svoj profil. | Visok | 1. ciklus; sudjelovanja na portfoliju u 2. | G06; odluka tima | Prihvaćen je kada se spremljene promjene profila prikazuju, umjetnik može odabrati vidljivost svojih djela, a njegov javni portfolio dostupan je putem poveznice. |
| FR-03 | Organizator može stvarati i uređivati prilike. | Visok | 1. ciklus | G06; odluka tima | Prihvaćen je kada organizator može spremiti novu priliku ili promjene svoje prilike te odrediti fizičku lokaciju na karti ili online lokaciju. |
| FR-04 | Posjetitelj može pregledavati i pretraživati prilike, a organizator upravljati njima i voditi ih kroz statuse. | Visok | 1. ciklus; automatsko zatvaranje i arhiviranje u 2. | G06; odluka tima | Prihvaćen je kada posjetitelj može pronaći javno dostupne objavljene prilike, a organizator pregledati svoje prilike i promijeniti njihov status. |
| FR-05 | Organizator može odrediti uvjete prijave, a umjetnik predati i upravljati svojom prijavom. | Visok | 2. ciklus | G06; odluka tima | Prihvaćen je kada organizator može postaviti uvjete prijave, a umjetnik predati, povući ili dopuniti prijavu i vidjeti njezino stanje. |
| FR-06 | Organizator može pregledati prijave i odlučivati o njima, a umjetnik pratiti odluke. | Visok | 2. ciklus | G06; odluka tima | Prihvaćen je kada organizator može prihvatiti ili odbiti prijavu, a umjetnik vidi odluku. |
| FR-07 | Organizator može objaviti rezultate, a umjetnik pregledati ishode, sudjelovanja na portfoliju i dodati prihvaćeno događanje u kalendar. | Srednji | 2. ciklus | G06; odluka tima | Prihvaćen je kada su objavljeni rezultati vidljivi sudionicima, ostvareno sudjelovanje prikazano na umjetnikovu portfoliju, a umjetnik može otvoriti Google kalendar s podacima prihvaćenog događanja. |
| FR-08 | Organizator može pregledati statistiku svojih prilika, a administrator izvještaje o korištenju platforme. | Srednji | 2. ciklus | G06; odluka tima | Prihvaćen je kada organizator vidi statistiku svojih prilika, a administrator mjesečni izvještaj. |
| FR-09 | Administrator može upravljati korisnicima, odobrenjem organizatora i sadržajem platforme. | Visok | 2. ciklus | G06; odluka tima | Prihvaćen je kada samo administrator može upravljati korisnicima i odobrenjem organizatora te sakriti neprimjeren sadržaj. |
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
| NF-06 | **Odziv:** popisi su uvijek straničeni (zadano 20, najviše 100 po stranici), a pretraga prilika koristi indekse u bazi. Pretraga prilika vraća rezultate u najviše 5 sekundi (početna vrijednost; konačnu brojku treba potvrditi s asistenticom). | Srednji | Odluka tima | Mjerenje vremena odgovora pretrage na demo podacima. |
| NF-07 | **Održivost:** u `master` ulazi samo promjena sa zelenim CI-jem (provjere Backend i Frontend) i jednim odobrenjem druge osobe. Osnovni testovi pokrivaju poslovna pravila i glavne tokove, shema baze mijenja se samo Liquibase migracijama, a SonarCloud tjedno pregledava kvalitetu koda. | Srednji | Pravila kolegija; odluka tima | Zaštita grane `master`, rezultati GitHub Actionsa, SonarCloud. |
| NF-08 | **Isporuka i rad:** spajanje u `master` automatski isporučuje novu inačicu na AWS EC2 (Docker). Poslužitelj pri paljenju sam povuče zadnju inačicu. Zapisi su dostupni u pregledniku, a baza se jednom dnevno sigurnosno kopira (čuva se zadnjih 7 kopija). | Srednji | Pravila kolegija; odluka tima | Pokretanja workflowa CD, `/actuator/health`, pregled zapisa na `/logs`. |
| NF-09 | **Dostupnost:** aplikacija nije stalno upaljena (zbog štednje AWS kredita), nego je dostupna na zahtjev i u terminima ocjenjivanja i demonstracija. | Nizak | Odluka tima | Workflow Server (`status`), varijabla `ALWAYS_ON` u razdobljima ocjenjivanja. |
| NF-10 | **Vanjske usluge:** poštuju se uvjeti korištenja OpenStreetMapa (najviše jedan zahtjev u sekundi prema Nominatimu, vlastito zaglavlje `User-Agent`, atribucija na karti). Ako usluga karte nije dostupna, aplikacija prikazuje poruku i omogućuje ručni unos položaja. | Srednji | Odluka tima (uvjeti korištenja OSM-a) | Test geokodiranja i pregled karte. |

## 2.3 Dionici i akteri

**Cilj:** Utvrditi uloge koje utječu na zahtjeve i razlikovati dionike od aktera koji komuniciraju s aplikacijom.

| ID uloge ili aktera | Odnos prema sustavu | Relevantni zahtjevi |
| --- | --- | --- |
|  |  |  |

---

## 2.4 Ograničenja i pretpostavke

**Cilj:** Zabilježiti projektno specifične granice i pretpostavke koje utječu na zahtjeve ili oblikovanje, zajedno s njihovim izvorom i praktičnom posljedicom. Ovo je njihovo mjerodavno mjesto u dokumentaciji.

| ID | Tip | Opis ograničenja ili pretpostavke | Izvor | Inženjerska posljedica / Provjera |
| --- | --- | --- | --- |
| C-01 | Ograničenje | [Fiksni tehnički ili procesni uvjet] | [Zadatak / Pravila] | [Utjecaj na arhitekturu i oblikovanje] |
| AS-01 | Pretpostavka | [Očekivani uvjet (npr. o podacima ili okolini)] | [Procjena tima] | [Kako i kada će se provjeriti valjanost] |
|  |  |  |  |

---

## 2.5 Značajne promjene zahtjeva

**Cilj:** Sačuvati razlog promjena koje mijenjaju dogovoreni opseg, prioritet ili kriterije prihvaćanja, uz istodobno održavanje prethodnih zahtjeva ažurnima.

| Datum | ID zahtjeva | Promjena i razlog | Dogovoreno s / dokaz |
| --- | --- | --- | --- |
| 31. 10. 2026. | FR-01–FR-10 | Sažeto je 52 detaljna funkcijska zahtjeva u 10 povezanih korisničkih ciljeva; zahtjevi i kriteriji prihvaćanja dodatno su skraćeni na osnovne ishode; MoSCoW je zamijenjen prioritetima Visok/Srednji/Nizak. Povezane zahtjeve za prijavu i upravljanje računom objedinjuje FR-01. Pojedinačni tokovi i rubni slučajevi ostaju u obrascima uporabe, API dokumentaciji i ispitivanju. | Bilješke sa sastanka i dogovor o sažimanju |
|  |  |  |  |

---

## 2.6 Sljedivost zahtjeva

**Cilj:** Osigurati dvosmjernu sljedivost — povezati zahtjeve s opisanim ponašanjem, dijelovima sustava koji ih ostvaruju te ispitivanjem ili drugom provjerom. 

| ID zahtjeva | Obrazac uporabe / modul | Arhitekturna realizacija | Ispitivanje ili provjera |
| --- | --- | --- | --- |
|  |  |  |  |

---

## 2.7 Pregled zahtjeva

**Cilj:** Zabilježiti važne povratne informacije o zahtjevima i odluke koje iz njih proizlaze na dvije formalne kontrolne točke projekta, u 7. i 14. tjednu.

| Kontrolna točka | Povratna informacija ili nalaz | Odluka o zahtjevu ili opsegu |
| --- | --- | --- |
| Kontrolna točka 1 (7. tjedan) | [Povratna informacija] | [Odluka; vidi odjeljak 2.5] |
| Demo 2 (11. tjedan) | [Povratna informacija] | [Odluka; vidi odjeljak 2.5] |
| Kontrolna točka 2 (14. tjedan) | [Povratna informacija] | [Odluka; vidi odjeljak 2.5] |

---

>**Završna provjera:** Zahtjevi su aktualni, provjerljivi i povezani. Svaki važan zahtjev moguće je pratiti od njegova opisa i ponašanja preko mjesta realizacije u sustavu do odgovarajućeg ispitivanja ili druge provjere. Poveznice koriste stvarne identifikatore i nazive iz projektne dokumentacije.
