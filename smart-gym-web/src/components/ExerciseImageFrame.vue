<script setup lang="ts">
/**
 * 通用图片框：为后续接入 MinIO 对象存储预留。
 *
 * 设计约定：
 * - src 为空 → 显示占位插画（内联 SVG，无网络请求）
 * - src 有值 → 原生 loading="lazy" 懒加载，加载中显示骨架
 * - 加载失败 → 自动回退占位插画（MinIO URL 过期、对象不存在等场景）
 * - 宽高比由 aspect-ratio 控制，父容器只管宽度，不写死像素高度
 */
import { ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    /** 图片地址；null/空串时显示占位图 */
    src?: string | null
    /** 加载失败时的提示文案 */
    alt?: string
    /** 宽高比，默认 4:3 */
    ratio?: string
    /** 圆角尺寸 */
    radius?: string
  }>(),
  { src: null, alt: '', ratio: '4 / 3', radius: '10px' },
)

const failed = ref(false)
const loaded = ref(false)

watch(
  () => props.src,
  () => {
    failed.value = false
    loaded.value = false
  },
)

function onLoad() {
  loaded.value = true
}

function onError() {
  failed.value = true
  loaded.value = false
}
</script>

<template>
  <div class="image-frame" :style="{ aspectRatio: ratio, borderRadius: radius }">
    <!-- 占位插画：动作示意图轮廓 + 待接入提示 -->
    <div v-if="!src || failed" class="placeholder" :aria-label="alt || '暂无图片'">
      <svg viewBox="0 0 120 90" fill="none" aria-hidden="true">
        <circle cx="34" cy="20" r="7" fill="currentColor" opacity="0.55" />
        <path
          d="M34 30 L34 52 M34 38 L20 48 M34 38 L52 30 M34 52 L26 70 M34 52 L44 70"
          stroke="currentColor"
          stroke-width="4.5"
          stroke-linecap="round"
          opacity="0.45"
        />
        <rect x="64" y="22" width="6" height="26" rx="2" fill="currentColor" opacity="0.35" />
        <rect x="86" y="22" width="6" height="26" rx="2" fill="currentColor" opacity="0.35" />
        <rect x="70" y="32" width="16" height="5" rx="2.5" fill="currentColor" opacity="0.35" />
        <path
          d="M58 34 H64 M76 34 H86"
          stroke="currentColor"
          stroke-width="3.5"
          stroke-linecap="round"
          opacity="0.3"
        />
      </svg>
      <span class="placeholder-text">{{ failed ? '图片加载失败' : '动作示意图片待接入' }}</span>
    </div>

    <!-- 真实图片：lazy 懒加载，成功后淡入 -->
    <img
      v-if="src && !failed"
      :src="src"
      :alt="alt"
      loading="lazy"
      decoding="async"
      :class="{ visible: loaded }"
      @load="onLoad"
      @error="onError"
    />

    <!-- 加载骨架（有 src 且未完成加载时） -->
    <div v-if="src && !failed && !loaded" class="skeleton" aria-hidden="true"></div>
  </div>
</template>

<style scoped>
.image-frame {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: var(--el-fill-color-light);
}

.placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--el-color-primary);
  background:
    radial-gradient(80% 60% at 50% 30%, rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.06), transparent),
    repeating-linear-gradient(
      -45deg,
      transparent 0 10px,
      rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.025) 10px 20px
    );
}
.placeholder svg {
  width: 44%;
  max-width: 130px;
}

.placeholder-text {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.3s ease;
}
img.visible {
  opacity: 1;
}

.skeleton {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    100deg,
    transparent 40%,
    rgba(255, 255, 255, 0.55) 50%,
    transparent 60%
  );
  background-color: var(--el-fill-color);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease infinite;
}

@keyframes shimmer {
  from {
    background-position: 120% 0;
  }
  to {
    background-position: -80% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton {
    animation: none;
  }
  img {
    transition: none;
  }
}
</style>
