import CloseIcon from '@mui/icons-material/Close'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import {
  Box,
  Dialog,
  DialogTitle,
  IconButton,
  Stack,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import type { Artwork } from './types'
import { hr } from '../../i18n/hr'

type ArtworkLightboxProps = {
  artwork: Artwork | null
  onClose: () => void
}

export function ArtworkLightbox({
  artwork,
  onClose,
}: ArtworkLightboxProps) {
  const [imageIndex, setImageIndex] = useState(0)
  const isOpen = artwork !== null

  useEffect(() => {
    if (!artwork) return

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
      } else if (event.key === 'ArrowLeft' && artwork.images.length > 1) {
        event.preventDefault()
        setImageIndex((index) =>
          index === 0 ? artwork.images.length - 1 : index - 1,
        )
      } else if (event.key === 'ArrowRight' && artwork.images.length > 1) {
        event.preventDefault()
        setImageIndex((index) => (index + 1) % artwork.images.length)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [artwork, onClose])

  const image = artwork?.images[imageIndex]

  return (
    <Dialog
      open={isOpen}
      onClose={onClose}
      aria-labelledby="artwork-lightbox-title"
      maxWidth="md"
      fullWidth
    >
      {artwork && (
        <>
          <Stack
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center', pr: 1 }}
          >
            <DialogTitle
              id="artwork-lightbox-title"
              component="h2"
              sx={{ flex: 1 }}
            >
              {artwork.title}
            </DialogTitle>
            <IconButton
              aria-label={hr.publicPortfolio.closeLightbox}
              onClick={onClose}
            >
              <CloseIcon />
            </IconButton>
          </Stack>
          <Box sx={{ px: { xs: 2, sm: 3 }, pb: 2 }}>
            <Stack
              direction="row"
              spacing={1}
              sx={{
                alignItems: 'center',
                justifyContent: 'center',
                width: '100%',
                minWidth: 0,
              }}
            >
              {artwork.images.length > 1 && (
                <IconButton
                  aria-label={hr.publicPortfolio.previousImage}
                  onClick={() =>
                    setImageIndex((index) =>
                      index === 0 ? artwork.images.length - 1 : index - 1,
                    )
                  }
                  sx={{ flexShrink: 0 }}
                >
                  <ChevronLeftIcon />
                </IconButton>
              )}
              <Box
                sx={{
                  flex: 1,
                  minWidth: 0,
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                }}
              >
                {image ? (
                  <Box
                    component="img"
                    src={image.file.url}
                    alt={
                      image.altText ||
                      hr.publicPortfolio.artworkImage(
                        artwork.title,
                        imageIndex + 1,
                        artwork.images.length,
                      )
                    }
                    sx={{
                      display: 'block',
                      width: '100%',
                      maxWidth: '100%',
                      maxHeight: { xs: '55vh', sm: '65vh' },
                      objectFit: 'contain',
                      borderRadius: 1,
                    }}
                  />
                ) : (
                  <Typography color="text.secondary" sx={{ py: 8 }}>
                    {hr.publicPortfolio.noArtworkImage}
                  </Typography>
                )}
              </Box>
              {artwork.images.length > 1 && (
                <IconButton
                  aria-label={hr.publicPortfolio.nextImage}
                  onClick={() =>
                    setImageIndex((index) => (index + 1) % artwork.images.length)
                  }
                  sx={{ flexShrink: 0 }}
                >
                  <ChevronRightIcon />
                </IconButton>
              )}
            </Stack>
            <Typography color="text.secondary" sx={{ mt: 2 }}>
              {[artwork.technique, `${artwork.year}.`, artwork.dimensions]
                .filter(Boolean)
                .join(' · ')}
              {artwork.description && (
                <>
                  <br />
                  {artwork.description}
                </>
              )}
            </Typography>
          </Box>
        </>
      )}
    </Dialog>
  )
}
