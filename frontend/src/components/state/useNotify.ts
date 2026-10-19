import { useContext } from 'react'
import { NotifyContext } from './NotifyProvider'

export function useNotify() {
  const notify = useContext(NotifyContext)
  if (!notify) {
    throw new Error('useNotify mora biti korišten unutar NotifyProvider.')
  }
  return notify
}
