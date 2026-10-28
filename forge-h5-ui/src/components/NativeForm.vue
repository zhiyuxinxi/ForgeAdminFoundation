<!--
  H5 专用原生 <form> 容器。

  背景：uni-app 会把模板中的 <form> 编译成 <uni-form> 自定义元素（运行时
  tagName 为 UNI-FORM，不是 FORM），导致 <uni-input> 内的原生
  input[type=password] 不在任何原生 form 内，Chromium 持续告警
  "Password field is not contained in a form"。

  本组件通过 render() 直接创建原生 <form> 元素：模板编译只改写模板中的标签，
  不会改写开发者写的 render 函数，因此 h('form', ...) 在 H5 运行时就是原生
  <form>，插槽内 uni-input 的原生 input 的 closest('form') 立即命中。

  平台边界：仅 H5 使用本组件（登录页模板 #ifdef H5 分支引用）。非 H5 平台
  （微信小程序等）的 <form> 本身就是各端原生表单组件，且渲染函数组件在小程序
  编译链中不可用，因此非 H5 分支继续使用 uni-app <form>，保持多端兼容。

  提交语义：内部对原生 submit 事件 preventDefault（阻止页面刷新/跳转），再以
  $emit('submit') 转发给页面复用的 guarded handler（与键盘 Enter、按钮点击
  同一入口，pending 期间重复触发被守卫去重）。
-->
<script>
import { h } from 'vue'

export default {
  name: 'NativeForm',
  inheritAttrs: false,
  emits: ['submit'],
  methods: {
    handleSubmit(event) {
      // 原生表单提交默认会导航/刷新页面，必须先拦截；uni-input 的 Enter 与
      // 按钮点击路径不经过这里，只有真正的原生 submit 事件才会进入。
      if (event && typeof event.preventDefault === 'function') {
        event.preventDefault()
      }
      this.$emit('submit', event)
    },
  },
  render() {
    const children = this.$slots.default ? this.$slots.default() : []
    return h('form', { ...this.$attrs, onSubmit: this.handleSubmit }, children)
  },
}
</script>
