<script setup lang="ts">
import {
	ChevronRightIcon,
	GaugeIcon,
	LayersIcon,
	PaletteIcon,
	PlayIcon,
	PlusIcon,
	SpinnerIcon,
	StopCircleIcon,
} from '@modrinth/assets'
import { defineMessages, injectNotificationManager, useVIntl } from '@modrinth/ui'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, inject } from 'vue'

import ThreevhyLogo from '@/components/ui/brand/ThreevhyLogo.vue'
import { loaderLabel } from '@/components/ui/home/format'
import InstancePicker from '@/components/ui/home/InstancePicker.vue'
import QuickModsPanel from '@/components/ui/home/QuickModsPanel.vue'
import ServersPanel from '@/components/ui/home/ServersPanel.vue'
import RecentWorldsList from '@/components/ui/world/RecentWorldsList.vue'
import { useAppEvent } from '@/composables/use-app-event'
import { useAppSettings } from '@/composables/use-app-settings.ts'
import { handleSevereError } from '@/composables/use-error.js'
import { useInstanceMods } from '@/composables/use-instance-mods'
import { useSelectedInstance } from '@/composables/use-selected-instance'
import { kill, run } from '@/helpers/instance'
import { get_all } from '@/helpers/process'
import { appSettingsQueryOptions } from '@/helpers/settings'
import { traceStartupStep } from '@/helpers/startup-debug'
import { useRootBreadcrumb } from '@/providers/breadcrumbs'
import { injectOnboardingChecklist } from '@/providers/onboarding-checklist'

// 3vhy Client home: S.A.M Client's layout (big logo, Play + version picker,
// quick mods, servers, feature cards) on top of the Modrinth App's launcher.
defineOptions({
	name: 'HomePage',
})

const { formatMessage } = useVIntl()
const { handleError } = injectNotificationManager()
const { hasCreatedInstance } = injectOnboardingChecklist()
const showCreationModal = inject<() => void>('showCreationModal')
const appSettings = useAppSettings()
const queryClient = useQueryClient()

const messages = defineMessages({
	home: { id: 'app.navigation.home', defaultMessage: 'Home' },
	welcome: { id: 'app.3vhy.home.welcome', defaultMessage: 'Welcome to 3vhy Client' },
	modsOn: { id: 'app.3vhy.home.stat.mods', defaultMessage: 'mods on' },
	instances: {
		id: 'app.3vhy.home.stat.instances',
		defaultMessage: '{count, plural, one {instance} other {instances}}',
	},
	ram: { id: 'app.3vhy.home.stat.ram', defaultMessage: 'GB RAM' },
	play: { id: 'app.3vhy.home.play', defaultMessage: 'Play' },
	stop: { id: 'app.3vhy.home.stop', defaultMessage: 'Stop' },
	installing: { id: 'app.3vhy.home.installing', defaultMessage: 'Installing' },
	createFirst: { id: 'app.3vhy.home.create-first', defaultMessage: 'Create' },
	caption: {
		id: 'app.3vhy.home.caption',
		defaultMessage: '{loader} {version} · mods and modpacks straight from Modrinth',
	},
	captionEmpty: {
		id: 'app.3vhy.home.caption-empty',
		defaultMessage: 'Grab a modpack from Modrinth or create your own instance to get started',
	},
	modpacksEyebrow: { id: 'app.3vhy.home.card.modpacks.eyebrow', defaultMessage: 'Modpacks' },
	modpacksBody: {
		id: 'app.3vhy.home.card.modpacks.body',
		defaultMessage: 'Download any modpack from Modrinth in one click',
	},
	modsEyebrow: { id: 'app.3vhy.home.card.mods.eyebrow', defaultMessage: 'Performance' },
	modsBody: {
		id: 'app.3vhy.home.card.mods.body',
		defaultMessage: 'Sodium, Lithium, Entity Culling and more',
	},
	shadersEyebrow: { id: 'app.3vhy.home.card.shaders.eyebrow', defaultMessage: 'Visuals' },
	shadersBody: {
		id: 'app.3vhy.home.card.shaders.body',
		defaultMessage: 'Shaders and resource packs for a fresh look',
	},
})

useRootBreadcrumb({
	slot: 'root',
	id: 'home',
	label: formatMessage(messages.home),
	to: '/',
	visual: { type: 'icon', component: PlayIcon },
})

const { instances, selected, select, instancesQuery } = useSelectedInstance()
if (hasCreatedInstance.value) {
	await traceStartupStep('Load home instances', () => instancesQuery.suspense()).catch(handleError)
}

const selectedId = computed(() => selected.value?.id ?? null)
const { enabledCount } = useInstanceMods(selectedId)

const settingsQuery = useQuery(appSettingsQueryOptions())
const ramGb = computed(() => {
	const megabytes =
		selected.value?.memory?.maximum ?? settingsQuery.data.value?.memory?.maximum ?? null
	if (!megabytes) return null
	return Math.round((megabytes / 1024) * 10) / 10
})

type ProcessMetadata = { uuid: string; instance_id: string }
const processesQuery = useQuery({
	queryKey: ['processes', 'list'],
	queryFn: (): Promise<ProcessMetadata[]> => get_all(),
})
useAppEvent('process', () => {
	void queryClient.invalidateQueries({ queryKey: ['processes', 'list'] })
})

const isRunning = computed(
	() =>
		!!selected.value &&
		(processesQuery.data.value ?? []).some((process) => process.instance_id === selected.value?.id),
)
const isInstalling = computed(
	() =>
		selected.value?.install_stage === 'minecraft_installing' ||
		selected.value?.install_stage === 'pack_installing',
)

const playLabel = computed(() => {
	if (isInstalling.value) return formatMessage(messages.installing)
	if (isRunning.value) return formatMessage(messages.stop)
	if (!selected.value) return formatMessage(messages.createFirst)
	return formatMessage(messages.play)
})

async function onPlay() {
	const instance = selected.value
	if (!instance) {
		showCreationModal?.()
		return
	}
	if (isRunning.value) {
		await kill(instance.id).catch(handleError)
		return
	}
	if (isInstalling.value || instance.quarantined) return
	await run(instance.id).catch((error) => handleSevereError(error, { instanceId: instance.id }))
}

const caption = computed(() =>
	selected.value
		? formatMessage(messages.caption, {
				loader: loaderLabel(selected.value.loader),
				version: selected.value.game_version,
			})
		: formatMessage(messages.captionEmpty),
)

const cards = computed(() => [
	{
		to: '/browse/modpack',
		icon: LayersIcon,
		eyebrow: formatMessage(messages.modpacksEyebrow),
		body: formatMessage(messages.modpacksBody),
		tone: 'cyan',
	},
	{
		to: '/browse/mod',
		icon: GaugeIcon,
		eyebrow: formatMessage(messages.modsEyebrow),
		body: formatMessage(messages.modsBody),
		tone: 'royal',
	},
	{
		to: '/browse/shader',
		icon: PaletteIcon,
		eyebrow: formatMessage(messages.shadersEyebrow),
		body: formatMessage(messages.shadersBody),
		tone: 'sky',
	},
])

// Pixel clouds in the hero sky, in grid cells: [x, y, width]
const clouds: [number, number, number][] = [
	[3, 3, 7],
	[28, 2, 9],
	[52, 5, 6],
]
</script>

<template>
	<div class="tvh-home">
		<section class="tvh-hero">
			<div class="tvh-hero__sky" aria-hidden="true">
				<svg
					class="tvh-hero__clouds"
					viewBox="0 0 64 24"
					preserveAspectRatio="xMidYMin slice"
					shape-rendering="crispEdges"
				>
					<g v-for="([x, y, w], index) in clouds" :key="index" class="tvh-cloud">
						<rect :x="x + 2" :y="y" :width="w - 3" height="1" />
						<rect :x="x" :y="y + 1" :width="w" height="1" />
					</g>
				</svg>
				<svg
					class="tvh-hero__hills"
					viewBox="0 0 64 12"
					preserveAspectRatio="none"
					shape-rendering="crispEdges"
				>
					<path class="tvh-hill--back" d="M0 6h6v-1h7v1h5v-2h8v1h6v1h9v-2h7v1h9v1h7v6H0z" />
					<path class="tvh-hill--front" d="M0 9h9v-1h8v1h11v-1h6v1h10v-2h8v1h12v4H0z" />
				</svg>
			</div>

			<div class="tvh-hero__content">
				<ThreevhyLogo size="lg" />
				<p class="tvh-hero__welcome">{{ formatMessage(messages.welcome) }}</p>

				<div class="tvh-stats">
					<span v-if="selected" class="tvh-stat">
						<b>{{ enabledCount }}</b> {{ formatMessage(messages.modsOn) }}
					</span>
					<span class="tvh-stat">
						<b>{{ instances.length }}</b>
						{{ formatMessage(messages.instances, { count: instances.length }) }}
					</span>
					<span v-if="ramGb" class="tvh-stat">
						<b>{{ ramGb }}</b> {{ formatMessage(messages.ram) }}
					</span>
				</div>

				<div class="tvh-playbar">
					<button
						class="tvh-play"
						:class="{ 'tvh-play--stop': isRunning }"
						:disabled="isInstalling"
						@click="onPlay"
					>
						<SpinnerIcon v-if="isInstalling" class="animate-spin" />
						<StopCircleIcon v-else-if="isRunning" />
						<PlusIcon v-else-if="!selected" />
						<PlayIcon v-else />
						<span>{{ playLabel }}</span>
					</button>
					<InstancePicker
						:instances="instances"
						:selected="selected"
						@select="select"
						@create="showCreationModal?.()"
					/>
				</div>
				<p class="tvh-hero__caption">{{ caption }}</p>
			</div>
		</section>

		<section class="tvh-cards">
			<RouterLink
				v-for="card in cards"
				:key="card.to"
				:to="card.to"
				class="tvh-card"
				:class="`tvh-card--${card.tone}`"
			>
				<span class="tvh-card__icon"><component :is="card.icon" /></span>
				<span class="tvh-card__text">
					<span class="tvh-card__eyebrow">{{ card.eyebrow }}</span>
					<span class="tvh-card__body">{{ card.body }}</span>
				</span>
				<ChevronRightIcon class="tvh-card__go" />
			</RouterLink>
		</section>

		<div v-if="instances.length > 0 && appSettings.showJumpIn" class="tvh-jump">
			<RecentWorldsList :recent-instances="instances" />
		</div>

		<Teleport to="#sidebar-teleport-target">
			<QuickModsPanel :instance="selected" />
			<ServersPanel :instances="instances" :selected="selected" />
		</Teleport>
	</div>
</template>

<style scoped lang="scss">
.tvh-home {
	display: flex;
	flex-direction: column;
	gap: 1.25rem;
	padding: 1.25rem 1.5rem 2rem;
}

.tvh-hero {
	// The hero is always the dark banner sky, even in light mode
	--threevhy-ice: #e3f3fc;
	--threevhy-card-bg: rgba(10, 26, 84, 0.62);
	--threevhy-card-border: rgba(140, 196, 234, 0.16);
	--color-contrast: #e3f3fc;
	--color-base: #b4cdea;
	--color-secondary: #9fb9de;
	--color-raised-bg: #0f1f4f;
	--color-button-bg: #18306b;
	--color-divider: rgba(140, 196, 234, 0.12);
	--color-brand: #5bc8e6;
	--color-brand-highlight: rgba(91, 200, 230, 0.2);
	position: relative;
	z-index: 2;
	border-radius: 1.5rem;
	min-height: 25rem;
	background: var(--threevhy-sky-panel);
	border: 1px solid var(--threevhy-card-border);
	box-shadow: inset 0 2px 0 rgba(91, 200, 230, 0.35);
}

.tvh-hero__sky {
	position: absolute;
	inset: 0;
	overflow: hidden;
	border-radius: inherit;
	pointer-events: none;
}

.tvh-hero__clouds {
	position: absolute;
	inset: 0;
	width: 100%;
	height: 100%;
}

.tvh-cloud {
	fill: rgba(227, 243, 252, 0.16);
	animation: tvh-drift 60s linear infinite alternate;

	&:nth-child(2) {
		animation-duration: 80s;
		fill: rgba(227, 243, 252, 0.11);
	}

	&:nth-child(3) {
		animation-duration: 70s;
	}
}

@keyframes tvh-drift {
	from {
		transform: translateX(-3px);
	}
	to {
		transform: translateX(3px);
	}
}

.tvh-hero__hills {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	width: 100%;
	height: 34%;
}

.tvh-hill--back {
	fill: rgba(140, 196, 234, 0.16);
}

.tvh-hill--front {
	fill: rgba(140, 196, 234, 0.24);
}

.tvh-hero__content {
	position: relative;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 0.85rem;
	padding: 2.75rem 1.5rem 1.75rem;
	text-align: center;
}

.tvh-hero__welcome {
	margin: 0;
	font-size: 1.05rem;
	font-weight: 600;
	color: var(--threevhy-pale);
}

.tvh-stats {
	display: flex;
	flex-wrap: wrap;
	justify-content: center;
	gap: 0.5rem;
}

.tvh-stat {
	padding: 0.35rem 0.85rem;
	border-radius: 999px;
	font-size: 0.85rem;
	color: var(--threevhy-pale);
	background: rgba(4, 18, 63, 0.35);
	border: 1px solid rgba(140, 196, 234, 0.22);

	b {
		color: #f2faff;
	}
}

.tvh-playbar {
	position: relative;
	z-index: 5;
	display: flex;
	align-items: stretch;
	gap: 0.6rem;
	margin-top: 1.25rem;
	padding: 0.45rem;
	border-radius: 1.4rem;
	background: rgba(4, 18, 63, 0.4);
	border: 1px solid rgba(140, 196, 234, 0.22);
	backdrop-filter: blur(8px);
}

.tvh-play {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	gap: 0.7rem;
	min-width: 15rem;
	padding: 0 2rem;
	border: none;
	border-radius: 1rem;
	font-size: 1.45rem;
	font-weight: 900;
	letter-spacing: 0.18em;
	text-transform: uppercase;
	color: #04123f;
	background: var(--threevhy-play-bg);
	box-shadow: var(--threevhy-play-shadow);
	cursor: pointer;
	transition:
		transform 0.1s ease,
		filter 0.15s ease,
		box-shadow 0.1s ease;

	svg {
		width: 1.4rem;
		height: 1.4rem;
	}

	&:hover:not(:disabled) {
		filter: brightness(1.06);
	}

	&:active:not(:disabled) {
		transform: translateY(4px);
		box-shadow:
			0 2px 0 #0d3f99,
			0 8px 18px rgba(4, 18, 80, 0.45);
	}

	&:disabled {
		cursor: progress;
		opacity: 0.8;
	}
}

.tvh-play--stop {
	color: #fff;
	background: linear-gradient(180deg, #ff8aa0 0%, #ff496e 60%, #d92a52 100%);
	box-shadow:
		0 6px 0 #9c1736,
		0 14px 30px rgba(80, 4, 24, 0.45);
}

.tvh-hero__caption {
	margin: 0.4rem 0 0;
	font-size: 0.85rem;
	color: var(--threevhy-pale);
	opacity: 0.85;
}

.tvh-cards {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 1rem;
}

.tvh-card {
	display: flex;
	align-items: center;
	gap: 0.9rem;
	padding: 1rem 1.1rem;
	border-radius: 1.1rem;
	text-decoration: none;
	background: var(--color-raised-bg);
	border: 1px solid var(--threevhy-card-border);
	transition:
		transform 0.15s ease,
		border-color 0.15s ease;

	&:hover {
		transform: translateY(-2px);
		border-color: var(--color-brand);

		.tvh-card__go {
			opacity: 1;
		}
	}
}

.tvh-card__icon {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	width: 3rem;
	height: 3rem;
	border-radius: 0.9rem;
	color: #f2faff;

	svg {
		width: 1.5rem;
		height: 1.5rem;
	}
}

.tvh-card--cyan .tvh-card__icon {
	background: linear-gradient(160deg, #6fd3ee 0%, #2f9be0 100%);
}

.tvh-card--royal .tvh-card__icon {
	background: linear-gradient(160deg, #3f7fe0 0%, #1a4fba 100%);
}

.tvh-card--sky .tvh-card__icon {
	background: linear-gradient(160deg, #8cc4ea 0%, #2a6fc4 100%);
}

.tvh-card__text {
	display: flex;
	flex-direction: column;
	gap: 0.15rem;
	min-width: 0;
	flex: 1;
}

.tvh-card__eyebrow {
	font-size: 0.72rem;
	font-weight: 800;
	letter-spacing: 0.14em;
	text-transform: uppercase;
	color: var(--color-brand);
}

.tvh-card__body {
	font-weight: 700;
	color: var(--color-contrast);
	line-height: 1.3;
}

.tvh-card__go {
	width: 1.1rem;
	height: 1.1rem;
	flex-shrink: 0;
	color: var(--color-secondary);
	opacity: 0;
	transition: opacity 0.15s ease;
}

@media (max-width: 1180px) {
	.tvh-cards {
		grid-template-columns: 1fr;
	}
}

@media (prefers-reduced-motion: reduce) {
	.tvh-cloud {
		animation: none;
	}
}
</style>
