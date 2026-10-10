# 3vhy Client Music HUD: prints what Windows' media controls say is playing, one JSON line a second.
# Works with any app that shows up in the Windows volume / media flyout (Spotify, browsers,
# Apple Music, Media Player and so on). Exits when the game (ParentId) closes.
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

function Get-Art($properties) {
	if ($null -eq $properties.Thumbnail) { return '' }
	$stream = Wait-Async ($properties.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
	if ($null -eq $stream) { return '' }
	$readStream = $null
	$image = $null
	$bitmap = $null
	$graphics = $null
	$output = $null
	try {
		$readStream = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($stream)
		$image = [System.Drawing.Image]::FromStream($readStream)
		$size = 64
		$side = [Math]::Min($image.Width, $image.Height)
		$source = New-Object System.Drawing.Rectangle ([int](($image.Width - $side) / 2)), ([int](($image.Height - $side) / 2)), $side, $side
		$bitmap = New-Object System.Drawing.Bitmap $size, $size
		$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
		$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
		$graphics.DrawImage($image, (New-Object System.Drawing.Rectangle 0, 0, $size, $size), $source, [System.Drawing.GraphicsUnit]::Pixel)
		$output = New-Object System.IO.MemoryStream
		$bitmap.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
		return [Convert]::ToBase64String($output.ToArray())
	} catch {
		return ''
	} finally {
		foreach ($item in @($graphics, $bitmap, $image, $readStream, $output)) { if ($null -ne $item) { $item.Dispose() } }
		$stream.Dispose()
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
$artSent = $false
$artTries = 0

while ($true) {
	if ($ParentId -gt 0 -and $null -eq (Get-Process -Id $ParentId -ErrorAction SilentlyContinue)) { exit 0 }
	try {
		$session = $manager.GetCurrentSession()
		if ($null -eq $session) {
			$lastKey = $null
			Write-Line '{"none":true}'
		} else {
			$properties = Wait-Async ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
			$timeline = $session.GetTimelineProperties()
			$playback = $session.GetPlaybackInfo()
			$title = if ($null -ne $properties) { [string]$properties.Title } else { '' }
			$artist = if ($null -ne $properties) { [string]$properties.Artist } else { '' }
			$album = if ($null -ne $properties) { [string]$properties.AlbumTitle } else { '' }
			$key = "$($session.SourceAppUserModelId)|$title|$artist|$album"
			if ($key -ne $lastKey) {
				$lastKey = $key
				$artSent = $false
				$artTries = 0
			}
			$playing = [string]$playback.PlaybackStatus -eq 'Playing'
			$position = $timeline.Position.TotalSeconds
			if ($playing -and $timeline.LastUpdatedTime.Year -gt 2000) {
				$position += ([DateTimeOffset]::Now - $timeline.LastUpdatedTime).TotalSeconds
			}
			$line = [ordered]@{
				key = $key
				app = [string]$session.SourceAppUserModelId
				title = $title
				artist = $artist
				album = $album
				playing = $playing
				position = [Math]::Round([Math]::Max(0, $position), 2)
				duration = [Math]::Round(($timeline.EndTime - $timeline.StartTime).TotalSeconds, 2)
			}
			if (-not $artSent -and $artTries -lt 5 -and $null -ne $properties) {
				$artTries++
				$art = Get-Art $properties
				if ($art -ne '') {
					$artSent = $true
					$line.art = $art
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
