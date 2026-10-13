import { computed, ref, watch } from 'vue'

/**
 * 3vhy Client's October look: white, with ghosts. It switches itself on in October and off again
 * in November. Turning it off in October keeps it off until next October; turning it on in
 * another month keeps it on until it's turned off. While it's on, `html` gets the
 * `threevhy-october` class and the light colours underneath it.
 */
const STORAGE_KEY = 'threevhy-october'
const today = new Date()
const isOctober = today.getMonth() === 9
const offThisOctober = `off-${today.getFullYear()}`

function loadChoice(): string {
	try {
		return window.localStorage.getItem(STORAGE_KEY) ?? 'auto'
	} catch {
		return 'auto'
	}
}

const choice = ref(loadChoice())
const active = computed(
	() => choice.value === 'on' || (isOctober && choice.value !== offThisOctober),
)

watch(
	active,
	(on) => {
		document.documentElement.classList.toggle('threevhy-october', on)
	},
	{ immediate: true },
)

function setEnabled(on: boolean): void {
	choice.value = on ? (isOctober ? 'auto' : 'on') : isOctober ? offThisOctober : 'auto'
	try {
		window.localStorage.setItem(STORAGE_KEY, choice.value)
	} catch {
		// storage blocked or full
	}
}

export const octoberActive = active

export function useOctober() {
	return { active, isOctober, setEnabled }
}
