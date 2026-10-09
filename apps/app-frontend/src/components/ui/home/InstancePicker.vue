<script setup lang="ts">
import { CheckIcon, ChevronDownIcon, CompassIcon, PlusIcon } from '@modrinth/assets'
import { Avatar, defineMessages, useVIntl } from '@modrinth/ui'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getInstanceIconUrl } from '@/helpers/instance'
import type { GameInstance } from '@/helpers/types'

import { loaderLabel } from './format'

// 3vhy Client: the "VERSION" box next to Play. Each entry is an instance
// (a Minecraft version + loader + its mods), most recently played first.
defineProps<{
	instances: GameInstance[]
	selected: GameInstance | null
}>()

const emit = defineEmits<{
	select: [id: string]
	create: []
}>()

const { formatMessage } = useVIntl()
const router = useRouter()

const messages = defineMessages({
	label: { id: 'app.3vhy.picker.label', defaultMessage: 'Instance' },
	none: { id: 'app.3vhy.picker.none', defaultMessage: 'No instances yet' },
	newInstance: { id: 'app.3vhy.picker.new', defaultMessage: 'New instance' },
	browseModpacks: {
		id: 'app.3vhy.picker.browse-modpacks',
		defaultMessage: 'Get a modpack from Modrinth',
	},
})

const open = ref(false)
const root = ref<HTMLElement>()

function onDocumentPointer(event: PointerEvent) {
	if (open.value && root.value && !root.value.contains(event.target as Node)) {
		open.value = false
	}
}

function onKey(event: KeyboardEvent) {
	if (event.key === 'Escape') open.value = false
}

onMounted(() => {
	document.addEventListener('pointerdown', onDocumentPointer)
	document.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => {
	document.removeEventListener('pointerdown', onDocumentPointer)
	document.removeEventListener('keydown', onKey)
})

function choose(id: string) {
	emit('select', id)
	open.value = false
}

function create() {
	open.value = false
	emit('create')
}

function browseModpacks() {
	open.value = false
	router.push('/browse/modpack')
}

function subtitle(instance: GameInstance) {
	return `${instance.game_version} · ${loaderLabel(instance.loader)}`
}
</script>

<template>
	<div ref="root" class="tvh-picker" :class="{ 'tvh-picker--open': open }">
		<button
			class="tvh-picker__button"
			type="button"
			:aria-expanded="open"
			aria-haspopup="listbox"
			@click="open = !open"
		>
			<span class="tvh-picker__text">
				<span class="tvh-picker__label">{{ formatMessage(messages.label) }}</span>
				<span class="tvh-picker__value">
					{{ selected ? selected.name : formatMessage(messages.none) }}
				</span>
				<span v-if="selected" class="tvh-picker__sub">{{ subtitle(selected) }}</span>
			</span>
			<ChevronDownIcon class="tvh-picker__chevron" />
		</button>

		<Transition name="tvh-pop">
			<div v-if="open" class="tvh-picker__menu" role="listbox">
				<div class="tvh-picker__scroll">
					<button
						v-for="instance in instances"
						:key="instance.id"
						type="button"
						role="option"
						:aria-selected="instance.id === selected?.id"
						class="tvh-picker__option"
						@click="choose(instance.id)"
					>
						<Avatar
							:src="getInstanceIconUrl(instance.icon_path) ?? undefined"
							size="2rem"
							no-shadow
							class="!rounded-lg"
						/>
						<span class="tvh-picker__option-text">
							<span class="tvh-picker__option-name">{{ instance.name }}</span>
							<span class="tvh-picker__option-sub">{{ subtitle(instance) }}</span>
						</span>
						<CheckIcon v-if="instance.id === selected?.id" class="tvh-picker__check" />
					</button>
				</div>
				<div class="tvh-picker__actions">
					<button type="button" class="tvh-picker__action" @click="create">
						<PlusIcon /> {{ formatMessage(messages.newInstance) }}
					</button>
					<button type="button" class="tvh-picker__action" @click="browseModpacks">
						<CompassIcon /> {{ formatMessage(messages.browseModpacks) }}
					</button>
				</div>
			</div>
		</Transition>
	</div>
</template>

<style scoped lang="scss">
.tvh-picker {
	position: relative;
	min-width: 15rem;
}

.tvh-picker__button {
	display: flex;
	align-items: center;
	gap: 0.75rem;
	width: 100%;
	height: 100%;
	min-height: 4.25rem;
	padding: 0.6rem 1rem 0.6rem 1.15rem;
	border: 1px solid var(--threevhy-card-border);
	border-radius: 1rem;
	background: var(--threevhy-card-bg);
	backdrop-filter: blur(10px);
	color: var(--color-contrast);
	text-align: left;
	cursor: pointer;
	transition: border-color 0.15s ease;

	&:hover,
	.tvh-picker--open & {
		border-color: var(--color-brand);
	}
}

.tvh-picker__text {
	display: flex;
	flex-direction: column;
	min-width: 0;
	flex: 1;
}

.tvh-picker__label {
	font-size: 0.68rem;
	font-weight: 800;
	letter-spacing: 0.16em;
	text-transform: uppercase;
	color: var(--color-secondary);
}

.tvh-picker__value {
	font-size: 1.1rem;
	font-weight: 800;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.tvh-picker__sub {
	font-size: 0.78rem;
	color: var(--color-secondary);
}

.tvh-picker__chevron {
	width: 1.1rem;
	height: 1.1rem;
	flex-shrink: 0;
	transition: transform 0.2s ease;

	.tvh-picker--open & {
		transform: rotate(180deg);
	}
}

.tvh-picker__menu {
	position: absolute;
	z-index: 20;
	left: 0;
	right: 0;
	top: calc(100% + 0.5rem);
	display: flex;
	flex-direction: column;
	padding: 0.4rem;
	border-radius: 1rem;
	border: 1px solid var(--threevhy-card-border);
	background: var(--color-raised-bg);
	box-shadow: 0 18px 40px rgba(2, 8, 40, 0.5);
	min-width: 18rem;
}

.tvh-picker__scroll {
	display: flex;
	flex-direction: column;
	max-height: 16rem;
	overflow-y: auto;
}

.tvh-picker__option,
.tvh-picker__action {
	display: flex;
	align-items: center;
	gap: 0.65rem;
	width: 100%;
	padding: 0.5rem 0.6rem;
	border: none;
	border-radius: 0.7rem;
	background: transparent;
	color: var(--color-base);
	text-align: left;
	cursor: pointer;

	&:hover {
		background: var(--color-button-bg);
		color: var(--color-contrast);
	}
}

.tvh-picker__option[aria-selected='true'] {
	background: var(--color-brand-highlight);
	color: var(--color-contrast);
}

.tvh-picker__option-text {
	display: flex;
	flex-direction: column;
	min-width: 0;
	flex: 1;
}

.tvh-picker__option-name {
	font-weight: 700;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.tvh-picker__option-sub {
	font-size: 0.75rem;
	color: var(--color-secondary);
}

.tvh-picker__check {
	width: 1rem;
	height: 1rem;
	color: var(--color-brand);
}

.tvh-picker__actions {
	display: flex;
	flex-direction: column;
	margin-top: 0.3rem;
	padding-top: 0.3rem;
	border-top: 1px solid var(--color-divider);

	svg {
		width: 1rem;
		height: 1rem;
	}
}

.tvh-pop-enter-active,
.tvh-pop-leave-active {
	transition:
		opacity 0.15s ease,
		transform 0.15s ease;
}

.tvh-pop-enter-from,
.tvh-pop-leave-to {
	opacity: 0;
	transform: translateY(-6px);
}
</style>
