# Deploys the whole backend. Safe to re-run: migrations are idempotent and an existing token is reused.
#   powershell -ExecutionPolicy Bypass -File supabase\deploy.ps1 -LearningLoggerPath C:\path\to\learning-logger
# Reads SUPABASE_ACCESS_TOKEN from .env and supabase.url from local.properties. Never prints a secret.
param(
    [Parameter(Mandatory = $true)]
    [string]$LearningLoggerPath
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$learning = (Resolve-Path -LiteralPath $LearningLoggerPath).Path

function Read-KeyValues($path) {
    $map = @{}
    foreach ($line in Get-Content $path) {
        if ($line -match '^\s*([^#=\s][^=]*?)\s*=\s*(.*?)\s*$') { $map[$Matches[1]] = $Matches[2] }
    }
    $map
}

$accessToken = (Read-KeyValues "$root\.env")['SUPABASE_ACCESS_TOKEN']
$url = (Read-KeyValues "$root\local.properties")['supabase.url'].TrimEnd('/')
if (-not $accessToken) { throw 'SUPABASE_ACCESS_TOKEN missing from .env' }
if (-not $url) { throw 'supabase.url missing from local.properties' }
$ref = ([uri]$url).Host.Split('.')[0]
$env:SUPABASE_ACCESS_TOKEN = $accessToken

# 1. Migrations, through the Management API so no database password is needed.
$headers = @{ Authorization = "Bearer $accessToken" }
foreach ($file in Get-ChildItem "$PSScriptRoot\migrations\*.sql" | Sort-Object Name) {
    # ReadAllText, not Get-Content -Raw: PS 5.1 attaches PSPath etc. and ConvertTo-Json serialises them.
    $body = @{ query = [IO.File]::ReadAllText($file.FullName) } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "https://api.supabase.com/v1/projects/$ref/database/query" `
        -Headers $headers -ContentType 'application/json' `
        -Body ([Text.Encoding]::UTF8.GetBytes($body)) | Out-Null
    Write-Host "Applied $($file.Name)"
}

# 2. The laptop secret. Reused if it already exists so the learning logger stays valid.
$secretDir = Join-Path $learning '.secrets'
$secretFile = Join-Path $secretDir 'streak.env'
$token = $null
if (Test-Path $secretFile) { $token = (Read-KeyValues $secretFile)['LEARNING_TOKEN'] }
if (-not $token) {
    $bytes = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $token = -join ($bytes | ForEach-Object { $_.ToString('x2') })
}
New-Item -ItemType Directory -Force $secretDir | Out-Null
$content = "LEARNING_TOKEN=$token`nSTREAK_URL=$url/functions/v1/record-session`n"
[IO.File]::WriteAllText($secretFile, $content)
Write-Host "Wrote $secretFile"

$gitignore = Join-Path $learning '.gitignore'
if (-not (Test-Path $gitignore) -or -not (Select-String -Path $gitignore -SimpleMatch '.secrets/' -Quiet)) {
    Add-Content -Path $gitignore -Value '.secrets/'
    Write-Host "Added .secrets/ to $gitignore"
}

# 3. Function secret and deploy. --use-api bundles on Supabase's side, so Docker isn't needed.
Push-Location $root
try {
    npx --yes supabase@latest secrets set "LEARNING_TOKEN=$token" --project-ref $ref
    if ($LASTEXITCODE -ne 0) { throw 'secrets set failed' }
    npx --yes supabase@latest functions deploy record-session --project-ref $ref --no-verify-jwt --use-api
    if ($LASTEXITCODE -ne 0) { throw 'functions deploy failed' }
} finally {
    Pop-Location
}
Write-Host 'Backend deployed.'
