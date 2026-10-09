import { useQuery } from '@tanstack/vue-query'
import dayjs from 'dayjs'
import { computed, ref, watch } from 'vue'

import type { GameInstance } from '@/helpers/types'
import { instanceListQueryOptions } from '@/pages/instance/query-options'

// 3vhy Client: the instance picked in the Home "version" picker. The sidebar's
// Logs and Folder buttons act on this same instance.
const STORAGE_KEY = '3vhy-selected-instance'

function readStored(): string | null {
	try {
		return window.localStorage.getItem(STORAGE_KEY)
	} catch {
		return null
	}
}

const selectedId = ref<string | null>(readStored())

watch(selectedId, (id) => {
	try {
		if (id) window.localStorage.setItem(STORAGE_KEY, id)
		else window.localStorage.removeItem(STORAGE_KEY)
	} catch {
		// storage blocked or full
	}
})

function lastActivity(instance: GameInstance) {
	return dayjs(instance.last_played ?? instance.created)
}

export function sortByRecent(instances: GameInstance[]): GameInstance[] {
	return instances.slice().sort((a, b) => lastActivity(b).diff(lastActivity(a)))
}

export function useSelectedInstance() {
	const instancesQuery = useQuery(instanceListQueryOptions())
	const instances = computed(() => sortByRecent(instancesQuery.data.value ?? []))

	// Falls back to the most recently played instance when nothing is picked
	// (or the picked one was deleted).
	const selected = computed<GameInstance | null>(
		() =>
			instances.value.find((instance) => instance.id === selectedId.value) ??
			instances.value[0] ??
			null,
	)

	function select(id: string) {
		selectedId.value = id
	}

	return { instances, selected, select, instancesQuery }
}
