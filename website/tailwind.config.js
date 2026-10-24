/** @type {import('tailwindcss').Config} */
export default {
	content: ['./src/**/*.{html,js}'],
	theme: {
		extend: {
			colors: {
				bg: { 0: '#05040a', 1: '#0b0915', 2: '#131024' },
				surface: { DEFAULT: '#19152d', hi: '#221c3c' },
				text: { DEFAULT: '#f6f3ff', 2: '#b4acd2', 3: '#7c7599' },
				indigo: '#5f04eb',
				violet: '#880ff9',
				purple: '#b51ff4',
				magenta: '#d129f5',
				pink: '#ff5ad9',
				lilac: { DEFAULT: '#cdb6ff', 2: '#ac86ff', 3: '#eba2ff' },
				gold: '#ffd27a',
				mint: '#8af5c8',
				low: '#ff5c84',
				ember: '#ff8a5c',
			},
			fontFamily: {
				sans: ['"Inter Variable"', 'Inter', 'ui-sans-serif', 'system-ui', '-apple-system', '"Segoe UI"', 'sans-serif'],
			},
			transitionTimingFunction: {
				out: 'cubic-bezier(.16, 1, .3, 1)',
				'in-out': 'cubic-bezier(.65, 0, .35, 1)',
				spring: 'cubic-bezier(.34, 1.56, .64, 1)',
			},
			maxWidth: { container: '1200px' },
		},
	},
	plugins: [],
}
