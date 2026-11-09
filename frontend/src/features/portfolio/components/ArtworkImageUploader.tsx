import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined'
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useRef, useState } from 'react'
import type { ChangeEvent, DragEvent } from 'react'
import { ApiError } from '../../../api/client'
import { ConfirmDialog } from '../ConfirmDialog'
import { hr } from '../../../i18n/hr'
import { usePublicSettings } from '../../settings/publicSettingsQueries'
import {
  useDeleteArtworkImage,
  useUploadArtworkImage,
} from '../portfolioQueries'
import type { Artwork } from '../types'

type ArtworkImageUploaderProps = {
  artwork?: Artwork
  artistId: string
  title: string
}

const ACCEPTED_TYPES = new Set(['image/jpeg', 'image/png'])
const MAX_ALT_TEXT_LENGTH = 300

function formatMegabytes(bytes: number) {
  const megabytes = bytes / (1024 * 1024)
  return `${Number.isInteger(megabytes) ? megabytes : megabytes.toFixed(1)} MB`
}

function uploadErrorMessage(error: unknown, maxBytes?: number) {
  if (!(error instanceof ApiError)) {
    return error instanceof Error ? error.message : hr.errors.generic
  }

  if (error.code === 'UNSUPPORTED_FILE_TYPE' || error.status === 415) {
    return hr.portfolio.images.unsupportedType
  }
  if (error.code === 'FILE_TOO_LARGE' || error.status === 413) {
    return maxBytes === undefined
      ? error.message
      : hr.portfolio.images.fileTooLarge(formatMegabytes(maxBytes))
  }
  if (error.code === 'IMAGE_LIMIT_REACHED' || error.status === 409) {
    return hr.portfolio.images.limitReached
  }
  if (error.code === 'NOT_FOUND' || error.status === 404) {
    return hr.portfolio.images.notFound
  }

  return error.message
}

export function ArtworkImageUploader({
  artwork,
  artistId,
  title,
}: ArtworkImageUploaderProps) {
  const artworkId = artwork?.id ?? ''
  const settings = usePublicSettings()
  const uploadImage = useUploadArtworkImage(artistId, artworkId)
  const deleteImage = useDeleteArtworkImage(artistId, artworkId)
  const [altText, setAltText] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)
  const [uploadingFile, setUploadingFile] = useState('')
  const [isUploading, setIsUploading] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const abortControllerRef = useRef<AbortController | null>(null)
  const images = artwork?.images ?? []
  const maxImages = settings.data?.artworkMaxImages
  const maxBytes = settings.data?.artworkMaxImageBytes
  const atLimit = maxImages !== undefined && images.length >= maxImages
  const busy = isUploading || uploadImage.isPending || deleteImage.isPending

  const showError = (error: unknown) => {
    setErrorMessage(uploadErrorMessage(error, maxBytes))
  }

  const uploadFiles = async (files: FileList | File[]) => {
    if (!artwork || !settings.data || busy) return

    setErrorMessage('')
    const controller = new AbortController()
    abortControllerRef.current = controller
    setIsUploading(true)
    let imageCount = images.length

    try {
      for (const file of Array.from(files)) {
        if (!ACCEPTED_TYPES.has(file.type)) {
          setErrorMessage(hr.portfolio.images.unsupportedType)
          break
        }
        if (file.size > settings.data.artworkMaxImageBytes) {
          setErrorMessage(
            hr.portfolio.images.fileTooLarge(
              formatMegabytes(settings.data.artworkMaxImageBytes),
            ),
          )
          break
        }
        if (imageCount >= settings.data.artworkMaxImages) {
          setErrorMessage(hr.portfolio.images.limitReached)
          break
        }

        setUploadingFile(file.name)
        const formData = new FormData()
        formData.append('file', file)
        formData.append(
          'altText',
          (altText.trim() || title).slice(0, MAX_ALT_TEXT_LENGTH),
        )
        try {
          await uploadImage.mutateAsync({
            body: formData,
            signal: controller.signal,
          })
          imageCount += 1
        } catch (error) {
          if (!controller.signal.aborted) showError(error)
          break
        }
      }
    } finally {
      abortControllerRef.current = null
      setUploadingFile('')
      setIsUploading(false)
    }
  }

  const handleFileInput = (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.currentTarget.files ?? [])
    if (files?.length) void uploadFiles(files)
    event.currentTarget.value = ''
  }

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    if (event.dataTransfer.files.length)
      void uploadFiles(event.dataTransfer.files)
  }

  const uploadDisabled =
    !artwork || !settings.data || settings.isError || atLimit || busy

  return (
    <>
      <Card
        component="section"
        aria-labelledby="artwork-images-title"
        sx={{ p: { xs: 2, sm: 3 } }}
      >
        <Stack spacing={2}>
          <Stack
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center', justifyContent: 'space-between' }}
          >
            <Typography id="artwork-images-title" component="h2" variant="h4">
              {hr.portfolio.images.title}
            </Typography>
            {maxImages !== undefined && (
              <Chip
                label={`${images.length}/${maxImages} ${hr.portfolio.images.countLabel(maxImages)}`}
                color={atLimit ? 'warning' : 'primary'}
                size="small"
              />
            )}
          </Stack>

          {!artwork ? (
            <>
              <Alert severity="info">{hr.portfolio.images.saveFirst}</Alert>
              <Box
                sx={{
                  border: 1,
                  borderColor: 'divider',
                  borderStyle: 'dashed',
                  borderRadius: 2,
                  p: { xs: 2, sm: 3 },
                  textAlign: 'center',
                }}
              >
                <ImageOutlinedIcon
                  aria-hidden="true"
                  sx={{ fontSize: 40, color: 'text.secondary' }}
                />
                <Typography color="text.secondary" sx={{ mb: 1 }}>
                  {hr.portfolio.images.dropHint}
                </Typography>
                <Button type="button" variant="outlined" disabled>
                  {hr.portfolio.images.choose}
                </Button>
              </Box>
              {settings.isPending && (
                <Typography color="text.secondary" role="status">
                  {hr.common.loading}
                </Typography>
              )}
              {settings.isError && (
                <Alert
                  severity="error"
                  action={
                    <Button
                      type="button"
                      color="inherit"
                      size="small"
                      onClick={() => void settings.refetch()}
                    >
                      {hr.common.retry}
                    </Button>
                  }
                >
                  {hr.portfolio.images.settingsError}
                </Alert>
              )}
              {settings.data && (
                <Typography variant="body2" color="text.secondary">
                  {hr.portfolio.images.limits(
                    formatMegabytes(settings.data.artworkMaxImageBytes),
                    settings.data.artworkMaxImages,
                  )}
                </Typography>
              )}
            </>
          ) : (
            <>
              {settings.isPending && (
                <Typography color="text.secondary" role="status">
                  {hr.common.loading}
                </Typography>
              )}
              {settings.isError && (
                <Alert
                  severity="error"
                  action={
                    <Button
                      type="button"
                      color="inherit"
                      size="small"
                      onClick={() => void settings.refetch()}
                    >
                      {hr.common.retry}
                    </Button>
                  }
                >
                  {hr.portfolio.images.settingsError}
                </Alert>
              )}
              {errorMessage && (
                <Alert severity="error" role="alert">
                  {errorMessage}
                </Alert>
              )}

              {images.length === 0 && !uploadingFile && (
                <Typography color="text.secondary" role="status">
                  {hr.portfolio.images.empty}
                </Typography>
              )}

              <Stack spacing={2}>
                {images.map((image) => (
                  <Stack
                    key={image.id}
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1.5}
                    sx={{ alignItems: { xs: 'stretch', sm: 'flex-start' } }}
                  >
                    <Box
                      component="img"
                      src={image.file.url}
                      alt={image.altText?.trim() || title}
                      sx={{
                        width: { xs: '100%', sm: 96 },
                        height: { xs: 180, sm: 72 },
                        flexShrink: 0,
                        objectFit: 'cover',
                        borderRadius: 1,
                      }}
                    />
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography variant="body2" color="text.secondary">
                        {hr.portfolio.images.altTextLabel}
                      </Typography>
                      <Typography sx={{ overflowWrap: 'anywhere' }}>
                        {image.altText?.trim() || title}
                      </Typography>
                      <Button
                        type="button"
                        color="error"
                        disabled={busy}
                        onClick={() => setDeleteTarget(image.id)}
                        sx={{ mt: 0.5, minHeight: 44 }}
                      >
                        {hr.portfolio.images.remove}
                      </Button>
                    </Box>
                  </Stack>
                ))}
              </Stack>

              {uploadingFile && (
                <Stack
                  direction="row"
                  spacing={1.5}
                  role="status"
                  aria-live="polite"
                  sx={{ alignItems: 'center' }}
                >
                  <CircularProgress
                    size={24}
                    aria-label={hr.portfolio.images.uploading}
                  />
                  <Typography sx={{ flex: 1, overflowWrap: 'anywhere' }}>
                    {hr.portfolio.images.uploadingFile(uploadingFile)}
                  </Typography>
                  <Button
                    type="button"
                    onClick={() => abortControllerRef.current?.abort()}
                    color="inherit"
                  >
                    {hr.common.cancel}
                  </Button>
                </Stack>
              )}

              {settings.data && (
                <>
                  {!atLimit && (
                    <TextField
                      label={hr.portfolio.images.altTextLabel}
                      placeholder={
                        title || hr.portfolio.images.altTextPlaceholder
                      }
                      value={altText}
                      onChange={(event) =>
                        setAltText(
                          event.target.value.slice(0, MAX_ALT_TEXT_LENGTH),
                        )
                      }
                      slotProps={{
                        htmlInput: { maxLength: MAX_ALT_TEXT_LENGTH },
                      }}
                      helperText={`${altText.length} / ${MAX_ALT_TEXT_LENGTH}`}
                      fullWidth
                      disabled={busy}
                    />
                  )}

                  {!atLimit ? (
                    <Box
                      onDragOver={(event) => event.preventDefault()}
                      onDrop={handleDrop}
                      sx={{
                        border: 1,
                        borderColor: 'divider',
                        borderStyle: 'dashed',
                        borderRadius: 2,
                        p: { xs: 2, sm: 3 },
                        textAlign: 'center',
                      }}
                    >
                      <ImageOutlinedIcon
                        aria-hidden="true"
                        sx={{ fontSize: 40, color: 'text.secondary' }}
                      />
                      <Typography color="text.secondary" sx={{ mb: 1 }}>
                        {hr.portfolio.images.dropHint}
                      </Typography>
                      <Button
                        type="button"
                        variant="outlined"
                        disabled={uploadDisabled}
                        onClick={() => inputRef.current?.click()}
                      >
                        {hr.portfolio.images.choose}
                      </Button>
                      <input
                        ref={inputRef}
                        type="file"
                        accept="image/jpeg,image/png"
                        multiple
                        hidden
                        aria-label={hr.portfolio.images.choose}
                        onChange={handleFileInput}
                      />
                    </Box>
                  ) : (
                    <Typography color="text.secondary" role="status">
                      {hr.portfolio.images.limitReachedHint}
                    </Typography>
                  )}

                  <Typography variant="body2" color="text.secondary">
                    {hr.portfolio.images.limits(
                      formatMegabytes(settings.data.artworkMaxImageBytes),
                      settings.data.artworkMaxImages,
                    )}
                  </Typography>
                </>
              )}
            </>
          )}
        </Stack>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title={hr.portfolio.images.deleteTitle}
        description={hr.portfolio.images.deleteDescription}
        confirmLabel={hr.portfolio.images.remove}
        isPending={deleteImage.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (!deleteTarget) return
          deleteImage.mutate(deleteTarget, {
            onSuccess: () => {
              setDeleteTarget(null)
              setErrorMessage('')
            },
            onError: showError,
          })
        }}
      />
    </>
  )
}
