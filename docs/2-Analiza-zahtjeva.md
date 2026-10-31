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
| FR-01 | Korisnik se registrira i prijavljuje isključivo Google računom, odjavljuje se, pri prvoj prijavi odabire ulogu umjetnika ili organizatora, a sustav ograničava funkcije prema ulozi. Uloga prvog administratora određuje se konfiguracijom. Korisnik može zatražiti brisanje vlastitog računa. | Visok | 1. ciklus; brisanje računa u 2. | G06; odluka tima | Prihvaćen je kada ponovna prijava istim Google računom vraća postojeći račun, novi korisnik dobiva jednu dopuštenu ulogu, zaštićene funkcije odbijaju neovlašten pristup, a brisanje računa uklanja ili anonimizira osobne podatke uz očuvanje povezanih zapisa. |
| FR-02 | Umjetnik uređuje svoj profil, prethodne izložbe i djela, učitava slike te određuje vidljivost djela; svaki umjetnik ima dijeljivi javni portfolio bez javnog imenika. Organizator uređuje naziv svoje organizacije. | Visok | 1. ciklus; sudjelovanja na portfoliju u 2. | G06; odluka tima | Prihvaćen je kada se spremljeni podaci i dopuštene slike prikazuju na profilu, javni portfolio na dijeljivoj poveznici prikazuje javni sadržaj, a privatna djela i datoteke dostupni su samo ovlaštenim osobama. |
| FR-03 | Organizator stvara i uređuje priliku s osnovnim podacima te fizičkom lokacijom na karti ili online poveznicom. | Visok | 1. ciklus | G06; odluka tima | Prihvaćen je kada valjana prilika bude spremljena kao privatni nacrt vlasnika, fizička lokacija ima položaj na karti, a online lokacija valjanu poveznicu; nevaljan ili nepotpun unos ne sprema se. |
| FR-04 | Organizator vodi priliku kroz dopuštene statuse, a sustav omogućuje arhiviranje te javni pregled i pretraživanje objavljenih prilika. | Visok | 1. ciklus; automatsko zatvaranje i zaštita brisanja u 2. | G06; odluka tima | Prihvaćen je kada se status mijenja samo dopuštenim prijelazom, prilika nakon isteka roka bude zatvorena, prilika s prijavama se arhivira umjesto briše, a javni posjetitelj vidi samo objavljene, neblokirane i neskrivene prilike koje odgovaraju odabranim filtrima. |
| FR-05 | Organizator za priliku određuje što prijava mora sadržavati, a umjetnik predaje, povlači ili dopunjava svoju prijavu u skladu s tim zahtjevima. | Visok | 2. ciklus | G06; odluka tima | Prihvaćen je kada se prijava može predati samo za otvorenu priliku i uz ispunjene obavezne stavke, neispravna ili ponovljena aktivna prijava bude odbijena bez spremanja, povučena prijava se može ponovno predati dok je prilika otvorena, a zatražena dopuna i nakon isteka roka. |
| FR-06 | Organizator pregledava prijave na svoje prilike i donosi odluke o njihovu statusu; umjetnik vidi status, povijest i poruke vezane uz svoju prijavu. | Visok | 2. ciklus | G06; odluka tima | Prihvaćen je kada organizator vidi prijave samo svojih prilika, dopuštene promjene statusa traže poruku za dopunu ili razlog odbijanja, a umjetnik vidi odluku i odgovarajuću poruku. |
| FR-07 | Organizator objavljuje i ispravlja rezultate, sudionicima se evidentira ostvareno sudjelovanje na portfoliju, a umjetnik može dodati prihvaćeno događanje u Google kalendar. | Srednji | 2. ciklus | G06; odluka tima | Prihvaćen je kada objava rezultata završi priliku i bude vidljiva na njezinu detalju, označeni sudionici budu prikazani na javnom portfoliju te umjetnik uz prihvaćenu prijavu može otvoriti kalendar s podacima događanja. |
| FR-08 | Organizator pregledava statistiku svojih prilika, a administrator pregledava mjesečne izvještaje o korištenju platforme. | Srednji | 2. ciklus | G06; odluka tima | Prihvaćen je kada organizator vidi točne brojeve samo za vlastite prilike, a administrator može vidjeti izvještaj za prethodni mjesec s brojem umjetnika, objavljenih prilika i predanih prijava. |
| FR-09 | Administrator upravlja korisnicima, ulogama, odobrenjem organizatora, vidljivošću sadržaja, šifrarnicima i ograničenjima datoteka. | Visok | 2. ciklus | G06; odluka tima | Prihvaćen je kada samo administrator može izvesti upravljačke radnje, organizator bez odobrenja ne može javno objaviti priliku, blokiran ili skriven sadržaj više nije javno dostupan, a promjene šifrarnika i ograničenja vrijede za nove unose i učitavanja. |
| FR-10 | Sustav korisniku prikazuje obavijesti unutar aplikacije o relevantnim promjenama vezanim uz njegov račun, prijave i rezultate. | Nizak | 2. ciklus; opcionalno | Odluka tima | Prihvaćen je kada korisnik vidi vlastite nepročitane obavijesti, može ih označiti pročitanima i otvoriti pripadnu stranicu, a ne vidi obavijesti drugih korisnika. |

**Niskoprioritetne dodatne mogućnosti:** plasman sudionika (1., 2. ili 3. mjesto), izravno umetanje događanja putem Google Calendar API-ja, grafikoni statistike, povijest i ručno generiranje izvještaja, zapisnik administratorskih radnji te obavijesti unutar aplikacije. Osnovni rezultati, statistika, automatski izvještaj i ostali zahtjevi iz tablice ostaju zasebno provjerljivi.

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
| 31. 10. 2026. | FR-01–FR-10 | Sažeto je 52 detaljna funkcijska zahtjeva u 10 povezanih korisničkih ciljeva; MoSCoW je zamijenjen prioritetima Visok/Srednji/Nizak, a kriteriji prihvaćanja skraćeni su na opažljive ishode. Pojedinačni rubni slučajevi ostaju u obrascima uporabe, API dokumentaciji i ispitivanju. | Bilješke sa sastanka i dogovor o sažimanju |
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
