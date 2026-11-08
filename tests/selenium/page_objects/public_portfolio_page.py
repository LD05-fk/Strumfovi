from selenium.webdriver.common.by import By

from page_objects.base_page import BasePage


class PublicPortfolioPage(BasePage):
    """Javni popis: prije provjere odsutnosti djela čekamo učitanu galeriju."""

    GALLERY = (By.CSS_SELECTOR, 'section[aria-labelledby="artworks-heading"]')

    def open_artist(self, artist_id):
        self.open_path(f"/artists/{artist_id}")
        # Galerija se prikazuje tek nakon dohvata podataka; skeleton nije dovoljan.
        self.wait_visible(self.GALLERY)
        return self

    def wait_for_artwork(self, title):
        return self.wait_visible((By.XPATH,
            "//section[@aria-labelledby='artworks-heading']"
            f"//h3[normalize-space()='{title}']"))

    def artwork_titles(self):
        gallery = self.wait_visible(self.GALLERY)
        return [heading.text for heading in gallery.find_elements(By.TAG_NAME, "h3")]
