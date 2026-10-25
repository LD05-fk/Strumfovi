import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../api/client'
import { useNavigate } from 'react-router'
import { useNotify } from '../../components/state/useNotify'
import { hr } from '../../i18n/hr'

export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const notify = useNotify()

  const mutation = useMutation({
    mutationFn: () => apiFetch('/auth/logout', { method: 'POST' }),
    onSuccess: () => {
      queryClient.clear()
      navigate('/')
    },
    onError: () => {
      notify(hr.errors.generic, 'error')
    },
  })

  return mutation
}
