import { Alert, Snackbar } from '@mui/material'
import {
  createContext,
  useCallback,
  useMemo,
  type ReactNode,
  useState,
} from 'react'

export type NotifySeverity = 'success' | 'error' | 'warning' | 'info'
type Notify = (message: string, severity?: NotifySeverity) => void

const NotifyContext = createContext<Notify | null>(null)

type Notice = {
  message: string
  severity: NotifySeverity
}

type NotifyProviderProps = {
  children: ReactNode
}

export function NotifyProvider({ children }: NotifyProviderProps) {
  const [notice, setNotice] = useState<Notice | null>(null)
  const notify = useCallback<Notify>((message, severity = 'success') => {
    setNotice({ message, severity })
  }, [])
  const contextValue = useMemo(() => notify, [notify])

  return (
    <NotifyContext.Provider value={contextValue}>
      {children}
      <Snackbar
        open={notice !== null}
        autoHideDuration={6000}
        onClose={(_, reason) => {
          if (reason !== 'clickaway') setNotice(null)
        }}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        {notice ? (
          <Alert
            severity={notice.severity}
            variant="filled"
            onClose={() => setNotice(null)}
            role="status"
            sx={{ width: '100%' }}
          >
            {notice.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </NotifyContext.Provider>
  )
}

export { NotifyContext }
export type { Notify }
