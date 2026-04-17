import 'element-plus/dist/index.css'
import './styles/index.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.config.globalProperties.$ELEMENT = { locale: zhCn }

app.mount('#app')
