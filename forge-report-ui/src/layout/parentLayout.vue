<template>
  <div class="flex flex-col min-h-screen">
    <router-view></router-view>
    <footer class="text-14 text-center text-gray-500 py-4 mt-auto">
      Copyright © 2023
      <span class="mx-1">|</span>
      {{ $ui("蒙ICP备2026004895号") }}
    </footer>
  </div>
</template>
