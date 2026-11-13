import MoreVertIcon from '@mui/icons-material/MoreVert'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Menu,
  MenuItem,
} from '@mui/material'
import { useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../../api/client'
import { useNotify } from '../../../components/state/useNotify'
import { hr } from '../../../i18n/hr'
import {
  useArchiveOpportunity,
  useChangeOpportunityStatus,
  useDeleteOpportunity,
} from '../opportunityQueries'
import type { OpportunityStatus } from '../types'
import { getOwnerActions, type OwnerAction } from './ownerActions'

type OwnerActionsBarProps = {
  opportunityId: string
  title: string
  status: OpportunityStatus
  archived: boolean
  deadline: string
}

export function OwnerActionsBar({
  opportunityId,
  title,
  status,
  archived,
  deadline,
}: OwnerActionsBarProps) {
  const navigate = useNavigate()
  const notify = useNotify()
  const queryClient = useQueryClient()
  const changeStatus = useChangeOpportunityStatus()
  const archive = useArchiveOpportunity()
  const deleteOpportunity = useDeleteOpportunity()

  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)
  const [confirmation, setConfirmation] = useState<OwnerAction | null>(null)
  const [hasApplications, setHasApplications] = useState(false)

  const actions = getOwnerActions(status, archived, deadline)
  const isPending =
    changeStatus.isPending || archive.isPending || deleteOpportunity.isPending

  function closeDialog() {
    if (isPending) return
    setConfirmation(null)
    setHasApplications(false)
  }

  function labelFor(action: OwnerAction): string {
    if (action.kind === 'archive')
      return hr.opportunities.detail.ownerActions.archive
    if (action.kind === 'delete')
      return hr.opportunities.detail.ownerActions.delete

    if (action.status === 'OPEN') {
      return status === 'PREPARATION'
        ? hr.opportunities.detail.ownerActions.publish
        : hr.opportunities.detail.ownerActions.reopenApplications
    }

    return action.status === 'CLOSED'
      ? hr.opportunities.detail.ownerActions.closeApplications
      : hr.opportunities.detail.ownerActions.startSelection
  }

  async function handleError(error: unknown, action: OwnerAction) {
    if (
      action.kind === 'delete' &&
      error instanceof ApiError &&
      error.status === 409 &&
      error.code === 'HAS_APPLICATIONS'
    ) {
      setHasApplications(true)
      return
    }

    if (
      error instanceof ApiError &&
      error.status === 409 &&
      error.code === 'INVALID_STATUS_TRANSITION'
    ) {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['my-opportunities'] }),
        queryClient.invalidateQueries({
          queryKey: ['opportunity', opportunityId],
        }),
      ])
    }

    const message =
      error instanceof ApiError &&
      error.status === 409 &&
      error.code === 'DEADLINE_PASSED'
        ? hr.opportunities.detail.deadlinePassed
        : error instanceof Error
          ? error.message
          : hr.errors.generic

    notify(message, 'error')
  }

  async function confirmAction() {
    if (!confirmation || isPending) return

    if (hasApplications) {
      try {
        await archive.mutateAsync(opportunityId)
        setConfirmation(null)
        setHasApplications(false)
        notify(hr.opportunities.detail.archivedSuccessfully, 'success')
      } catch (error) {
        await handleError(error, { kind: 'archive' })
      }
      return
    }

    try {
      if (confirmation.kind === 'status') {
        await changeStatus.mutateAsync({
          opportunityId,
          status: confirmation.status,
        })
        notify(hr.opportunities.detail.statusChanged, 'success')
      } else if (confirmation.kind === 'archive') {
        await archive.mutateAsync(opportunityId)
        notify(hr.opportunities.detail.archivedSuccessfully, 'success')
      } else {
        await deleteOpportunity.mutateAsync(opportunityId)
        notify(hr.opportunities.detail.deletedSuccessfully, 'success')
        setConfirmation(null)
        await navigate('/my-opportunities')
        return
      }

      setConfirmation(null)
    } catch (error) {
      await handleError(error, confirmation)
    }
  }

  const confirmationTitle = hasApplications
    ? hr.opportunities.detail.deleteHasApplicationsTitle
    : confirmation?.kind === 'status'
      ? hr.opportunities.detail.confirmStatusTitle(labelFor(confirmation))
      : confirmation?.kind === 'archive'
        ? hr.opportunities.detail.confirmArchiveTitle
        : hr.opportunities.detail.confirmDeleteTitle

  const confirmationDescription = hasApplications
    ? hr.opportunities.detail.deleteHasApplicationsDescription(title)
    : confirmation?.kind === 'status'
      ? hr.opportunities.detail.confirmStatusDescription(
          labelFor(confirmation),
          title,
        )
      : confirmation?.kind === 'archive'
        ? hr.opportunities.detail.confirmArchiveDescription(title)
        : hr.opportunities.detail.confirmDeleteDescription(title)

  const confirmLabel = hasApplications
    ? hr.opportunities.detail.ownerActions.archive
    : confirmation
      ? labelFor(confirmation)
      : hr.common.confirm

  return (
    <>
      <IconButton
        aria-label={hr.opportunities.detail.ownerActions.menu}
        aria-haspopup="menu"
        aria-expanded={Boolean(menuAnchor)}
        aria-controls={menuAnchor ? 'owner-actions-menu' : undefined}
        onClick={(event) => setMenuAnchor(event.currentTarget)}
      >
        <MoreVertIcon />
      </IconButton>

      <Menu
        id="owner-actions-menu"
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={() => setMenuAnchor(null)}
      >
        <MenuItem
          component={RouterLink}
          to={`/organizer/opportunities/${opportunityId}/edit`}
          onClick={() => setMenuAnchor(null)}
        >
          {hr.opportunities.detail.ownerActions.edit}
        </MenuItem>
        {actions.map((action) => (
          <MenuItem
            key={
              action.kind === 'status' ? `status-${action.status}` : action.kind
            }
            onClick={() => {
              setMenuAnchor(null)
              setConfirmation(action)
              setHasApplications(false)
            }}
          >
            {labelFor(action)}
          </MenuItem>
        ))}
      </Menu>

      <Dialog
        open={confirmation !== null}
        onClose={closeDialog}
        aria-labelledby="owner-action-confirm-title"
        aria-describedby="owner-action-confirm-description"
      >
        <DialogTitle id="owner-action-confirm-title">
          {confirmationTitle}
        </DialogTitle>
        <DialogContent>
          {hasApplications && (
            <Alert severity="info" sx={{ mb: 2 }}>
              {hr.opportunities.detail.deleteHasApplicationsHint}
            </Alert>
          )}
          <DialogContentText id="owner-action-confirm-description">
            {confirmationDescription}
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ flexWrap: 'wrap', p: 2 }}>
          <Button onClick={closeDialog} disabled={isPending}>
            {hr.common.cancel}
          </Button>
          <Button
            variant="contained"
            color={
              confirmation?.kind === 'delete' && !hasApplications
                ? 'error'
                : 'primary'
            }
            onClick={() => void confirmAction()}
            disabled={isPending}
          >
            {isPending ? hr.common.loading : confirmLabel}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
