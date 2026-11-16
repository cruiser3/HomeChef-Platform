import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'vant/lib/index.css'
import './style.css'
import App from './App.vue'
import router from './router/index.js'
import {
  Button, Form, Field, Cell, CellGroup, Icon, Popup, Empty, Loading,
  Image, Search, Tag, Stepper, Rate, Switch,
} from 'vant'

const app = createApp(App)

app.use(createPinia())
app.use(router)

;[
  Button, Form, Field, Cell, CellGroup, Icon, Popup, Empty, Loading,
  Image, Search, Tag, Stepper, Rate, Switch,
].forEach((component) => {
  app.use(component)
})

app.mount('#app')
