import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { ApiError, apiFetch } from '../../api/client'
import type { CurrentUser } from './types'
import type { ChooseRoleFormValues } from './pages/chooseRoleSchema'
import { useNotify } from '../../components/state/useNotify'
import { hr } from '../../i18n/hr'

export function useChooseRole() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const notify = useNotify()

  const mutation = useMutation({
    mutationFn: (values: ChooseRoleFormValues) =>
      apiFetch<CurrentUser>('/me/role', { method: 'POST', json: values }),
    onSuccess: (user) => {
      queryClient.setQueryData(['me'], user)
      navigate('/profile')
    },
    onError: (error) => {
      if (
        error instanceof ApiError &&
        error.status === 409 &&
        error.code === 'ROLE_ALREADY_CHOSEN'
      ) {
        void queryClient.invalidateQueries({ queryKey: ['me'] })
        notify(hr.auth.roleAlreadyChosen, 'error')
        navigate('/')
        return
      }
    },
  })

  return mutation
}
