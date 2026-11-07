import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined'
import {
  Box,
  Card,
  CardActions,
  CardContent,
  CardMedia,
  IconButton,
  Stack,
  Typography,
} from '@mui/material'
import { Link as RouterLink } from 'react-router'
import { hr } from '../../i18n/hr'
import type { Artwork } from './types'
import { HiddenByAdminBadge, VisibilityChip } from './VisibilityChip'

type ArtworkCardProps = {
  artwork: Artwork
  deletePending: boolean
  onDelete: (artwork: Artwork) => void
}

export function ArtworkCard({
  artwork,
  deletePending,
  onDelete,
}: ArtworkCardProps) {
  const image = artwork.images[0]

  return (
    <Card
      component="article"
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minWidth: 0,
        '&:hover': { boxShadow: 3 },
      }}
    >
      {image ? (
        <Box sx={{ px: 2, pt: 2 }}>
          <CardMedia
            component="img"
            image={image.file.url}
            alt={image.altText?.trim() || artwork.title}
            loading="lazy"
            sx={{
              aspectRatio: '4 / 3',
              objectFit: 'cover',
              borderRadius: 1,
            }}
          />
        </Box>
      ) : (
        <Box
          role="img"
          aria-label={`${artwork.title} — ${hr.portfolio.noImage}`}
          sx={{
            aspectRatio: '4 / 3',
            mx: 2,
            mt: 2,
            display: 'grid',
            placeItems: 'center',
            borderRadius: 1,
            bgcolor: 'primary.light',
            color: 'primary.main',
          }}
        >
          <ImageOutlinedIcon aria-hidden="true" sx={{ fontSize: 48 }} />
        </Box>
      )}
      <CardContent sx={{ flex: 1, minWidth: 0, pt: image ? 0 : 2 }}>
        <Typography
          component="h2"
          variant="h6"
          sx={{ overflowWrap: 'anywhere' }}
        >
          {artwork.title}
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 0.5 }}>
          {artwork.technique}, {artwork.year}.
        </Typography>
        <Stack
          direction="row"
          spacing={1}
          useFlexGap
          sx={{ mt: 1, flexWrap: 'wrap' }}
        >
          <VisibilityChip visibility={artwork.visibility} />
          {artwork.hiddenByAdmin && <HiddenByAdminBadge />}
        </Stack>
      </CardContent>
      <CardActions sx={{ px: 2, pb: 2, pt: 0, justifyContent: 'flex-end' }}>
        <IconButton
          component={RouterLink}
          to={`/portfolio/artworks/${encodeURIComponent(artwork.id)}`}
          aria-label={`${hr.common.edit}: ${artwork.title}`}
          sx={{ minWidth: 44, minHeight: 44 }}
        >
          <EditOutlinedIcon />
        </IconButton>
        <IconButton
          color="error"
          onClick={() => onDelete(artwork)}
          disabled={deletePending}
          aria-label={`${hr.common.delete}: ${artwork.title}`}
          sx={{ minWidth: 44, minHeight: 44 }}
        >
          <DeleteOutlineIcon />
        </IconButton>
      </CardActions>
    </Card>
  )
}
