<template>
	<svg
		xmlns="http://www.w3.org/2000/svg"
		viewBox="0 0 43 15"
		shape-rendering="crispEdges"
		fill="currentColor"
		role="img"
		aria-label="3vhy"
		class="tvh-wordmark"
		:class="{ 'tvh-wordmark--drop': drop }"
	>
		<path
			class="tvh-letter"
			:style="{ '--i': 0 }"
			d="M2 0h6v1h-6zM1 1h8v1h-8zM0 2h4v1h-4zM6 2h4v1h-4zM0 3h3v1h-3zM6 3h4v1h-4zM6 4h4v1h-4zM3 5h6v1h-6zM3 6h7v1h-7zM6 7h4v1h-4zM0 8h3v1h-3zM6 8h4v1h-4zM0 9h4v1h-4zM5 9h5v1h-5zM1 10h9v1h-9zM2 11h6v1h-6z"
		/>
		<path
			class="tvh-letter"
			:style="{ '--i': 1 }"
			d="M11 2h4v1h-4zM17 2h4v1h-4zM11 3h4v1h-4zM17 3h4v1h-4zM11 4h4v1h-4zM17 4h4v1h-4zM11 5h4v1h-4zM17 5h4v1h-4zM12 6h4v1h-4zM17 6h4v1h-4zM12 7h9v1h-9zM13 8h8v1h-8zM13 9h7v1h-7zM14 10h6v1h-6zM15 11h4v1h-4z"
		/>
		<path
			class="tvh-letter"
			:style="{ '--i': 2 }"
			d="M23 0h2v1h-2zM22 1h4v1h-4zM22 2h4v1h-4zM22 3h4v1h-4zM22 4h4v1h-4zM27 4h4v1h-4zM22 5h10v1h-10zM22 6h4v1h-4zM28 6h4v1h-4zM22 7h4v1h-4zM28 7h4v1h-4zM22 8h4v1h-4zM28 8h4v1h-4zM22 9h4v1h-4zM28 9h4v1h-4zM22 10h4v1h-4zM28 10h4v1h-4zM23 11h3v1h-3zM29 11h3v1h-3z"
		/>
		<path
			class="tvh-letter"
			:style="{ '--i': 3 }"
			d="M33 2h4v1h-4zM39 2h4v1h-4zM33 3h4v1h-4zM39 3h4v1h-4zM33 4h4v1h-4zM39 4h4v1h-4zM33 5h4v1h-4zM39 5h4v1h-4zM33 6h4v1h-4zM39 6h4v1h-4zM33 7h5v1h-5zM39 7h4v1h-4zM34 8h9v1h-9zM35 9h8v1h-8zM39 10h4v1h-4zM33 11h3v1h-3zM39 11h4v1h-4zM33 12h4v1h-4zM38 12h4v1h-4zM34 13h8v1h-8zM35 14h5v1h-5z"
		/>
	</svg>
</template>

<script setup lang="ts">
// The 3vhy wordmark with each letter as its own shape, so the letters can
// drop in one after another (used on the loading screen).
withDefaults(defineProps<{ drop?: boolean }>(), { drop: false })
</script>

<style scoped>
.tvh-wordmark {
	overflow: visible;
}

.tvh-letter {
	transform-box: fill-box;
	transform-origin: 50% 100%;
}

.tvh-wordmark--drop .tvh-letter {
	animation: tvh-letter-drop 0.62s cubic-bezier(0.34, 1.56, 0.64, 1) both;
	animation-delay: calc(0.1s + var(--i) * 0.09s);
}

@keyframes tvh-letter-drop {
	0% {
		opacity: 0;
		transform: translateY(-9px);
	}
	60% {
		opacity: 1;
		transform: translateY(0.6px) scaleY(0.92);
	}
}

@media (prefers-reduced-motion: reduce) {
	.tvh-wordmark--drop .tvh-letter {
		animation: none;
	}
}
</style>
