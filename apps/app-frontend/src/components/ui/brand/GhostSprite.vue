<template>
	<svg
		class="ghost-sprite"
		:class="{ 'ghost-sprite--left': facing === 'left' }"
		:viewBox="`0 0 ${COLUMNS} ${ROWS.length}`"
		:style="{ width: `${size}px`, height: `${(size * ROWS.length) / COLUMNS}px` }"
		shape-rendering="crispEdges"
		aria-hidden="true"
	>
		<rect
			v-for="(cell, index) in cells"
			:key="index"
			:x="cell.x"
			:y="cell.y"
			:width="cell.w"
			height="1"
			:class="`ghost-sprite__${cell.part}`"
		/>
	</svg>
</template>

<script setup lang="ts">
/**
 * A pixel ghost for the October look: white, with lilac shading on its right side and tail,
 * dark eyes with a glint, rosy cheeks and a little "o" of a mouth.
 */
withDefaults(defineProps<{ size?: number; facing?: 'left' | 'right' }>(), {
	size: 64,
	facing: 'right',
})

// # body, s shade, o eyes and mouth, g glint, b blush, . empty
const ROWS = [
	'......####......',
	'....########....',
	'...##########s..',
	'..###########ss.',
	'.############ss.',
	'.############sss',
	'.##go####go##sss',
	'###oo####oo##sss',
	'###oo####oo##sss',
	'#bb########bbsss',
	'#######oo####sss',
	'#######oo####sss',
	'#############sss',
	'############ssss',
	'###########sssss',
	'##ssssssssssssss',
	'##.####..####.ss',
	'#...##....##...s',
]
const COLUMNS = 16

const PARTS: Record<string, string> = { '#': 'body', s: 'shade', o: 'eye', g: 'glint', b: 'blush' }

type Cell = { x: number; y: number; w: number; part: string }

/** Runs of the same colour on each row, so the SVG stays small. */
const cells: Cell[] = []
ROWS.forEach((row, y) => {
	let x = 0
	while (x < row.length) {
		const part = PARTS[row[x]]
		let end = x + 1
		while (end < row.length && row[end] === row[x]) end++
		if (part) cells.push({ x, y, w: end - x, part })
		x = end
	}
})
</script>

<style scoped lang="scss">
.ghost-sprite {
	display: block;
	overflow: visible;
	filter: drop-shadow(0 6px 0 rgba(122, 110, 152, 0.18));
}

.ghost-sprite--left {
	transform: scaleX(-1);
}

.ghost-sprite__body {
	fill: #ffffff;
}

.ghost-sprite__shade {
	fill: #e6e0f1;
}

.ghost-sprite__eye {
	fill: #221c30;
}

.ghost-sprite__glint {
	fill: #ffffff;
}

.ghost-sprite__blush {
	fill: #f7b9cb;
}
</style>
