// Svi tekstovi sučelja na hrvatskom. Komponente ne sadrže tekst izravno, nego ga uzimaju odavde: hr.common.save.
// Svaka značajka dodaje svoj ključ (auth, opportunities, applications…) kad joj zatrebaju tekstovi.
export const hr = {
  appName: 'ArtConnect',

  common: {
    save: 'Spremi',
    cancel: 'Odustani',
    delete: 'Obriši',
    edit: 'Uredi',
    back: 'Natrag',
    confirm: 'Potvrdi',
    close: 'Zatvori',
    search: 'Pretraži',
    loading: 'Učitavanje…',
    retry: 'Pokušaj ponovno',
    empty: 'Nema podataka.',
  },

  errors: {
    generic: 'Nešto je pošlo po zlu. Pokušajte ponovno.',
    notFound: 'Stranica ne postoji.',
    forbidden: 'Nemate pristup ovoj stranici.',
  },

  map: {
    ariaLabel: 'Karta lokacije prilike',
    unavailable: 'Karta trenutno nije dostupna.',
    setCenterMarker:  'Postavi oznaku u središte karte',
    dragHint: 'Oznaku možete povući na točan položaj.',
    noMarkerHint: 'Oznaka nije postavljena.',
    details: 'Detalji',
    openInOsm: 'Otvori na OpenStreetMapu',
    draggableMarkerTitle: 'Pomična oznaka lokacije',
  },
} as const
