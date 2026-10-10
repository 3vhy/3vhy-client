import { invoke } from '@tauri-apps/api/core'

/** A 3vhy Client release on GitHub that's newer than the running app. */
export interface LauncherUpdate {
	version: string
	name: string | null
	/** Release notes, in Markdown */
	notes: string
	published_at: string | null
	/** Download URL of the installer */
	url: string
	/** Size of the installer in bytes */
	size: number
	/** The release page on GitHub */
	page_url: string | null
}

export async function checkForLauncherUpdate(): Promise<LauncherUpdate | null> {
	return await invoke('plugin:launcher-update|check_for_3vhy_update')
}

/**
 * Downloads the newest release and starts its installer. 3vhy Client closes while the
 * installer runs and opens again afterwards, so this rarely resolves.
 */
export async function installLauncherUpdate(): Promise<void> {
	return await invoke('plugin:launcher-update|install_3vhy_update')
}
