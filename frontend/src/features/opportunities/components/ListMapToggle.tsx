import { ToggleButton, ToggleButtonGroup } from '@mui/material'
import { OPPORTUNITY_VIEWS } from '../types'
import type { OpportunityView } from '../types'
import { hr } from '../../../i18n/hr'

const views = hr.opportunities.search.view

type ListMapToggleProps = {
  view: OpportunityView
  onChange: (view: OpportunityView) => void
}

export function ListMapToggle({ view, onChange }: ListMapToggleProps) {
  return (
    <ToggleButtonGroup
      exclusive
      size="small"
      color="primary"
      value={view}
      aria-label={views.label}
      // null dolazi kad korisnik klikne već odabrani prikaz, prikaz je uvijek jedan od dva
      onChange={(_event, value: OpportunityView | null) => {
        if (value !== null) onChange(value)
      }}
    >
      {OPPORTUNITY_VIEWS.map((value) => (
        <ToggleButton key={value} value={value}>
          {views.options[value]}
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
  )
}
