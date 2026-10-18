# 1. Opseg projekta

**Cilj:** Objasniti odobreni projektni zadatak vlastitim riječima tima. Čitatelj treba razumjeti problem, ciljane korisnike, cilj projekta i njegove granice bez prethodnog čitanja detaljnih zahtjeva.

## 1.1 Problem i cilj projekta

**Cilj:** Jasno razgraničiti problem koji se rješava od predloženog rješenja i stvarne koristi za korisnika.

Mladi umjetnici na početku karijere mogu teško pronaći prilike za izlaganje i natjecanje. Natječaji, izložbe i radionice objavljuju se na web-stranicama galerija i institucija, u newsletterima i na društvenim mrežama, a umjetnik ih mora sam pratiti i svaki put iznova slati svoje radove i životopis. Organizatori, s druge strane, prijave dobivaju razbacano (e-poštom, porukama, tablicama) i teško ih ujednačeno pregledati i usporediti.

ArtConnect je web-aplikacija koja te dvije strane povezuje na jednom mjestu. Umjetnik izradi digitalni portfolio (biografija, djela, prethodne izložbe), pretražuje objavljene prilike i prijavljuje se na njih. Organizator objavljuje prilike, definira što prijava mora sadržavati, pregledava pristigle prijave i objavljuje rezultate. Administrator vodi korisnike i sadržaj platforme. Korištenje je besplatno.

Cilj projekta je isporučiti funkcionalnu, javno dostupnu aplikaciju koja pokriva cijeli taj tok: od prijave putem Googlea, preko portfolija i objave prilike s lokacijom na karti, do prijave, odluke organizatora i objave rezultata.

Očekivana korist za umjetnika je što jednom unesen portfolio može koristiti za sve prijave i što status svake prijave prati na jednom mjestu. Za organizatora je korist što prijave stižu u ujednačenom obliku, prema zahtjevima koje je sam postavio.

## 1.2 Ciljani korisnici i dionici

**Cilj:** Utvrditi osobe, uloge i organizacije čije potrebe i interesi određuju projektni zadatak.

| Skupina korisnika ili dionika | Potreba ili interes relevantan za ovaj projekt |
| --- | --- |
| Posjetitelj (neprijavljen) | Pregledati objavljene prilike i javne portfolije umjetnika bez prijave. Za prijavu na priliku treba se prijaviti. |
| Umjetnik | Izraditi i urediti portfolio (biografija, djela, izložbe, vidljivost po djelu), pretraživati prilike i prijaviti se na njih, pratiti status prijave, dodati prihvaćeno događanje u svoj kalendar. |
| Organizator | Objaviti priliku (s lokacijom na karti), odrediti što prijava mora sadržavati, pregledati prijave i promijeniti im status uz razlog odbijanja, objaviti rezultate, vidjeti osnovnu statistiku. Novog organizatora mora odobriti administrator. |
| Administrator | Upravljati korisnicima i ulogama, odobravati organizatore, blokirati korisnike, sakriti neprikladan sadržaj, održavati popise područja umjetnosti i vrsta prilika te ograničenja učitavanja datoteka, pratiti mjesečni izvještaj o korištenju. |
| Nositelj kolegija i asistentica (dionici) | Ocjenjuju projekt prema zadatku i kriterijima kolegija. Ne koriste aplikaciju, ali njihovi zahtjevi (OAuth prijava, vanjska integracija, javno postavljanje, dokumentacija) određuju ograničenja projekta. |

## 1.3 Granice projekta

**Cilj:** Jasno definirati granice sustava — što je obuhvaćeno odobrenim zadatkom, što je svjesno izostavljeno ili odgođeno te o kojim vanjskim uslugama sustav izravno ovisi.

**Uključeno:**

Projekt se isporučuje u dva ciklusa.

- **Prvi ciklus** (predaja 20. 11. 2026.): prijava putem Googlea s odabirom uloge (umjetnik ili organizator), portfolio umjetnika (profil, djela s slikama, prethodne izložbe, javni portfolio), objava i uređivanje prilika s lokacijom na karti, javni popis prilika s pretraživanjem i filtrima. Aplikacija je javno postavljena.
- **Drugi ciklus** (predaja 22. 1. 2027.): prijave na prilike prema zahtjevima koje određuje organizator, pregled prijava i promjena statusa, objava rezultata, dodavanje događanja u Google kalendar, statistika za organizatora, administracija (korisnici, uloge, odobravanje organizatora, blokiranje korisnika, skrivanje sadržaja, šifrarnici, postavke), mjesečni izvještaj za administratora te brisanje vlastitog računa. Automatsko zatvaranje prijava nakon isteka roka i zaštita prilika s prijavama od brisanja isporučuju se također u drugom ciklusu.

**Izvan opsega ili odgođeno:**

- Obavijesti e-poštom: korisnici status prijave vide na stranici prijave (obavijesti unutar aplikacije su opcionalne).
- Učitavanje videa: video se prilaže samo kao poveznica (npr. YouTube ili Vimeo), jer bi pohrana i obrada videa zahtijevala neproporcionalno puno resursa.
- Više jezika: sučelje je samo na hrvatskom.
- Automatska provjera dobnih kriterija: kriteriji prijave su informativni tekst, jer bi za provjeru trebalo prikupljati datum rođenja umjetnika.
- Naknadno usklađivanje kalendara: promjene datuma događanja koje organizator napravi nakon dodavanja u kalendar ne prenose se u Google kalendar.
- Javni imenik umjetnika: portfolio se otvara poveznicom koju umjetnik dijeli, a ne pretraživanjem umjetnika.
- Filtar statistike po razdoblju i izvoz statistike: statistika prikazuje trenutne brojeve.
- Plaćanje i naplata: korištenje platforme je besplatno.

**Opcionalni dijelovi** (ako tim zaostane, odustaje se redom): 1. obavijesti unutar aplikacije, 2. poredak sudionika (1., 2., 3. mjesto) u rezultatima, 3. zapisnik radnji administratora, 4. grafikoni u statistici (ostaju samo brojevi), 5. integracija s Google Calendar API-jem (zamjenjuje je poveznica „Dodaj u kalendar"), 6. povijest i generiranje mjesečnih izvještaja na zahtjev (ostaje automatski izvještaj). Nikad se ne izostavljaju: prijava putem Googlea, portfolio, prilike s kartom, prijave sa zahtjevima po prilici, odobravanje organizatora, upravljanje ulogama, ispitivanje, javno postavljanje i dokumentacija.

**Vanjske ovisnosti:**

- **Google (OpenID Connect / OAuth 2.0):** prijava i odjava korisnika; registracija i prijava rade isključivo putem Googlea.
- **OpenStreetMap:** prikaz karte (biblioteka Leaflet, pločice karte) i pretvaranje adrese u koordinate (Nominatim), umjesto Google Maps API-ja koji traži obračunski račun.
- **Google Calendar API:** dodavanje prihvaćenog događanja u kalendar umjetnika, uz odobrenje korisnika pri prvom korištenju.
- **Poslužitelj:** AWS EC2 s javnom adresom, na kojem radi aplikacija i baza.

## 1.4 Postojeći pristupi i opravdanost projekta

**Cilj:** Objasniti kako korisnici trenutačno rješavaju problem i koja preostala potreba opravdava odobreni projekt.

Mladi umjetnici danas prilike najčešće pronalaze na stranicama pojedinih galerija i institucija, u newsletterima, na društvenim mrežama i u općim oglasnicima natječaja, a svoj rad predstavljaju na vlastitim web-stranicama ili profilima na društvenim mrežama. Ti kanali dobro služe za objavu i prikaz rada, ali nisu međusobno povezani: umjetnik prilike traži na više mjesta i za svaku se prijavljuje zasebno, a organizator prijave prikuplja kanalima koje sam odabere i bez zajedničke strukture.

ArtConnect nudi preostalo što ti kanali ne daju na jednom mjestu: portfolio vezan uz prijavu na priliku, zahtjevi prijave koje određuje sam organizator, praćenje statusa prijave i objavu rezultata. Namjerno ne zamjenjuje društvene mreže ni službene stranice institucija, nego ih nadopunjuje. Pregled konkretnih postojećih platformi nije dio opsega ovog poglavlja; ako se naknadno navede određeni proizvod, uz njega treba navesti izvor.

---

>**Završna provjera:** Opis problema, cilj projekta, korisnici i dionici, granice opsega te postojeći pristupi međusobno su usklađeni. Navedene koristi ne predstavljaju neprovjerene tvrdnje, a uključene i izostavljene funkcionalnosti odgovaraju odobrenom projektnom zadatku.
