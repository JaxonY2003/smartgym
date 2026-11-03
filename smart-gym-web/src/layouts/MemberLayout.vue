<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 刷新后 Pinia 状态重置但 token 还在：拉取当前用户恢复 header 显示与管理员入口
onMounted(() => {
  if (auth.isLoggedIn && !auth.user) {
    auth.fetchCurrentUser().catch(() => {
      // /auth/me 失败（token 已失效等）由 request.ts 401 逻辑处理跳转
    })
  }
})

const menus = [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
  { path: '/agent', title: 'AI 助理', icon: 'ChatDotRound' },
  { path: '/workout-plan', title: '训练计划', icon: 'TrophyBase' },
  { path: '/workout-session', title: '训练记录', icon: 'List' },
  { path: '/nutrition', title: '饮食管理', icon: 'Food' },
  { path: '/exercises', title: '动作库', icon: 'Apple' },
  { path: '/profile', title: '健康档案', icon: 'User' },
]

async function handleLogout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">SmartGym Copilot</div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
      <div v-if="auth.isAdmin" class="admin-entry">
        <router-link to="/admin">管理后台</router-link>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="spacer" />
        <el-dropdown v-if="auth.user">
          <span class="user-chip">
            {{ auth.user.displayName }}
            <el-tag v-if="auth.isAdmin" size="small" type="warning">ADMIN</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout { height: 100vh; }
.aside {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--el-border-color-light);
}
.brand {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
}
.menu { flex: 1; border-right: none; }
.admin-entry {
  padding: 12px;
  text-align: center;
  font-size: 13px;
}
.header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-light);
}
.spacer { flex: 1; }
.user-chip { cursor: pointer; display: flex; gap: 6px; align-items: center; }
.main { background: var(--el-fill-color-lighter); }
</style>
