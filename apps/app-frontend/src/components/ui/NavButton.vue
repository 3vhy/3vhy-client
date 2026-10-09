<template>
	<component
		:is="isLink ? RouterLink : 'button'"
		:to="isLink ? to : undefined"
		v-bind="$attrs"
		:disabled="isLink ? undefined : disabled"
		:active-class="isLink && isSubpage ? '' : undefined"
		:class="[
			{
				'router-link-active': isPrimary && isPrimary(route),
				'subpage-active': isSubpage && isSubpage(route),
				disabled: disabled,
			},
			label ? 'nav-tile' : 'w-12 rounded-full h-12 text-2xl',
		]"
		class="nav-button border-none text-primary cursor-pointer flex items-center justify-center transition-all bg-transparent hover:bg-button-bg hover:text-contrast"
		@click="onClick"
	>
		<template v-if="label">
			<span class="nav-tile__icon"><slot /></span>
			<span class="nav-tile__label">{{ label }}</span>
		</template>
		<slot v-else />
	</component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()

type RouteFunction = (route: RouteLocationNormalizedLoaded) => boolean

const props = withDefaults(
	defineProps<{
		to: (() => void) | string
		isPrimary?: RouteFunction
		isSubpage?: RouteFunction
		highlightOverride?: boolean
		disabled?: boolean
		/** 3vhy Client: shows a text label under the icon (S.A.M-style nav tile) */
		label?: string | undefined
	}>(),
	{
		disabled: false,
	},
)

const isLink = computed(() => typeof props.to === 'string')

function onClick() {
	if (typeof props.to === 'function') {
		props.to()
	}
}

defineOptions({
	inheritAttrs: false,
})
</script>

<style lang="scss" scoped>
.nav-button {
	position: relative;

	&::before {
		content: '';
		position: absolute;
		inset: 0;
		background-color: var(--color-button-bg-selected);
		border-radius: 50%;
		opacity: 0;
		scale: 0.4;
		z-index: -1;
		transition:
			opacity 0.25s var(--ease-out-expo),
			scale 0.25s var(--ease-out-expo);
	}
}

// 3vhy Client: labeled nav tiles
.nav-tile {
	flex-direction: column;
	gap: 0.3rem;
	width: 100%;
	min-height: 4.25rem;
	padding: 0.55rem 0.25rem;
	border-radius: 1rem;

	&::before {
		border-radius: 1rem;
	}
}

.nav-tile__icon {
	display: flex;
	font-size: 1.45rem;

	:deep(svg) {
		width: 1.45rem;
		height: 1.45rem;
	}
}

.nav-tile__label {
	font-size: 0.78rem;
	font-weight: 600;
	line-height: 1;
	white-space: nowrap;
}

.nav-tile.router-link-active {
	color: #f2faff;

	&::before {
		background: linear-gradient(160deg, #3fb6e8 0%, #1a6fd0 55%, #1a4fba 100%);
		box-shadow:
			0 4px 0 #0d3f99,
			0 10px 22px rgba(26, 79, 186, 0.45);
	}

	svg {
		filter: none;
	}
}

.router-link-active,
.subpage-active {
	svg {
		filter: drop-shadow(0 0 0.5rem black);
	}
}

.router-link-active {
	@apply text-[--color-button-text-selected];
	&::before {
		background-color: var(--color-button-bg-selected);
		scale: 1;
		opacity: 1;
	}
}

.subpage-active {
	@apply text-contrast bg-button-bg;
}
</style>
