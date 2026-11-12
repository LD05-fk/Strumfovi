import { describe, expect, it } from 'vitest'
import { dayjs } from '../../../app/dayjs'
import { getOwnerActions } from './ownerActions'

const now = dayjs('2026-11-12T12:00:00Z')

describe('getOwnerActions', () => {
  it.each([
    [
      'PREPARATION',
      [
        { kind: 'status', status: 'OPEN' },
        { kind: 'archive' },
        { kind: 'delete' },
      ],
    ],
    [
      'OPEN',
      [
        { kind: 'status', status: 'CLOSED' },
        { kind: 'archive' },
        { kind: 'delete' },
      ],
    ],
    ['SELECTION', [{ kind: 'archive' }, { kind: 'delete' }]],
    ['FINISHED', [{ kind: 'archive' }, { kind: 'delete' }]],
  ] as const)('vraća dopuštene radnje za status %s', (status, expected) => {
    expect(getOwnerActions(status, false, '2026-12-01T00:00:00Z', now)).toEqual(
      expected,
    )
  })

  it('za CLOSED dopušta ponovno otvaranje samo prije roka', () => {
    expect(
      getOwnerActions('CLOSED', false, '2026-11-13T12:00:00Z', now),
    ).toEqual([
      { kind: 'status', status: 'OPEN' },
      { kind: 'status', status: 'SELECTION' },
      { kind: 'archive' },
      { kind: 'delete' },
    ])

    expect(
      getOwnerActions('CLOSED', false, '2026-11-12T12:00:00Z', now),
    ).toEqual([
      { kind: 'status', status: 'SELECTION' },
      { kind: 'archive' },
      { kind: 'delete' },
    ])
  })

  it.each(['PREPARATION', 'OPEN', 'CLOSED', 'SELECTION', 'FINISHED'] as const)(
    'za arhiviranu priliku u statusu %s nudi samo brisanje',
    (status) => {
      expect(
        getOwnerActions(status, true, '2026-12-01T00:00:00Z', now),
      ).toEqual([{ kind: 'delete' }])
    },
  )
})
