import { defineConfig } from 'vite'
import { viteSingleFile } from 'vite-plugin-singlefile'

export default defineConfig({
	root: 'src',
	base: './',
	plugins: [viteSingleFile({ removeViteModuleLoader: true })],
	build: {
		outDir: '../dist',
		emptyOutDir: true,
		assetsInlineLimit: 100000000,
		cssCodeSplit: false,
		chunkSizeWarningLimit: 4000,
	},
})
