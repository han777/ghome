<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../utils/api'

const router = useRouter()

onMounted(async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    router.replace('/login')
    return
  }

  try {
    const user: any = await api.get('/sys/profile')
    const roles = (user.roles || []).map((r: any) => r.roleCode)
    const isAdmin = roles.includes('ROLE_ADMIN')
    const isUser = roles.includes('ROLE_USER')

    if (isAdmin && isUser) {
      router.replace('/role-selection')
    } else if (isAdmin) {
      router.replace('/admin')
    } else if (isUser) {
      router.replace('/m')
    } else {
      router.replace('/admin')
    }
  } catch {
    router.replace('/login')
  }
})
</script>

<template>
  <div></div>
</template>