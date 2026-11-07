import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import {
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  CardMedia,
  Pagination,
  Stack,
  Typography,
} from '@mui/material'
import type { Artwork } from './types'
import { ArtworkLightbox } from './ArtworkLightbox'
import { hr } from '../../i18n/hr'

type ArtworkGalleryProps = {
  artworks: Artwork[]
  page: number
  dataPage: number
  total: number
  pageSize: number
  onPageChange: (page: number) => void
}

export function ArtworkGallery({
  artworks,
  page,
  dataPage,
  total,
  pageSize,
  onPageChange,
}: ArtworkGalleryProps) {
  const [selectedArtwork, setSelectedArtwork] = useState<Artwork | null>(null)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const lastFocusedPage = useRef(page)
  const totalPages = Math.ceil(total / pageSize)
  const outOfRange = page > Math.max(totalPages, 1)

  useEffect(() => {
    if (dataPage !== page || lastFocusedPage.current === page) return

    headingRef.current?.focus()
    lastFocusedPage.current = page
  }, [dataPage, page])

  const handlePageChange = (_event: ChangeEvent<unknown>, value: number) =>
    onPageChange(value)

  return (
    <Box component="section" aria-labelledby="artworks-heading">
      <Typography
        id="artworks-heading"
        component="h2"
        variant="h3"
        tabIndex={-1}
        ref={headingRef}
        sx={{ mb: 2 }}
      >
        {hr.publicPortfolio.artworks}
      </Typography>
      {artworks.length === 0 ? (
        <Box
          role="status"
          sx={{
            p: { xs: 3, sm: 6 },
            textAlign: 'center',
            border: 1,
            borderStyle: 'dashed',
            borderColor: 'divider',
            borderRadius: 1.5,
            bgcolor: 'background.paper',
          }}
        >
          <Typography component="p" variant="h6">
            {outOfRange
              ? hr.publicPortfolio.noArtworksOnPage
              : hr.publicPortfolio.noPublicArtworks}
          </Typography>
          {outOfRange && (
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={1}
              sx={{ justifyContent: 'center', mt: 2 }}
            >
              <Button variant="outlined" onClick={() => onPageChange(1)}>
                {hr.publicPortfolio.firstPage}
              </Button>
              {totalPages > 1 && (
                <Button
                  variant="contained"
                  onClick={() => onPageChange(totalPages)}
                >
                  {hr.publicPortfolio.lastAvailablePage(totalPages)}
                </Button>
              )}
            </Stack>
          )}
        </Box>
      ) : (
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: {
              xs: 'repeat(2, minmax(0, 1fr))',
              sm: 'repeat(3, minmax(0, 1fr))',
              md: 'repeat(4, minmax(0, 1fr))',
            },
            gap: 2,
          }}
        >
          {artworks.map((artwork) => {
            const image = artwork.images[0]
            return (
              <Card key={artwork.id}>
                <CardActionArea
                  onClick={() => setSelectedArtwork(artwork)}
                  aria-label={`${artwork.title}, ${artwork.year}`}
                >
                  {image ? (
                    <CardMedia
                      component="img"
                      image={image.file.url}
                      alt={image.altText || artwork.title}
                      sx={{ aspectRatio: '4 / 3', objectFit: 'cover' }}
                    />
                  ) : (
                    <Box
                      role="img"
                      aria-label={artwork.title}
                      sx={{
                        aspectRatio: '4 / 3',
                        bgcolor: 'primary.light',
                      }}
                    />
                  )}
                  <CardContent>
                    <Typography component="h3" variant="h6" noWrap>
                      {artwork.title}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {artwork.technique} · {artwork.year}.
                    </Typography>
                  </CardContent>
                </CardActionArea>
              </Card>
            )
          })}
        </Box>
      )}
      {!outOfRange && totalPages > 1 && (
        <Stack sx={{ alignItems: 'center', mt: 3 }}>
          <Pagination
            count={totalPages}
            page={page}
            onChange={handlePageChange}
            color="primary"
            aria-label={hr.publicPortfolio.artworks}
          />
        </Stack>
      )}
      <ArtworkLightbox
        key={selectedArtwork?.id ?? 'closed'}
        artwork={selectedArtwork}
        onClose={() => setSelectedArtwork(null)}
      />
    </Box>
  )
}
