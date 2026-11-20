<#
CodeClear launcher (PowerShell). Bootstrap only: all behavior lives in the backend.
Usage: codeclear.ps1 [options] [path ...]   review the Java files under each path (default: the current folder)
       codeclear.ps1 --help                 show the review options
       codeclear.ps1 serve                  start the web application and open the browser
A leading "review" is accepted and ignored, so "codeclear.ps1 review src" also works.
Env: CODECLEAR_PORT (serve, default 8080), CODECLEAR_JAR (explicit jar path), CODECLEAR_NO_BROWSER=1 (serve),
     CODECLEAR_RULES (rules folder for a review, default: review next to this script)
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
# Read the first line that contains: version "NN...". Line 1 is not enough: JAVA_TOOL_OPTIONS makes java print
# "Picked up JAVA_TOOL_OPTIONS: ..." first.
$verLine = & java -version 2>&1 | ForEach-Object { $_.ToString() } | Where-Object { $_ -match 'version "(\d+)' } | Select-Object -First 1
$javaMajor = if ($verLine -match 'version "(\d+)') { [int]$Matches[1] } else { 0 }
if ($javaMajor -lt $RequiredJava) {
    Write-Error "Java $RequiredJava or newer is required (found: $(if ($javaMajor) { $javaMajor } else { 'unknown' }))"
}

# 2b. The default is a command-line review: no server, no browser, nothing started that the review does not need.
#     Only "serve" starts the web application. The rules folder defaults to the "review" folder next to this script
#     (CODECLEAR_RULES overrides it); it goes first so an explicit --rules=<dir> from the caller wins.
$rest = @($args)
if ($rest.Count -gt 0 -and $rest[0] -eq 'serve') {
    $rest = @($rest | Select-Object -Skip 1)
} else {
    if ($rest.Count -gt 0 -and $rest[0] -eq 'review') { $rest = @($rest | Select-Object -Skip 1) }
    $RulesDir = if ($env:CODECLEAR_RULES) { $env:CODECLEAR_RULES } else { Join-Path $Dir 'review' }
    $extra = @()
    if (Test-Path $RulesDir) { $extra = @("--rules=$RulesDir") }
    & java --enable-native-access=ALL-UNNAMED -jar $Jar review @extra @rest
    exit $LASTEXITCODE
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
& java --enable-native-access=ALL-UNNAMED -jar $Jar "--server.port=$Port" @rest
exit $LASTEXITCODE
