param(
    [ValidateSet('Servidor', 'Pruebas', 'Paquete')]
    [string]$Modo = 'Servidor'
)

$proyecto = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$javaLocal = Join-Path $env:USERPROFILE '.cache\codex-jdk21\jdk-21.0.12.1+1'

if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $javaLocal 'bin\java.exe'))) {
    $env:JAVA_HOME = $javaLocal
}

$java = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    (Get-Command java.exe -ErrorAction SilentlyContinue).Source
}

if (-not $java -or -not (Test-Path -LiteralPath $java)) {
    throw 'Se requiere JDK 21. Configure JAVA_HOME o instale JDK 21 antes de ejecutar el proyecto.'
}

$version = (& $java -version 2>&1 | Select-Object -First 1)
if ($version -notmatch '"21(?:\.|\")') {
    throw "Se requiere JDK 21; versión encontrada: $version"
}

$objetivo = switch ($Modo) {
    'Pruebas' { 'test' }
    'Paquete' { 'package' }
    default { 'spring-boot:run' }
}

Push-Location -LiteralPath $proyecto
try {
    & (Join-Path $proyecto 'mvnw.cmd') $objetivo
    if ($LASTEXITCODE -ne 0) {
        throw "Maven terminó con código $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
