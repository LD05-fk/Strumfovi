import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { hr } from '../../i18n/hr'

type ConfirmDialogProps = {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  isPending?: boolean
  onCancel: () => void
  onConfirm: () => void
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  isPending = false,
  onCancel,
  onConfirm,
}: ConfirmDialogProps) {
  const theme = useTheme()
  const fullScreen = useMediaQuery(theme.breakpoints.down('sm'))

  return (
    <Dialog
      open={open}
      onClose={isPending ? undefined : onCancel}
      fullScreen={fullScreen}
      aria-labelledby="portfolio-confirm-title"
      aria-describedby="portfolio-confirm-description"
      sx={{
        '& .MuiDialog-container': {
          alignItems: { xs: 'stretch', sm: 'center' },
        },
        '& .MuiDialog-paper': {
          width: { sm: 480 },
          maxWidth: '100%',
        },
      }}
    >
      <DialogTitle id="portfolio-confirm-title">{title}</DialogTitle>
      <DialogContent>
        <DialogContentText id="portfolio-confirm-description">
          {description}
        </DialogContentText>
      </DialogContent>
      <DialogActions sx={{ p: 2, mt: { xs: 'auto', sm: 0 } }}>
        <Button onClick={onCancel} disabled={isPending}>
          {hr.common.cancel}
        </Button>
        <Button
          color="error"
          variant="contained"
          onClick={onConfirm}
          disabled={isPending}
        >
          {isPending ? hr.common.loading : confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
