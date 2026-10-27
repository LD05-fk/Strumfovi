import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { queryClient } from '../../../app/queryClient'
import { API_ERROR_EVENT, type ApiErrorEventDetail } from '../../../api/client'
import { isProtectedRoutePath } from './routeAccess'

export function AuthErrorHandler() {
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    function handleApiError(event: Event) {
      const { status, code, path } = (event as CustomEvent<ApiErrorEventDetail>)
        .detail

      if (status === 403 && code === 'ONBOARDING_REQUIRED') {
        navigate('/onboarding', { replace: true })
        return
      }

      if (
        status === 401 &&
        path !== '/me' &&
        isProtectedRoutePath(location.pathname)
      ) {
        void queryClient.invalidateQueries({ queryKey: ['me'] })
      }
    }

    window.addEventListener(API_ERROR_EVENT, handleApiError)
    return () => window.removeEventListener(API_ERROR_EVENT, handleApiError)
  }, [location.pathname, navigate])

  return null
}
