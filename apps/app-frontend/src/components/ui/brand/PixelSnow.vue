<template>
	<canvas ref="canvas" class="pixel-snow" aria-hidden="true" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * Pixel snow: the square flakes of the 3vhy night sky, falling slowly with a gentle sway.
 * Fills its parent, never takes clicks, pauses while the window is hidden and stays still
 * when the system asks for reduced motion.
 */
const props = withDefaults(
	defineProps<{
		/** Flakes per 10,000 square pixels. */
		density?: number
		/** Fall speed in pixels per second, before each flake's own variation. */
		speed?: number
	}>(),
	{ density: 0.9, speed: 16 },
)

type Flake = { x: number; y: number; size: number; speed: number; sway: number; alpha: number }

const canvas = ref<HTMLCanvasElement | null>(null)
const flakes: Flake[] = []
let width = 0
let height = 0
let frame = 0
let lastTime = 0
let observer: ResizeObserver | null = null
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')

function makeFlake(startY: number): Flake {
	return {
		x: Math.random() * width,
		y: startY,
		size: Math.random() < 0.28 ? 2 : 1,
		speed: props.speed * (0.55 + Math.random() * 0.9),
		sway: Math.random() * Math.PI * 2,
		alpha: 0.18 + Math.random() * 0.45,
	}
}

function resize() {
	const element = canvas.value
	if (!element) return
	const dpr = window.devicePixelRatio || 1
	width = element.clientWidth
	height = element.clientHeight
	element.width = Math.round(width * dpr)
	element.height = Math.round(height * dpr)
	const wanted = Math.round(((width * height) / 10000) * props.density)
	while (flakes.length < wanted) flakes.push(makeFlake(Math.random() * height))
	flakes.length = Math.min(flakes.length, wanted)
	draw(0)
}

function draw(seconds: number) {
	const element = canvas.value
	const context = element?.getContext('2d')
	if (!element || !context) return
	const dpr = window.devicePixelRatio || 1
	const pixel = Math.max(1, Math.round(dpr * 2))
	context.clearRect(0, 0, element.width, element.height)
	const time = performance.now() / 1000
	for (const flake of flakes) {
		flake.y += flake.speed * flake.size * 0.7 * seconds
		if (flake.y > height + 4) Object.assign(flake, makeFlake(-4))
		const x = flake.x + Math.sin(time * 0.6 + flake.sway) * 6
		context.fillStyle = `rgba(227, 243, 252, ${flake.alpha})`
		context.fillRect(
			Math.round(x * dpr),
			Math.round(flake.y * dpr),
			pixel * flake.size,
			pixel * flake.size,
		)
	}
}

function loop(now: number) {
	const seconds = lastTime ? Math.min(0.1, (now - lastTime) / 1000) : 0
	lastTime = now
	if (!document.hidden) draw(seconds)
	frame = requestAnimationFrame(loop)
}

onMounted(() => {
	resize()
	if (canvas.value) {
		observer = new ResizeObserver(resize)
		observer.observe(canvas.value)
	}
	if (!reducedMotion.matches) frame = requestAnimationFrame(loop)
})

onBeforeUnmount(() => {
	cancelAnimationFrame(frame)
	observer?.disconnect()
})
</script>

<style scoped>
.pixel-snow {
	position: absolute;
	inset: 0;
	width: 100%;
	height: 100%;
	pointer-events: none;
	image-rendering: pixelated;
}

:global(html.light-mode) .pixel-snow {
	display: none;
}
</style>
