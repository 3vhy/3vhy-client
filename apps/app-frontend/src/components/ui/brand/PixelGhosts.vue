<template>
	<canvas ref="canvas" class="pixel-ghosts" aria-hidden="true" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * Little pixel ghosts drifting up the October backdrop, bobbing side to side, in place of the
 * snow. Fills its parent, never takes clicks, pauses while the window is hidden and stays still
 * when the system asks for reduced motion.
 */
const props = withDefaults(
	defineProps<{
		/** Ghosts per 100,000 square pixels. */
		density?: number
	}>(),
	{ density: 1.4 },
)

const SPRITE = [
	'...####...',
	'.########.',
	'##########',
	'#oo####oo#',
	'#oo####oo#',
	'##########',
	'##########',
	'##########',
	'#.##..##.#',
]

type Ghost = { x: number; y: number; scale: number; speed: number; sway: number; alpha: number }

const canvas = ref<HTMLCanvasElement | null>(null)
const ghosts: Ghost[] = []
let width = 0
let height = 0
let frame = 0
let lastTime = 0
let observer: ResizeObserver | null = null
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')

function makeGhost(startY: number): Ghost {
	return {
		x: Math.random() * width,
		y: startY,
		scale: Math.random() < 0.3 ? 3 : 2,
		speed: 6 + Math.random() * 10,
		sway: Math.random() * Math.PI * 2,
		alpha: 0.1 + Math.random() * 0.16,
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
	const wanted = Math.round(((width * height) / 100000) * props.density)
	while (ghosts.length < wanted) ghosts.push(makeGhost(Math.random() * height))
	ghosts.length = Math.min(ghosts.length, wanted)
	draw(0)
}

function drawGhost(context: CanvasRenderingContext2D, ghost: Ghost, x: number, dpr: number) {
	const pixel = Math.max(1, Math.round(ghost.scale * dpr))
	const left = Math.round(x * dpr)
	const top = Math.round(ghost.y * dpr)
	SPRITE.forEach((row, rowIndex) => {
		for (let column = 0; column < row.length; column++) {
			const part = row[column]
			if (part === '.') continue
			context.fillStyle =
				part === 'o'
					? `rgba(34, 28, 48, ${ghost.alpha * 1.6})`
					: `rgba(143, 132, 173, ${ghost.alpha})`
			context.fillRect(left + column * pixel, top + rowIndex * pixel, pixel, pixel)
		}
	})
}

function draw(seconds: number) {
	const element = canvas.value
	const context = element?.getContext('2d')
	if (!element || !context) return
	const dpr = window.devicePixelRatio || 1
	context.clearRect(0, 0, element.width, element.height)
	const time = performance.now() / 1000
	for (const ghost of ghosts) {
		ghost.y -= ghost.speed * seconds
		if (ghost.y < -40) Object.assign(ghost, makeGhost(height + 10))
		const x = ghost.x + Math.sin(time * 0.7 + ghost.sway) * 10
		drawGhost(context, ghost, x, dpr)
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
.pixel-ghosts {
	position: absolute;
	inset: 0;
	width: 100%;
	height: 100%;
	pointer-events: none;
	image-rendering: pixelated;
}
</style>
