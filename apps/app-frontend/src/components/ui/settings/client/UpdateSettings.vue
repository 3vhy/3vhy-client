<script setup lang="ts">
import {
	CheckCircleIcon,
	DownloadIcon,
	ExternalIcon,
	IssuesIcon,
	RefreshCwIcon,
} from '@modrinth/assets'
import {
	Button,
	defineMessages,
	injectNotificationManager,
	useFormatBytes,
	useFormatDateTime,
	useRelativeTime,
	useVIntl,
} from '@modrinth/ui'
import { renderString } from '@modrinth/utils/parse'
import { getVersion } from '@tauri-apps/api/app'
import { computed, onMounted, ref } from 'vue'

import ThreevhyGlyph from '@/components/ui/brand/ThreevhyGlyph.vue'
import { launcherUpdateMessages } from '@/components/ui/launcher-update/messages'
import PixelProgressBar from '@/components/ui/launcher-update/PixelProgressBar.vue'
import {
	appUpdateState,
	checkForAppUpdate,
	installAvailableAppUpdate,
} from '@/providers/app-update'

const { formatMessage } = useVIntl()
const { handleError } = injectNotificationManager()
const formatBytes = useFormatBytes()
const formatDate = useFormatDateTime({ dateStyle: 'long' })
const formatRelativeTime = useRelativeTime()

const {
	availableUpdate: update,
	checkedAt,
	checkError,
	checking,
	downloadedBytes,
	downloading,
	progress,
	restarting,
} = appUpdateState

const messages = defineMessages({
	title: {
		id: 'app.3vhy.settings.updates.title',
		defaultMessage: '3vhy Client',
	},
	version: {
		id: 'app.3vhy.settings.updates.version',
		defaultMessage: 'Version {version}',
	},
	check: {
		id: 'app.3vhy.settings.updates.check',
		defaultMessage: 'Check for updates',
	},
	checking: {
		id: 'app.3vhy.settings.updates.checking',
		defaultMessage: 'Checking for updates…',
	},
	upToDate: {
		id: 'app.3vhy.settings.updates.up-to-date',
		defaultMessage: "You're on the latest version",
	},
	checked: {
		id: 'app.3vhy.settings.updates.checked',
		defaultMessage: 'Checked {time}',
	},
	checkFailed: {
		id: 'app.3vhy.settings.updates.check-failed',
		defaultMessage: "Couldn't check for updates",
	},
	released: {
		id: 'app.3vhy.settings.updates.released',
		defaultMessage: 'Released {date}',
	},
	whatsNew: {
		id: 'app.3vhy.settings.updates.whats-new',
		defaultMessage: "What's new",
	},
	viewOnGitHub: {
		id: 'app.3vhy.settings.updates.view-on-github',
		defaultMessage: 'View on GitHub',
	},
	automatic: {
		id: 'app.3vhy.settings.updates.automatic',
		defaultMessage:
			'3vhy Client looks for updates when it starts and every few hours. Updating also brings the latest in-game 3vhy mod to your instances.',
	},
})

const currentVersion = ref<string | null>(null)

const busy = computed(() => checking.value || downloading.value || restarting.value)
const renderedNotes = computed(() => (update.value?.notes ? renderString(update.value.notes) : ''))
const releaseDetails = computed(() => {
	if (!update.value) {
		return ''
	}
	const size = formatMessage(launcherUpdateMessages.downloadSize, {
		size: formatBytes(update.value.size, 1),
	})
	if (!update.value.published_at) {
		return size
	}
	const released = formatMessage(messages.released, {
		date: formatDate(update.value.published_at),
	})
	return `${released} · ${size}`
})

function check() {
	checkForAppUpdate().catch((error) => console.warn('Failed to check for updates:', error))
}

function updateNow() {
	installAvailableAppUpdate().catch(handleError)
}

onMounted(async () => {
	if (checkedAt.value === null && !checking.value) {
		check()
	}
	currentVersion.value = await getVersion()
})
</script>

<template>
	<div class="flex flex-col gap-5">
		<section class="tvh-about">
			<div class="tvh-about__mark" aria-hidden="true">
				<ThreevhyGlyph class="tvh-about__glyph" />
			</div>
			<div class="tvh-about__text">
				<h2>{{ formatMessage(messages.title) }}</h2>
				<p v-if="currentVersion">
					{{ formatMessage(messages.version, { version: currentVersion }) }}
				</p>
			</div>
			<Button :disabled="busy" @click="check">
				<RefreshCwIcon :class="{ 'tvh-spin': checking }" />
				{{ formatMessage(messages.check) }}
			</Button>
		</section>

		<section v-if="update" class="tvh-release" aria-live="polite">
			<div class="tvh-release__head">
				<div>
					<h3>
						{{ formatMessage(launcherUpdateMessages.available, { version: update.version }) }}
					</h3>
					<p class="tvh-release__meta">{{ releaseDetails }}</p>
				</div>
			</div>
			<p class="tvh-release__mod">{{ formatMessage(launcherUpdateMessages.includesMod) }}</p>

			<div v-if="restarting" class="tvh-status">
				<RefreshCwIcon class="tvh-status__icon tvh-spin" />
				<div>
					<p class="tvh-status__title">{{ formatMessage(launcherUpdateMessages.restarting) }}</p>
					<p class="tvh-status__detail">
						{{ formatMessage(launcherUpdateMessages.restartingHint) }}
					</p>
				</div>
			</div>
			<div v-else-if="downloading" class="tvh-release__progress">
				<PixelProgressBar
					:progress="progress"
					:label="formatMessage(launcherUpdateMessages.downloading, { version: update.version })"
				/>
				<p class="tvh-release__meta">
					{{
						formatMessage(launcherUpdateMessages.downloadProgress, {
							downloaded: formatBytes(downloadedBytes, 1),
							total: formatBytes(update.size, 1),
						})
					}}
				</p>
			</div>
			<div v-else class="tvh-release__actions">
				<Button type="colored" color="brand" @click="updateNow">
					<DownloadIcon />
					{{ formatMessage(launcherUpdateMessages.updateNow) }}
				</Button>
				<a v-if="update.page_url" :href="update.page_url" class="tvh-release__link">
					{{ formatMessage(messages.viewOnGitHub) }}
					<ExternalIcon aria-hidden="true" />
				</a>
			</div>

			<div v-if="renderedNotes" class="tvh-release__notes">
				<h4>{{ formatMessage(messages.whatsNew) }}</h4>
				<!-- eslint-disable-next-line vue/no-v-html -->
				<div class="markdown-body" v-html="renderedNotes" />
			</div>
		</section>

		<div v-else-if="checking" class="tvh-status">
			<RefreshCwIcon class="tvh-status__icon tvh-spin" />
			<p class="tvh-status__title">{{ formatMessage(messages.checking) }}</p>
		</div>

		<div v-else-if="checkError" class="tvh-status tvh-status--error">
			<IssuesIcon class="tvh-status__icon" />
			<div>
				<p class="tvh-status__title">{{ formatMessage(messages.checkFailed) }}</p>
				<p class="tvh-status__detail">{{ checkError }}</p>
			</div>
		</div>

		<div v-else-if="checkedAt !== null" class="tvh-status tvh-status--ok">
			<CheckCircleIcon class="tvh-status__icon" />
			<div>
				<p class="tvh-status__title">{{ formatMessage(messages.upToDate) }}</p>
				<p class="tvh-status__detail">
					{{ formatMessage(messages.checked, { time: formatRelativeTime(checkedAt) }) }}
				</p>
			</div>
		</div>

		<p class="m-0 text-sm text-secondary">{{ formatMessage(messages.automatic) }}</p>
	</div>
</template>

<style scoped lang="scss">
.tvh-about {
	display: flex;
	align-items: center;
	gap: 1rem;
	padding: 1rem 1.1rem;
	clip-path: var(--threevhy-pixel-corners-lg);
	background: var(--threevhy-sky-panel);
	color: #f2faff;
}

.tvh-about__mark {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	width: 3.25rem;
	height: 3.25rem;
	clip-path: var(--threevhy-pixel-corners);
	background: rgba(4, 18, 63, 0.45);
	box-shadow: inset 0 -3px 0 rgba(2, 8, 40, 0.4);
}

.tvh-about__glyph {
	width: 1.6rem;
	height: 1.8rem;
	color: var(--threevhy-ice);
}

.tvh-about__text {
	flex: 1;
	min-width: 0;

	h2 {
		margin: 0;
		font-size: 1.5rem;
		line-height: 1.1;
		color: #f2faff;
	}

	p {
		margin: 0.2rem 0 0;
		font-weight: 600;
		color: var(--threevhy-pale);
	}
}

.tvh-release {
	display: flex;
	flex-direction: column;
	gap: 0.75rem;
	padding: 1rem 1.1rem 1.1rem;
	clip-path: var(--threevhy-pixel-corners-lg);
	background:
		linear-gradient(180deg, rgba(91, 200, 230, 0.14) 0%, rgba(91, 200, 230, 0) 4rem),
		var(--threevhy-card-bg);
	border-top: 3px solid var(--threevhy-cyan);
}

.tvh-release__head h3 {
	margin: 0;
	font-family: var(--threevhy-font-pixel);
	font-size: 1.2rem;
	font-weight: 600;
	color: var(--color-contrast);
}

.tvh-release__meta {
	margin: 0.15rem 0 0;
	font-size: 0.8rem;
	color: var(--color-secondary);
	font-variant-numeric: tabular-nums;
}

.tvh-release__mod {
	margin: 0;
	color: var(--color-base);
}

.tvh-release__progress {
	display: flex;
	flex-direction: column;
	gap: 0.35rem;
	max-width: 28rem;
}

.tvh-release__actions {
	display: flex;
	align-items: center;
	gap: 1rem;
}

.tvh-release__link {
	display: inline-flex;
	align-items: center;
	gap: 0.3rem;
	font-size: 0.875rem;
	font-weight: 700;
	color: var(--color-brand);
	text-decoration: none;

	&:hover {
		text-decoration: underline;
	}

	svg {
		width: 0.9rem;
		height: 0.9rem;
	}
}

.tvh-release__notes {
	display: flex;
	flex-direction: column;
	gap: 0.4rem;
	padding-top: 0.75rem;
	border-top: 1px solid var(--color-divider);

	h4 {
		margin: 0;
		font-size: 0.75rem;
		font-weight: 700;
		letter-spacing: 0.06em;
		text-transform: uppercase;
		color: var(--color-secondary);
	}

	.markdown-body {
		max-height: 14rem;
		overflow-y: auto;
		font-size: 0.875rem;

		:deep(:is(h1, h2, h3)) {
			margin: 0.5rem 0 0.25rem;
			padding: 0;
			border: none;
			font-size: 1rem;
		}

		:deep(> :first-child) {
			margin-top: 0;
		}
	}
}

.tvh-status {
	display: flex;
	align-items: flex-start;
	gap: 0.7rem;
	padding: 0.85rem 1rem;
	border-radius: 0.375rem;
	background: var(--threevhy-card-bg);
	border: 1px solid var(--threevhy-card-border);

	p {
		margin: 0;
	}
}

.tvh-status__icon {
	flex-shrink: 0;
	width: 1.25rem;
	height: 1.25rem;
	margin-top: 0.05rem;
	color: var(--threevhy-sky);
}

.tvh-status__title {
	font-weight: 700;
	color: var(--color-contrast);
}

.tvh-status .tvh-status__detail {
	margin-top: 0.15rem;
	font-size: 0.8rem;
	color: var(--color-secondary);
}

.tvh-status--ok .tvh-status__icon {
	color: var(--color-green);
}

.tvh-status--error .tvh-status__icon {
	color: var(--color-orange);
}

.tvh-spin {
	animation: tvh-settings-spin 1s linear infinite;
}

@keyframes tvh-settings-spin {
	to {
		transform: rotate(360deg);
	}
}

@media (prefers-reduced-motion: reduce) {
	.tvh-spin {
		animation: none;
	}
}
</style>
