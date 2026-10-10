<script setup lang="ts">
import {
	BoxIcon,
	LayersIcon,
	PaletteIcon,
	PlayIcon,
	PlusIcon,
	ServerIcon,
	SparklesIcon,
	SpinnerIcon,
	StopCircleIcon,
} from '@modrinth/assets'
import { defineMessages, injectNotificationManager, useVIntl } from '@modrinth/ui'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, inject, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import PixelSnow from '@/components/ui/brand/PixelSnow.vue'
import ThreevhyLogo from '@/components/ui/brand/ThreevhyLogo.vue'
import InGamePanel from '@/components/ui/home/InGamePanel.vue'
import InstancePicker from '@/components/ui/home/InstancePicker.vue'
import QuickModsPanel from '@/components/ui/home/QuickModsPanel.vue'
import ServersPanel from '@/components/ui/home/ServersPanel.vue'
import { THREEVHY_VERSIONS } from '@/components/ui/home/threevhy'
import RecentWorldsList from '@/components/ui/world/RecentWorldsList.vue'
import { useAppEvent } from '@/composables/use-app-event'
import { useAppSettings } from '@/composables/use-app-settings.ts'
import { handleSevereError } from '@/composables/use-error.js'
import { useInstanceMods } from '@/composables/use-instance-mods'
import { useSelectedInstance } from '@/composables/use-selected-instance'
import { install_create_instance, installJobInstanceId } from '@/helpers/install'
import { kill, run } from '@/helpers/instance'
import { maxMemoryQueryOptions } from '@/helpers/jre.js'
import { get_all } from '@/helpers/process'
import { appSettingsQueryOptions } from '@/helpers/settings'
import { traceStartupStep } from '@/helpers/startup-debug'
import { useRootBreadcrumb } from '@/providers/breadcrumbs'
import { injectOnboardingChecklist } from '@/providers/onboarding-checklist'

// 3vhy Client home: the banner sky with Play front and centre, and a hotbar of
// shortcuts underneath — press 1 to 5 to jump to one, like in Minecraft.
defineOptions({
	name: 'HomePage',
})

const { formatMessage } = useVIntl()
const { handleError } = injectNotificationManager()
const { hasCreatedInstance } = injectOnboardingChecklist()
const showCreationModal = inject<() => void>('showCreationModal')
const appSettings = useAppSettings()
const queryClient = useQueryClient()
const router = useRouter()

const messages = defineMessages({
	home: { id: 'app.navigation.home', defaultMessage: 'Home' },
	welcome: { id: 'app.3vhy.home.welcome', defaultMessage: 'Welcome to 3vhy Client' },
	modsOn: { id: 'app.3vhy.home.stat.mods', defaultMessage: 'mods on' },
	instances: {
		id: 'app.3vhy.home.stat.instances',
		defaultMessage: '{count, plural, one {instance} other {instances}}',
	},
	ram: { id: 'app.3vhy.home.stat.ram', defaultMessage: 'GB RAM' },
	ramTooltip: {
		id: 'app.3vhy.home.stat.ram.tooltip',
		defaultMessage:
			'Your PC has {total} GB. Minecraft can use {allocated} GB (change it in Settings).',
	},
	play: { id: 'app.3vhy.home.play', defaultMessage: 'Play' },
	stop: { id: 'app.3vhy.home.stop', defaultMessage: 'Stop' },
	installing: { id: 'app.3vhy.home.installing', defaultMessage: 'Installing' },
	createFirst: { id: 'app.3vhy.home.create-first', defaultMessage: 'Set up' },
	hotbarLabel: { id: 'app.3vhy.home.hotbar', defaultMessage: 'Find content on Modrinth' },
	hotbarKey: { id: 'app.3vhy.home.hotbar.key', defaultMessage: 'Press {key}' },
	modpacks: { id: 'app.3vhy.home.hotbar.modpacks', defaultMessage: 'Modpacks' },
	mods: { id: 'app.3vhy.home.hotbar.mods', defaultMessage: 'Mods' },
	shaders: { id: 'app.3vhy.home.hotbar.shaders', defaultMessage: 'Shaders' },
	resourcePacks: { id: 'app.3vhy.home.hotbar.resource-packs', defaultMessage: 'Packs' },
	servers: { id: 'app.3vhy.home.hotbar.servers', defaultMessage: 'Servers' },
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
// How much of it Minecraft is allowed to use (shown on hover)
const allocatedGb = computed(() => {
	const megabytes =
		selected.value?.memory?.maximum ?? settingsQuery.data.value?.memory?.maximum ?? null
	if (!megabytes) return null
	return Math.round((megabytes / 1024) * 10) / 10
})
// The PC's total memory (reported in KB)
const maxMemoryQuery = useQuery(maxMemoryQueryOptions())
const ramGb = computed(() => {
	const kilobytes = maxMemoryQuery.data.value
	if (!kilobytes) return null
	return Math.round(kilobytes / 1024 / 1024)
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

/** Makes a Fabric instance for a 3vhy-supported version; the 3vhy mod is added on launch. */
async function createThreevhyInstance(gameVersion: string) {
	const job = await install_create_instance({
		name: `3vhy Client ${gameVersion}`,
		gameVersion,
		loader: 'fabric',
		loaderVersion: 'stable',
		iconPath: null,
	}).catch(handleError)
	const instanceId = job ? installJobInstanceId(job) : null
	if (instanceId) select(instanceId)
}

async function onPlay() {
	const instance = selected.value
	if (!instance) {
		await createThreevhyInstance(THREEVHY_VERSIONS[0])
		return
	}
	if (isRunning.value) {
		await kill(instance.id).catch(handleError)
		return
	}
	if (isInstalling.value || instance.quarantined) return
	await run(instance.id).catch((error) => handleSevereError(error, { instanceId: instance.id }))
}

const hotbar = computed(() => [
	{ to: '/browse/modpack', icon: LayersIcon, label: formatMessage(messages.modpacks) },
	{ to: '/browse/mod', icon: BoxIcon, label: formatMessage(messages.mods) },
	{ to: '/browse/shader', icon: SparklesIcon, label: formatMessage(messages.shaders) },
	{ to: '/browse/resourcepack', icon: PaletteIcon, label: formatMessage(messages.resourcePacks) },
	{ to: '/browse/server', icon: ServerIcon, label: formatMessage(messages.servers) },
])

function onHotbarKey(event: KeyboardEvent) {
	if (event.ctrlKey || event.metaKey || event.altKey || event.repeat) return
	const target = event.target as HTMLElement | null
	if (
		target?.closest('input, textarea, select, [contenteditable="true"], [role="dialog"]') ||
		document.querySelector('[role="dialog"][aria-modal="true"]')
	) {
		return
	}
	const slot = hotbar.value[Number(event.key) - 1]
	if (slot) {
		event.preventDefault()
		router.push(slot.to)
	}
}

onMounted(() => window.addEventListener('keydown', onHotbarKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onHotbarKey))

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
				<PixelSnow :density="1.6" :speed="12" />
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
					<span
						v-if="ramGb"
						v-tooltip="
							allocatedGb
								? formatMessage(messages.ramTooltip, { total: ramGb, allocated: allocatedGb })
								: undefined
						"
						class="tvh-stat"
					>
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
						@create-threevhy="createThreevhyInstance"
					/>
				</div>
			</div>

			<nav class="tvh-hotbar" :aria-label="formatMessage(messages.hotbarLabel)">
				<RouterLink
					v-for="(slot, index) in hotbar"
					:key="slot.to"
					v-tooltip="formatMessage(messages.hotbarKey, { key: index + 1 })"
					:to="slot.to"
					class="tvh-slot"
				>
					<span class="tvh-slot__key" aria-hidden="true">{{ index + 1 }}</span>
					<component :is="slot.icon" class="tvh-slot__icon" />
					<span class="tvh-slot__label">{{ slot.label }}</span>
				</RouterLink>
			</nav>
		</section>

		<div v-if="instances.length > 0 && appSettings.showJumpIn" class="tvh-jump">
			<RecentWorldsList :recent-instances="instances" />
		</div>

		<Teleport to="#sidebar-teleport-target">
			<QuickModsPanel :instance="selected" />
			<InGamePanel :instance="selected" />
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
	--color-raised-bg: #0e2159;
	--color-button-bg: #16307a;
	--color-divider: rgba(140, 196, 234, 0.12);
	--color-brand: #5bc8e6;
	--color-brand-highlight: rgba(91, 200, 230, 0.2);
	position: relative;
	z-index: 2;
	min-height: 24.5rem;
	margin-bottom: 2.6rem;
}

.tvh-hero__sky {
	position: absolute;
	inset: 0;
	overflow: hidden;
	pointer-events: none;
	clip-path: var(--threevhy-pixel-corners-lg);
	background: var(--threevhy-stars), var(--threevhy-sky-panel);
	box-shadow: inset 0 2px 0 rgba(91, 200, 230, 0.55);
}

.tvh-hero__clouds {
	position: absolute;
	inset: 0;
	width: 100%;
	height: 100%;
}

.tvh-cloud {
	fill: rgba(227, 243, 252, 0.15);
	animation: tvh-drift 60s linear infinite alternate;

	&:nth-child(2) {
		animation-duration: 80s;
		fill: rgba(227, 243, 252, 0.1);
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
	padding: 2.6rem 1.5rem 4.6rem;
	text-align: center;
}

.tvh-hero__welcome {
	margin: 0;
	font-size: 1rem;
	font-weight: 500;
	color: var(--threevhy-pale);
}

.tvh-stats {
	display: flex;
	flex-wrap: wrap;
	justify-content: center;
	gap: 0.5rem;
}

.tvh-stat {
	padding: 0.32rem 0.8rem 0.38rem;
	font-size: 0.85rem;
	color: var(--threevhy-pale);
	background: rgba(4, 18, 63, 0.45);
	clip-path: var(--threevhy-pixel-corners);

	b {
		color: #f2faff;
		font-weight: 700;
	}
}

.tvh-playbar {
	position: relative;
	z-index: 5;
	display: flex;
	align-items: stretch;
	gap: 0.6rem;
	margin-top: 1.1rem;
}

.tvh-play {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	gap: 0.75rem;
	min-width: 15rem;
	padding: 0 2.2rem 4px;
	border: none;
	font-family: var(--threevhy-font-pixel);
	font-size: 1.75rem;
	font-weight: 600;
	color: #04123f;
	background: var(--threevhy-play-bg);
	clip-path: var(--threevhy-pixel-corners);
	box-shadow:
		inset 0 -6px 0 rgba(4, 18, 63, 0.4),
		inset 0 3px 0 rgba(255, 255, 255, 0.45);
	cursor: pointer;
	transition:
		transform 0.08s ease,
		filter 0.15s ease;

	svg {
		width: 1.4rem;
		height: 1.4rem;
	}

	&:hover:not(:disabled) {
		filter: brightness(1.07);
	}

	&:active:not(:disabled) {
		transform: translateY(3px);
		box-shadow:
			inset 0 -2px 0 rgba(4, 18, 63, 0.4),
			inset 0 3px 0 rgba(4, 18, 63, 0.15);
	}

	&:focus-visible {
		outline: none;
		box-shadow:
			inset 0 0 0 3px #fff,
			inset 0 -6px 0 rgba(4, 18, 63, 0.4);
	}

	&:disabled {
		cursor: progress;
		opacity: 0.8;
	}
}

.tvh-play--stop {
	color: #fff;
	background: linear-gradient(180deg, #ff8aa0 0%, #ff496e 55%, #d92a52 100%);
}

// Hotbar, sitting on the bottom edge of the sky like Minecraft's
.tvh-hotbar {
	position: absolute;
	left: 50%;
	bottom: -2.4rem;
	z-index: 3;
	display: flex;
	gap: 4px;
	padding: 6px;
	translate: -50% 0;
	background: #050c2a;
	clip-path: var(--threevhy-pixel-corners);
	box-shadow:
		inset 0 0 0 2px rgba(140, 196, 234, 0.22),
		inset 0 -4px 0 rgba(0, 0, 0, 0.35);
}

.tvh-slot {
	position: relative;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 0.3rem;
	width: 5.25rem;
	height: 4.25rem;
	color: var(--threevhy-pale);
	text-decoration: none;
	background: #0b1a4f;
	box-shadow:
		inset 3px 3px 0 rgba(0, 0, 0, 0.35),
		inset -3px -3px 0 rgba(140, 196, 234, 0.12);
	transition:
		background 0.12s,
		color 0.12s;

	&:hover,
	&:focus-visible {
		outline: none;
		color: #fff;
		background: #12286e;
		box-shadow:
			inset 0 0 0 3px #e3f3fc,
			inset 3px 3px 0 3px rgba(0, 0, 0, 0.25);
	}

	&:active {
		background: #0e2159;
	}
}

.tvh-slot__key {
	position: absolute;
	top: 4px;
	left: 6px;
	font-family: var(--threevhy-font-pixel);
	font-size: 0.72rem;
	color: rgba(207, 232, 247, 0.45);
}

.tvh-slot__icon {
	width: 1.45rem;
	height: 1.45rem;
	color: var(--threevhy-cyan);
}

.tvh-slot__label {
	font-size: 0.78rem;
	font-weight: 600;
}

@media (prefers-reduced-motion: reduce) {
	.tvh-cloud {
		animation: none;
	}

	.tvh-play:active:not(:disabled) {
		transform: none;
	}
}
</style>
