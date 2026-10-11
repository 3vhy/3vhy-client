# 3vhy Client Music HUD: prints what Windows' media controls say is playing, one JSON line a second.
# Works with any app that shows up in the Windows volume / media flyout (Spotify, browsers,
# Apple Music, Media Player and so on). Exits when the game (ParentId) closes.
#
# Each line has the song, its position, and (once per song or app) the cover and the app's icon as
# base64 PNGs. When a cover or icon can't be read, artError / iconError say why.
param(
	[int]$ParentId = 0,
	[switch]$Once
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false

function Write-Line([string]$line) {
	[Console]::Out.WriteLine($line)
	[Console]::Out.Flush()
}

function Write-Problem([string]$message) {
	Write-Line ('{"error":' + (ConvertTo-Json -Compress $message) + '}')
}

try {
	Add-Type -AssemblyName System.Runtime.WindowsRuntime
	Add-Type -AssemblyName System.Drawing
	$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
	$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType = WindowsRuntime]
	$null = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]
	$null = [Windows.Storage.Streams.DataReader, Windows.Storage.Streams, ContentType = WindowsRuntime]
} catch {
	Write-Problem "Windows media controls aren't available: $($_.Exception.Message)"
	exit 1
}

$asTaskMethod = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
	$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and
	$_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
} | Select-Object -First 1

function Wait-Async($operation, [Type]$resultType) {
	$task = $asTaskMethod.MakeGenericMethod($resultType).Invoke($null, @($operation))
	if (-not $task.Wait(4000)) { return $null }
	return $task.Result
}

# Reads a whole WinRT stream into a byte array, two ways in case one isn't available
function Read-StreamBytes($stream) {
	$firstProblem = $null
	try {
		$size = [uint32]$stream.Size
		if ($size -eq 0) { throw 'the image is empty' }
		$reader = [Windows.Storage.Streams.DataReader]::new($stream.GetInputStreamAt(0))
		try {
			$loaded = Wait-Async ($reader.LoadAsync($size)) ([uint32])
			if ($null -eq $loaded -or $loaded -eq 0) { throw 'reading the image timed out' }
			$bytes = New-Object byte[] $loaded
			$reader.ReadBytes($bytes)
			return , $bytes
		} finally {
			$reader.Dispose()
		}
	} catch {
		$firstProblem = $_.Exception.Message
	}
	try {
		$stream.Seek(0)
		$netStream = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($stream)
		$copy = New-Object System.IO.MemoryStream
		$netStream.CopyTo($copy)
		$netStream.Dispose()
		return , $copy.ToArray()
	} catch {
		throw "couldn't read the image ($firstProblem; $($_.Exception.Message))"
	}
}

# Any image the system can open -> a size x size PNG (cropped to a square from the middle)
function Convert-ToPng([byte[]]$bytes, [int]$size) {
	$memory = New-Object System.IO.MemoryStream (, $bytes)
	$image = $null
	$bitmap = $null
	$graphics = $null
	$output = $null
	try {
		$image = [System.Drawing.Image]::FromStream($memory)
		$side = [Math]::Min($image.Width, $image.Height)
		$source = New-Object System.Drawing.Rectangle ([int](($image.Width - $side) / 2)), ([int](($image.Height - $side) / 2)), $side, $side
		$bitmap = New-Object System.Drawing.Bitmap $size, $size
		$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
		$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
		$graphics.DrawImage($image, (New-Object System.Drawing.Rectangle 0, 0, $size, $size), $source, [System.Drawing.GraphicsUnit]::Pixel)
		$output = New-Object System.IO.MemoryStream
		$bitmap.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
		return [Convert]::ToBase64String($output.ToArray())
	} finally {
		foreach ($item in @($graphics, $bitmap, $image, $output, $memory)) { if ($null -ne $item) { $item.Dispose() } }
	}
}

function Get-Cover($properties) {
	if ($null -eq $properties.Thumbnail) { throw 'the app has no cover for this yet' }
	$stream = Wait-Async ($properties.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
	if ($null -eq $stream) { throw 'opening the cover timed out' }
	try {
		$bytes = Read-StreamBytes $stream
	} finally {
		$stream.Dispose()
	}
	return Convert-ToPng $bytes 64
}

# Windows names apps by an app id: "Spotify.exe", "Chrome", "MSEdge", a packaged app's
# "Family!App", or a path that starts with a known folder's id
$knownFolders = @{
	'{1AC14E77-02E7-4E5D-B744-2EB1AE5198B7}' = [Environment]::GetFolderPath('System')
	'{6D809377-6AF0-444B-8957-A3773F02200E}' = $env:ProgramW6432
	'{7C5A40EF-A0FB-4BFC-874A-C0F2E0B9FA8E}' = ${env:ProgramFiles(x86)}
	'{F38BF404-1D43-42F2-9305-67DE0B28FC23}' = [Environment]::GetFolderPath('Windows')
	'{905E63B6-C1BF-494E-B29C-65B732D3D21A}' = $env:ProgramFiles
}
$processNames = @{
	'308046B0AF4A39CB' = 'firefox'
	'MSEdge' = 'msedge'
	'Chrome' = 'chrome'
	'Brave' = 'brave'
	'Opera' = 'opera'
	'OperaGX' = 'opera'
	'Vivaldi' = 'vivaldi'
}

function Get-ExePath([string]$app) {
	if ($app -match '^(\{[0-9A-Fa-f-]+\})\\(.+)$') {
		$base = $knownFolders[$Matches[1].ToUpper()]
		if ($base) {
			$path = Join-Path $base $Matches[2]
			if (Test-Path -LiteralPath $path -PathType Leaf) { return $path }
		}
	}
	if ($app -match '^[A-Za-z]:\\' -and (Test-Path -LiteralPath $app -PathType Leaf)) { return $app }
	$name = [System.IO.Path]::GetFileNameWithoutExtension(($app -split '\\')[-1])
	if ($processNames.ContainsKey($app)) { $name = $processNames[$app] }
	$process = Get-Process -Name $name -ErrorAction SilentlyContinue | Where-Object { $_.Path } | Select-Object -First 1
	if ($process) { return $process.Path }
	return $null
}

function Get-PackagedLogo([string]$app) {
	$family = ($app -split '!')[0]
	$package = Get-AppxPackage | Where-Object { $_.PackageFamilyName -eq $family } | Select-Object -First 1
	if (-not $package) { return $null }
	[xml]$manifest = Get-Content -LiteralPath (Join-Path $package.InstallLocation 'AppxManifest.xml')
	$relative = $null
	foreach ($application in @($manifest.Package.Applications.Application)) {
		$visual = $application.VisualElements
		if ($visual -and $visual.Square44x44Logo) { $relative = $visual.Square44x44Logo; break }
		if ($visual -and $visual.Square150x150Logo) { $relative = $visual.Square150x150Logo; break }
	}
	if (-not $relative) { $relative = $manifest.Package.Properties.Logo }
	if (-not $relative) { return $null }
	$full = Join-Path $package.InstallLocation $relative
	if (Test-Path -LiteralPath $full -PathType Leaf) { return $full }
	# Packaged logos come as name.scale-100.png, name.targetsize-48.png and so on
	$folder = Split-Path $full
	$stem = [System.IO.Path]::GetFileNameWithoutExtension($full)
	$extension = [System.IO.Path]::GetExtension($full)
	$files = @(Get-ChildItem -LiteralPath $folder -Filter "$stem*$extension" -ErrorAction SilentlyContinue)
	foreach ($preferred in @('targetsize-48', 'targetsize-32', 'scale-200', 'scale-100')) {
		$match = $files | Where-Object { $_.Name -like "*$preferred*" -and $_.Name -notlike '*contrast*' } | Select-Object -First 1
		if ($match) { return $match.FullName }
	}
	if ($files.Count -gt 0) { return $files[0].FullName }
	return $null
}

function Get-AppIcon([string]$app) {
	if ($app -like '*!*') {
		$logo = Get-PackagedLogo $app
		if ($logo) { return Convert-ToPng ([System.IO.File]::ReadAllBytes($logo)) 32 }
	}
	$exe = Get-ExePath $app
	if (-not $exe) { throw "couldn't find the program for $app" }
	$icon = [System.Drawing.Icon]::ExtractAssociatedIcon($exe)
	$bitmap = $icon.ToBitmap()
	$output = New-Object System.IO.MemoryStream
	try {
		$bitmap.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
		return Convert-ToPng $output.ToArray() 32
	} finally {
		$output.Dispose()
		$bitmap.Dispose()
		$icon.Dispose()
	}
}

try {
	$manager = Wait-Async ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
} catch {
	Write-Problem "Couldn't open Windows media controls: $($_.Exception.Message)"
	exit 1
}
if ($null -eq $manager) {
	Write-Problem "Windows media controls didn't answer"
	exit 1
}

$lastKey = $null
$coverSent = $false
$coverPolls = 0
$lastApp = $null
$iconSent = $false
$iconTries = 0

while ($true) {
	if ($ParentId -gt 0 -and $null -eq (Get-Process -Id $ParentId -ErrorAction SilentlyContinue)) { exit 0 }
	try {
		$session = $manager.GetCurrentSession()
		if ($null -eq $session) {
			$lastKey = $null
			Write-Line '{"none":true}'
		} else {
			$app = [string]$session.SourceAppUserModelId
			$properties = Wait-Async ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
			$timeline = $session.GetTimelineProperties()
			$playback = $session.GetPlaybackInfo()
			$title = if ($null -ne $properties) { [string]$properties.Title } else { '' }
			$artist = if ($null -ne $properties) { [string]$properties.Artist } else { '' }
			$album = if ($null -ne $properties) { [string]$properties.AlbumTitle } else { '' }
			$key = "$app|$title|$artist|$album"
			if ($key -ne $lastKey) {
				$lastKey = $key
				$coverSent = $false
				$coverPolls = 0
			}
			if ($app -ne $lastApp) {
				$lastApp = $app
				$iconSent = $false
				$iconTries = 0
			}
			$playing = [string]$playback.PlaybackStatus -eq 'Playing'
			$position = $timeline.Position.TotalSeconds
			if ($playing -and $timeline.LastUpdatedTime.Year -gt 2000) {
				$position += ([DateTimeOffset]::Now - $timeline.LastUpdatedTime).TotalSeconds
			}
			$line = [ordered]@{
				key = $key
				app = $app
				title = $title
				artist = $artist
				album = $album
				playing = $playing
				position = [Math]::Round([Math]::Max(0, $position), 2)
				duration = [Math]::Round(($timeline.EndTime - $timeline.StartTime).TotalSeconds, 2)
			}
			# Browsers often add the cover a few seconds after the title: try every second at first,
			# then every 5 seconds for a minute
			$coverPolls++
			if (-not $coverSent -and $null -ne $properties -and ($coverPolls -le 5 -or ($coverPolls -le 60 -and $coverPolls % 5 -eq 0))) {
				try {
					$line.art = Get-Cover $properties
					$coverSent = $true
				} catch {
					$line.artError = $_.Exception.Message
				}
			}
			if (-not $iconSent -and $iconTries -lt 3) {
				$iconTries++
				try {
					$line.icon = Get-AppIcon $app
					$iconSent = $true
				} catch {
					$line.iconError = $_.Exception.Message
				}
			}
			Write-Line ($line | ConvertTo-Json -Compress)
		}
	} catch {
		Write-Problem $_.Exception.Message
	}
	if ($Once) { exit 0 }
	Start-Sleep -Milliseconds 1000
}
