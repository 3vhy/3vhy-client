<script setup lang="ts">
import { BoxIcon } from '@modrinth/assets'
import {
	Avatar,
	type ContentItem,
	defineMessages,
	injectNotificationManager,
	Toggle,
	useVIntl,
} from '@modrinth/ui'
import { computed, ref } from 'vue'

import { modDisplayName, useInstanceMods } from '@/composables/use-instance-mods'
import { toggle_disable_project } from '@/helpers/instance'
import type { GameInstance } from '@/helpers/types'

// 3vhy Client: S.A.M-style "Quick mods" — flip mods in the selected instance on/off
// without opening the full content page.
const props = defineProps<{
	instance: GameInstance | null
}>()

const MAX_SHOWN = 6

const { formatMessage } = useVIntl()
const { handleError } = injectNotificationManager()

const messages = defineMessages({
	title: { id: 'app.3vhy.quick-mods.title', defaultMessage: 'Quick mods' },
	allMods: { id: 'app.3vhy.quick-mods.all', defaultMessage: 'All mods' },
	empty: {
		id: 'app.3vhy.quick-mods.empty',
		defaultMessage: 'No mods in this instance yet.',
	},
	browse: { id: 'app.3vhy.quick-mods.browse', defaultMessage: 'Browse mods' },
	noInstance: {
		id: 'app.3vhy.quick-mods.no-instance',
		defaultMessage: 'Pick an instance to see its mods.',
	},
	toggleLabel: { id: 'app.3vhy.quick-mods.toggle', defaultMessage: 'Enable {name}' },
})

const instanceId = computed(() => props.instance?.id ?? null)
const { query, mods, refresh } = useInstanceMods(instanceId)
const shown = computed(() => mods.value.slice(0, MAX_SHOWN))
const pending = ref(new Set<string>())

function iconFor(item: ContentItem) {
	return item.project?.icon_url ?? item.embedded_metadata?.icon_url ?? undefined
}

function canToggle(item: ContentItem) {
	return !!item.file_path && !item.toggleDisabled && !pending.value.has(item.file_name)
}

async function setEnabled(item: ContentItem, enabled: boolean) {
	if (!instanceId.value || !item.file_path || !canToggle(item)) return
	pending.value = new Set(pending.value).add(item.file_name)
	try {
		await toggle_disable_project(instanceId.value, item.file_path, enabled)
	} catch (error) {
		handleError(error as Error)
	} finally {
		const next = new Set(pending.value)
		next.delete(item.file_name)
		pending.value = next
		await refresh()
	}
}
</script>

<template>
	<section class="tvh-panel">
		<header class="tvh-panel__header">
			<h3>{{ formatMessage(messages.title) }}</h3>
			<RouterLink
				v-if="instance"
				:to="`/instance/${encodeURIComponent(instance.id)}/projects/mod`"
				class="tvh-panel__link"
			>
				{{ formatMessage(messages.allMods) }}
			</RouterLink>
		</header>

		<p v-if="!instance" class="tvh-panel__empty">{{ formatMessage(messages.noInstance) }}</p>
		<div v-else-if="query.isLoading.value" class="tvh-panel__list">
			<div v-for="n in 4" :key="n" class="tvh-row tvh-row--skeleton" />
		</div>
		<div v-else-if="shown.length === 0" class="tvh-panel__empty">
			<p>{{ formatMessage(messages.empty) }}</p>
			<RouterLink to="/browse/mod" class="tvh-panel__link">
				{{ formatMessage(messages.browse) }}
			</RouterLink>
		</div>
		<ul v-else class="tvh-panel__list">
			<li v-for="mod in shown" :key="mod.file_name" class="tvh-row">
				<span class="tvh-row__icon">
					<Avatar v-if="iconFor(mod)" :src="iconFor(mod)" size="1.75rem" no-shadow />
					<BoxIcon v-else />
				</span>
				<span class="tvh-row__name" :title="modDisplayName(mod)">{{ modDisplayName(mod) }}</span>
				<Toggle
					:model-value="mod.enabled !== false"
					:disabled="!canToggle(mod)"
					:aria-label="formatMessage(messages.toggleLabel, { name: modDisplayName(mod) })"
					@update:model-value="(value) => setEnabled(mod, !!value)"
				/>
			</li>
		</ul>
	</section>
</template>

<style scoped lang="scss" src="./panel.scss"></style>
