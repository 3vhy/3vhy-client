<template>
	<Transition name="splash-fade" @after-leave="onAfterLeave">
		<div v-if="!doneLoading" class="splash-screen" data-tauri-drag-region>
			<!-- 3vhy Client: the loading screen is the 3vhy banner sky -->
			<div class="splash-sky" aria-hidden="true" data-tauri-drag-region>
				<span class="splash-cloud c1" />
				<span class="splash-cloud c2" />
				<span class="splash-cloud c3" />
				<svg
					class="splash-hills"
					viewBox="0 0 64 12"
					preserveAspectRatio="none"
					shape-rendering="crispEdges"
				>
					<path class="hill-back" d="M0 6h6v-1h7v1h5v-2h8v1h6v1h9v-2h7v1h9v1h7v6H0z" />
					<path class="hill-front" d="M0 9h9v-1h8v1h11v-1h6v1h10v-2h8v1h12v4H0z" />
				</svg>
			</div>
			<div class="splash-content" data-tauri-drag-region>
				<ThreevhyWordmarkLetters drop class="splash-wordmark" />
				<span class="splash-client">CLIENT</span>
				<div
					class="splash-blocks"
					role="progressbar"
					aria-valuemin="0"
					aria-valuemax="100"
					:aria-valuenow="Math.round(Math.min(loadingProgress, 100))"
				>
					<span
						v-for="index in BLOCKS"
						:key="index"
						class="splash-block"
						:class="{ on: index <= filledBlocks }"
					/>
				</div>
				<span class="splash-message">{{ message ?? '\u00a0' }}</span>
			</div>
		</div>
	</Transition>
</template>

<script setup>
import { injectLoadingState } from '@modrinth/ui'
import { computed, onMounted, ref, watch } from 'vue'

import ThreevhyWordmarkLetters from '@/components/ui/brand/ThreevhyWordmarkLetters.vue'
import { useAppEvent } from '@/composables/use-app-event'
import { debugStartup } from '@/helpers/startup-debug'

const BLOCKS = 16

const doneLoading = ref(false)
const loadingProgress = ref(0)
const message = ref()
const filledBlocks = computed(() =>
	Math.floor((Math.min(loadingProgress.value, 100) / 100) * BLOCKS),
)

const MIN_DISPLAY_MS = 500
const mountedAt = Date.now()

const loading = injectLoadingState()
onMounted(() => debugStartup('Splash mounted'))

function onAfterLeave() {
	debugStartup('Splash fade completed', { displayedMs: Date.now() - mountedAt })
	loading.setEnabled(true)
}

watch(
	[loading.barEnabled, loading.pending],
	([barEnabled, pending]) => {
		debugStartup('Splash loading state changed', { barEnabled, pending })
		if (barEnabled) {
			return
		}

		if (pending) {
			loadingProgress.value = 0
			fakeLoadingIncrease()
			return
		}

		const elapsed = Date.now() - mountedAt
		const delay = Math.max(0, MIN_DISPLAY_MS - elapsed)
		debugStartup('Splash dismissal scheduled', { delayMs: delay, displayedMs: elapsed })

		setTimeout(() => {
			if (loading.pending.value) {
				debugStartup('Splash dismissal deferred: new loading work')
				return
			}
			doneLoading.value = true
			debugStartup('Splash fade started', { displayedMs: Date.now() - mountedAt })
		}, delay)
	},
	{ immediate: true },
)

function fakeLoadingIncrease() {
	if (loadingProgress.value < 95) {
		setTimeout(() => {
			loadingProgress.value += 2
			fakeLoadingIncrease()
		}, 5)
	}
}

useAppEvent('loading', (e) => {
	if (e.event.type === 'directory_move') {
		loadingProgress.value = 100 * (e.fraction ?? 1)
		message.value = 'Updating app directory...'
	}
})
</script>

<style scoped lang="scss">
.splash-screen {
	position: fixed;
	inset: 0;
	z-index: 10000;
	overflow: hidden;
	background: #061d8c;
}

.splash-fade-leave-active {
	transition:
		opacity 0.35s ease-in,
		transform 0.35s ease-in;
}

.splash-fade-leave-to {
	opacity: 0;
	transform: scale(1.02);
}

.splash-sky {
	position: absolute;
	inset: 0;
	background: linear-gradient(
		180deg,
		#5bc8e6 0%,
		#1a4fba 7%,
		#061d8c 22%,
		#0b2a9a 50%,
		#2a6fc4 76%,
		#8cc4ea 100%
	);
}

.splash-cloud {
	position: absolute;
	height: 16px;
	background: rgba(227, 243, 252, 0.14);
	animation: splash-drift linear infinite;

	&::before {
		content: '';
		position: absolute;
		left: 32px;
		right: 44px;
		bottom: 100%;
		height: 16px;
		background: inherit;
	}
}

.c1 {
	top: 14%;
	width: 180px;
	animation-duration: 70s;
	animation-delay: -25s;
}

.c2 {
	top: 26%;
	width: 240px;
	animation-duration: 95s;
	animation-delay: -60s;
	background: rgba(227, 243, 252, 0.09);
}

.c3 {
	top: 9%;
	width: 130px;
	animation-duration: 82s;
	animation-delay: -8s;
	background: rgba(227, 243, 252, 0.11);
}

@keyframes splash-drift {
	from {
		transform: translateX(-280px);
	}
	to {
		transform: translateX(110vw);
	}
}

.splash-hills {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	width: 100%;
	height: 24%;
}

.hill-back {
	fill: rgba(140, 196, 234, 0.2);
}

.hill-front {
	fill: rgba(207, 232, 247, 0.28);
}

.splash-content {
	position: absolute;
	inset: 0;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding-bottom: 6vh;
	color: #e3f3fc;
}

.splash-wordmark {
	width: min(360px, 44vw);
	height: auto;
	filter: drop-shadow(0 5px 0 rgba(3, 14, 70, 0.55));
}

.splash-client {
	margin-top: 16px;
	padding-left: 0.6em;
	font-size: 15px;
	font-weight: 800;
	letter-spacing: 0.6em;
	color: #5bc8e6;
	animation: splash-fade-in 0.5s 0.55s ease-out both;
}

.splash-blocks {
	display: grid;
	grid-template-columns: repeat(16, 1fr);
	gap: 4px;
	width: min(320px, 60vw);
	margin-top: 40px;
	padding: 5px;
	background: rgba(2, 10, 45, 0.55);
	clip-path: var(--threevhy-pixel-corners);
	animation: splash-fade-in 0.5s 0.65s ease-out both;
}

.splash-block {
	height: 12px;
	background: rgba(227, 243, 252, 0.09);
	transition: background 0.2s;

	&.on {
		background: linear-gradient(180deg, #a6ebfa, #5bc8e6 55%, #2b8fdb);
	}
}

.splash-message {
	margin-top: 14px;
	min-height: 1.2em;
	font-size: 13px;
	color: #cfe8f7;
}

@keyframes splash-fade-in {
	from {
		opacity: 0;
	}
}

@media (prefers-reduced-motion: reduce) {
	.splash-cloud,
	.splash-client,
	.splash-blocks {
		animation: none;
	}
}
</style>
