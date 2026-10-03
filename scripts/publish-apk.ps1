param(
    [Parameter(Mandatory = $true)][string]$ApkPath
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$apk = (Resolve-Path -LiteralPath $ApkPath).Path
$buildTools = Get-ChildItem -LiteralPath "$env:LOCALAPPDATA/Android/Sdk/build-tools" -Directory |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'aapt2.exe') } |
    Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (!$buildTools) { throw 'No se encontró aapt2 en el Android SDK.' }
$badging = (& (Join-Path $buildTools.FullName 'aapt2.exe') dump badging $apk) -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'No se pudo inspeccionar el APK.' }
$package = [regex]::Match($badging, "package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'")
$minimum = [regex]::Match($badging, "(?m)^(?:sdkVersion|minSdkVersion):'([0-9]+)'")
if (!$package.Success -or !$minimum.Success -or $package.Groups[1].Value -ne 'com.intu.taxi') {
    throw 'APK inválido o de otra aplicación.'
}
$code = [int]$package.Groups[2].Value
$name = $package.Groups[3].Value
if ($code -lt 1) { throw 'versionCode inválido.' }
$current = Invoke-RestMethod -Uri 'https://viajaconintu.pages.dev/api/apk' -Headers @{ 'Cache-Control' = 'no-cache' }
if ($current.updateAvailable -and $current.versionCode -ge $code) {
    throw 'Esta versión ya fue publicada. Incrementa versionCode antes de publicar otro binario.'
}
# The existing QA signing key remains the normal build key; never uninstall to update.
& (Join-Path $buildTools.FullName 'apksigner.bat') verify $apk
if ($LASTEXITCODE -ne 0) { throw 'La firma del APK no es válida.' }
$size = (Get-Item -LiteralPath $apk).Length
$sha = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
$manifestPath = Join-Path $taskRoot "build/published-apk-$code.json"
New-Item -ItemType Directory -Path (Split-Path -Parent $manifestPath) -Force | Out-Null
Push-Location (Join-Path $taskRoot 'web')
try {
    # Keep a fixed copy for clients that accepted this version before the next publication.
    & npx.cmd wrangler r2 object put "intu-apk/releases/$code/intu.apk" --file $apk --content-type application/vnd.android.package-archive --remote
    if ($LASTEXITCODE -ne 0) { throw 'Falló la subida del APK versionado.' }
    $downloadedPath = Join-Path $taskRoot "build/published-apk-$code-verified.apk"
    Invoke-WebRequest -Uri ("https://viajaconintu.pages.dev/descargar?versionCode=$code&verify=" + [guid]::NewGuid()) -OutFile $downloadedPath
    if ((Get-FileHash -LiteralPath $downloadedPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $sha) {
        throw 'El APK descargado no coincide con el binario local. No se anunciará esta versión.'
    }
    & npx.cmd wrangler r2 object put intu-apk/intu.apk --file $apk --content-type application/vnd.android.package-archive --remote
    if ($LASTEXITCODE -ne 0) { throw 'Falló la subida de la descarga actual.' }
    $uploaded = Invoke-RestMethod -Uri ('https://viajaconintu.pages.dev/api/apk?publication=' + [guid]::NewGuid()) -Headers @{ 'Cache-Control' = 'no-cache' }
    if (!$uploaded.available -or $uploaded.size -ne $size) { throw 'El APK publicado no coincide en tamaño.' }
    $metadata = [ordered]@{
        packageName = 'com.intu.taxi'; versionCode = $code; versionName = $name
        minSdk = [int]$minimum.Groups[1].Value; size = $size; sha256 = $sha; apkUploaded = $uploaded.uploaded
    }
    [System.IO.File]::WriteAllText($manifestPath, ($metadata | ConvertTo-Json), [System.Text.UTF8Encoding]::new($false))
    # Publishing metadata last prevents announcing an APK that is still uploading.
    & npx.cmd wrangler r2 object put intu-apk/latest.json --file $manifestPath --content-type application/json --cache-control no-store --remote
    if ($LASTEXITCODE -ne 0) { throw 'Falló la publicación de metadata.' }
    $verified = Invoke-RestMethod -Uri ('https://viajaconintu.pages.dev/api/apk?verify=' + [guid]::NewGuid()) -Headers @{ 'Cache-Control' = 'no-cache' }
    if (!$verified.updateAvailable -or $verified.versionCode -ne $code -or $verified.sha256 -ne $sha) {
        throw 'La API no confirmó la versión publicada. Revisa Pages y latest.json.'
    }
    Write-Output "Publicada Intu $name ($code), SHA256 $sha"
    Write-Output $verified.downloadUrl
} finally { Pop-Location }
