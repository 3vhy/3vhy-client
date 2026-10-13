// 3vhy Client Setup — screen flow and calls into the Rust side
;(() => {
	const tauri = window.__TAURI__
	const invoke = (cmd, args) => tauri.core.invoke(cmd, args)
	const $ = (id) => document.getElementById(id)

	// ---------- October ----------

	// In October the setup turns white, with ghosts, a moon, bats and a little graveyard
	const october = new Date().getMonth() === 9

	/** An SVG of a pixel sprite: each character is one cell, coloured by its class in `parts`. */
	function pixelSvg(rows, parts, className) {
		let rects = ''
		rows.forEach((row, y) => {
			for (let x = 0; x < row.length; x++) {
				const part = parts[row[x]]
				if (!part) continue
				let end = x
				while (end < row.length && row[end] === row[x]) end++
				rects += `<rect x="${x}" y="${y}" width="${end - x}" height="1" ${part}/>`
				x = end - 1
			}
		})
		const width = rows[0].length
		return `<svg class="${className}" viewBox="0 0 ${width} ${rows.length}" shape-rendering="crispEdges" aria-hidden="true">${rects}</svg>`
	}

	const GHOST = [
		'......####......',
		'....########....',
		'...##########s..',
		'..###########ss.',
		'.############ss.',
		'.############sss',
		'.##go####go##sss',
		'###oo####oo##sss',
		'###oo####oo##sss',
		'#bb########bbsss',
		'#######oo####sss',
		'#######oo####sss',
		'#############sss',
		'############ssss',
		'###########sssss',
		'##ssssssssssssss',
		'##.####..####.ss',
		'#...##....##...s',
	]
	const GHOST_PARTS = {
		'#': 'fill="#ffffff"',
		s: 'fill="#e6e0f1"',
		o: 'fill="#221c30"',
		g: 'fill="#ffffff"',
		b: 'fill="#f7b9cb"',
	}

	function moonSvg() {
		const rows = []
		for (let y = 0; y < 20; y++) {
			const half = Math.round(Math.sqrt(Math.max(0, 100 - (y + 0.5 - 10) ** 2)))
			rows.push(' '.repeat(10 - half) + '#'.repeat(half * 2) + ' '.repeat(10 - half))
		}
		const craters = [
			[5, 5, 3, 4],
			[12, 11, 3, 4],
			[13, 4, 2, 2],
			[6, 13, 2, 2],
		]
		for (const [cx, cy, w, h] of craters) {
			for (let y = cy; y < cy + h; y++) {
				rows[y] = rows[y].slice(0, cx) + 'c'.repeat(w) + rows[y].slice(cx + w)
			}
		}
		return pixelSvg(rows, { '#': 'fill="#fcfaff"', c: 'fill="#ebe5f4"' }, 'spooky-moon')
	}

	function batSvg(style) {
		const up = ['#.........#', '##.......##', '###.#.#.###', '.#########.', '...#####...']
		const down = ['....#.#....', '..#######..', '.#########.', '##.#####.##', '#.........#']
		const group = (rows, className) =>
			`<g class="${className}">${pixelSvg(rows, { '#': 'class="px"' }, '').replace(/^<svg[^>]*>|<\/svg>$/g, '')}</g>`
		return `<svg class="spooky-bat" style="${style}" viewBox="0 0 11 5" shape-rendering="crispEdges" aria-hidden="true">${group(up, 'wings-up')}${group(down, 'wings-down')}</svg>`
	}

	const GRAVES = {
		stone: ['.###.', '#####', '#####', '#####', '#####', '#####'],
		cross: ['..#..', '.###.', '..#..', '..#..', '..#..', '..#..'],
		pumpkin: ['..t...', '.oooo.', 'oyooyo', 'oooooo', 'ooyyoo', '.oooo.'],
	}

	function graveSvg(kind, left, bottom) {
		const parts =
			kind === 'pumpkin'
				? { t: 'class="stem"', o: 'class="body"', y: 'class="face"' }
				: { '#': 'class="px"' }
		return pixelSvg(GRAVES[kind], parts, `spooky-grave spooky-grave--${kind}`).replace(
			'<svg ',
			`<svg style="left:${left}%;bottom:${bottom}%" `,
		)
	}

	if (october) {
		document.documentElement.classList.add('october')
		const scene = document.createElement('div')
		scene.className = 'spooky'
		scene.innerHTML = [
			moonSvg(),
			batSvg('top:58px;right:150px'),
			batSvg('top:104px;right:190px;width:17px;animation-duration:11s;animation-delay:-3s'),
			batSvg('top:80px;right:36px;width:14px;animation-duration:7s;animation-delay:-5s'),
			pixelSvg(GHOST, GHOST_PARTS, 'spooky-ghost spooky-ghost--far'),
			pixelSvg(GHOST, GHOST_PARTS, 'spooky-ghost spooky-ghost--left'),
			pixelSvg(GHOST, GHOST_PARTS, 'spooky-ghost spooky-ghost--right').replace(
				'<svg ',
				'<svg style="transform:scaleX(-1)" ',
			),
			graveSvg('stone', 5, 6.5),
			graveSvg('pumpkin', 18, 8.7),
			graveSvg('cross', 26, 8.7),
			graveSvg('cross', 75, 10.8),
			graveSvg('pumpkin', 85, 8.7),
			graveSvg('stone', 93, 8.7),
		].join('')
		document.querySelector('.sky').appendChild(scene)
	}

	const state = {
		info: null,
		dir: '',
		exe: null,
		installing: false,
		countdown: null,
	}

	const TIPS = [
		'Flip mods on and off from <b>Quick mods</b> on the Home screen.',
		'Your recent servers show up under <b>Servers</b>. One click on <b>Join</b> drops you right back in.',
		'Any modpack on Modrinth is one click away in the <b>Modpacks</b> tab.',
		'Pick an instance next to the <b>Play</b> button to switch Minecraft versions.',
		'<b>Logs</b> and <b>Folder</b> in the sidebar open the instance you picked on Home.',
		'Prefer it bright? Switch to the light theme in <b>Settings</b>.',
	]

	// ---------- Screens ----------

	const ORDER = ['welcome', 'options', 'installing', 'done', 'error']
	let current = 'welcome'

	function show(name) {
		if (name === current) return
		const from = $(`screen-${current}`)
		const to = $(`screen-${name}`)
		const back = ORDER.indexOf(name) < ORDER.indexOf(current)
		from.classList.remove('active')
		from.classList.add(back ? 'leaving-back' : 'leaving')
		setTimeout(() => from.classList.remove('leaving', 'leaving-back'), 600)
		to.classList.add('active')
		current = name
		const primary = to.dataset.primary && $(to.dataset.primary)
		if (primary) setTimeout(() => primary.focus({ preventScroll: true }), 350)
	}

	// ---------- Setup info ----------

	function setDir(dir, flash) {
		state.dir = dir
		$('install-path-text').textContent = dir
		$('install-path').title = dir
		if (flash) {
			const box = $('install-path')
			box.classList.remove('flash')
			void box.offsetWidth
			box.classList.add('flash')
		}
	}

	async function load() {
		const info = await invoke('setup_info')
		state.info = info
		setDir(info.default_dir)
		$('version-label').textContent = `Version ${info.version}`
		$('space-hint').textContent = `Needs about ${info.size_mb} MB of space`
		const verb = info.existing ? 'Update' : 'Install'
		$('install-label').textContent = verb
		$('options-install-label').textContent = verb
		$('installing-title').textContent = info.existing
			? 'Updating 3vhy Client'
			: 'Installing 3vhy Client'

		const notice = $('welcome-notice')
		if (!info.supported || !info.has_payload) {
			notice.hidden = false
			notice.textContent = !info.supported
				? '3vhy Client Setup only runs on Windows.'
				: 'This copy of the setup is missing the 3vhy Client installer. Download it again.'
			$('btn-install').disabled = true
			$('btn-options-install').disabled = true
		}
	}

	// ---------- Install ----------

	const BLOCKS = 24
	const blocksEl = $('progress-blocks')
	for (let i = 0; i < BLOCKS; i++) {
		const block = document.createElement('div')
		block.className = 'block'
		blocksEl.appendChild(block)
	}

	let shownPercent = 0
	let targetPercent = 0
	let animating = false

	function renderProgress() {
		// Ease the shown number toward the real one so jumps feel smooth
		shownPercent += (targetPercent - shownPercent) * 0.18
		if (Math.abs(targetPercent - shownPercent) < 0.2) shownPercent = targetPercent
		const value = Math.round(shownPercent)
		$('percent').textContent = value
		$('progress').setAttribute('aria-valuenow', String(value))
		const filled = Math.floor((shownPercent / 100) * BLOCKS)
		blocksEl.childNodes.forEach((block, index) => {
			block.classList.toggle('on', index < filled)
			block.classList.toggle('next', index === filled && shownPercent < 100)
		})
		if (shownPercent !== targetPercent) {
			requestAnimationFrame(renderProgress)
		} else {
			animating = false
		}
	}

	function setProgress(percent, stage) {
		targetPercent = Math.max(targetPercent, Math.min(100, percent))
		if (stage) $('stage-label').textContent = stage === 'Done' ? 'Finishing up…' : `${stage}…`
		if (!animating) {
			animating = true
			requestAnimationFrame(renderProgress)
		}
	}

	let tipIndex = Math.floor(Math.random() * TIPS.length)
	let tipTimer = null

	function nextTip() {
		const tip = $('tip')
		tip.classList.add('fading')
		setTimeout(() => {
			tipIndex = (tipIndex + 1) % TIPS.length
			$('tip-text').innerHTML = TIPS[tipIndex]
			tip.classList.remove('fading')
		}, 400)
	}

	async function startInstall() {
		if (state.installing || !state.info) return
		state.installing = true
		shownPercent = 0
		targetPercent = 0
		setProgress(0, 'Getting ready')
		$('tip-text').innerHTML = TIPS[tipIndex]
		tipTimer = setInterval(nextTip, 4500)
		show('installing')

		try {
			const exe = await invoke('install', {
				dir: state.dir,
				desktop: $('opt-desktop').checked,
				startMenu: $('opt-startmenu').checked,
			})
			state.exe = exe
			setProgress(100, 'Done')
			await new Promise((resolve) => setTimeout(resolve, 700))
			finish()
		} catch (error) {
			$('error-message').textContent = String(error)
			show('error')
		} finally {
			state.installing = false
			clearInterval(tipTimer)
		}
	}

	// ---------- Done ----------

	function burst() {
		const host = $('burst')
		host.innerHTML = ''
		const colors = october
			? ['#221c30', '#8f84ad', '#ff8a2b', '#d5d0e1', '#4a4063']
			: ['#e3f3fc', '#5bc8e6', '#8cc4ea', '#1a4fba', '#a6ebfa']
		for (let i = 0; i < 26; i++) {
			const spark = document.createElement('span')
			const angle = (Math.PI * 2 * i) / 26 + Math.random() * 0.3
			const distance = 110 + Math.random() * 120
			spark.className = 'spark'
			spark.style.setProperty('--x', `${Math.cos(angle) * distance}px`)
			spark.style.setProperty('--y', `${Math.sin(angle) * distance}px`)
			spark.style.setProperty('--r', `${Math.random() * 360}deg`)
			spark.style.setProperty('--c', colors[i % colors.length])
			spark.style.animationDelay = `${Math.random() * 0.12}s`
			host.appendChild(spark)
		}
	}

	function finish() {
		const updated = state.info?.existing
		$('done-subtitle').textContent = updated
			? '3vhy Client is up to date and ready to play.'
			: '3vhy Client is installed and ready to play.'
		show('done')
		setTimeout(burst, 150)

		if ($('opt-launch').checked) {
			let seconds = 3
			$('countdown-n').textContent = seconds
			$('countdown').hidden = false
			state.countdown = setInterval(() => {
				seconds -= 1
				$('countdown-n').textContent = seconds
				if (seconds <= 0) {
					cancelCountdown()
					launchAndClose()
				}
			}, 1000)
		}
	}

	function cancelCountdown() {
		clearInterval(state.countdown)
		state.countdown = null
		$('countdown').hidden = true
	}

	async function launchAndClose() {
		cancelCountdown()
		if (!state.exe) return
		try {
			await invoke('launch', { exe: state.exe })
			await invoke('close')
		} catch (error) {
			$('error-message').textContent = String(error)
			show('error')
		}
	}

	// ---------- Wiring ----------

	async function tryClose() {
		const closed = await invoke('close')
		if (!closed) {
			const button = $('btn-close')
			button.classList.remove('shake')
			void button.offsetWidth
			button.classList.add('shake')
		}
	}

	function openLicense() {
		$('license-modal').hidden = false
		$('btn-license-close').focus()
	}

	function closeLicense() {
		$('license-modal').hidden = true
	}

	$('btn-minimize').addEventListener('click', () => invoke('minimize'))
	$('btn-close').addEventListener('click', tryClose)
	$('btn-install').addEventListener('click', startInstall)
	$('btn-options').addEventListener('click', () => show('options'))
	$('btn-options-back').addEventListener('click', () => show('welcome'))
	$('btn-options-cancel').addEventListener('click', () => show('welcome'))
	$('btn-options-install').addEventListener('click', startInstall)
	$('btn-browse').addEventListener('click', async () => {
		const picked = await invoke('pick_folder', { current: state.dir })
		if (picked) setDir(picked, true)
	})
	$('btn-launch').addEventListener('click', launchAndClose)
	$('btn-cancel-launch').addEventListener('click', cancelCountdown)
	$('btn-done-close').addEventListener('click', tryClose)
	$('btn-retry').addEventListener('click', startInstall)
	$('btn-error-options').addEventListener('click', () => show('options'))
	$('btn-license').addEventListener('click', openLicense)
	$('btn-license-close').addEventListener('click', closeLicense)
	$('btn-license-full').addEventListener('click', () =>
		invoke('open_link', { url: 'https://www.gnu.org/licenses/gpl-3.0.html' }),
	)
	$('license-modal').addEventListener('click', (event) => {
		if (event.target === event.currentTarget) closeLicense()
	})

	document.addEventListener('keydown', (event) => {
		if (event.key === 'Escape' && !$('license-modal').hidden) closeLicense()
	})

	// No right-click menu or text selection outside the error message
	document.addEventListener('contextmenu', (event) => event.preventDefault())

	tauri.event.listen('setup-progress', (event) => {
		setProgress(event.payload.percent, event.payload.stage)
	})

	load().catch((error) => {
		$('welcome-notice').hidden = false
		$('welcome-notice').textContent = `Setup couldn't start: ${error}`
	})
})()
