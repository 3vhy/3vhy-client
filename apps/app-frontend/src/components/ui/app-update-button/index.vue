<template>
	<Button
		v-if="isUpdateVisible && update"
		v-tooltip="formatMessage(launcherUpdateMessages.available, { version: update.version })"
		type="outlined"
		native-type="button"
		class="!h-[34px] text-sm !transition-[opacity,transform,background-color,color,filter] !duration-200 ease-out !text-brand [&>svg]:!text-inherit !shadow-[inset_0_0_0_1px_var(--color-brand)] hover:!bg-brand focus-visible:!bg-brand hover:!text-[var(--color-accent-contrast)] focus-visible:!text-[var(--color-accent-contrast)]"
		:disabled="downloading"
		:aria-busy="downloading"
		@click="handleUpdateClick"
	>
		<DownloadIcon />
		<span v-if="downloading">
			{{ formatMessage(messages.downloadingUpdate) }}
			<span class="inline-block w-[3ch] text-right tabular-nums">{{ downloadPercent }}%</span>
		</span>
		<span v-else>{{ formatMessage(messages.update) }}</span>
	</Button>
</template>

<script setup lang="ts">
import { DownloadIcon } from '@modrinth/assets'
import { Button, defineMessages, injectNotificationManager, useVIntl } from '@modrinth/ui'

import { launcherUpdateMessages } from '@/components/ui/launcher-update/messages'
import { appUpdateState, installAvailableAppUpdate } from '@/providers/app-update'

const { formatMessage } = useVIntl()
const { handleError } = injectNotificationManager()

const messages = defineMessages({
	update: {
		id: 'app.action-bar.update',
		defaultMessage: 'Update',
	},
	downloadingUpdate: {
		id: 'app.action-bar.downloading-update',
		defaultMessage: 'Downloading update',
	},
})

const {
	availableUpdate: update,
	downloading,
	downloadPercent,
	isVisible: isUpdateVisible,
} = appUpdateState

function handleUpdateClick() {
	if (downloading.value) {
		return
	}
	installAvailableAppUpdate().catch(handleError)
}
</script>
