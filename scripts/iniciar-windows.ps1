param(
    [ValidateSet('Servidor', 'Pruebas', 'Paquete')]
    [string]$Modo = 'Servidor'
)

$proyecto = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$javaLocal = Join-Path $env:USERPROFILE '.cache\codex-jdk21\jdk-21.0.12.1+1'

$candidatosJdk = @()
if ($env:JAVA_HOME) { $candidatosJdk += $env:JAVA_HOME }
if (Test-Path -LiteralPath (Join-Path $javaLocal 'bin\java.exe')) { $candidatosJdk += $javaLocal }

$directoriosJava = @(
    (Join-Path $env:ProgramFiles 'Java'),
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
    (Join-Path $env:ProgramFiles 'Microsoft'),
    (Join-Path $env:ProgramFiles 'Amazon Corretto'),
    (Join-Path $env:ProgramFiles 'Zulu')
)
if (${env:ProgramFiles(x86)}) {
    $directoriosJava += (Join-Path ${env:ProgramFiles(x86)} 'Java')
}

foreach ($directorio in $directoriosJava) {
    if (-not (Test-Path -LiteralPath $directorio)) { continue }
    $candidatosJdk += Get-ChildItem -LiteralPath $directorio -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '(^jdk-?21|^microsoft-jdk-21|^openjdk-21|^jdk21|^amazon-corretto-21|^zulu21)' } |
        Sort-Object Name -Descending |
        Select-Object -ExpandProperty FullName
}

$jdkValido = $candidatosJdk | Select-Object -Unique | Where-Object {
    $javaCandidato = Join-Path $_ 'bin\java.exe'
    $javacCandidato = Join-Path $_ 'bin\javac.exe'
    if (-not (Test-Path -LiteralPath $javaCandidato) -or -not (Test-Path -LiteralPath $javacCandidato)) { return $false }
    $versionCandidata = (& $javaCandidato -version 2>&1 | Select-Object -First 1)
    $versionCandidata -match '"21(?:\.|\")'
} | Select-Object -First 1

if ($jdkValido) {
    $env:JAVA_HOME = $jdkValido
}

$java = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    (Get-Command java.exe -ErrorAction SilentlyContinue).Source
}

if (-not $java -or -not (Test-Path -LiteralPath $java)) {
    throw 'Se requiere JDK 21. El script buscó JAVA_HOME, las carpetas estándar de Java y el PATH. Configure JAVA_HOME con la carpeta raíz del JDK 21 instalado.'
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
