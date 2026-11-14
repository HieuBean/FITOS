# =============================================================================
#  FITOS - Khoi dong nhanh (khong build), tach tien trinh khoi phien hien tai
#  Dung: powershell -NoProfile -ExecutionPolicy Bypass -File .\start-services.ps1
# =============================================================================
$WorkRoot   = 'C:\gymco'
$BackendDir = Join-Path $WorkRoot 'backend'
$FrontendDir= Join-Path $WorkRoot 'frontend'
$LogDir     = Join-Path $PSScriptRoot '.dsh'

function Test-Port($port) {
    try {
        $c = New-Object Net.Sockets.TcpClient
        $iar = $c.BeginConnect('127.0.0.1', $port, $null, $null)
        $open = $iar.AsyncWaitHandle.WaitOne(1200)
        if ($open) { $c.EndConnect($iar) }
        $c.Close(); return $open
    } catch { return $false }
}

# --- Backend -------------------------------------------------------------
if (-not (Test-Port 8080)) {
    $cp = (Join-Path $BackendDir 'target\classes') + ';' + (Get-Content (Join-Path $LogDir 'cp.txt') -Raw).Trim()
    $env:DB_HOST='127.0.0.1'; $env:DB_PORT='5432'
    $env:DB_USER='fitos_user'; $env:DB_PASSWORD='supersecret'
    $env:FITOS_BOOTSTRAP_ADMIN_PASSWORD='AdminBootstrap2026!'
    $env:REDIS_HOST='127.0.0.1'; $env:REDIS_PORT='6379'
    $bat = Join-Path $LogDir 'start-backend.bat'
    @"
@echo off
cd /d "$BackendDir"
java -Xmx1g -cp "$cp" com.fitos.FitosApplication > "$LogDir\backend-run.log" 2>&1
"@ | Set-Content -Path $bat -Encoding ASCII
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', $bat -WindowStyle Hidden | Out-Null
    for ($i=0; $i -lt 60; $i++) { Start-Sleep -Seconds 2; if (Test-Port 8080) { break } }
}

# --- Frontend ------------------------------------------------------------
if (-not (Test-Port 3000)) {
    $bat = Join-Path $LogDir 'start-frontend.bat'
    @"
@echo off
cd /d "$FrontendDir"
set NEXT_TELEMETRY_DISABLED=1
node node_modules\next\dist\bin\next start -p 3000 > "$LogDir\frontend-run.log" 2>&1
"@ | Set-Content -Path $bat -Encoding ASCII
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', $bat -WindowStyle Hidden | Out-Null
    for ($i=0; $i -lt 40; $i++) { Start-Sleep -Seconds 2; if (Test-Port 3000) { break } }
}

"BE8080=" + (Test-Port 8080)
"FE3000=" + (Test-Port 3000)
