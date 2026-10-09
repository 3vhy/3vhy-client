<script setup lang="ts">
import { PlusIcon, StopCircleIcon } from '@modrinth/assets'
import { Avatar, defineMessages, injectNotificationManager, useVIntl } from '@modrinth/ui'
import { useQueries, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed } from 'vue'

import {
	recentWorldsKey,
	recentWorldsQueryOptions,
	serverStatusQueryOptions,
} from '@/components/ui/world/queries'
import { useAppEvent } from '@/composables/use-app-event'
import { kill } from '@/helpers/instance'
import { get_all } from '@/helpers/process'
import type { GameInstance } from '@/helpers/types'
import { type ServerData, type ServerWorld, start_join_server } from '@/helpers/worlds'

// 3vhy Client: Modrinth's "Jump in" for servers, in the S.A.M sidebar format —
// every server you've played recently, one click to join straight back in.
const props = defineProps<{
	instances: GameInstance[]
	selected: GameInstance | null
}>()

const MAX_SERVERS = 6

const { formatMessage } = useVIntl()
const { handleError } = injectNotificationManager()
const queryClient = useQueryClient()

const messages = defineMessages({
	title: { id: 'app.3vhy.servers.title', defaultMessage: 'Servers' },
	add: { id: 'app.3vhy.servers.add', defaultMessage: 'Add' },
	join: { id: 'app.3vhy.servers.join', defaultMessage: 'Join' },
	stop: { id: 'app.3vhy.servers.stop', defaultMessage: 'Stop' },
	online: { id: 'app.3vhy.servers.online', defaultMessage: '{count} online' },
	empty: {
		id: 'app.3vhy.servers.empty',
		defaultMessage: 'Servers you join show up here so you can hop back in with one click.',
	},
})

type RecentServer = { world: ServerWorld & { instance_id: string }; instance: GameInstance }

const recentQuery = useQuery(recentWorldsQueryOptions(20))
const servers = computed<RecentServer[]>(() => {
	const seen = new Set<string>()
	const list: RecentServer[] = []
	for (const world of recentQuery.data.value ?? []) {
		if (world.type !== 'server' || !world.address) continue
		const instance = props.instances.find((candidate) => candidate.id === world.instance_id)
		if (!instance || instance.quarantined) continue
		const key = world.address.toLowerCase()
		if (seen.has(key)) continue
		seen.add(key)
		list.push({ world, instance })
		if (list.length >= MAX_SERVERS) break
	}
	return list
})

const statusQueries = useQueries({
	queries: computed(() =>
		servers.value.map(({ world }) => ({
			...serverStatusQueryOptions(world.address, null),
			select: (data: ServerData) => ({ address: world.address, data }),
		})),
	),
})
const statusByAddress = computed(() => {
	const map: Record<string, ServerData> = {}
	for (const query of statusQueries.value) {
		if (query.data) map[query.data.address] = query.data.data
	}
	return map
})

type ProcessMetadata = { instance_id: string }
const processesQuery = useQuery({
	queryKey: ['processes', 'list'],
	queryFn: (): Promise<ProcessMetadata[]> => get_all(),
})
const processes = computed(() => processesQuery.data.value ?? [])

useAppEvent('process', () => {
	void queryClient.invalidateQueries({ queryKey: ['processes', 'list'] })
	void queryClient.invalidateQueries({ queryKey: recentWorldsKey })
})

function isRunning(instanceId: string) {
	return processes.value.some((process) => process.instance_id === instanceId)
}

async function join(server: RecentServer) {
	await start_join_server(server.world.instance_id, server.world.address).catch(handleError)
}

async function stop(server: RecentServer) {
	await kill(server.instance.id).catch(handleError)
}

function initial(name: string) {
	return (name.trim()[0] ?? '?').toUpperCase()
}

const addTarget = computed(() =>
	props.selected ? `/instance/${encodeURIComponent(props.selected.id)}/worlds` : '/library',
)
</script>

<template>
	<section class="tvh-panel">
		<header class="tvh-panel__header">
			<h3>{{ formatMessage(messages.title) }}</h3>
			<RouterLink :to="addTarget" class="tvh-panel__link">
				<PlusIcon class="tvh-panel__link-icon" /> {{ formatMessage(messages.add) }}
			</RouterLink>
		</header>

		<p v-if="!recentQuery.isLoading.value && servers.length === 0" class="tvh-panel__empty">
			{{ formatMessage(messages.empty) }}
		</p>
		<ul v-else class="tvh-panel__list">
			<li v-for="server in servers" :key="server.world.address" class="tvh-row tvh-row--server">
				<span class="tvh-row__icon tvh-row__icon--server">
					<Avatar
						v-if="statusByAddress[server.world.address]?.status?.favicon ?? server.world.icon"
						:src="statusByAddress[server.world.address]?.status?.favicon ?? server.world.icon"
						size="2.25rem"
						no-shadow
					/>
					<span v-else class="tvh-row__initial">{{ initial(server.world.name) }}</span>
				</span>
				<span class="tvh-row__text">
					<span class="tvh-row__name" :title="server.world.name">{{ server.world.name }}</span>
					<span class="tvh-row__meta">
						<template v-if="statusByAddress[server.world.address]?.status?.players">
							<span class="tvh-dot" />
							{{
								formatMessage(messages.online, {
									count:
										statusByAddress[
											server.world.address
										]?.status?.players?.online.toLocaleString() ?? '0',
								})
							}}
						</template>
						<template v-else>{{ server.world.address }}</template>
					</span>
				</span>
				<button
					v-if="isRunning(server.instance.id)"
					class="tvh-join tvh-join--stop"
					@click="stop(server)"
				>
					<StopCircleIcon /> {{ formatMessage(messages.stop) }}
				</button>
				<button v-else class="tvh-join" @click="join(server)">
					{{ formatMessage(messages.join) }}
				</button>
			</li>
		</ul>
	</section>
</template>

<style scoped lang="scss" src="./panel.scss"></style>
