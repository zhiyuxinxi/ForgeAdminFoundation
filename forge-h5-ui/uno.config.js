import { defineConfig, presetAttributify, presetIcons, presetUno } from 'unocss'
import presetRemToPx from '@unocss/preset-rem-to-px'

export default defineConfig({
  presets: [
    presetUno(),
    presetAttributify(),
    presetIcons({
      warn: true,
      prefix: 'i-',
      extraProperties: {
        display: 'inline-block',
        width: '1em',
        height: '1em',
      },
    }),
    presetRemToPx({ baseFontSize: 4 }),
  ],
  shortcuts: [
    ['wh-full', 'w-full h-full'],
    ['f-c-c', 'flex justify-center items-center'],
    ['flex-col', 'flex flex-col'],
    ['card-shadow', 'rounded-2 shadow-[0_2px_8px_rgba(0,0,0,0.15)]'],
  ],
  theme: {
    colors: {
      primary: 'var(--primary-color, #0891b2)',
    },
  },
})
