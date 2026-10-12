<template>
  <div class="flex flex-col min-h-screen">
    <layout-main class="flex-1"></layout-main>
    <footer class="text-14 text-center text-gray-500 py-4">
      {{ $ui("蒙ICP备2026004895号") }}
    </footer>
  </div>
</template>

<script lang="ts" setup>
import { LayoutMain } from '@/layout/components/LayoutMain/index'
</script>
