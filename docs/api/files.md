# Učitane datoteke

Sve putanje počinju s `/api/v1`. Ovaj endpoint je **jedini** način posluživanja
učitanih datoteka — volumen s datotekama nije izravno dostupan preglednikom.
Pravo pristupa ovisi o namjeni datoteke (`purpose`), a ne o tome tko ju je
učitao: slika djela prati pravila vidljivosti djela, prilog prijave vidljiv je
samo vlasniku i administratoru.

Adresa datoteke dolazi iz polja `file.url` u odgovorima koji sadrže slike, npr.
`ArtworkImage` u [djelima](artworks.md).

## GET /files/{fileId}

Poslužuje sadržaj datoteke. Putanja je javna; ako postoji sesija, prijavljeni
korisnik uzima se u obzir pri odluci o pristupu. Odgovor se šalje kao stream,
bez učitavanja cijele datoteke u memoriju.

| Parametar | Tip | Opis |
| --- | --- | --- |
| `fileId` | UUID | ID iz `file.id`, odnosno iz adrese `file.url` |

### Odgovor `200 OK`

Tijelo je sadržaj datoteke, uz zaglavlja:

| Zaglavlje | Vrijednost |
| --- | --- |
| `Content-Type` | `image/jpeg`, `image/png` ili `application/pdf` (zapisana vrsta sadržaja) |
| `Content-Length` | veličina datoteke u bajtovima |
| `Content-Disposition` | `inline; filename="<izvorno ime>"; filename*=UTF-8''<izvorno ime>` |
| `X-Content-Type-Options` | `nosniff` |
| `Cache-Control` | `public, max-age=3600` za javne slike, `private, no-store` za sve ostalo |

`Content-Disposition` je `inline`, pa se slika i PDF otvaraju u pregledniku.
Izvorno ime dolazi u oba oblika: ASCII zamjena za starije klijente i
`filename*=UTF-8''` s postotkovnim kodiranjem za imena s hrvatskim znakovima.

Javnom slikom smatra se slika djela koje je `PUBLIC`, nije skriveno
(`hiddenByAdmin: false`) i čiji je autor aktivan. Sve ostalo — privatno ili
skriveno djelo, blokiran ili obrisan autor, prilog prijave — dobiva
`private, no-store`, pa se ne kešira ni u pregledniku ni na posredniku, čak ni
kad ga vlasnik ili administrator smije vidjeti.

### Pravo pristupa

| Pozivatelj | Slika javnog djela | Slika privatnog ili skrivenog djela | Slika djela blokiranog ili obrisanog autora | Prilog prijave |
| --- | --- | --- | --- | --- |
| Posjetitelj (neprijavljen) | 200 | `404 NOT_FOUND` | `404 NOT_FOUND` | `403 FORBIDDEN` |
| Drugi prijavljeni korisnik | 200 | `404 NOT_FOUND` | `404 NOT_FOUND` | `403 FORBIDDEN` |
| Vlasnik datoteke | 200 | 200 | 200 | 200 |
| Administrator | 200 | 200 | 200 | 200 |

Slika djela koristi isto pravilo vidljivosti kao samo djelo (vidi
[djela](artworks.md#pravila-vidljivosti-za-oba-get-endpointa)): odbijanje je
`404`, a ne `403`, kako se ne bi otkrilo da privatna ili skrivena datoteka
postoji. Prilog prijave u Fazi 1 vidi samo vlasnik i administrator; pravo
organizatora prilike na prilog prijave dodaje se u EPIC-04 (APP-04-BE3).

Slika koja je učitana, ali još nije vezana na djelo, dostupna je samo vlasniku i
administratoru.

### Greške

| Status | Kod | Kada |
| --- | --- | --- |
| 403 | `FORBIDDEN` | prilog prijave (`APPLICATION_ATTACHMENT`) kojemu pozivatelj nije vlasnik ni administrator |
| 403 | `ONBOARDING_REQUIRED` | prijavljeni korisnik još nije odabrao ulogu (opće pravilo za `/api/v1/**`) |
| 404 | `NOT_FOUND` | `fileId` nije ispravan UUID, datoteka ne postoji, nema je na disku, ili je slika koju pozivatelj ne smije vidjeti |

Datoteka zapisana u bazi koja je nestala s diska vraća `404 NOT_FOUND` i
zapisuje se u log na razini `ERROR`.

### Nuspojave

Nema — zahtjev samo čita.
