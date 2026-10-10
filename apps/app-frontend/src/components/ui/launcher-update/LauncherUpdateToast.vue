<script setup lang="ts">
import { DownloadIcon, RefreshCwIcon } from '@modrinth/assets'
import { Button, injectNotificationManager, useFormatBytes, useVIntl } from '@modrinth/ui'
import { watch } from 'vue'

import ThreevhyGlyph from '@/components/ui/brand/ThreevhyGlyph.vue'
import { appUpdateState, installAvailableAppUpdate, snoozeAppUpdate } from '@/providers/app-update'

import { launcherUpdateMessages as messages } from './messages'
import PixelProgressBar from './PixelProgressBar.vue'

const emit = defineEmits<{
	dismiss: []
}>()

const { formatMessage } = useVIntl()
const formatBytes = useFormatBytes()
const { handleError } = injectNotificationManager()
const {
	availableUpdate: update,
	downloading,
	downloadedBytes,
	progress,
	restarting,
} = appUpdateState

function updateNow() {
	installAvailableAppUpdate().catch(handleError)
}

function later() {
	if (update.value) {
		snoozeAppUpdate(update.value.version)
	}
	emit('dismiss')
}

watch(update, (current) => {
	if (!current && !downloading.value && !restarting.value) {
		emit('dismiss')
	}
})
</script>

<template>
	<div v-if="update" class="tvh-update">
		<div class="tvh-update__card" role="status" aria-live="polite">
			<div class="tvh-update__head">
				<div class="tvh-update__mark" aria-hidden="true">
					<RefreshCwIcon v-if="restarting" class="tvh-update__spin" />
					<ThreevhyGlyph v-else class="tvh-update__glyph" />
				</div>
				<div class="tvh-update__text">
					<template v-if="restarting">
						<h3>{{ formatMessage(messages.restarting) }}</h3>
						<p>{{ formatMessage(messages.restartingHint) }}</p>
					</template>
					<template v-else-if="downloading">
						<h3>{{ formatMessage(messages.downloading, { version: update.version }) }}</h3>
						<p>{{ formatMessage(messages.includesMod) }}</p>
					</template>
					<template v-else>
						<h3>{{ formatMessage(messages.available, { version: update.version }) }}</h3>
						<p>{{ formatMessage(messages.includesMod) }}</p>
						<p class="tvh-update__meta">
							{{ formatMessage(messages.downloadSize, { size: formatBytes(update.size, 1) }) }}
						</p>
					</template>
				</div>
			</div>

			<div v-if="downloading && !restarting" class="tvh-update__progress">
				<PixelProgressBar
					:progress="progress"
					:label="formatMessage(messages.downloading, { version: update.version })"
				/>
				<p class="tvh-update__meta">
					{{
						formatMessage(messages.downloadProgress, {
							downloaded: formatBytes(downloadedBytes, 1),
							total: formatBytes(update.size, 1),
						})
					}}
				</p>
			</div>

			<div v-else-if="!restarting" class="tvh-update__actions">
				<Button type="colored" color="brand" @click="updateNow">
					<DownloadIcon />
					{{ formatMessage(messages.updateNow) }}
				</Button>
				<Button @click="later">
					{{ formatMessage(messages.later) }}
				</Button>
			</div>
		</div>
	</div>
</template>

<style scoped lang="scss">
.tvh-update {
	filter: drop-shadow(0 14px 28px rgba(2, 8, 40, 0.55));
}

.tvh-update__card {
	display: flex;
	flex-direction: column;
	gap: 0.9rem;
	padding: 1rem 1.1rem 1.1rem;
	clip-path: var(--threevhy-pixel-corners-lg);
	background:
		linear-gradient(180deg, rgba(91, 200, 230, 0.14) 0%, rgba(91, 200, 230, 0) 3.5rem),
		var(--surface-3);
	border-top: 3px solid var(--threevhy-cyan);
}

.tvh-update__head {
	display: flex;
	align-items: flex-start;
	gap: 0.8rem;
}

.tvh-update__mark {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	width: 2.6rem;
	height: 2.6rem;
	clip-path: var(--threevhy-pixel-corners);
	color: #f2faff;
	background: linear-gradient(160deg, #3fb6e8 0%, #1a4fba 100%);
	box-shadow: var(--threevhy-block-edge);
}

.tvh-update__glyph {
	width: 1.25rem;
	height: 1.4rem;
}

.tvh-update__spin {
	width: 1.25rem;
	height: 1.25rem;
	animation: tvh-update-spin 1s linear infinite;
}

.tvh-update__text {
	display: flex;
	flex-direction: column;
	gap: 0.2rem;
	min-width: 0;

	h3 {
		margin: 0;
		font-family: var(--threevhy-font-pixel);
		font-size: 1.1rem;
		font-weight: 600;
		line-height: 1.25;
		color: var(--color-contrast);
	}

	p {
		margin: 0;
		font-size: 0.875rem;
		line-height: 1.4;
		color: var(--color-base);
	}
}

.tvh-update__meta,
.tvh-update__text .tvh-update__meta {
	margin: 0;
	font-size: 0.78rem;
	color: var(--color-secondary);
	font-variant-numeric: tabular-nums;
}

.tvh-update__progress {
	display: flex;
	flex-direction: column;
	gap: 0.35rem;
}

.tvh-update__actions {
	display: flex;
	gap: 0.5rem;
}

@keyframes tvh-update-spin {
	to {
		transform: rotate(360deg);
	}
}

@media (prefers-reduced-motion: reduce) {
	.tvh-update__spin {
		animation: none;
	}
}
</style>
