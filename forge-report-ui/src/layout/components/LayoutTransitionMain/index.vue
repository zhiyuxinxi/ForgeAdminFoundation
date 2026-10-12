<template>
  <router-view #default="{ Component, route }">
    <keep-alive>
      <component
        v-if="!route.meta.noKeepAlive"
        :is="Component"
        :key="route.fullPath"
      />
    </keep-alive>
    <component
      v-if="route.meta.noKeepAlive"
      :is="Component"
      :key="route.fullPath"
    />
  </router-view>
</template>
