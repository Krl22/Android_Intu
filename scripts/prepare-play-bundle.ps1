param([switch]$CreateUploadKey, [switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$jdkDirectory = Join-Path $projectRoot '.gradle/jdks/jdk-17.0.20.1+1'
if (-not (Test-Path (Join-Path $jdkDirectory 'bin/keytool.exe'))) {
    throw 'No está disponible el JDK del proyecto. Configura jdkDirectory antes de continuar.'
}
$privateDirectory = Join-Path $env:USERPROFILE '.gradle/intu-play'
$credentialsFile = Join-Path $privateDirectory 'upload-key.json'
$uploadKeystore = Join-Path $privateDirectory 'upload-key.p12'
$environmentNames = @('INTU_PLAY_KEY_PASSWORD', 'JAVA_HOME',
    'ORG_GRADLE_PROJECT_INTU_KEYSTORE_FILE', 'ORG_GRADLE_PROJECT_INTU_KEYSTORE_PASSWORD',
    'ORG_GRADLE_PROJECT_INTU_KEY_ALIAS', 'ORG_GRADLE_PROJECT_INTU_KEY_PASSWORD')
$previousEnvironment = @{}
foreach ($variableName in $environmentNames) {
    $previousEnvironment[$variableName] = [Environment]::GetEnvironmentVariable($variableName, 'Process')
}
Push-Location $projectRoot
try {
    if ($CreateUploadKey -and -not (Test-Path $credentialsFile)) {
        if (Test-Path $uploadKeystore) { throw 'Ya existe una clave sin su configuración. No se reemplazará.' }
        New-Item -ItemType Directory -Path $privateDirectory -Force | Out-Null
        $randomBytes = New-Object byte[] 36
        $random = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $random.GetBytes($randomBytes) } finally { $random.Dispose() }
        $password = [Convert]::ToBase64String($randomBytes)
        $env:INTU_PLAY_KEY_PASSWORD = $password
        & (Join-Path $jdkDirectory 'bin/keytool.exe') -genkeypair -keystore $uploadKeystore -storetype PKCS12 `
            -alias intu-play-upload -keyalg RSA -keysize 3072 -validity 10000 `
            -dname 'CN=Intu Upload' -storepass:env INTU_PLAY_KEY_PASSWORD -keypass:env INTU_PLAY_KEY_PASSWORD
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la clave de subida.' }
        @{ keystore = $uploadKeystore; alias = 'intu-play-upload'; password = $password } |
            ConvertTo-Json | Set-Content -LiteralPath $credentialsFile -Encoding utf8
        Write-Output 'Clave de subida creada fuera del repositorio. Conserva una copia privada de la carpeta .gradle/intu-play.'
    }
    if (-not (Test-Path $credentialsFile)) { throw 'Falta la clave de subida. Ejecuta primero con -CreateUploadKey.' }
    $credentials = Get-Content -LiteralPath $credentialsFile -Raw | ConvertFrom-Json
    if (-not (Test-Path -LiteralPath $credentials.keystore)) { throw 'No se encontró el archivo de la clave de subida.' }
    $env:INTU_PLAY_KEY_PASSWORD = $credentials.password
    $env:JAVA_HOME = $jdkDirectory
    if ($Build) {
        # Solo este proceso usa la clave Play; no cambia la firma de los APK de QA o de la web.
        $env:ORG_GRADLE_PROJECT_INTU_KEYSTORE_FILE = $credentials.keystore
        $env:ORG_GRADLE_PROJECT_INTU_KEYSTORE_PASSWORD = $credentials.password
        $env:ORG_GRADLE_PROJECT_INTU_KEY_ALIAS = $credentials.alias
        $env:ORG_GRADLE_PROJECT_INTU_KEY_PASSWORD = $credentials.password
        & (Join-Path $projectRoot 'gradlew-local.cmd') :app:bundleRelease :app:lintRelease --max-workers=1 `
            '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8' --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'La compilación o lint de release falló.' }
        $bundle = Join-Path $projectRoot 'app/build/outputs/bundle/release/app-release.aab'
        $verification = & (Join-Path $jdkDirectory 'bin/jarsigner.exe') '-J-Duser.language=en' -verify $bundle 2>&1
        if ($LASTEXITCODE -ne 0 -or ($verification -join "`n") -notmatch 'jar verified') {
            throw 'El AAB no tiene una firma verificable.'
        }
        $certificate = & (Join-Path $jdkDirectory 'bin/keytool.exe') '-J-Duser.language=en' -printcert -jarfile $bundle 2>&1
        if ($LASTEXITCODE -ne 0 -or ($certificate -join "`n") -match 'Android Debug') {
            throw 'El certificado del AAB no es válido para esta entrega Play.'
        }
        # Verifica los segmentos ELF de las bibliotecas ARM64 del propio AAB.
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $archive = [IO.Compression.ZipFile]::OpenRead($bundle)
        try {
            $nativeLibraries = @($archive.Entries | Where-Object { $_.FullName -match '^base/lib/arm64-v8a/.*\.so$' })
            if ($nativeLibraries.Count -eq 0) { throw 'No se encontraron bibliotecas ARM64 en el AAB.' }
            foreach ($entry in $nativeLibraries) {
                $stream = $entry.Open()
                $memory = New-Object IO.MemoryStream
                try { $stream.CopyTo($memory); $bytes = $memory.ToArray() }
                finally { $stream.Dispose(); $memory.Dispose() }
                if ($bytes[0] -ne 0x7f -or $bytes[1] -ne 0x45 -or $bytes[2] -ne 0x4c -or $bytes[3] -ne 0x46 `
                    -or $bytes[4] -ne 2 -or $bytes[5] -ne 1) { throw "ELF64 inválido: $($entry.FullName)" }
                $headerOffset = [BitConverter]::ToUInt64($bytes, 32)
                $headerSize = [BitConverter]::ToUInt16($bytes, 54)
                $headerCount = [BitConverter]::ToUInt16($bytes, 56)
                for ($headerIndex = 0; $headerIndex -lt $headerCount; $headerIndex++) {
                    $offset = [int]($headerOffset + $headerIndex * $headerSize)
                    if ([BitConverter]::ToUInt32($bytes, $offset) -eq 1) {
                        $alignment = [BitConverter]::ToUInt64($bytes, $offset + 48)
                        if ($alignment -lt 16384) { throw "Alineación menor que 16 KB: $($entry.FullName)" }
                    }
                }
            }
            Write-Output "ELF ARM64: $($nativeLibraries.Count) bibliotecas con segmentos alineados a 16 KB."
        } finally { $archive.Dispose() }
        $outputDirectory = Join-Path $projectRoot 'build/play'
        New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
        $targetBundle = Join-Path $outputDirectory 'intu-play.aab'
        Copy-Item -LiteralPath $bundle -Destination $targetBundle
        $certificate | Set-Content -LiteralPath (Join-Path $outputDirectory 'upload-certificate.txt') -Encoding utf8
        Get-FileHash -LiteralPath $targetBundle -Algorithm SHA256 | Format-List Path,Hash
        Write-Output 'AAB firmado y lint release correctos. No se subió a Play Console.'
    }
} finally {
    foreach ($variableName in $environmentNames) {
        [Environment]::SetEnvironmentVariable($variableName, $previousEnvironment[$variableName], 'Process')
    }
    Pop-Location
}
