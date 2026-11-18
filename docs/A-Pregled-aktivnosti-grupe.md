# A. Pregled aktivnosti grupe

## A.1 Evidencija sastanaka

**Kratice članova tima:** LD = Luka Dakić; LDi = Luka Dimjašević; LS = Luka Stare; BTŠ = Bepa Tabulov-Štrelov; VN = Vedran Novaković; NM = Nikola Mustač; BK = Bruno Kosmačin. Asistentica je navedena u svojstvu sudionice jer ime nije zabilježeno.

| Datum i sudionici | Glavna tema | Donesena odluka ili zadatak | Poveznica na zadatak ili rezultat, ako postoji |
| --- | --- | --- | --- |
| 22. 10. 2026.; LD, LDi, LS, BTŠ, VN, NM, BK | Prioriteti i organizacija rada za prvi ciklus | Tim se usredotočuje na zadatke Milestonea 1, prvo na autentifikaciju, zatim portfolio i prilike. Svi usput bilježe uporabu AI-ja; dokumentacija i testovi izrađuju se tijekom rada. | [Detaljne bilješke sastanka](./meeting-notes/Meeting1-sazetak-2026-10-22.md) |
| 26. 10. 2026.; LD, LDi, LS, BTŠ, VN, NM, BK i asistentica | Dorada funkcijskih i nefunkcijskih zahtjeva | Dogovoreno je sažeti funkcijske zahtjeve u manji broj kratkih, jasnih korisničkih ciljeva te pojednostaviti nefunkcijske zahtjeve. Zahtjevi trebaju biti razumljivi i provjerljivi; kao sigurnosno ograničenje navedeno je da aplikacija ne traži ni pohranjuje lozinke, nego koristi Google prijavu. Tim će doraditi analizu zahtjeva. | [Analiza zahtjeva](2-Analiza-zahtjeva.md) |
| 29. 10. 2026.; LD, NM, LS, LDi | Dokumentacija i testiranje Google prijave | Dokumentacija se dovršava do 31. 10. kako bi se provjerila prije predaje. Članovi šalju voditelju tima Google adrese za testiranje; nakon spajanja promjena tim provjerava aplikaciju i u produkciji. | [Detaljne bilješke sastanka](./meeting-notes/Meeting2-sazetak-2026-10-29.md) |
| 5. 11. 2026.; LD, LS, VN | Kratki sync o tijeku rada | Svatko je ukratko rekao na čemu radi; nema novih odluka. | [Detaljne bilješke sastanka](./meeting-notes/Meeting3-sazetak-2026-11-05.md) |
| 16. 11. 2026.; LD, LDi, LS, BTŠ, VN, NM, BK i asistentica | Provjera stanja aplikacije i dokumentacije prije predaje 1. ciklusa | Asistentica je imala primjedbu samo na dijagram obrazaca uporabe (§3.1), koji tim dorađuje; ostalo je bilo u redu. | [Detaljne bilješke sastanka](./meeting-notes/Meeting4-sazetak-2026-11-16.md) |

## A.2 Plan rada

**Cilj:** Prikazati planirani redoslijed rada i odgovornosti u timu. Prikažite projektne tjedne ili druga kratka razdoblja koja tim koristi, uključujući pripremu za formalne provjere.

**Trenutačni plan:** glavni prikaz je ploča [ArtConnect - razvoj](https://github.com/users/LD05-fk/projects/1) na GitHubu. Svaka faza je jedan [Milestone](https://github.com/LD05-fk/Projekt-PROGI/milestones), a zadaci imaju oznaku uloge i odgovornu osobu. Plan održava ažurnim voditelj tima (LD).

| Razdoblje | Glavni planirani rad | Odgovorni članovi | Poveznica na zadatke |
| --- | --- | --- | --- |
| Faza 1 · Ciklus 1 (do 20. 11. 2026.) | Temelji projekta, Google prijava i uloge, profil i portfolio umjetnika, objava i pretraga prilika s kartom, javni poslužitelj, dokumentacija do §4.3 | svi | [Faza 1](https://github.com/LD05-fk/Projekt-PROGI/milestone/1) |
| Faza 2 · Međuispiti (do 4. 12. 2026.) | Smanjen kapacitet zbog međuispita; uvjeti prijave na prilike i automatsko zatvaranje prijava | prema dostupnosti | [Faza 2](https://github.com/LD05-fk/Projekt-PROGI/milestone/2) |
| Faza 3 · Retrospektiva i razvoj (do 23. 12. 2026.) | Retrospektiva 1. ciklusa; predaja prijava i odluke organizatora; administracija (korisnici, odobravanje organizatora, skrivanje sadržaja, šifrarnici); alfa prezentacija | svi | [Faza 3](https://github.com/LD05-fk/Projekt-PROGI/milestone/3) |
| Faza 4 · Zimski odmor (do 6. 1. 2027.) | Rezultati i sudjelovanja, poveznica za Google kalendar, statistika organizatora, mjesečni izvještaj | prema dostupnosti | [Faza 4](https://github.com/LD05-fk/Projekt-PROGI/milestone/4) |
| Faza 5 · Završni sprint (do 22. 1. 2027.) | Brisanje računa, opcionalne mogućnosti prema raspoloživom vremenu, test opterećenja, završna dokumentacija i predaja | svi | [Faza 5](https://github.com/LD05-fk/Projekt-PROGI/milestone/5) |
| Faza 6 · Demonstracija (do 29. 1. 2027.) | Završna demonstracija i priprema za usmeni ispit | svi | [Faza 6](https://github.com/LD05-fk/Projekt-PROGI/milestone/6) |

Nedovršeni zadaci na kraju faze ne zatvaraju se, nego prelaze u sljedeću fazu (vidi [Home](Home.md#organizacija-i-proces)).

## A.3 Aktivnosti i doprinosi članova

**Cilj:** Prikazati koji su članovi radili na važnim projektnim aktivnostima i približan trud koji prijavljuju. Uz implementaciju uključite oblikovanje, dokumentaciju, integraciju, ispitivanje i postavljanje.

Tablica prikazuje rad u 1. ciklusu prema zatvorenim zadacima na GitHubu (osoba dodijeljena zadatku). Tim nije vodio evidenciju sati, pa sati nisu navedeni; trud se vidi iz broja i vrste zadataka i iz povezanih PR-ova.

| Član | Aktivnosti u 1. ciklusu | Reprezentativni zadaci |
| --- | --- | --- |
| Luka Dakić (LD) | Vođenje tima i razrada zadataka; repozitorij, CI/CD, poslužitelj (AWS EC2, HTTPS, nadzor), Google OAuth klijent; kostur backenda i frontenda; dizajnerski sustav i nacrti svih ekrana 1. ciklusa; dokumentacija (poglavlja 1–4, README, Home); testovi „Mojih prilika” | FND-01, FND-07, FND-08, FND-09, FND-10, FND-11, FND-13, FND-06-DES, DOC-07, DOC-08, OPP-03-TEST |
| Luka Dimjašević (LDi) | Baza i Liquibase migracije (korisnici, profili, šifrarnici); prethodne izložbe; dohvat datoteka; javna pretraga prilika (backend i frontend s kartom); brisanje i arhiviranje prilika | FND-03-DB, AUTH-01-DB, PORT-02-BE2, PORT-02-BE3, PORT-04-BE4, OPP-02-BE2, OPP-02-FE1, OPP-03-BE1 |
| Luka Stare (LS) | Google prijava na backendu; sesija i korisnički izbornik, odabir uloge, profil organizatora na frontendu; obrazac prilike s lokacijom; detalj prilike; stranica „Moje prilike” | AUTH-01-BE1, AUTH-01-FE1, AUTH-02-FE, AUTH-03-FE, OPP-01-FE2, OPP-01-FE3, OPP-02-FE2, OPP-03-FE |
| Bepa Tabulov-Štrelov (BTŠ) | Testna infrastruktura (Testcontainers, Vitest, Selenium); E2E testovi izgleda, prijave, odabira uloge i profila organizatora; početna stranica | FND-12, FND-06-TEST, AUTH-01-FE2, AUTH-01-TEST, AUTH-02-TEST, AUTH-03-TEST |
| Vedran Novaković (VN) | Glavni izgled aplikacije; djela i slike u portfoliju (baza, backend, frontend); pohrana datoteka; javni profil umjetnika; tablice prilika; prilike organizatora; dijagram obrazaca uporabe | FND-06-FE2, PORT-03-DB, PORT-03-BE1, PORT-03-BE2, PORT-03-BE3, PORT-03-BE4, PORT-03-FE1, PORT-03-FE2, PORT-04-BE1, PORT-04-BE2, PORT-04-BE3, PORT-05-BE, PORT-05-TEST, OPP-01-DB, OPP-03-BE3, DOC-10 |
| Nikola Mustač (NM) | Razvojna prijava za testove; profil umjetnika i organizatora (backend i frontend); prethodne izložbe i slike djela na frontendu; javni portfolio; karta (Leaflet) i geokodiranje; detalj prilike; testovi objave prilika; dijagram obrazaca uporabe | AUTH-01-BE3, AUTH-03-BE, PORT-01-BE1, PORT-01-FE, PORT-02-FE, PORT-04-FE, PORT-05-FE, OPP-01-BE3, OPP-01-FE1, OPP-02-BE1, OPP-01-TEST, DOC-10 |
| Bruno Kosmačin (BK) | Zajednička obrada grešaka i straničenje; sigurnosni kostur (sesija, CSRF, uloge); javni šifrarnici; CSRF, `/me` i odjava; odabir uloge; prilike (stvaranje, uređivanje, statusi); administracija korisnika i šifrarnika; testovi portfolija | FND-03-BE2, FND-04, FND-05, AUTH-01-BE2, AUTH-02-BE, OPP-01-BE1, OPP-01-BE2, OPP-03-BE2, ADM-01-BE2, ADM-04-BE1, PORT-01-TEST, PORT-02-TEST, PORT-03-TEST, PORT-04-TEST |

Pregledi pull requestova nisu u tablici: svaki PR pregledao je i odobrio član koji nije autor.

## A.4 Pregled promjena u repozitoriju

**Cilj:** Prikazati kada su promjene napravljene u repozitoriju i tko ih je napravio, uz napomenu da sam broj zapisa promjena ne mjeri kvalitetu doprinosa.

**Prikaz aktivnosti repozitorija:** [Insights → Contributors](https://github.com/LD05-fk/Projekt-PROGI/graphs/contributors) za granu `master`, razdoblje od 9. 10. 2026. (početak repozitorija) do predaje 1. ciklusa.

U 1. ciklusu spojeno je 66 pull requestova: BK 14, VN 13, NM 11, LDi 10, LS 8, BTŠ 6, LD 4. Voditelj tima ima manje PR-ova jer je temelje projekta (repozitorij, CI/CD, poslužitelj, kostur aplikacije) postavio na početku, prije uvođenja zaštite grane `master`, a dokumentaciju 1. ciklusa predaje skupno na kraju ciklusa.

Graf pokazuje samo commitove; ne prikazuje dizajn, pregled koda ni organizaciju rada.

## A.5 Ključni inženjerski izazovi i rješenja

**Cilj:** Dokumentirati stvarne tehničke i organizacijske prepreke s kojima se tim suočio, poduzete korektivne inženjerske korake te konačni ishod. Odaberite konkretne izazove umjesto općenite pohvale timskog rada.

| Izazov | Poduzeta radnja | Ishod ili preostalo ograničenje |
| --- | --- | --- |
| Prvih 52 funkcijska zahtjeva bila su predetaljna i teška za čitanje (povratna informacija asistentice, 26. 10.) | Zahtjevi su sažeti u 10 korisničkih ciljeva s kratkim kriterijima prihvaćanja; detalji su premješteni u obrasce uporabe, API dokumentaciju i ispitivanje | Kraća i čitljivija analiza zahtjeva; dio zadataka iz početne razrade i dalje se poziva na stare zahtjeve i ažurira se postupno |
| Ograničen AWS kredit za javni poslužitelj | Poslužitelj se pali i gasi na zahtjev (GitHub Actions) i sam se gasi noću, osim u razdobljima ocjenjivanja | Do 29. 10. potrošeno je oko 2 $ od 180 $ kredita; aplikacija nije stalno dostupna izvan razdoblja ocjenjivanja |
| Nominatim dopušta najviše jedan zahtjev u sekundi za cijelu aplikaciju | Geokodiranje ide preko backenda, koji zahtjeve ograničava na jednom mjestu; organizator može ručno postaviti lokaciju na karti | Pravilo korištenja poštuje se; pri većem broju istodobnih upita korisnik čeka ili dobiva poruku da pokuša ponovno |
| Mnogo paralelnih zadataka u istom kodu (sedam članova, oko 120 zadataka u Fazi 1) | Zadaci podijeljeni po ulogama s jasnim ovisnostima; zadatak prelazi u „Ready” tek kad su gotovi oni o kojima ovisi; svaki PR traži zelen CI i odobrenje drugog člana | `master` je ostao stabilan (CI zelen) i automatski se isporučuje na poslužitelj |
| Google prijava ne može se lako automatizirati u testovima | Dodana je razvojna prijava `POST /dev/login-as`, dostupna samo lokalno, pa E2E testovi mogu glumiti različite uloge | E2E testovi pokrivaju glavne tokove; samu Google prijavu tim provjerava ručno na javnoj adresi |

--- | --- | --- |
|  |  |  |

---

> **Završna provjera:** Sastanci navode smislene ishode i odgovorne osobe; plan odražava namjeravani rad; tablica uloženog rada i generirani prikaz repozitorija odnose se na ovaj projekt; konkretni izazovi povezani su s radnjama i njihovim rezultatima.