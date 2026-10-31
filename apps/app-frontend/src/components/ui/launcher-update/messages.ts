import { defineMessages } from '@modrinth/ui'

export const launcherUpdateMessages = defineMessages({
	available: {
		id: 'app.Eventide.update.available',
		defaultMessage: 'Eventide {version} is available',
	},
	includesMod: {
		id: 'app.Eventide.update.includes-mod',
		defaultMessage: 'Includes the latest in-game Eventide mod, so your instances get it too.',
	},
	downloadSize: {
		id: 'app.Eventide.update.download-size',
		defaultMessage: '{size} download',
	},
	updateNow: {
		id: 'app.Eventide.update.update-now',
		defaultMessage: 'Update now',
	},
	later: {
		id: 'app.Eventide.update.later',
		defaultMessage: 'Later',
	},
	downloading: {
		id: 'app.Eventide.update.downloading',
		defaultMessage: 'Downloading Eventide {version}',
	},
	downloadProgress: {
		id: 'app.Eventide.update.download-progress',
		defaultMessage: '{downloaded} of {total}',
	},
	restarting: {
		id: 'app.Eventide.update.restarting',
		defaultMessage: 'Restarting…',
	},
	restartingHint: {
		id: 'app.Eventide.update.restarting-hint',
		defaultMessage: "Eventide closes to install the update and opens again when it's done.",
	},
})
