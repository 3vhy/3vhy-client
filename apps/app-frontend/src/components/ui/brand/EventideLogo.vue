<template>
	<div class="eventide-logo" :class="`eventide-logo--${size}`" aria-label="Eventide">
		<EventideGlyph class="eventide-logo__glyph" />
		<EventideWordmark class="eventide-logo__word" />
	</div>
</template>

<script setup lang="ts">
import EventideGlyph from './EventideGlyph.vue'
import EventideWordmark from './EventideWordmark.vue'

withDefaults(defineProps<{ size?: 'sm' | 'lg' }>(), {
	size: 'sm',
})
</script>

<style scoped lang="scss">
.eventide-logo {
	display: inline-flex;
	align-items: center;
	gap: 0.45rem;
	color: var(--brand-wordmark);
	user-select: none;
}

.eventide-logo__glyph {
	height: 1.6rem;
	width: auto;
}

.eventide-logo__word {
	height: 0.95rem;
	width: auto;
}

.eventide-logo--lg {
	gap: 0.9rem;

	.eventide-logo__glyph {
		height: 3.6rem;
	}

	.eventide-logo__word {
		height: 2.1rem;
	}
}
</style>
