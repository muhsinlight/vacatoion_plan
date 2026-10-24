<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { formatMoney } from '../../utils/format'

/*
 * Tutar degisince eski degerden yenisine sayar. Bicimlendirme
 * `utils/format.js` icindeki `formatMoney` ile ayni kalir, yani tr-TR.
 */
const props = defineProps({
  min: { type: [Number, String], default: null },
  max: { type: [Number, String], default: null },
  currency: { type: String, default: 'TRY' },
  duration: { type: Number, default: 520 },
})

const shownMin = ref(toNumber(props.min))
const shownMax = ref(toNumber(props.max))

let frame = null

function toNumber(value) {
  return value == null || value === '' ? null : Number(value)
}

const reduceMotion =
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

function easeOut(t) {
  return 1 - (1 - t) ** 3
}

function stop() {
  if (frame != null && typeof cancelAnimationFrame === 'function') {
    cancelAnimationFrame(frame)
  }
  frame = null
}

function animateTo(nextMin, nextMax) {
  stop()
  const fromMin = shownMin.value
  const fromMax = shownMax.value

  const skip =
    reduceMotion ||
    nextMin == null ||
    nextMax == null ||
    fromMin == null ||
    fromMax == null ||
    typeof requestAnimationFrame !== 'function'

  if (skip) {
    shownMin.value = nextMin
    shownMax.value = nextMax
    return
  }

  const started = performance.now()
  const step = (now) => {
    const progress = Math.min(1, (now - started) / props.duration)
    const eased = easeOut(progress)
    shownMin.value = Math.round(fromMin + (nextMin - fromMin) * eased)
    shownMax.value = Math.round(fromMax + (nextMax - fromMax) * eased)
    if (progress < 1) {
      frame = requestAnimationFrame(step)
      return
    }
    shownMin.value = nextMin
    shownMax.value = nextMax
    frame = null
  }
  frame = requestAnimationFrame(step)
}

watch(
  () => [toNumber(props.min), toNumber(props.max)],
  ([nextMin, nextMax]) => animateTo(nextMin, nextMax),
)

onBeforeUnmount(stop)
</script>

<template>
  <span class="tabular-nums">{{ formatMoney(shownMin, shownMax, currency) }}</span>
</template>
