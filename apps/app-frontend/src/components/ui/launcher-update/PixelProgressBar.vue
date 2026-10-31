<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
	/** 0 to 1 */
	progress: number
	label?: string
}>()

const percent = computed(() => Math.min(100, Math.max(0, Math.round(props.progress * 100))))
</script>

<template>
	<div class="tvh-progress">
		<div
			class="tvh-progress__track"
			role="progressbar"
			aria-valuemin="0"
			aria-valuemax="100"
			:aria-valuenow="percent"
			:aria-label="label"
		>
			<div class="tvh-progress__fill" :style="{ width: `${percent}%` }" />
		</div>
		<span class="tvh-progress__percent">{{ percent }}%</span>
	</div>
</template>

<style scoped lang="scss">
.tvh-progress {
	display: flex;
	align-items: center;
	gap: 0.6rem;
}

.tvh-progress__track {
	position: relative;
	flex: 1;
	height: 0.75rem;
	padding: 2px;
	clip-path: var(--eventide-pixel-corners);
	background: var(--eventide-chip-bg);
	box-shadow: inset 0 2px 0 rgba(2, 8, 40, 0.35);
	overflow: hidden;
}

.tvh-progress__fill {
	height: 100%;
	background: repeating-linear-gradient(
		90deg,
		var(--eventide-cyan) 0 6px,
		var(--eventide-mid) 6px 8px
	);
	box-shadow: inset 0 -2px 0 rgba(4, 18, 63, 0.35);
	transition: width 0.2s steps(4, end);
}

.tvh-progress__percent {
	min-width: 3ch;
	font-family: var(--eventide-font-pixel);
	font-size: 0.85rem;
	font-weight: 600;
	font-variant-numeric: tabular-nums;
	text-align: right;
	color: var(--color-contrast);
}

@media (prefers-reduced-motion: reduce) {
	.tvh-progress__fill {
		transition: none;
	}
}
</style>
