# 2. Analiza zahtjeva

**Cilj:** Iz odobrenog opsega projekta izvesti prepoznatljive, prioritetizirane i provjerljive zahtjeve. Zahtjevi se mogu mijenjati; pri završnoj predaji moraju odražavati dogovorenu aplikaciju.

## 2.1 Funkcijski zahtjevi

**Cilj:** Navesti što aplikacija mora omogućiti korisnicima, odakle pojedina mogućnost proizlazi i koji se opažljivi rezultat smatra prihvaćanjem.

Zahtjevi su grupirani po područjima (epicima plana rada). Svaki ima stabilan ID `FR-xx` na koji se pozivaju obrasci uporabe (poglavlje 3), ispitni slučajevi (poglavlje 5) i Issuei; ID-jevi se ne mijenjaju ni kad se zahtjev ukloni.

**Prioritet (MoSCoW).** *Must*: ne izostavlja se ni uz zaostajanje (osnovne funkcionalnosti koje je tim odredio kao neodvojive od projekta) ili je nužan za njih. *Should*: traži ga zadatak, ali se može pojednostaviti ako tim zaostane. *Could*: opcionalni dio; uz njega je naveden redni broj reza prema dogovorenom redoslijedu odustajanja (od 1, od čega se odustaje prvo, do 6, od čega zadnje).

**Ciklus.** Uz naslov svake skupine navedeno je u kojem se ciklusu isporučuje: 1. ciklus (predaja 20. 11. 2026.) ili 2. ciklus (predaja 22. 1. 2027.).

**Izvor.** `G06` je odobreni projektni zadatak (ArtConnect). „Odluka tima” označava pojašnjenje ili odluku koju je tim donio pri razradi zadatka (npr. način rada odobravanja organizatora, pravila prijava ili tehnička pravila).

**Uloge:** posjetitelj je neprijavljeni korisnik; umjetnik, organizator i administrator opisani su u §1.2.

### 2.1.1 Računi i prijava (1. ciklus; brisanje računa u 2.)

| ID | Opis | Prioritet | Izvor | Kriteriji prihvaćanja |
| --- | --- | --- | --- | --- |
| FR-01 | Korisnik se prijavljuje isključivo putem Googlea (OpenID Connect / OAuth 2.0); aplikacija nema lozinki. | Must | G06; odluka tima | Na početnoj stranici posjetitelj vidi gumb „Prijava s Googleom” i nakon uspješne prijave je prijavljen. Ponovna prijava istim Google računom ne stvara novog korisnika. Neuspjela prijava vraća posjetitelja na početnu stranicu s porukom „Prijava nije uspjela. Pokušajte ponovno.” |
| FR-02 | Korisnik se može odjaviti, a sesija je poslužiteljska s ograničenim trajanjem. | Must | Odluka tima | „Odjava” vraća korisnika na početnu stranicu kao posjetitelja. Sesija istječe nakon 8 sati neaktivnosti i ostaje valjana nakon ponovnog pokretanja poslužitelja. Zahtjev za zaštićeni podatak bez sesije dobiva odgovor 401. |
| FR-03 | Pri prvoj prijavi korisnik jednom odabire ulogu umjetnika ili organizatora; uloga administratora ne može se odabrati. | Must | Odluka tima | Korisnik bez uloge preusmjeren je na odabir uloge i ne može koristiti ostale funkcije dok je ne odabere. Gumb „Potvrdi” onemogućen je dok uloga nije odabrana. Nova uloga vrijedi odmah, bez ponovne prijave. Korisnik s odabranom ulogom ne može je sam promijeniti ni ponoviti odabir (poruka „Uloga je već odabrana”). |
| FR-04 | Prvi administrator unaprijed je određen e-adresom iz konfiguracije poslužitelja i aktivira se prvom prijavom tog Google računa. | Must | Odluka tima | Prva prijava računom s konfiguriranom e-adresom daje ulogu administratora bez odabira uloge. |
| FR-05 | Pristup stranicama i radnjama ovisi o ulozi: neprijavljen korisnik dobiva ponudu prijave, a prijavljen korisnik krive uloge zabranu pristupa. Izbornik prikazuje samo stavke koje uloga smije koristiti. | Must | Odluka tima | Posjetitelj na zaštićenoj stranici vidi „Prijava potrebna”. Prijavljeni korisnik krive uloge vidi stranicu 403. Izbornik umjetnika ima „Portfolio” i „Moje prijave”, organizatora „Moje prilike” i „Statistika”, administratora „Administracija”. |
| FR-06 | Posjetitelj bez prijave može pregledavati objavljene prilike i javne portfolije umjetnika; za prijavu na priliku mora se prijaviti. | Must | G06; odluka tima | Neprijavljeni posjetitelj otvara popis i detalj javne prilike te javni portfolio umjetnika bez prijave. Pokušaj prijave na priliku traži prijavu. |
| FR-07 | Organizator ima profil s nazivom organizacije; organizacija je samo naziv, a ime dolazi s Google računa. | Must | Odluka tima | Organizator vidi svoje ime (samo za čitanje), naziv organizacije i status odobrenja („Odobren” / „Čeka odobrenje”). Naziv do 200 znakova može se promijeniti i ostaje spremljen; dulji unos prikazuje grešku. Umjetnik i administrator ne mogu čitati ni mijenjati profil organizatora. |
| FR-08 | Korisnik može trajno obrisati vlastiti račun; osobni podaci se brišu ili anonimiziraju, a prijave i prilike ostaju s oznakom „Obrisani korisnik”. | Should | Odluka tima | Gumb „Obriši račun” omogućen je tek nakon upisa riječi „OBRISI”. Nakon brisanja umjetnikov portfolio i slike nisu dostupni, a organizator vidi njegove prijave kao „Obrisani korisnik”. Otvorene prilike obrisanog organizatora zatvaraju se, a prilike ostaju s organizatorom „Obrisani korisnik”. Ponovna prijava istim Google računom vodi na odabir uloge kao novi korisnik. Jedini aktivni administrator ne može obrisati svoj račun. |

### 2.1.2 Profil i portfolio umjetnika (1. ciklus)

| ID | Opis | Prioritet | Izvor | Kriteriji prihvaćanja |
| --- | --- | --- | --- | --- |
| FR-09 | Umjetnik uređuje svoj profil: biografiju, opis umjetničkog rada, više područja umjetnosti iz popisa koji održava administrator (najviše 10) i više poveznica na web-stranice ili društvene mreže (najviše 10). | Must | G06; odluka tima | Spremljeni podaci ostaju jednaki nakon osvježavanja stranice. Poveznica koja ne počinje s `http://` ili `https://` odbija se uz poruku uz polje. Nije moguće dodati više od 10 područja ni 10 poveznica. Umjetnik bez spremljenog profila vidi prazan obrazac, bez greške. |
| FR-10 | Umjetnik vodi popis prethodnih izložbi i projekata (naslov, godina, mjesto). | Must | G06; odluka tima | Umjetnik dodaje, uređuje i briše (uz potvrdu) izložbu. Godina izvan raspona 1900. do tekuće godine odbija se. Popis je poredan po godini silazno. Tuđu izložbu nije moguće mijenjati. |
| FR-11 | Umjetnik dodaje, uređuje i briše djela u svom portfoliju. Naslov, godina i tehnika su obavezni, dimenzije i opis neobavezni. Vidljivost (javno ili privatno) određuje se za svako djelo posebno. | Must | G06; odluka tima | Spremanje bez obaveznog polja prikazuje grešku uz polje; godina u budućnosti nije dopuštena. „Moj portfolio” prikazuje sva vlastita djela s oznakom „Javno” ili „Privatno”. Promjena u „Privatno” odmah uklanja djelo s javnog portfolija. Brisanje djela uklanja i njegove slike. Tuđe djelo nije moguće urediti ni obrisati. |
| FR-12 | Umjetnik uz djelo učitava do tri slike (samo JPG ili PNG, najviše 5 MB po slici) s neobaveznim alternativnim tekstom i može ih pojedinačno brisati. | Must | Odluka tima | Učitana slika prikazuje se uz djelo uz brojač „2/3”. Četvrta slika se odbija. PDF ili datoteka koja nije stvarno JPG/PNG odbija se porukom „Dopuštene su samo JPG i PNG slike”, a slika veća od 5 MB porukom „Slika je veća od 5 MB”. Na novom djelu slike se mogu dodati tek nakon prvog spremanja. |
| FR-13 | Datoteke se otvaraju samo uz provjeru prava: slike javnog djela svatko, a ostale samo osobe kojima su namijenjene. | Must | Odluka tima | Slika privatnog ili od administratora skrivenog djela ne otvara se posjetitelju, a vlasniku i administratoru se otvara. Prilog prijave otvaraju samo njezin autor, organizator te prilike i administrator. |
| FR-14 | Svaki umjetnik ima javni portfolio na vlastitoj dijeljivoj poveznici koji prikazuje profil, javna djela, prethodne izložbe i (od 2. ciklusa) sudjelovanja ostvarena preko platforme. Javnog imenika umjetnika nema. | Must | G06 | Posjetitelj bez prijave na poveznici portfolija vidi profil, područja, poveznice, javna djela (straničena) i izložbe; privatna djela nisu prikazana. Klik na djelo otvara pregled sa svim slikama i podacima. Vlasnik na svojoj stranici vidi traku „Ovo je vaš javni profil”. Profil nepostojećeg, blokiranog ili obrisanog umjetnika i profil organizatora daju „Umjetnik nije pronađen”. |

### 2.1.3 Prilike (1. ciklus; automatsko zatvaranje i zaštita brisanja u 2.)

| ID | Opis | Prioritet | Izvor | Kriteriji prihvaćanja |
| --- | --- | --- | --- | --- |
| FR-15 | Organizator objavljuje i uređuje priliku s poljima: naslov, opis, vrsta (natječaj, izložba, radionica, ostalo), više područja umjetnosti, rok prijave, neobavezni kriteriji (informativni tekst), nagrada ili naknada te početak i kraj događanja (kraj neobavezan). Prilika nastaje u statusu „U pripremi” kao privatni nacrt. | Must | G06; odluka tima | Nakon spremanja organizator je na detalju s oznakom „U pripremi – vidljivo samo vama”. Obavezna polja se provjeravaju uz poruku uz polje. Organizator može urediti svoju priliku u svakom statusu; tuđu priliku ne može (403). Umjetnik ne može otvoriti obrazac nove prilike. Organizator na čekanju odobrenja može stvarati prilike samo u pripremi. |
| FR-16 | Lokacija prilike je fizička (adresa prikazana na karti) ili online (poveznica). Za fizičku lokaciju organizator upisuje adresu, sustav je pretvara u položaj na karti, a organizator može povući oznaku na točan položaj ili je postaviti ručno. | Must | G06; odluka tima | „Pronađi na karti” za postojeću adresu nudi pronađene adrese i postavlja oznaku na OpenStreetMap karti; oznaka se može pomaknuti mišem ili postaviti tipkovnicom. Ako adresa nije pronađena ili usluga karte nije dostupna, prikazuje se poruka i oznaka se postavlja ručno. Fizička prilika bez oznake i online prilika bez poveznice ne mogu se spremiti. |
| FR-17 | Fizičke prilike prikazuju se na OpenStreetMap karti (na detalju i kao oznake u pretrazi) uz obaveznu atribuciju. | Must | G06; odluka tima | Detalj fizičke prilike prikazuje adresu i kartu s oznakom; online prilika prikazuje poveznicu bez karte. Prikaz „Karta” u pretrazi prikazuje oznake i poruku o prilikama bez fizičke lokacije; klik na oznaku vodi na detalj. „© OpenStreetMap contributors” vidljivo je na svakoj karti. |
| FR-18 | Organizator ručno vodi priliku kroz statuse: u pripremi → otvoreno za prijave → prijave zatvorene → odabir u tijeku → završeno. Jedini korak unatrag je ponovno otvaranje zatvorenih prijava prije isteka roka. Status „završeno” postiže se samo objavom rezultata. | Must | G06; odluka tima | Prikazani gumbi odgovaraju prijelazima dopuštenima iz trenutnog statusa, a svaki prijelaz traži potvrdu. Nedopušteni prijelaz i ponovno otvaranje nakon roka odbijaju se porukom. Nakon objave prilika je vidljiva posjetitelju u pretrazi. Drugi organizator ne može mijenjati status tuđe prilike. |
| FR-19 | Otvorena prilika kojoj je istekao rok automatski prelazi u „Prijave zatvorene”, a ponovno otvaranje moguće je tek nakon što organizator produlji rok. | Must | Odluka tima | Otvorena prilika s isteklim rokom najkasnije minutu nakon roka ima status „Prijave zatvorene” i nije u zadanoj javnoj pretrazi. Prilika s rokom u budućnosti ostaje otvorena. Ponovno otvaranje prije produljenja roka odbija se, nakon produljenja uspijeva. |
| FR-20 | Organizator može obrisati priliku samo ako nema prijava; inače je arhivira. Arhivirana prilika nije u javnoj pretrazi, ali ostaje dostupna organizatoru i umjetnicima koji su se na nju prijavili. | Must | Odluka tima | Brisanje prilike bez prijava uklanja je s popisa. Pokušaj brisanja prilike s ijednom prijavom (i povučenom) odbija se, a sučelje nudi arhiviranje. Arhivirana prilika nalazi se na kartici „Arhivirane” i ne vidi se u javnoj pretrazi; umjetnik s prijavom na nju vidi je na detalju, a ostali dobivaju „nije pronađeno”. |
| FR-21 | Javno pretraživanje prilika: ključna riječ, područje umjetnosti, vrsta, grad, vrsta lokacije, status (zadano „Otvoreno za prijave”) i raspon roka, sortiranje po roku ili datumu objave te straničenje. | Must | G06; odluka tima | Filtar „grad: Zagreb” prikazuje samo zagrebačke prilike. Odabrani filtri, sortiranje i stranica nalaze se u adresi stranice pa su dijeljivi i ostaju nakon osvježavanja. Pretraga bez rezultata prikazuje prazno stanje, a „Očisti filtre” vraća rezultate. Popis vrsta i područja u filtrima dolazi iz popisa koje održava administrator. |
| FR-22 | Javno su vidljive samo prilike u statusima „otvoreno”, „prijave zatvorene”, „odabir u tijeku” i „završeno” koje nisu arhivirane ni skrivene i čiji organizator nije blokiran. Vlasnik i administrator vide i ostale. | Must | Odluka tima | Prilike u pripremi, arhivirane, skrivene i od blokiranog organizatora nisu u pretrazi, a njihov detalj posjetitelju vraća „nije pronađeno”. Vlasnik i administrator vide ih. Prilike obrisanog organizatora ostaju javne s organizatorom „Obrisani korisnik”. |
| FR-23 | Detalj prilike prikazuje sva polja, lokaciju, popis priloga koje prijava treba sadržavati i objavljene rezultate. | Must | G06 | Detalj prikazuje naslov, opis, vrstu, područja, rok, kriterije, nagradu, datume i naziv organizatora. Odjeljak „Što treba priložiti” prikazuje za svaku stavku vrstu, najviši broj i je li obavezna (ili „Prijava ne zahtijeva priloge.”). Za završenu priliku prikazuje se odjeljak „Rezultati”. |
| FR-24 | Organizator na stranici „Moje prilike” vidi sve svoje prilike u svim statusima, odvojene na aktivne i arhivirane, s filtrom statusa i brojem prijava. | Must | G06; odluka tima | Organizator vidi samo vlastite prilike, s oznakom statusa i oznakom „Skriveno od administratora” kad je primjenjivo. Uz svaku priliku prikazan je broj prijava (bez povučenih) razvrstan po statusima; brojevi prijava prikazuju se od 2. ciklusa. Popis je straničen. |

### 2.1.4 Zahtjevi prijave i prijave (2. ciklus)

| ID | Opis | Prioritet | Izvor | Kriteriji prihvaćanja |
| --- | --- | --- | --- | --- |
| FR-25 | Organizator za svaku priliku sam određuje što prijava mora sadržavati: popis stavki, svaka s nazivom, vrstom (slike JPG/PNG, PDF, tekst, poveznica, djela iz portfolija), najvećim brojem priloga i oznakom obavezno/neobavezno te redoslijedom. Video se ne učitava, nego prilaže kao poveznica. | Must | G06; odluka tima | Stavke se dodaju, uređuju, premještaju (i tipkovnicom) i uklanjaju; nakon spremanja prikazuju se istim redoslijedom. Najveći broj uz stavku je od 1 do 10, stavki je najviše 20, a zbroj najvećih brojeva datotečnih stavki (slike, PDF) ne smije prijeći 10; takva definicija se odbija. Stavka bez naziva ne može se spremiti. Zahtjevi se mogu mijenjati u svakom statusu prilike. |
| FR-26 | Umjetnik se prijavljuje na otvorenu priliku slanjem priloga prema zahtjevima prilike, uz ograničenja veličine i broja datoteka. Jedna aktivna prijava po prilici. | Must | G06; odluka tima | Prijava se predaje samo dok je prilika otvorena i rok nije prošao. Obavezna stavka traži 1 do najvećeg broja priloga, neobavezna 0 do najvećeg broja. Slika je najviše 5 MB, PDF najviše 10 MB, a prijava najviše 10 datoteka; vrsta se provjerava po sadržaju datoteke. Neispravna prijava se ne predaje, uz poruku uz odgovarajuću stavku. Druga aktivna prijava istog umjetnika na istu priliku odbija se. Nakon povlačenja moguća je ponovna prijava dok je prilika otvorena. |
| FR-27 | Umjetnik uz prijavu može priložiti djela iz vlastitog portfolija, uključujući privatna; organizator prilike vidi priložena djela. | Must | Odluka tima | U prijavi se nude sva umjetnikova djela osim onih koje je administrator sakrio. Organizator na pregledu prijave vidi sliku i podatke priloženog privatnog djela, dok je drugi organizator, drugi umjetnik i posjetitelj ne mogu otvoriti. Organizator vidi trenutnu verziju djela. |
| FR-28 | Umjetnik na stranici „Moje prijave” vidi sve svoje prijave i na detalju prijave status, priloge, povijest statusa te poruke organizatora. | Must | G06 | Popis sadrži naslov prilike, status i datume, može se filtrirati po statusu i uključuje povučene prijave te prijave na prilike koje je administrator sakrio ili organizator arhivirao. Detalj prikazuje povijest promjena statusa i poruke organizatora („Potrebna dopuna”, „Razlog odbijanja”). Tuđu prijavu nije moguće vidjeti. |
| FR-29 | Umjetnik može povući prijavu prije odluke organizatora. | Must | Odluka tima | Povlačenje iz statusa predana, u razmatranju ili potrebna dopuna traži potvrdu; status postaje „Povučena”, a prijava ostaje zapisana s prilozima. Za prihvaćenu ili odbijenu prijavu povlačenje nije ponuđeno. |
| FR-30 | Kad organizator zatraži dopunu, umjetnik uređuje prijavu i ponovno je predaje. | Must | Odluka tima | Prijava u statusu „Potrebna dopuna” prikazuje poruku organizatora i nezadovoljene stavke. Nakon dopune i ponovne predaje status je „Predana” i povijest bilježi ponovnu predaju, čak i nakon isteka roka prijave. |
| FR-31 | Organizator pregledava prijave na svoje prilike: popis s filtrom po statusu, sortiranjem po datumu predaje i straničenjem, te pregled pojedine prijave s prilozima. | Must | G06 | Popis bez filtra ne sadrži povučene prijave; filtar „Povučene” prikazuje samo njih. Obrisani umjetnik prikazan je kao „Obrisani korisnik”. Na pregledu su vidljivi slike, PDF-ovi, tekst, poveznice i priložena djela. Drugi organizator ne vidi prijave tuđe prilike. Administrator vidi popis i pregled prijava, ali ne mijenja status. |
| FR-32 | Organizator mijenja status prijave: u razmatranju, potrebna dopuna (uz obaveznu poruku), prihvaćena, odbijena (uz obavezan razlog koji umjetnik vidi). Promjena je moguća bilo kada nakon predaje, i dok je prilika otvorena. | Must | G06; odluka tima | Nude se samo prijelazi dopušteni iz trenutnog statusa. „Potrebna dopuna” bez poruke i „Odbij” bez razloga nisu mogući. Umjetnik vidi poruku ili razlog. Prihvaćena i odbijena prijava su završna stanja. Administrator i drugi organizator ne mogu mijenjati status. |
| FR-33 | Kad organizator izmijeni zahtjeve prijave nakon što su stigle prijave, neodlučene prijave koje više ne zadovoljavaju zahtjeve automatski prelaze u „Potrebna dopuna” uz poruku što nedostaje. | Must | Odluka tima | Prije spremanja organizator vidi upozorenje o mogućem prijelazu, a nakon spremanja broj prebačenih prijava. Pogođena prijava (predana ili u razmatranju) dobiva generiranu poruku koja imenuje nedostajuću stavku i zapis povijesti „Automatski (sustav)”. Prihvaćene, odbijene i povučene prijave ostaju nepromijenjene. |

### 2.1.5 Rezultati i sudjelovanja (2. ciklus)

| ID | Opis | Prioritet | Izvor | Kriteriji prihvaćanja |
| --- | --- | --- | --- | --- |
| FR-34 | Nakon događanja organizator objavljuje rezultate: tekst objave i označene prihvaćene umjetnike koji su sudjelovali. Objava istim korakom prebacuje priliku u „Završeno”, a rezultati se poslije mogu ispraviti. | Should | G06; odluka tima | Gumb „Objavi rezultate” dostupan je samo uz priliku u statusu „Odabir u tijeku”, „Uredi rezultate” uz završenu. U obrascu se nude samo umjetnici s prihvaćenom prijavom; objava bez sudionika je moguća. Nakon potvrde prilika je „Završena”, a na detalju prilike vidljiv je odjeljak „Rezultati”. Umjetnik s prijavom na tu priliku vidi okvir „Rezultati su objavljeni”. Ispravkom uklonjen sudionik više nije naveden. |
| FR-35 | Umjetnici označeni kao sudionici automatski dobivaju sudjelovanje na javnom portfoliju, označeno kao ostvareno preko platforme. | Should | G06; odluka tima | Nakon objave rezultata posjetitelj na portfoliju označenog umjetnika vidi sudjelovanje (naslov prilike, vrsta, organizator, datum) s oznakom „Ostvareno preko ArtConnecta”, odvojeno od ručno unesenih izložbi. Umjetnik uklonjen ispravkom gubi sudjelovanje. Sudjelovanja na skrivenoj prilici ili prilici blokiranog organizatora ne prikazuju se. Umjetnik bez sudjelovanja nema taj odjeljak. |
| FR-36 | Organizator uz sudionike može navesti plasman (1., 2. ili 3. mjesto); plasman nije obavezan. | Could (rez 2) | Odluka tima | Plasman je jedinstven unutar prilike (zauzeti plasman nije dostupan drugom umjetniku). U odjeljku „Rezultati” sudionici s plasmanom navedeni su prvi s oznakom, ista oznaka vidi se uz sudjelovanje na portfoliju. Ispravkom se plasmani mogu zamijeniti ili ukloniti. |

### 2.1.6 Kalendar (2. ciklus)

| ID | Opis | Prioritet | Izvor | Kriteriji prihvaćanja |
| --- | --- | --- | --- | --- |
| FR-37 | Umjetnik s prihvaćenom prijavom može događanje dodati u svoj Google kalendar jednim klikom, preko poveznice koju sastavlja poslužitelj (bez dozvole aplikaciji). | Should | G06; odluka tima | Gumb „Dodaj u Google kalendar” vidljiv je samo uz prihvaćenu prijavu na priliku koja ima datum početka; otvara Google kalendar u novoj kartici s ispunjenim naslovom, vremenom, mjestom (adresa ili online poveznica) i opisom s poveznicom na priliku. Bez datuma završetka događaj traje 1 sat. Organizator i administrator ne mogu dobiti poveznicu. Ako preglednik blokira novu karticu, nudi se poveznica „Otvori Google kalendar”. |
| FR-38 | Događaj se u kalendar umjetnika može umetnuti izravno preko Google Calendar API-ja, uz dozvolu koja se traži tek pri prvom korištenju; ako ne uspije, ostaje poveznica iz FR-37. | Could (rez 5) | Odluka tima | Prije dozvole umjetnik vidi objašnjenje zašto se traži. Nakon dozvole događaj se umeće točno jednom i prikazuje se poveznica na događaj; osvježavanje ne stvara novi. Ako umjetnik odbije dozvolu ili Google nije dostupan, prikazuju se poruka i poveznica iz FR-37. Dozvola se ne traži pri prijavi. |

### 2.1.7 Statistika organizatora (2. ciklus)

| ID | Opis | Prioritet | Izvor | Kriteriji prihvaćanja |
| --- | --- | --- | --- | --- |
| FR-39 | Organizator vidi osnovnu statistiku svojih prilika: broj različitih prihvaćenih umjetnika, broj prijava po području umjetnosti i broj prijava po prilici. | Should | G06; odluka tima | Stranica „Statistika” prikazuje karticu „Prihvaćeni umjetnici” i dvije tablice. Povučene prijave nisu uračunate. Prijava na priliku s dva područja broji se u oba područja. Prilike bez prijava prikazane su s nulom. Naslov prilike vodi na njezine prijave. Brojevi drugih organizatora nikad se ne prikazuju. Umjetnik i administrator stranicu ne mogu otvoriti. Bez filtra razdoblja i izvoza. |
| FR-40 | Statistika se uz tablice prikazuje i jednostavnim stupčastim grafikonima. | Could (rez 4) | Odluka tima | Iznad svake tablice prikazan je grafikon s istim kategorijama i brojevima kao tablica. Tablice ostaju dostupne kao tekstualna alternativa. Ako se grafikoni ne učitaju, prikazuju se samo tablice. |

### 2.1.8 Administracija i izvještaji (2. ciklus)

| ID | Opis | Prioritet | Izvor | Kriteriji prihvaćanja |
| --- | --- | --- | --- | --- |
| FR-41 | Administrator pregledava korisnike s filtrima po ulozi, statusu, odobrenju organizatora i tražilicom. | Must | G06; odluka tima | Stavku „Administracija” vidi samo administrator; ostali na tim stranicama dobivaju 403. Filtri se nalaze u adresi stranice i popis je straničen. |
| FR-42 | Administrator mijenja ulogu korisnika; podaci korisnika ostaju, a posljedice ovise o staroj ulozi. | Must | Odluka tima | Kad organizator postane umjetnik, njegove otvorene prilike prelaze u „Prijave zatvorene”. Kad umjetnik postane organizator, njegove neodlučene prijave postaju „Povučene”, a prihvaćene i odbijene ostaju. U oba slučaja korisnik se odjavljuje. Korisnik koji dobije ulogu organizatora čeka odobrenje. Jedini aktivni administrator ne može sebi oduzeti ulogu. Odabir iste uloge odbija se. |
| FR-43 | Novi organizator mora biti odobren od administratora prije objave prilika; do tada može uređivati profil i stvarati prilike u pripremi. Organizatori postojeći prije uvođenja odobravanja ostaju odobreni. | Must | G06; odluka tima | Organizator na čekanju vidi obavijest o čekanju, a gumb „Objavi” onemogućen je uz objašnjenje; pokušaj objave odbija se. Administrator vidi organizatore na čekanju i jednim klikom ih odobrava; odobreni organizator tada objavljuje bez ponovne prijave. Do uvođenja odobravanja organizatori su automatski odobreni. |
| FR-44 | Administrator može blokirati i odblokirati korisnika. | Should | G06; odluka tima | Blokirani korisnik ne može se prijaviti, a postojeće sesije prekidaju se; na početnoj stranici vidi poruku „Vaš račun je blokiran”. Njegov profil, djela i prilike nisu javno vidljivi, a organizatoru se otvorene prilike zatvaraju. Nakon odblokiranja sadržaj je ponovno javan, a zatvorene prilike ostaju zatvorene. Administrator ne može blokirati sebe. |
| FR-45 | Administrator može sakriti i vratiti neprimjerenu priliku ili djelo (meko uklanjanje) te vidjeti popis sakrivenog sadržaja. | Should | G06; odluka tima | Sakrivena prilika ili djelo nisu vidljivi nikome osim administratoru i vlasniku, koji vidi oznaku „Skriveno od administratora”. Status prilike, prijave i rezultati se ne mijenjaju; umjetnik s prijavom na skrivenu priliku i dalje je vidi u „Mojim prijavama”. Popis se može filtrirati po vrsti i omogućuje vraćanje, nakon čega je sadržaj ponovno javan. Ostali korisnici ne vide gumbe „Sakrij” i „Vrati”. |
| FR-46 | Administrator održava popise područja umjetnosti i vrsta prilika (dodavanje, uređivanje, deaktivacija); iz njih se generiraju filtri pretraživanja. | Should | G06; odluka tima | Novo područje pojavljuje se u filtru pretraživanja. Unos postojeće šifre odbija se. Stavka koja se koristi ne može se obrisati, nego samo deaktivirati: nestaje iz filtara i novih unosa, a postojeći zapisi je zadržavaju. Nekorištena stavka može se obrisati. |
| FR-47 | Administrator mijenja ograničenja učitavanja datoteka (veličina slike djela i broj slika po djelu, veličina slike i PDF-a u prijavi, broj datoteka po prijavi). | Should | G06; odluka tima | Nova ograničenja vrijede odmah za nova učitavanja; postojeće datoteke ostaju. Vrijednost izvan dopuštenih granica odbija se uz grešku i ništa se ne sprema. Zadane vrijednosti su: slika djela 5 MB (najviše 3 po djelu), slika u prijavi 5 MB, PDF 10 MB, najviše 10 datoteka po prijavi. |
| FR-48 | Sustav automatski generira mjesečni izvještaj o korištenju za prethodni mjesec, koji administrator vidi kao pokazatelje: broj registriranih umjetnika, objavljenih prilika i predanih prijava (novi u mjesecu i ukupno). | Should | G06; odluka tima | Izvještaj za prethodni kalendarski mjesec nastaje 1. u mjesecu bez ičije radnje i sprema se. Administrator na stranici „Izvještaji” vidi najnoviji izvještaj kao tri kartice s mjesecom i oznakom „Automatski”. Ponovno generiranje istog mjeseca prepisuje zapis. Kad izvještaja nema, prikazuje se prazno stanje. Umjetnik i organizator ne mogu ga otvoriti. |
| FR-49 | Administrator vidi povijest svih izvještaja i može generirati izvještaj za završeni mjesec na zahtjev. | Could (rez 6) | Odluka tima | Povijest je poredana od najnovijeg i straničena. Generiranje za mjesec koji već ima izvještaj upozorava na prepisivanje; zapis dobiva oznaku „Ručno”. Tekući i budući mjeseci ne mogu se odabrati. Umjetnik i organizator ne mogu generirati izvještaj. |
| FR-50 | Sve administratorske radnje (promjena uloge, blokiranje, odobravanje organizatora, skrivanje sadržaja, promjena postavki i šifrarnika) bilježe se u dnevnik koji administrator pregledava. | Could (rez 3) | Odluka tima | Nakon uspješne radnje prvi zapis dnevnika opisuje tu radnju (tko, što, nad kim). Dnevnik se može filtrirati po vrsti radnje. Radnja koja ne uspije ne stvara zapis. Zapisi se ne mijenjaju i ne brišu. Samo administrator vidi dnevnik. |

### 2.1.9 Obavijesti u aplikaciji (2. ciklus, opcionalno)

| ID | Opis | Prioritet | Izvor | Kriteriji prihvaćanja |
| --- | --- | --- | --- | --- |
| FR-51 | Sustav stvara obavijest za događaje: promjena statusa prijave (umjetniku, i automatska „potrebna dopuna”), nova ili ponovno predana prijava (organizatoru), odobrenje organizatora i promjena uloge (korisniku), objavljeni ili ispravljeni rezultati (umjetniku). | Could (rez 1) | Odluka tima | Nakon predaje prijave organizator ima novu nepročitanu obavijest s poveznicom na popis prijava; nakon promjene statusa umjetnik ima obavijest s poveznicom na prijavu. Povlačenje prijave i neuspjela promjena ne stvaraju obavijest. Pogreška pri stvaranju obavijesti ne mijenja ishod izvorne radnje. E-poštom se obavijesti ne šalju. |
| FR-52 | Prijavljeni korisnik vidi zvono s brojem nepročitanih obavijesti i popis obavijesti, te ih može označiti pročitanima. | Could (rez 1) | Odluka tima | Oznaka na zvonu osvježava se unutar 70 s bez ponovnog učitavanja. Klik na obavijest označava je pročitanom i vodi na pripadnu stranicu; „Označi sve kao pročitano” uklanja oznaku. Korisnik vidi samo svoje obavijesti. Posjetitelj i korisnik koji još nije odabrao ulogu ne vide zvono. |

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
|  |  |  |  |

---

## 2.6 Sljedivost zahtjeva

**Cilj:** Osigurati dvosmjernu sljedivost — povezati zahtjeve s opisanim ponašanjem, dijelovima sustava koji ih ostvaruju te ispitivanjem ili drugom provjerom. 

| ID zahtjeva | Obrazac uporabe / modul | Arhitekturna realizacija | Ispitivanje ili provjera |
| --- | --- | --- | --- |
| F-001 | [UC-001] | [Komponenta, modul ili drugo mjesto realizacije] | [ST-01, CT-01 ili druga provjera] |
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

