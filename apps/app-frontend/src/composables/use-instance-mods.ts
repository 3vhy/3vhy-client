import type { ContentItem } from '@modrinth/ui'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, type Ref } from 'vue'

import { get_content_items, get_linked_modpack_content } from '@/helpers/instance'

// 3vhy Client: every mod in an instance — the ones you added plus the ones that
// came with a Modrinth modpack — for the Home screen's Quick mods and mod count.
export function instanceModsKey(instanceId: string | null) {
	return ['3vhy', 'instance-mods', instanceId] as const
}

export function modDisplayName(item: ContentItem) {
	return item.project?.title ?? item.file_name.replace(/\.(jar|disabled)$/g, '')
}

export function useInstanceMods(instanceId: Ref<string | null>) {
	const queryClient = useQueryClient()
	const queryKey = computed(() => instanceModsKey(instanceId.value))

	const query = useQuery({
		queryKey,
		enabled: computed(() => !!instanceId.value),
		staleTime: 15_000,
		queryFn: async (): Promise<ContentItem[]> => {
			const id = instanceId.value
			if (!id) return []
			const [own, fromPack] = await Promise.all([
				get_content_items(id),
				get_linked_modpack_content(id).catch(() => [] as ContentItem[]),
			])
			const byFile = new Map<string, ContentItem>()
			for (const item of [...fromPack, ...own]) {
				if (item.project_type !== 'mod') continue
				// The 3vhy mod itself is managed by the launcher, not toggled here
				if (item.file_name.toLowerCase().startsWith('3vhy-client')) continue
				byFile.set(item.file_path ?? item.file_name, item)
			}
			return [...byFile.values()].sort((a, b) => modDisplayName(a).localeCompare(modDisplayName(b)))
		},
	})

	const mods = computed(() => query.data.value ?? [])
	const enabledCount = computed(() => mods.value.filter((mod) => mod.enabled !== false).length)

	function refresh() {
		return queryClient.invalidateQueries({ queryKey: queryKey.value })
	}

	return { query, mods, enabledCount, refresh }
}
