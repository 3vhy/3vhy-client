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

// 3vhy Client: labeled nav tiles. The active one is a pixel block with a
// cyan marker on the window edge.
.nav-tile {
	flex-direction: column;
	gap: 0.35rem;
	width: 100%;
	min-height: 4.1rem;
	padding: 0.55rem 0.25rem 0.6rem;
	border-radius: 0;
	background: transparent;

	&::before {
		border-radius: 0;
		clip-path: var(--threevhy-pixel-corners);
	}

	&:hover {
		background: transparent;

		&::before {
			opacity: 1;
			scale: 1;
			background: rgba(140, 196, 234, 0.1);
		}
	}
}

.nav-tile__icon {
	display: flex;
	font-size: 1.4rem;

	:deep(svg) {
		width: 1.4rem;
		height: 1.4rem;
	}
}

.nav-tile__label {
	font-size: 0.75rem;
	font-weight: 600;
	line-height: 1;
	white-space: nowrap;
}

.nav-tile.router-link-active {
	color: #f2faff;

	&::before {
		background: linear-gradient(180deg, #4fbdea 0%, #2b8fdb 45%, #1a4fba 100%);
		box-shadow: var(--threevhy-block-edge);
	}

	&::after {
		content: '';
		position: absolute;
		left: -0.6rem;
		top: 50%;
		width: 4px;
		height: 1.5rem;
		translate: 0 -50%;
		background: #5bc8e6;
		box-shadow: 0 0 10px rgba(91, 200, 230, 0.8);
	}

	svg {
		filter: none;
	}
}

.nav-tile.subpage-active {
	background: transparent;

	&::before {
		opacity: 1;
		scale: 1;
		background: rgba(91, 200, 230, 0.14);
		box-shadow: inset 0 0 0 1px rgba(91, 200, 230, 0.35);
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
