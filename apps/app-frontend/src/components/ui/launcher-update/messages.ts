import { defineMessages } from '@modrinth/ui'

export const launcherUpdateMessages = defineMessages({
	available: {
		id: 'app.3vhy.update.available',
		defaultMessage: '3vhy Client {version} is available',
	},
	includesMod: {
		id: 'app.3vhy.update.includes-mod',
		defaultMessage: 'Includes the latest in-game 3vhy mod, so your instances get it too.',
	},
	downloadSize: {
		id: 'app.3vhy.update.download-size',
		defaultMessage: '{size} download',
	},
	updateNow: {
		id: 'app.3vhy.update.update-now',
		defaultMessage: 'Update now',
	},
	later: {
		id: 'app.3vhy.update.later',
		defaultMessage: 'Later',
	},
	downloading: {
		id: 'app.3vhy.update.downloading',
		defaultMessage: 'Downloading 3vhy Client {version}',
	},
	downloadProgress: {
		id: 'app.3vhy.update.download-progress',
		defaultMessage: '{downloaded} of {total}',
	},
	restarting: {
		id: 'app.3vhy.update.restarting',
		defaultMessage: 'Restarting…',
	},
	restartingHint: {
		id: 'app.3vhy.update.restarting-hint',
		defaultMessage: "3vhy Client closes to install the update and opens again when it's done.",
	},
})
