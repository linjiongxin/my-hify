import 'element-plus/dist/index.css'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import './styles/index.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElPagination } from 'element-plus'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 全局注册分页组件（unplugin-vue-components 未自动导入）
app.component('ElPagination', ElPagination)

app.use(createPinia())
app.use(router)

app.mount('#app')
