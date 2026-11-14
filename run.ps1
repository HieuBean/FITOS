# =============================================================================
#  FITOS - Chay toan bo he thong (PostgreSQL + Backend + Frontend)
#
#  Cach dung:
#     .\run.bat                  (khuyen dung - tu xu ly execution policy)
#     .\run.ps1
#     .\run.ps1 -SkipBuild       (khong build lai, chay ngay)
#     .\run.ps1 -AiWorkout       (bat them AI Coach service)
#     .\run.ps1 -ResetDb         (xoa sach database roi tao lai)
# =============================================================================
param(
    [switch]$SkipBuild,
    [switch]$AiWorkout,
    [switch]$ResetDb
)
# Cac lenh ngoai (psql, java, mvn) ghi thong bao ra stderr, nen khong dung
# 'Stop' toan cuc; thay vao do kiem tra $LASTEXITCODE o tung buoc quan trong.
$ErrorActionPreference = 'Continue'

# --- Duong dan -----------------------------------------------------------
# Thu muc du an co dau tieng Viet ("D:\gym co") nen Java khong doc duoc
# classpath khi truyen qua tham so dong lenh. Dung junction khong dau cach
# "C:\gymco" tro vao cung thu muc de tranh van de nay.
# LUU Y: lay duong dan tu $PSScriptRoot (gia tri luc chay) thay vi viet
# chuoi co dau, vi PowerShell 5.1 doc file .ps1 khong BOM theo bang ma ANSI.
$ProjectRoot = $PSScriptRoot
$WorkRoot    = 'C:\gymco'
$BackendDir  = Join-Path $WorkRoot 'backend'
$FrontendDir = Join-Path $WorkRoot 'frontend'
$LogDir      = Join-Path $ProjectRoot '.dsh'

# --- Thong so ------------------------------------------------------------
$MvnHome = 'C:\Users\Admin\.m2\wrapper\dists\apache-maven-3.9.15\0226a00282e400185496f3b60ec5a3f029cbdc6893912937d4876d57695224e1\bin'
$Psql    = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$DbName  = 'fitos'
$DbUser  = 'fitos_user'
$DbPass  = 'supersecret'
$DbAdminPass = '123456'
$AdminPass   = 'AdminBootstrap2026!'

function Write-Step($msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn2($msg){ Write-Host "  [!]  $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "  [X]  $msg" -ForegroundColor Red }

function Test-Port($port) {
    try {
        $c = New-Object Net.Sockets.TcpClient
        $iar = $c.BeginConnect('127.0.0.1', $port, $null, $null)
        $open = $iar.AsyncWaitHandle.WaitOne(1200)
        if ($open) { $c.EndConnect($iar) }
        $c.Close()
        return $open
    } catch { return $false }
}

# =========================================================================
Write-Step '1/6  Kiem tra dieu kien'
# =========================================================================
if (-not (Test-Path (Join-Path $ProjectRoot 'backend\pom.xml'))) {
    Write-Err "Khong tim thay ma nguon backend trong $ProjectRoot"
    exit 1
}
if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory $LogDir -Force | Out-Null }

# Junction (tranh loi encoding duong dan tieng Viet)
if (-not (Test-Path (Join-Path $WorkRoot 'backend\pom.xml'))) {
    if (Test-Path $WorkRoot) { (Get-Item $WorkRoot).Delete() }
    New-Item -ItemType Junction -Path $WorkRoot -Target $ProjectRoot | Out-Null
    Write-Ok "Da tao junction C:\gymco -> $ProjectRoot"
} else {
    Write-Ok 'Junction C:\gymco san sang'
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Write-Err 'Chua cai Java 17'; exit 1 }
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { Write-Err 'Chua cai Node.js'; exit 1 }
Write-Ok ("Java " + ((& java -version 2>&1 | Select-Object -First 1) -replace '.*version "([^"]+)".*', '$1'))
Write-Ok ("Node " + (node -v))

# =========================================================================
Write-Step '2/6  Khoi dong PostgreSQL'
# =========================================================================
$pg = Get-Service postgresql* -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $pg) { Write-Err 'Khong tim thay dich vu PostgreSQL'; exit 1 }
if ($pg.Status -ne 'Running') {
    Start-Service $pg.Name
    Start-Sleep -Seconds 3
    Write-Ok "Da khoi dong $($pg.Name)"
} else {
    Write-Ok "$($pg.Name) dang chay"
}

$env:PGPASSWORD = $DbAdminPass
if ($ResetDb) {
    & $Psql -U postgres -h 127.0.0.1 -q -c "DROP DATABASE IF EXISTS $DbName WITH (FORCE);" 2>&1 | Out-Null
    Write-Warn2 'Da xoa database cu (ResetDb)'
}
$exists = & $Psql -U postgres -h 127.0.0.1 -tAc "SELECT 1 FROM pg_database WHERE datname='$DbName'" 2>&1
if ("$exists" -notmatch '1') {
    & $Psql -U postgres -h 127.0.0.1 -q -c "CREATE DATABASE $DbName OWNER $DbUser;" 2>&1 | Out-Null
    Write-Ok "Da tao database '$DbName'"
} else {
    Write-Ok "Database '$DbName' da ton tai"
}
& $Psql -U postgres -h 127.0.0.1 -d $DbName -q -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;" 2>&1 | Out-Null

# =========================================================================
Write-Step '3/6  Build backend'
# =========================================================================
$cpFile  = Join-Path $LogDir 'cp.txt'
$classes = Join-Path $BackendDir 'target\classes\com\fitos\FitosApplication.class'
$needBuild = $true
if ($SkipBuild -and (Test-Path $classes) -and (Test-Path $cpFile)) { $needBuild = $false }

if ($needBuild) {
    $env:Path = "$MvnHome;$env:Path"
    Push-Location $BackendDir
    try {
        if (-not (Test-Path $cpFile)) {
            mvn -o -B -q "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath" "-Dmdep.outputFile=target/cp.txt" 2>&1 | Out-Null
            if (Test-Path 'target\cp.txt') { Copy-Item 'target\cp.txt' $cpFile -Force }
        }
        mvn -o -B -q -DskipTests compile 2>&1 | Out-Null
        $rc = $LASTEXITCODE
    } finally { Pop-Location }
    if ($rc -ne 0) { Write-Err "Build backend that bai (ma $rc)"; exit 1 }
    Write-Ok 'Build backend xong'
} else {
    Write-Ok 'Dung lai build cu (SkipBuild)'
}

# =========================================================================
Write-Step '4/6  Khoi dong backend (port 8080)'
# =========================================================================
if (Test-Port 8080) {
    Write-Warn2 'Port 8080 dang co tien trinh chay - bo qua'
} else {
    $cp = (Join-Path $BackendDir 'target\classes') + ';' + (Get-Content $cpFile -Raw).Trim()
    $env:DB_HOST = '127.0.0.1'; $env:DB_PORT = '5432'
    $env:DB_USER = $DbUser;      $env:DB_PASSWORD = $DbPass
    $env:FITOS_BOOTSTRAP_ADMIN_PASSWORD = $AdminPass
    $env:REDIS_HOST = '127.0.0.1'; $env:REDIS_PORT = '6379'

    $log = Join-Path $LogDir 'backend-run.log'
    Start-Process -FilePath 'java' `
        -ArgumentList @('-Xmx1g', '-cp', $cp, 'com.fitos.FitosApplication') `
        -WorkingDirectory $BackendDir `
        -RedirectStandardOutput $log -RedirectStandardError "$log.err" `
        -WindowStyle Hidden | Out-Null

    $ok = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 2
        if (Test-Port 8080) { $ok = $true; break }
        if ((Test-Path $log) -and ((Get-Content $log -Raw) -match 'APPLICATION FAILED')) { break }
    }
    if ($ok) { Write-Ok 'Backend dang chay' }
    else {
        Write-Err 'Backend khong khoi dong duoc. 20 dong cuoi log:'
        if (Test-Path $log) { Get-Content $log -Tail 20 }
        exit 1
    }
}

# =========================================================================
Write-Step '5/6  Build frontend'
# =========================================================================
$feBuild = Join-Path $FrontendDir '.next\BUILD_ID'
if ($SkipBuild -and (Test-Path $feBuild)) {
    Write-Ok 'Dung lai build cu (SkipBuild)'
} else {
    $env:NEXT_TELEMETRY_DISABLED = '1'
    $env:NEXT_PUBLIC_BACKEND_URL = 'http://localhost:8080/api'
    Push-Location $FrontendDir
    try {
        if (-not (Test-Path 'node_modules')) { npm ci 2>&1 | Out-Null }
        npx next build 2>&1 | Out-Null
        $rc = $LASTEXITCODE
    } finally { Pop-Location }
    if ($rc -ne 0) { Write-Err 'Build frontend that bai'; exit 1 }
    Write-Ok 'Build frontend xong'
}

# =========================================================================
Write-Step '6/6  Khoi dong frontend (port 3000)'
# =========================================================================
if (Test-Port 3000) {
    Write-Warn2 'Port 3000 dang co tien trinh chay - bo qua'
} else {
    Start-Process -FilePath 'node.exe' `
        -ArgumentList 'node_modules\next\dist\bin\next', 'start', '-p', '3000' `
        -WorkingDirectory $FrontendDir `
        -RedirectStandardOutput (Join-Path $LogDir 'frontend-run.log') `
        -RedirectStandardError (Join-Path $LogDir 'frontend-run.log.err') `
        -WindowStyle Hidden | Out-Null

    $ok = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 2
        if (Test-Port 3000) { $ok = $true; break }
    }
    if ($ok) { Write-Ok 'Frontend dang chay' } else { Write-Err 'Frontend khong khoi dong duoc'; exit 1 }
}

# =========================================================================
if ($AiWorkout) {
    Write-Step 'Tuy chon  AI Workout Coach (port 8004)'
    if (Test-Port 8004) {
        Write-Warn2 'Port 8004 da chay'
    } else {
        $py = Join-Path $WorkRoot 'ai-workout\.venv\Scripts\python.exe'
        if (-not (Test-Path $py)) {
            Write-Warn2 'Chua co ai-workout\.venv. Cai bang:'
            Write-Host '      cd ai-workout'
            Write-Host '      python -m venv .venv'
            Write-Host '      .venv\Scripts\pip install -r requirements.txt'
        } else {
            Start-Process -FilePath $py -ArgumentList '-m','uvicorn','main:app','--host','0.0.0.0','--port','8004' `
                -WorkingDirectory (Join-Path $WorkRoot 'ai-workout') `
                -RedirectStandardOutput (Join-Path $LogDir 'ai-workout.log') `
                -RedirectStandardError (Join-Path $LogDir 'ai-workout.log.err') -WindowStyle Hidden | Out-Null
            Start-Sleep -Seconds 4
            if (Test-Port 8004) { Write-Ok 'AI Workout dang chay' } else { Write-Warn2 'AI Workout khong khoi dong duoc (khong bat buoc)' }
        }
    }
}

# =========================================================================
Write-Host ''
Write-Host '=================================================================' -ForegroundColor Green
Write-Host '  HE THONG DANG CHAY' -ForegroundColor Green
Write-Host '=================================================================' -ForegroundColor Green
Write-Host ''
Write-Host '  Giao dien :  http://localhost:3000' -ForegroundColor White
Write-Host '  Backend   :  http://localhost:8080' -ForegroundColor White
Write-Host '  Swagger   :  http://localhost:8080/swagger-ui.html' -ForegroundColor White
Write-Host ''
Write-Host '  Tai khoan (mat khau trong ngoac):' -ForegroundColor White
Write-Host '    admin@fitos.com    (AdminBootstrap2026!)   Quan tri vien'
Write-Host '    quanly@fitos.demo  (FitOs@2026)            Quan ly studio'
Write-Host '    letan@fitos.demo   (FitOs@2026)            Le tan / CRM'
Write-Host '    hlv@fitos.demo     (FitOs@2026)            Huan luyen vien'
Write-Host '    hoivien@fitos.demo (FitOs@2026)            Hoi vien'
Write-Host ''
Write-Host '  Dung he thong:  .\stop.bat' -ForegroundColor DarkGray
Write-Host '  Xem log:        .dsh\backend-run.log  |  .dsh\frontend-run.log' -ForegroundColor DarkGray
Write-Host ''
