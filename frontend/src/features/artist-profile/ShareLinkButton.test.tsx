import { fireEvent, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { NotifyProvider } from '../../components/state/NotifyProvider'
import { renderWithProviders } from '../../test/renderWithProviders'
import { ShareLinkButton } from './ShareLinkButton'

const originalClipboard = Object.getOwnPropertyDescriptor(
  navigator,
  'clipboard',
)
const writeText = vi.fn<() => Promise<void>>()

beforeEach(() => {
  Object.defineProperty(navigator, 'clipboard', {
    configurable: true,
    value: { writeText },
  })
})

afterEach(() => {
  if (originalClipboard) {
    Object.defineProperty(navigator, 'clipboard', originalClipboard)
  } else {
    Reflect.deleteProperty(navigator, 'clipboard')
  }
  vi.clearAllMocks()
})

function renderButton() {
  return renderWithProviders(
    <NotifyProvider>
      <ShareLinkButton userId="artist-123" />
    </NotifyProvider>,
  )
}

describe('ShareLinkButton', () => {
  it('copies the public artist profile link', async () => {
    writeText.mockResolvedValue(undefined)
    renderButton()

    fireEvent.click(
      screen.getByRole('button', { name: 'Kopiraj javnu poveznicu' }),
    )

    expect(writeText).toHaveBeenCalledWith(
      `${window.location.origin}/artists/artist-123`,
    )
    expect(
      await screen.findByText('Javna poveznica kopirana.'),
    ).toBeInTheDocument()
  })

  it('notifies the user when clipboard access fails', async () => {
    writeText.mockRejectedValue(new Error('Clipboard unavailable'))
    renderButton()

    fireEvent.click(
      screen.getByRole('button', { name: 'Kopiraj javnu poveznicu' }),
    )

    expect(
      await screen.findByText(
        'Nije moguće kopirati poveznicu. Pokušajte ponovno.',
      ),
    ).toBeInTheDocument()
  })
})
