const LOADER_LABELS: Record<string, string> = {
	vanilla: 'Vanilla',
	fabric: 'Fabric',
	forge: 'Forge',
	neoforge: 'NeoForge',
	quilt: 'Quilt',
}

export function loaderLabel(loader: string): string {
	return LOADER_LABELS[loader] ?? loader.charAt(0).toUpperCase() + loader.slice(1)
}
