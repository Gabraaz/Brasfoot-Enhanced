param(
    [string]$Executable = (Join-Path $PSScriptRoot "..\bf 26-27 - patrocinio.exe")
)

$ErrorActionPreference = "Stop"
$jdk = "C:\Program Files\Java\jdk-19\bin"
$javac = Join-Path $jdk "javac.exe"
$java = Join-Path $jdk "java.exe"
$classes = Join-Path $env:TEMP ("brasfoot-contract-fix-" + [guid]::NewGuid())

if (!(Test-Path -LiteralPath $Executable)) { throw "Executável não encontrado: $Executable" }
if (!(Test-Path -LiteralPath $javac)) { throw "JDK não encontrada: $javac" }

New-Item -ItemType Directory -Path $classes -Force | Out-Null
try {
    $exports = @(
        "--add-exports", "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
        "--add-exports", "java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED"
    )
    & $javac @exports -d $classes (Join-Path $PSScriptRoot "PatchSponsorshipContract.java")
    if ($LASTEXITCODE -ne 0) { throw "Não foi possível compilar o patch." }
    & $java @exports -cp $classes PatchSponsorshipContract $Executable
    if ($LASTEXITCODE -ne 0) { throw "Não foi possível aplicar o patch." }
} finally {
    if (Test-Path -LiteralPath $classes) {
        Remove-Item -LiteralPath $classes -Recurse -Force
    }
}


