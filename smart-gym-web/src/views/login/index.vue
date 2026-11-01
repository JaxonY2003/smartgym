<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Apple, Lightning, Lock, Medal, Message, TrendCharts, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { errorMessageOf } from '@/constants/errorMessages'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  account: '',
  password: '',
  displayName: '',
})

const rules: FormRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { type: 'email', message: '首版请使用邮箱注册', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码为 8～64 位', trigger: 'blur' },
  ],
  displayName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 30, message: '昵称为 2～30 个字符', trigger: 'blur' },
  ],
}

const registerRules: FormRules = {
  ...rules,
  // 注册时才校验昵称，登录时隐藏该字段
}

const features = [
  { icon: Lightning, title: '智能训练计划', desc: '根据目标与身体限制自动编排' },
  { icon: Apple, title: '个性化饮食建议', desc: '结合过敏与偏好精准推荐' },
  { icon: TrendCharts, title: '数据趋势看板', desc: '训练、体重与营养一目了然' },
  { icon: Medal, title: 'Health Coach Agent', desc: '7×24 小时的 AI 健康教练' },
]

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login({ account: form.account, password: form.password })
      ElMessage.success('登录成功')
    } else {
      await auth.register({
        account: form.account,
        password: form.password,
        displayName: form.displayName,
      })
      // 注册成功后直接登录
      await auth.login({ account: form.account, password: form.password })
      ElMessage.success('注册成功')
    }
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e) {
    const code = (e as { code?: string }).code
    ElMessage.error(errorMessageOf(code))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <!-- 左侧品牌区 -->
    <aside class="brand-panel">
      <div class="glow glow-a" aria-hidden="true"></div>
      <div class="glow glow-b" aria-hidden="true"></div>
      <div class="glow glow-c" aria-hidden="true"></div>
      <div class="brand-grid" aria-hidden="true"></div>

      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
            <defs>
              <linearGradient id="sgDumbbell" x1="0" y1="0" x2="48" y2="48">
                <stop offset="0" stop-color="#5eead4" />
                <stop offset="1" stop-color="#34d399" />
              </linearGradient>
            </defs>
            <rect x="4" y="19" width="7" height="10" rx="2" fill="url(#sgDumbbell)" />
            <rect x="37" y="19" width="7" height="10" rx="2" fill="url(#sgDumbbell)" />
            <rect x="12" y="14" width="6" height="20" rx="2" fill="url(#sgDumbbell)" />
            <rect x="30" y="14" width="6" height="20" rx="2" fill="url(#sgDumbbell)" />
            <rect x="18" y="21" width="12" height="6" rx="3" fill="url(#sgDumbbell)" />
          </svg>
          <span>SmartGym Copilot</span>
        </div>

        <h1 class="brand-headline">让每一次训练<br />都有 AI 教练陪伴</h1>
        <p class="brand-sub">
          读取你的真实健康数据，生成可执行的训练与饮食方案，并根据执行情况持续调整。
        </p>

        <ul class="feature-list">
          <li v-for="f in features" :key="f.title">
            <span class="feature-icon"><el-icon :size="18"><component :is="f.icon" /></el-icon></span>
            <span class="feature-text">
              <strong>{{ f.title }}</strong>
              <small>{{ f.desc }}</small>
            </span>
          </li>
        </ul>

        <div class="brand-loop">
          <span>健康档案</span><i>→</i>
          <span>智能计划</span><i>→</i>
          <span>执行记录</span><i>→</i>
          <span>动态调整</span>
        </div>
      </div>

      <footer class="brand-footer">
        本系统提供一般性的健身与健康饮食信息，不能替代医生或注册营养师的专业建议。
      </footer>
    </aside>

    <!-- 右侧表单区 -->
    <main class="form-panel">
      <div class="form-card">
        <div class="form-head">
          <div class="form-badge">SMARTGYM COPILOT</div>
          <h2>{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h2>
          <p>{{ mode === 'login' ? '登录后继续你的训练与饮食计划' : '几秒钟完成注册，立刻拥有你的 AI 健康教练' }}</p>
        </div>

        <div class="segmented" role="tablist">
          <button type="button" role="tab" :class="{ active: mode === 'login' }" @click="mode = 'login'">
            登录
          </button>
          <button type="button" role="tab" :class="{ active: mode === 'register' }" @click="mode = 'register'">
            注册
          </button>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="mode === 'register' ? registerRules : rules"
          label-position="top"
          size="large"
          @keyup.enter="handleSubmit"
        >
          <el-form-item label="邮箱" prop="account">
            <el-input v-model="form.account" placeholder="you@example.com" :prefix-icon="Message" />
          </el-form-item>
          <el-form-item v-if="mode === 'register'" label="昵称" prop="displayName">
            <el-input v-model="form.displayName" maxlength="30" placeholder="2～30 个字符" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="8～64 位"
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="handleSubmit">
            {{ mode === 'login' ? '登 录' : '注册并登录' }}
          </el-button>
        </el-form>

        <p class="form-foot">继续使用即表示同意我们的服务条款与隐私政策</p>
      </div>
    </main>
  </div>
</template>

<style scoped>
.auth-page {
  --brand-deep: #05253a;
  --brand-mid: #07364a;
  --brand-teal: #2dd4bf;
  --brand-emerald: #34d399;

  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
}

/* ---------- 左侧品牌区 ---------- */
.brand-panel {
  position: relative;
  flex: 1.15;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: 48px 56px;
  color: #e6f7f3;
  background:
    radial-gradient(120% 90% at 85% 10%, rgba(45, 212, 191, 0.18), transparent 55%),
    radial-gradient(100% 80% at 10% 95%, rgba(52, 211, 153, 0.14), transparent 55%),
    linear-gradient(158deg, var(--brand-mid) 0%, var(--brand-deep) 58%, #031a29 100%);
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  pointer-events: none;
}
.glow-a {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -80px;
  background: rgba(45, 212, 191, 0.22);
  animation: drift-a 14s ease-in-out infinite alternate;
}
.glow-b {
  width: 300px;
  height: 300px;
  bottom: -60px;
  left: -100px;
  background: rgba(52, 211, 153, 0.18);
  animation: drift-b 18s ease-in-out infinite alternate;
}
.glow-c {
  width: 200px;
  height: 200px;
  top: 40%;
  left: 55%;
  background: rgba(94, 234, 212, 0.1);
  animation: drift-a 22s ease-in-out infinite alternate-reverse;
}

.brand-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(230, 247, 243, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(230, 247, 243, 0.045) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(80% 70% at 60% 40%, #000 30%, transparent 100%);
  pointer-events: none;
}

@keyframes drift-a {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(-40px, 30px) scale(1.12); }
}
@keyframes drift-b {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(50px, -30px) scale(1.08); }
}

.brand-content {
  position: relative;
  max-width: 520px;
  margin-top: 5vh;
  animation: fade-up 0.7s ease both;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.brand-logo svg {
  width: 38px;
  height: 38px;
}

.brand-headline {
  margin: 0 0 18px;
  font-size: clamp(30px, 3.4vw, 44px);
  line-height: 1.22;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.brand-sub {
  margin: 0 0 36px;
  font-size: 15px;
  line-height: 1.8;
  color: rgba(214, 243, 236, 0.78);
}

.feature-list {
  list-style: none;
  margin: 0 0 36px;
  padding: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px 22px;
}
.feature-list li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.feature-icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  color: var(--brand-teal);
  background: rgba(45, 212, 191, 0.14);
  border: 1px solid rgba(45, 212, 191, 0.22);
}
.feature-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  line-height: 1.4;
}
.feature-text strong {
  font-size: 14px;
  font-weight: 600;
}
.feature-text small {
  font-size: 12px;
  color: rgba(214, 243, 236, 0.62);
}

.brand-loop {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}
.brand-loop span {
  padding: 6px 14px;
  border-radius: 999px;
  color: #a7f3e5;
  background: rgba(45, 212, 191, 0.1);
  border: 1px solid rgba(45, 212, 191, 0.25);
}
.brand-loop i {
  font-style: normal;
  color: rgba(167, 243, 229, 0.45);
}

.brand-footer {
  position: relative;
  margin-top: 32px;
  font-size: 12px;
  line-height: 1.6;
  color: rgba(214, 243, 236, 0.45);
}

/* ---------- 右侧表单区 ---------- */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background:
    radial-gradient(90% 70% at 50% 0%, rgba(45, 212, 191, 0.06), transparent 60%),
    #f5f8fa;
}

.form-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 40px 28px;
  background: #fff;
  border-radius: 20px;
  border: 1px solid rgba(15, 42, 60, 0.06);
  box-shadow:
    0 24px 60px -24px rgba(5, 37, 58, 0.18),
    0 2px 8px rgba(5, 37, 58, 0.05);
  animation: fade-up 0.6s ease 0.1s both;
}

.form-head {
  margin-bottom: 26px;
  text-align: center;
}
.form-badge {
  display: inline-block;
  margin-bottom: 14px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: #0e7f6f;
  background: rgba(45, 212, 191, 0.12);
}
.form-head h2 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  color: #10263a;
}
.form-head p {
  margin: 0;
  font-size: 14px;
  color: #7b8a96;
}

.segmented {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  padding: 4px;
  margin-bottom: 26px;
  border-radius: 12px;
  background: #edf1f4;
}
.segmented button {
  padding: 9px 0;
  border: none;
  border-radius: 9px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  color: #6b7a86;
  background: transparent;
  cursor: pointer;
  transition: all 0.25s ease;
}
.segmented button.active {
  color: #0b3d3a;
  background: #fff;
  box-shadow: 0 2px 8px rgba(5, 37, 58, 0.1);
}
.segmented button:focus-visible {
  outline: 2px solid var(--brand-teal);
  outline-offset: 1px;
}

.form-card :deep(.el-form-item__label) {
  font-weight: 600;
  color: #33475a;
}
.form-card :deep(.el-input__wrapper) {
  border-radius: 10px;
}
.form-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--brand-teal) inset, 0 0 0 3px rgba(45, 212, 191, 0.15);
}

.submit {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #fff;
  background: linear-gradient(135deg, #10b981, #2dd4bf);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}
.submit:hover,
.submit:focus-visible {
  border: none;
  color: #fff;
  background: linear-gradient(135deg, #10b981, #2dd4bf);
  filter: brightness(1.06);
  transform: translateY(-1px);
  box-shadow: 0 10px 24px -8px rgba(16, 185, 129, 0.55);
}

.form-foot {
  margin: 22px 0 0;
  text-align: center;
  font-size: 12px;
  color: #9aa7b1;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---------- 响应式 ---------- */
@media (max-width: 1080px) {
  .brand-panel {
    padding: 36px 36px;
  }
  .feature-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 880px) {
  .brand-panel {
    display: none;
  }
  .form-panel {
    min-height: 100vh;
    min-height: 100dvh;
    background:
      radial-gradient(90% 50% at 50% 0%, rgba(45, 212, 191, 0.1), transparent 60%),
      linear-gradient(168deg, #eef4f6, #f7fafb 40%);
  }
  .form-card {
    padding: 32px 26px 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .glow,
  .brand-content,
  .form-card,
  .submit {
    animation: none;
    transition: none;
  }
}
</style>
