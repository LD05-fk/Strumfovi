import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { Button } from '@mui/material'
import { hr } from '../../i18n/hr'
import { useNotify } from '../../components/state/useNotify'

type ShareLinkButtonProps = {
  userId: string
}

export function ShareLinkButton({ userId }: ShareLinkButtonProps) {
  const notify = useNotify()

  const copyLink = async () => {
    try {
      if (!navigator.clipboard) {
        throw new Error(hr.artistProfile.copyFailed)
      }

      await navigator.clipboard.writeText(
        `${window.location.origin}/artists/${userId}`,
      )
      notify(hr.artistProfile.copiedPublicLink, 'success')
    } catch {
      notify(hr.artistProfile.copyFailed, 'error')
    }
  }

  return (
    <Button
      type="button"
      onClick={() => void copyLink()}
      startIcon={<ContentCopyIcon />}
      sx={{ minHeight: 36, px: 1.5 }}
    >
      {hr.artistProfile.copyPublicLink}
    </Button>
  )
}
