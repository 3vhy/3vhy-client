<template>
	<div class="threevhy-logo" :class="`threevhy-logo--${size}`" aria-label="3vhy Client">
		<ThreevhyWordmark class="threevhy-logo__mark" />
		<span v-if="showClient" class="threevhy-logo__client">CLIENT</span>
	</div>
</template>

<script setup lang="ts">
import ThreevhyWordmark from './ThreevhyWordmark.vue'

withDefaults(defineProps<{ size?: 'sm' | 'lg'; showClient?: boolean }>(), {
	size: 'sm',
	showClient: true,
})
</script>

<style scoped lang="scss">
.threevhy-logo {
	display: inline-flex;
	align-items: center;
	gap: 0.6rem;
	color: var(--threevhy-ice);
	user-select: none;
}

.threevhy-logo__mark {
	height: 1.15rem;
	width: auto;
	filter: drop-shadow(0 1px 0 rgba(3, 14, 70, 0.6));
}

.threevhy-logo__client {
	font-size: 0.62rem;
	font-weight: 800;
	letter-spacing: 0.32em;
	padding: 0.2rem 0.45rem;
	border-radius: 0.4rem;
	color: var(--threevhy-sky);
	background: var(--threevhy-chip-bg);
}

.threevhy-logo--lg {
	gap: 1.1rem;

	.threevhy-logo__mark {
		height: 3.4rem;
		filter: drop-shadow(0 4px 0 rgba(3, 14, 70, 0.55));
	}

	.threevhy-logo__client {
		font-size: 1.05rem;
		padding: 0;
		background: none;
		letter-spacing: 0.4em;
	}
}
</style>

<style lang="scss">
html.light-mode .threevhy-logo__client {
	color: #1a4fba;
}
</style>
