import { Avatar, Box, Chip, Link, Paper, Stack, Typography } from '@mui/material'
import type { ArtistPublicProfile } from '../artist-profile/types'
import { hr } from '../../i18n/hr'

type ArtistHeaderProps = {
  profile: ArtistPublicProfile
}

function initials(displayName: string) {
  return displayName
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0] ?? '')
    .join('')
    .toLocaleUpperCase()
}

function linkText(label: string | null, url: string) {
  if (label?.trim()) return label

  try {
    return new URL(url).hostname
  } catch {
    return url
  }
}

export function ArtistHeader({ profile }: ArtistHeaderProps) {
  return (
    <Paper
      component="section"
      aria-label={profile.displayName}
      sx={{ p: { xs: 2, sm: 3 } }}
    >
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2.5}
        sx={{ alignItems: { xs: 'center', sm: 'flex-start' } }}
      >
        <Avatar
          src={profile.avatarUrl ?? undefined}
          alt={profile.displayName}
          sx={{
            width: 88,
            height: 88,
            bgcolor: 'primary.main',
            color: 'primary.contrastText',
            fontSize: '1.75rem',
            flexShrink: 0,
          }}
        >
          {initials(profile.displayName)}
        </Avatar>
        <Box sx={{ minWidth: 0, flex: 1, width: '100%' }}>
          <Typography component="h1" variant="h2">
            {profile.displayName}
          </Typography>
          {profile.artFields.length > 0 && (
            <Stack
              direction="row"
              useFlexGap
              spacing={1}
              sx={{ my: 1, flexWrap: 'wrap' }}
            >
              {profile.artFields.map((field) => (
                <Chip
                  key={field.id}
                  label={field.name}
                  sx={{
                    bgcolor: 'primary.light',
                    color: 'primary.dark',
                  }}
                />
              ))}
            </Stack>
          )}
          {profile.biography && (
            <Typography sx={{ mt: 1 }}>{profile.biography}</Typography>
          )}
          {profile.artistStatement && (
            <Box sx={{ mt: 1.5 }}>
              <Typography component="h2" variant="h6">
                {hr.publicPortfolio.aboutWork}
              </Typography>
              <Typography color="text.secondary">
                {profile.artistStatement}
              </Typography>
            </Box>
          )}
          {profile.links.length > 0 && (
            <Stack
              direction="row"
              useFlexGap
              spacing={2}
              sx={{ mt: 1.5, flexWrap: 'wrap' }}
            >
              {profile.links.map((link, index) => (
                <Link
                  key={`${link.url}-${index}`}
                  href={link.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  sx={{ fontWeight: 600 }}
                >
                  {linkText(link.label, link.url)} ↗
                </Link>
              ))}
            </Stack>
          )}
        </Box>
      </Stack>
    </Paper>
  )
}
