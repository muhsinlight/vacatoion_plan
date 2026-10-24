<script setup>
defineOptions({ inheritAttrs: false })

defineProps({
  name: { type: String, required: true },
})
</script>

<template>
  <svg
    class="icon"
    viewBox="0 0 24 24"
    width="18"
    height="18"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    v-bind="$attrs"
  >
    <template v-if="name === 'home'">
      <path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1z" />
    </template>
    <template v-else-if="name === 'tag'">
      <path d="M12 3h7a1 1 0 0 1 1 1v7l-9.5 9.5a1.5 1.5 0 0 1-2.1 0L3.5 15.6a1.5 1.5 0 0 1 0-2.1z" />
      <circle cx="16" cy="8" r="1.2" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'case'">
      <rect x="3" y="7" width="18" height="13" rx="2" />
      <path d="M8 7V5.5A1.5 1.5 0 0 1 9.5 4h5A1.5 1.5 0 0 1 16 5.5V7M3 12h18" />
    </template>
    <template v-else-if="name === 'user'">
      <circle cx="12" cy="8" r="3.2" />
      <path d="M5 19.2c1.4-2.8 3.8-4.2 7-4.2s5.6 1.4 7 4.2" />
    </template>
    <template v-else-if="name === 'pin'">
      <path d="M12 21s6.5-5.4 6.5-10.2a6.5 6.5 0 1 0-13 0C5.5 15.6 12 21 12 21z" />
      <circle cx="12" cy="10.6" r="1.8" />
    </template>
    <template v-else-if="name === 'calendar'">
      <rect x="4" y="5" width="16" height="15" rx="2" />
      <path d="M8 3.5V7M16 3.5V7M4 10h16" />
    </template>
    <template v-else-if="name === 'users'">
      <circle cx="9" cy="9" r="2.6" />
      <path d="M3.8 18.2c.9-2.3 2.7-3.4 5.2-3.4s4.3 1.1 5.2 3.4" />
      <circle cx="16.2" cy="9.2" r="2.1" />
      <path d="M16 14.8c1.8.2 3.2 1.1 4 2.9" />
    </template>
    <template v-else-if="name === 'bus'">
      <rect x="4" y="5" width="16" height="11" rx="2" />
      <path d="M4 11h16M8 19v-3M16 19v-3M8 16h.01M16 16h.01" />
    </template>
    <template v-else-if="name === 'bed'">
      <path d="M4 18V8M4 14h16v4M8 14V11h5a4 4 0 0 1 4 3" />
    </template>
    <template v-else-if="name === 'style'">
      <path d="M5 19c4-1 6-4 6.5-8.5C13 7 15 5 18 4c-.2 3.2-1.2 5.2-3.2 6.6C12.6 12 10 13.2 5 19z" />
    </template>
    <template v-else-if="name === 'chevron'">
      <path d="m7 10 5 5 5-5" />
    </template>
    <template v-else-if="name === 'logout'">
      <path d="M10 7V5a1 1 0 0 1 1-1h8v16h-8a1 1 0 0 1-1-1v-2" />
      <path d="M4 12h10M11 8l4 4-4 4" />
    </template>
    <template v-else-if="name === 'menu'">
      <path d="M4 7h16M4 12h16M4 17h16" />
    </template>
    <template v-else-if="name === 'search'">
      <circle cx="11" cy="11" r="6" />
      <path d="m20 20-3.5-3.5" />
    </template>
    <template v-else-if="name === 'refresh'">
      <path d="M20 12a8 8 0 1 1-2.3-5.7" />
      <path d="M20 4v6h-6" />
    </template>
    <template v-else-if="name === 'train'">
      <rect x="5" y="4" width="14" height="12" rx="3" />
      <path d="M5 11h14M12 4v7M8 20l2-4M16 20l-2-4M9 19h6" />
    </template>
    <template v-else-if="name === 'plane'">
      <path d="M10.5 13.5 3 11.2l1-2 6.8 1.1 3.3-3.9-5-4.1 1.6-.9 6.7 3.3 2.1-2.4a1.6 1.6 0 0 1 2.4 2.1l-2.4 2.8 3.3 6.7-.9 1.6-4.1-5-3.9 3.3 1.1 6.8-2 1z" />
    </template>
    <template v-else-if="name === 'car'">
      <path d="M4 15v-2.2a2 2 0 0 1 .2-.9l1.9-3.6A2 2 0 0 1 7.9 7h8.2a2 2 0 0 1 1.8 1.1l1.9 3.6a2 2 0 0 1 .2.9V15" />
      <path d="M3.5 15h17M5 15v2M19 15v2" />
      <circle cx="7.5" cy="17" r="1.6" />
      <circle cx="16.5" cy="17" r="1.6" />
    </template>
    <template v-else-if="name === 'house'">
      <path d="M4 11 12 4.5 20 11v8.5a.5.5 0 0 1-.5.5h-15a.5.5 0 0 1-.5-.5z" />
      <path d="M9.5 20v-5h5v5" />
    </template>
    <template v-else-if="name === 'key'">
      <circle cx="8.5" cy="8.5" r="4" />
      <path d="m11.5 11.5 7 7M15.5 15.5l-1.8 1.8M18 18l-1.8 1.8" />
    </template>
    <template v-else-if="name === 'tent'">
      <path d="M12 4.5 20.5 19H3.5z" />
      <path d="M12 4.5V19M8 19l4-7 4 7" />
    </template>
    <template v-else-if="name === 'ban'">
      <circle cx="12" cy="12" r="8" />
      <path d="m6.5 17.5 11-11" />
    </template>
    <template v-else-if="name === 'museum'">
      <path d="M3.5 9.5 12 4.5l8.5 5" />
      <path d="M5.5 9.5V17M9.5 9.5V17M14.5 9.5V17M18.5 9.5V17M4 19.5h16" />
    </template>
    <template v-else-if="name === 'leaf'">
      <path d="M20 4c-8 0-13 3.6-13 9a5.5 5.5 0 0 0 2.2 4.4C12 14 15.5 11.6 19 11c-3.4 1.6-6.6 4-9.8 8.5" />
    </template>
    <template v-else-if="name === 'utensils'">
      <path d="M7 3.5v7a2.5 2.5 0 0 0 5 0v-7M9.5 13v7.5" />
      <path d="M16.5 3.5c1.8 1 2.8 2.7 2.8 5 0 1.8-.9 3-2.3 3.4V20.5" />
    </template>
    <template v-else-if="name === 'beach'">
      <path d="M12 20.5V11" />
      <path d="M4 11h16a8 8 0 0 0-16 0z" />
      <path d="M8.5 20.5c1.2-1 2.3-1 3.5 0 1.2 1 2.3 1 3.5 0" />
    </template>
    <template v-else-if="name === 'bag'">
      <path d="M5.5 8h13l1 12H4.5z" />
      <path d="M9 8V6a3 3 0 0 1 6 0v2" />
    </template>
    <template v-else-if="name === 'compass'">
      <circle cx="12" cy="12" r="8.5" />
      <path d="m14.8 9.2-1.6 4.4-4.4 1.6 1.6-4.4z" />
    </template>
    <template v-else-if="name === 'plus'">
      <path d="M12 5.5v13M5.5 12h13" />
    </template>
    <template v-else-if="name === 'check'">
      <path d="m5.5 12.5 4 4 9-9" />
    </template>
    <template v-else-if="name === 'clock'">
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 7.5V12l3 2" />
    </template>
    <template v-else-if="name === 'wallet'">
      <rect x="3.5" y="6" width="17" height="13" rx="2.5" />
      <path d="M3.5 10.5h17M16 14.5h1.5" />
    </template>
    <template v-else-if="name === 'sparkles'">
      <path d="M12 4.5 13.4 9 18 10.4 13.4 11.8 12 16.3 10.6 11.8 6 10.4 10.6 9z" />
      <path d="M18.5 16.5 19 18l1.5.5-1.5.5-.5 1.5-.5-1.5L16.5 19l1.5-.5z" />
    </template>
    <template v-else-if="name === 'wifi-off'">
      <path d="M5 12.5c2-2 4.4-3 7-3M19 12.5c-1.1-1.1-2.3-1.9-3.6-2.4" />
      <path d="M8.8 15.4A5.4 5.4 0 0 1 12 14.6c.8 0 1.5.2 2.2.5" />
      <path d="M12 19.2h.01" />
      <path d="m4 5 16 14" />
    </template>
    <template v-else-if="name === 'alert'">
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 8v5" />
      <path d="M12 16.5h.01" />
    </template>
  </svg>
</template>
