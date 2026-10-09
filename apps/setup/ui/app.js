// 3vhy Client Setup — screen flow and calls into the Rust side
;(() => {
	const tauri = window.__TAURI__
	const invoke = (cmd, args) => tauri.core.invoke(cmd, args)
	const $ = (id) => document.getElementById(id)

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
		const colors = ['#e3f3fc', '#5bc8e6', '#8cc4ea', '#1a4fba', '#a6ebfa']
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
