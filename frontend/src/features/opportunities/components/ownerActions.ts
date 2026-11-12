import { dayjs } from '../../../app/dayjs'
import type { OpportunityStatus } from '../types'

export type OwnerAction =
  | { kind: 'status'; status: 'OPEN' | 'CLOSED' | 'SELECTION' }
  | { kind: 'archive' }
  | { kind: 'delete' }

export function getOwnerActions(
  status: OpportunityStatus,
  archived: boolean,
  deadline: string,
  now = dayjs(),
): OwnerAction[] {
  const actions: OwnerAction[] = []

  if (!archived) {
    if (status === 'PREPARATION') {
      actions.push({ kind: 'status', status: 'OPEN' })
    } else if (status === 'OPEN') {
      actions.push({ kind: 'status', status: 'CLOSED' })
    } else if (status === 'CLOSED') {
      if (dayjs(deadline).isAfter(now)) {
        actions.push({ kind: 'status', status: 'OPEN' })
      }
      actions.push({ kind: 'status', status: 'SELECTION' })
    }

    actions.push({ kind: 'archive' })
  }

  actions.push({ kind: 'delete' })
  return actions
}
