import { screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { EmptyState } from '../components/state/EmptyState'
import { renderWithProviders } from './renderWithProviders'

it('prikazuje EmptyState koristeći renderWithProviders', () => {
  renderWithProviders(<EmptyState title="Nema pronađenih stavki." />)

  expect(screen.getByText('Nema pronađenih stavki.')).toBeTruthy()
})
