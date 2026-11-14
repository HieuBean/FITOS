# =============================================================================
#  FITOS - Dung toan bo he thong
#  Cach dung:  .\stop.ps1
# =============================================================================
param(
    [switch]$KeepDb
)
$ErrorActionPreference = 'Continue'

function Write-Step($msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "  [OK] $msg" -ForegroundColor Green }

function Stop-Port($port, $label) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        try {
            Stop-Process -Id $conn.OwningProcess -Force -ErrorAction Stop
            Write-Ok "Da dung $label (port $port, PID $($conn.OwningProcess))"
        } catch {
            Write-Host "  [!] Khong dung duoc $label (PID $($conn.OwningProcess)): $($_.Exception.Message)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  - $label (port $port) khong chay"
    }
}

Write-Step 'Dung cac service ung dung'
Stop-Port 3000 'Frontend'
Stop-Port 8080 'Backend'
Stop-Port 8004 'AI Workout'
Stop-Port 8001 'AI CRM'
Stop-Port 8002 'AI Commerce'
Stop-Port 8003 'AI Operations'

# Don tien trinh node/java con sot (khong dong vao PostgreSQL)
Write-Step 'Don tien trinh con sot'
$killed = 0
Get-Process node -ErrorAction SilentlyContinue | ForEach-Object {
    try { Stop-Process -Id $_.Id -Force; $killed++ } catch {}
}
Get-Process java -ErrorAction SilentlyContinue | ForEach-Object {
    try { Stop-Process -Id $_.Id -Force; $killed++ } catch {}
}
Write-Ok "Da dung $killed tien trinh node/java"

if (-not $KeepDb) {
    Write-Step 'PostgreSQL'
    Write-Host '  - Van dang chay (du lieu duoc giu lai).'
    Write-Host '    Muon tat:  Stop-Service postgresql-x64-18' -ForegroundColor DarkGray
}

Write-Host ''
Write-Host '  Da dung he thong.' -ForegroundColor Green
Write-Host '  Khoi dong lai:  .\run.ps1 -SkipBuild' -ForegroundColor DarkGray
Write-Host ''
