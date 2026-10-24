<script setup>
import TripPreviewSections from '../TripPreviewSections.vue'

defineProps({
  preview: { type: Object, required: true },
})
</script>

<template>
  <div class="plan-evidence">
    <TripPreviewSections :preview="preview" :show-totals="false" />
  </div>
</template>

<style scoped>
/*
 * TripPreviewSections global `panel` / `chip` siniflarini kullanir;
 * Planla sayfasinda Tailwind kartlariyla ayni dilde gorunmesi icin
 * yalnizca bu sarmalayicida :deep ile yumusatilir.
 */
.plan-evidence :deep(.muted) {
  color: var(--color-ink-500);
  font-size: 0.8125rem;
  line-height: 1.45;
}

.plan-evidence :deep(.panel),
.plan-evidence :deep(.table-wrap),
.plan-evidence :deep(.local-fare),
.plan-evidence :deep(.tour) {
  margin-top: 1.5rem;
  border-radius: 16px;
  border: 1px solid var(--color-ink-200);
  background: #fff;
  padding: 1.25rem;
  box-shadow: var(--shadow-soft);
}

.plan-evidence :deep(h2) {
  margin: 0 0 0.75rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-ink-900);
}

.plan-evidence :deep(h3) {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-ink-900);
}

.plan-evidence :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
}

.plan-evidence :deep(th),
.plan-evidence :deep(td) {
  padding: 0.5rem 0.35rem;
  border-bottom: 1px solid var(--color-ink-100);
  vertical-align: top;
}

.plan-evidence :deep(th) {
  font-weight: 600;
  color: var(--color-ink-700);
  text-align: left;
}

.plan-evidence :deep(.amount) {
  text-align: right;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.plan-evidence :deep(.chip) {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  border: 1px solid var(--color-ink-200);
  padding: 0.125rem 0.5rem;
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--color-ink-700);
  background: var(--color-ink-50);
}

.plan-evidence :deep(.chip--accent) {
  border-color: var(--color-brand-200);
  background: var(--color-brand-50);
  color: var(--color-brand-700);
}

.plan-evidence :deep(.plain-list) {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--color-ink-700);
  font-size: 0.8125rem;
  line-height: 1.5;
}

.plan-evidence :deep(.evidence-list) {
  gap: 0.5rem;
}

.plan-evidence :deep(a) {
  color: var(--color-brand-700);
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.plan-evidence :deep(.day-block) {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-ink-100);
}

.plan-evidence :deep(.day-block ol) {
  display: grid;
  gap: 0.5rem;
  margin: 0;
  padding: 0;
  list-style: none;
  counter-reset: tour-stop;
}

.plan-evidence :deep(.tour-stop) {
  display: grid;
  grid-template-columns: 1.5rem auto minmax(0, 1fr) auto;
  column-gap: 0.75rem;
  row-gap: 0.15rem;
  align-items: baseline;
  padding: 0.7rem 0.85rem;
  border-radius: 12px;
  background: var(--color-ink-50);
  counter-increment: tour-stop;
}

.plan-evidence :deep(.tour-stop::before) {
  content: counter(tour-stop);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-ink-500);
  font-variant-numeric: tabular-nums;
}

.plan-evidence :deep(.tour-kind) {
  color: var(--color-brand-700);
  font-size: 0.75rem;
  font-weight: 700;
}

.plan-evidence :deep(.tour-stop strong) {
  min-width: 0;
  font-size: 0.9375rem;
  line-height: 1.4;
}

.plan-evidence :deep(.tour-mins) {
  color: var(--color-ink-600, var(--color-ink-500));
  font-size: 0.8125rem;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.plan-evidence :deep(.tour-stop p) {
  grid-column: 3 / -1;
  margin: 0.15rem 0 0;
}

.plan-evidence :deep(.empty),
.plan-evidence :deep(.empty-block) {
  border-radius: 16px;
  border: 1px dashed var(--color-ink-200);
  background: var(--color-ink-50);
  padding: 1rem;
  font-size: 0.8125rem;
  color: var(--color-ink-500);
}
</style>
