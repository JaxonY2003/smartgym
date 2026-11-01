<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menus = [
  { path: '/admin', title: '用户管理' },
  { path: '/admin/agent-logs', title: 'Agent 日志' },
  { path: '/admin/prompts', title: 'Prompt 版本' },
]

function handleLogout() {
  auth.clearLocal()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="brand">管理后台</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="back">
        <router-link to="/dashboard">返回会员端</router-link>
        <a href="javascript:void(0)" @click="handleLogout">退出登录</a>
      </div>
    </el-aside>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.layout { height: 100vh; }
.aside {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--el-border-color-light);
}
.brand { height: 60px; display: flex; align-items: center; justify-content: center; font-weight: 600; }
.back {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}
.main { background: var(--el-fill-color-lighter); }
</style>
