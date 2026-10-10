import { computed, ref } from 'vue'

import {
	checkForLauncherUpdate,
	installLauncherUpdate,
	type LauncherUpdate,
} from '@/helpers/launcher-update'
import type { AppEvents } from '@/providers/app-events'

export type { LauncherUpdate }

/** How long "Later" keeps the update toast away for the same version */
export const APP_UPDATE_SNOOZE_MS = 24 * 60 * 60 * 1000

const SNOOZE_STORAGE_KEY = '3vhy-app-update-snooze'

interface UpdateSnooze {
	version: string
	until: number
}

const availableUpdate = ref<LauncherUpdate | null>(null)
const checking = ref(false)
const checkedAt = ref<number | null>(null)
const checkError = ref<string | null>(null)
const downloading = ref(false)
const progress = ref(0)
const restarting = ref(false)

export const appUpdateState = {
	availableUpdate,
	checking,
	checkedAt,
	checkError,
	downloading,
	progress,
	restarting,
	downloadPercent: computed(() => Math.trunc(progress.value * 100)),
	downloadedBytes: computed(() => Math.round(progress.value * (availableUpdate.value?.size ?? 0))),
	isVisible: computed(() => !!availableUpdate.value && !restarting.value),
}

let runningCheck: Promise<LauncherUpdate | null> | null = null

/** Asks GitHub whether a newer 3vhy Client is out. Calls made while one is running share it. */
export function checkForAppUpdate(): Promise<LauncherUpdate | null> {
	runningCheck ??= (async () => {
		checking.value = true
		try {
			const update = await checkForLauncherUpdate()
			if (!downloading.value) {
				availableUpdate.value = update
			}
			checkError.value = null
			return update
		} catch (error) {
			checkError.value = errorMessage(error)
			throw error
		} finally {
			checking.value = false
			checkedAt.value = Date.now()
			runningCheck = null
		}
	})()
	return runningCheck
}

/**
 * Downloads the newest release and runs its installer, which closes 3vhy Client and opens it
 * again on the new version. The in-game mod is bundled, so it's updated along with it.
 */
export async function installAvailableAppUpdate(): Promise<void> {
	if (downloading.value || restarting.value) {
		return
	}

	downloading.value = true
	progress.value = 0
	try {
		await installLauncherUpdate()
		restarting.value = true
	} catch (error) {
		progress.value = 0
		throw error
	} finally {
		downloading.value = false
	}
}

/** Follows the installer download, which the backend reports as a launcher update loading bar. */
export function trackAppUpdateDownload(events: AppEvents): () => void {
	return events.on('loading', (event) => {
		if (event.event.type === 'launcher_update' && downloading.value) {
			progress.value = event.fraction ?? 1
		}
	})
}

function errorMessage(error: unknown): string {
	if (error && typeof error === 'object' && 'message' in error) {
		return String(error.message)
	}
	return String(error)
}

function readSnooze(): UpdateSnooze | null {
	try {
		const raw = localStorage.getItem(SNOOZE_STORAGE_KEY)
		const parsed = raw ? (JSON.parse(raw) as Partial<UpdateSnooze>) : null
		if (typeof parsed?.version !== 'string' || typeof parsed.until !== 'number') {
			return null
		}
		return { version: parsed.version, until: parsed.until }
	} catch {
		return null
	}
}

/** Keeps the update toast for this version from showing again for a while. */
export function snoozeAppUpdate(version: string, now = Date.now()): void {
	try {
		localStorage.setItem(
			SNOOZE_STORAGE_KEY,
			JSON.stringify({ version, until: now + APP_UPDATE_SNOOZE_MS } satisfies UpdateSnooze),
		)
	} catch (error) {
		console.warn('Failed to remember the update snooze:', error)
	}
}

export function isAppUpdateSnoozed(version: string, now = Date.now()): boolean {
	const snooze = readSnooze()
	return snooze?.version === version && now < snooze.until
}
