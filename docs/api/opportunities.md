# Prilike — geokodiranje

Ovaj odjeljak dokumentira endpoint za geokodiranje adrese.

## `GET /api/v1/geocoding/search`

Geokodira adresu preko OSM Nominatima. Frontend poziva samo ArtConnect API; javni Nominatim poziva isključivo backend. Rezultati se ne spremaju u bazu.

**Pristup:** prijavljeni korisnik s ulogom `ORGANIZER`, neovisno o tome je li organizator odobren. Organizator može geokodirati dok priprema priliku; odobrenje je potrebno zasebno za objavu prilike.

| Parametar | Obavezno | Ograničenje |
| --- | --- | --- |
| `street` | da | 3–200 znakova |
| `city` | da | 2–100 znakova |
| `postalCode` | ne | najviše 10 znakova |

Uspješan odgovor sadrži najviše pet rezultata:

```json
{
  "data": [{
    "displayName": "Unska 3, Zagreb",
    "latitude": 45.801235,
    "longitude": 15.971235,
    "city": "Zagreb",
    "postalCode": "10000"
  }]
}
```

Ako Nominatim ne pronađe adresu, odgovor je `200` s `"data": []`; organizator tada ručno postavlja oznaku na karti.

Backend traži Nominatimov `/search` s `format=jsonv2`, `addressdetails=1`, `limit=5` i `accept-language=hr`. Grad se bira redoslijedom `city`, `town`, `village`, `municipality`; koordinate se pretvaraju u brojeve i zaokružuju na šest decimala. Svaki zahtjev šalje `User-Agent: ArtConnect/1.0 (+https://artconnect-fer.duckdns.org)`.

Zahtjevi prema Nominatimu globalno su ograničeni na najviše jedan u sekundi. Zahtjev čeka slobodan termin najviše dvije sekunde; nakon toga API vraća `429 RATE_LIMITED`. Timeout, mrežna greška, HTTP 4xx/5xx ili neispravan odgovor vanjskog servisa vraća `502 GEOCODING_UNAVAILABLE`. Detalji vanjskog odgovora ne šalju se klijentu.

| HTTP | Kod greške | Značenje |
| --- | --- | --- |
| 200 | — | Rezultati su u `data`; nema rezultata znači prazan niz. |
| 400 | `VALIDATION_ERROR` | Nedostaje parametar ili ne zadovoljava ograničenje duljine. |
| 401 | `UNAUTHENTICATED` | Nema valjane sesije. |
| 403 | `FORBIDDEN` | Prijavljeni korisnik nema ulogu organizatora. |
| 429 | `RATE_LIMITED` | Globalni limit Nominatima nije dostupan unutar dvije sekunde. |
| 502 | `GEOCODING_UNAVAILABLE` | Nominatim nije dostupan ili je vratio neispravan odgovor. |

U testovima se `artconnect.geocoding.base-url` usmjerava na lokalni lažni servis; testovi nikad ne pozivaju `nominatim.openstreetmap.org`.
