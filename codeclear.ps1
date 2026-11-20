<#
CodeClear launcher (PowerShell). Bootstrap only: all behavior lives in the backend.
Env: CODECLEAR_PORT (default 8080), CODECLEAR_JAR (explicit jar path), CODECLEAR_NO_BROWSER=1
#>
$ErrorActionPreference = 'Stop'

$Dir = $PSScriptRoot
$Port = if ($env:CODECLEAR_PORT) { $env:CODECLEAR_PORT } else { '8080' }
$RequiredJava = 25

# 1. Locate the prebuilt jar (next to this script, or the Maven output for development).
$Jar = $env:CODECLEAR_JAR
if (-not $Jar) {
    foreach ($c in @((Join-Path $Dir 'codeclear.jar'), (Join-Path $Dir 'target/codeclear.jar'))) {
        if (Test-Path $c) { $Jar = $c; break }
    }
}
if (-not $Jar -or -not (Test-Path $Jar)) {
    Write-Error "codeclear.jar not found next to $PSCommandPath (set CODECLEAR_JAR to override)"
}

# 2. Java check.
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "java not found on PATH; Java $RequiredJava or newer is required"
}
$verLine = (& java -version 2>&1 | Select-Object -First 1).ToString()
$javaMajor = if ($verLine -match 'version "(\d+)') { [int]$Matches[1] } else { 0 }
if ($javaMajor -lt $RequiredJava) {
    Write-Error "Java $RequiredJava or newer is required (found: $(if ($javaMajor) { $javaMajor } else { 'unknown' }))"
}

# 3. Copilot CLI is optional (only needed for the Copilot provider), so warn rather than fail.
if (-not (Get-Command copilot.exe -ErrorAction SilentlyContinue)) {
    Write-Warning 'copilot.exe not found on PATH; the Copilot provider will be unavailable'
}

# 4. Open the browser once the backend reports healthy (background job, best effort).
if ($env:CODECLEAR_NO_BROWSER -ne '1') {
    Start-Job -ArgumentList $Port -ScriptBlock {
        param($p)
        for ($i = 0; $i -lt 60; $i++) {
            try {
                Invoke-RestMethod "http://127.0.0.1:$p/api/health" -TimeoutSec 2 | Out-Null
                Start-Process "http://127.0.0.1:$p/"
                return
            } catch { Start-Sleep -Seconds 1 }
        }
    } | Out-Null
}

Write-Host "Starting CodeClear on http://127.0.0.1:$Port/ (Ctrl+C to stop)"
& java -jar $Jar "--server.port=$Port" @args
exit $LASTEXITCODE
