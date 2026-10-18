import { useEffect } from 'react'
import { hr } from '../../i18n/hr'

export function usePageTitle(title: string) {
  useEffect(() => {
    document.title = `${title} | ${hr.appName}`
  }, [title])
}
