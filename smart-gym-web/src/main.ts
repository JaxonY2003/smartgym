import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import '@/styles/index.css'

// ElMessageBox / ElMessage / ElNotification 等函数式 API 不经过模板，
// unplugin-vue-components 的按需样式注入覆盖不到，必须显式引入样式
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/notification/style/css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
