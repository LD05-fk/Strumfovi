// Svi tekstovi sučelja na hrvatskom. Komponente ne sadrže tekst izravno, nego ga uzimaju odavde: hr.common.save.
// Svaka značajka dodaje svoj ključ (auth, opportunities, applications…) kad joj zatrebaju tekstovi.
export const hr = {
  appName: 'ArtConnect',

  home: {
    title: 'Umjetnici i prilike na jednom mjestu',
    description:
      'ArtConnect povezuje umjetnike s organizatorima izložbi, natječaja i rezidencija.',
    browseOpportunities: 'Pregledaj prilike',
    googleSignIn: 'Prijava s Googleom',
  },

  navigation: {
    opportunities: 'Prilike',
    artistProfile: 'Moj profil',
    artistPortfolio: 'Moj portfolio',
    artistApplications: 'Moje prijave',
    organizerOpportunities: 'Moje prilike',
    statistics: 'Statistika',
    administration: 'Administracija',
    organizerProfile: 'Profil',
  },

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
    home: 'Na početnu',
  },

  auth: {
    loginRequiredTitle: 'Prijava potrebna',
    loginRequiredDescription:
      'Za pristup ovoj stranici prijavite se svojim Google računom.',
    onboardingTitle: 'Odabir uloge',
    roles: {
      ARTIST: 'Umjetnik',
      ORGANIZER: 'Organizator',
      ADMIN: 'Administrator',
    },
  },

  errors: {
    generic: 'Nešto je pošlo po zlu. Pokušajte ponovno.',
    notFound: 'Stranica ne postoji.',
    forbidden: 'Nemate pristup ovoj stranici.',
    pageNotFoundDescription: 'Adresa ne postoji ili je premještena.',
    errorTitle: 'Došlo je do pogreške',
  },

  opportunityStatus: {
    PREPARATION: 'U pripremi',
    OPEN: 'Otvorena',
    CLOSED: 'Zatvorena',
    SELECTION: 'Odabir u tijeku',
    FINISHED: 'Završena',
  },

  applicationStatus: {
    SUBMITTED: 'Poslana',
    UNDER_REVIEW: 'U razmatranju',
    NEEDS_INFO: 'Potrebne informacije',
    ACCEPTED: 'Prihvaćena',
    REJECTED: 'Odbijena',
    WITHDRAWN: 'Povučena',
  },

  artistProfile: {
    pageTitle: 'Uređivanje profila',
    biography: 'Biografija',
    biographyHint: 'Recite nešto o sebi.',
    artistStatement: 'Opis umjetničkog rada',
    artistStatementHint: 'Opišite svoj pristup i teme kojima se bavite.',
    artFields: 'Područja umjetnosti',
    upToTen: '(do 10)',
    artFieldsHint: 'Odaberite do 10 područja.',
    artFieldsEmpty: 'Nema dostupnih područja umjetnosti.',
    inactiveField: 'neaktivno',
    links: 'Poveznice',
    linksHint: 'Dodajte poveznice na svoje stranice i društvene mreže.',
    noLinks: 'Još nema poveznica.',
    linkLabel: 'Naziv poveznice',
    linkUrl: 'URL poveznice',
    characterCount: (count: number) => `${count} / 5000`,
    addLink: 'Dodaj poveznicu',
    removeLink: 'Ukloni poveznicu',
    moveLinkUp: 'Pomakni poveznicu gore',
    moveLinkDown: 'Pomakni poveznicu dolje',
    addLinkLimit: 'Dosegnut je najveći broj poveznica (10).',
    saveProfile: 'Spremi profil',
    saved: 'Profil spremljen',
    viewPublicProfile: 'Pogledaj javni profil',
    copyPublicLink: 'Kopiraj javnu poveznicu',
    copiedPublicLink: 'Javna poveznica kopirana.',
    copyFailed: 'Nije moguće kopirati poveznicu. Pokušajte ponovno.',
    emptyProfileHint:
      'Popunite biografiju i dodajte područja umjetnosti kako bi vas organizatori lakše pronašli.',
    leaveTitle: 'Napuštate stranicu?',
    leaveMessage: 'Imate nespremljene promjene koje će biti izgubljene.',
    continueEditing: 'Nastavi uređivati',
    discardChanges: 'Odbaci promjene',
    validation: {
      linkLabelMaxLength: 'Naziv poveznice smije imati najviše 60 znakova.',
      linkUrlRequired: 'URL poveznice je obavezan.',
      linkUrlMaxLength: 'URL poveznice smije imati najviše 1024 znakova.',
      linkUrlFormat:
        'Unesite valjan URL koji počinje s http:// ili https://.',
      biographyMaxLength: 'Biografija smije imati najviše 5000 znakova.',
      artistStatementMaxLength:
        'Opis umjetničkog rada smije imati najviše 5000 znakova.',
      artFieldsMaxCount: 'Možete odabrati najviše 10 područja umjetnosti.',
      artFieldsUnique: 'Područja umjetnosti ne smiju se ponavljati.',
      linksMaxCount: 'Možete unijeti najviše 10 poveznica.',
    },
  },

  map: {
    ariaLabel: 'Karta lokacije prilike',
    unavailable: 'Karta trenutno nije dostupna.',
    setCenterMarker: 'Postavi oznaku u središte karte',
    dragHint: 'Oznaku možete povući na točan položaj.',
    noMarkerHint: 'Oznaka nije postavljena.',
    details: 'Detalji',
    openInOsm: 'Otvori na OpenStreetMapu',
    draggableMarkerTitle: 'Pomična oznaka lokacije',
  },
} as const
