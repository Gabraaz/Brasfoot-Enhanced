$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$buildPath = Join-Path $PSScriptRoot 'transfer-analysis'
$jdkPath = 'C:\Program Files\Java\jdk-19\bin'
$exports = @('--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED')
$exeBytes = [IO.File]::ReadAllBytes((Join-Path $projectPath 'bf 26-27 - patrocinio.exe'))
$zipOffset = -1
for ($i = 0; $i -lt $exeBytes.Length - 4; $i++) {
    if ($exeBytes[$i] -eq 80 -and $exeBytes[$i+1] -eq 75 -and $exeBytes[$i+2] -eq 3 -and $exeBytes[$i+3] -eq 4) { $zipOffset = $i; break }
}
if ($zipOffset -lt 0) { throw 'JAR embutido não encontrado' }
[IO.File]::WriteAllBytes((Join-Path $buildPath 'game.jar'), $exeBytes[$zipOffset..($exeBytes.Length-1)])
& "$jdkPath\javac.exe" --release 8 -encoding UTF-8 -cp "$buildPath\game.jar" -d "$buildPath\classes" "$PSScriptRoot\NegotiationRules.java" "$PSScriptRoot\TransferNegotiation.java" "$PSScriptRoot\SponsorshipState.java" "$PSScriptRoot\NegotiationRulesTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação do módulo' }
& "$jdkPath\java.exe" -cp "$buildPath\classes" mods.NegotiationRulesTest
if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes' }
& "$jdkPath\javac.exe" @exports -d "$buildPath\classes" "$PSScriptRoot\PatchTransfers.java"
if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação do patch' }
$outputPath = Join-Path $buildPath ('transfer-test-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.exe')
& "$jdkPath\java.exe" @exports -cp "$buildPath\classes;$buildPath\game.jar" PatchTransfers "$projectPath\bf 26-27 - patrocinio.exe" "$buildPath\classes" $outputPath
if ($LASTEXITCODE -ne 0) { throw 'Falha na geração do executável de teste' }
& "$jdkPath\javac.exe" -encoding UTF-8 -cp "$buildPath\game.jar;$buildPath\classes" -d "$buildPath\test-classes" "$PSScriptRoot\TransferIntegrationTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação dos testes de integração' }
& "$jdkPath\java.exe" -Xverify:all -cp "$outputPath;$buildPath\test-classes" mods.TransferIntegrationTest
if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes de integração; não instale este executável' }
& "$jdkPath\javac.exe" -encoding UTF-8 -cp "$buildPath\game.jar;$buildPath\classes" -d "$buildPath\test-classes" "$PSScriptRoot\SponsorshipStateTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação do teste de patrocínio' }
& "$jdkPath\java.exe" -Xverify:all -cp "$outputPath;$buildPath\test-classes" mods.SponsorshipStateTest
if ($LASTEXITCODE -ne 0) { throw 'Falha no teste de patrocínio; não instale este executável' }
