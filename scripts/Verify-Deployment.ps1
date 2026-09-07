param(
    [string]$BaseUri = 'https://t08-passkey-portfolio.vercel.app',
    [string]$Origin = $BaseUri
)

$ErrorActionPreference = 'Stop'
$BaseUri = $BaseUri.TrimEnd('/')
$session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$startupMessage = 'Service is starting. Please retry shortly.'

function Test-StartupResponse($Response) {
    return [int]$Response.StatusCode -eq 503 -and `
        $Response.Content.Trim() -eq $startupMessage
}

function Invoke-Get([string]$Path) {
    foreach ($attempt in 1..10) {
        $response = Invoke-WebRequest "$BaseUri$Path" -WebSession $session `
            -SkipHttpErrorCheck -TimeoutSec 60
        if (-not (Test-StartupResponse $response)) { return $response }

        Write-Host "GET $Path is starting; retry $attempt/10"
        Start-Sleep -Seconds 2
    }
    throw "GET $Path remained in startup state after 10 attempts"
}

function Assert-Status($Response, [int]$Expected, [string]$Label) {
    if ([int]$Response.StatusCode -ne $Expected) {
        throw "$Label expected $Expected, received $($Response.StatusCode)"
    }
    Write-Output "$Label => $Expected"
}

function Send-Json([string]$Path, $Body, [hashtable]$Headers) {
    foreach ($attempt in 1..10) {
        # PowerShell retains request headers on WebRequestSession; clear them so
        # negative checks cannot accidentally reuse the previous CSRF/Origin.
        $session.Headers.Clear()
        $response = Invoke-WebRequest -Uri "$BaseUri$Path" -Method Post -WebSession $session `
            -ContentType 'application/json' -Headers $Headers `
            -Body ($Body | ConvertTo-Json -Compress -Depth 10) `
            -SkipHttpErrorCheck -TimeoutSec 60
        if (-not (Test-StartupResponse $response)) { return $response }

        Write-Host "POST $Path is starting; retry $attempt/10"
        Start-Sleep -Seconds 2
    }
    throw "POST $Path remained in startup state after 10 attempts"
}

$health = Invoke-Get '/health'
Assert-Status $health 200 'GET /health'
if (($health.Content | ConvertFrom-Json).status -ne 'UP') { throw 'Health is not UP' }

$public = Invoke-Get '/'
Assert-Status $public 200 'GET /'
foreach ($path in @('/private', '/api/private-items', '/api/passkeys')) {
    $response = Invoke-Get $path
    Assert-Status $response 401 "Unauthenticated GET $path"
    if ($response.Headers['Cache-Control'] -notmatch 'no-store') {
        throw "Private response is missing no-store: $path"
    }
}

$access = Invoke-Get '/access'
Assert-Status $access 200 'GET /access'
$csrfMatch = [regex]::Match($access.Content, '<meta name="csrf-token" content="([^"]+)"')
if (-not $csrfMatch.Success) { throw 'CSRF meta tag missing' }
$headers = @{ Origin = $Origin; 'X-CSRF-Token' = $csrfMatch.Groups[1].Value }
$body = @{ displayName = 'Synthetic deployment check'; nickname = 'Synthetic check key' }
$first = Send-Json '/api/webauthn/register/options' $body $headers
Assert-Status $first 200 'Registration options 1'
$second = Send-Json '/api/webauthn/register/options' $body $headers
Assert-Status $second 200 'Registration options 2'
$one = $first.Content | ConvertFrom-Json
$two = $second.Content | ConvertFrom-Json
if ($one.publicKey.challenge -eq $two.publicKey.challenge) { throw 'Repeated challenge' }
if ($two.publicKey.rp.id -ne ([uri]$Origin).Host) { throw 'Unexpected RP ID' }
Write-Output 'Challenges differ; RP ID matches; values redacted'

$cancel = Send-Json '/api/webauthn/register/cancel' @{ceremonyId=$two.ceremonyId} $headers
Assert-Status $cancel 204 'Cancel synthetic registration'
$badOrigin = Send-Json '/api/webauthn/register/options' $body @{
    Origin = 'https://invalid.example'; 'X-CSRF-Token' = $csrfMatch.Groups[1].Value
}
Assert-Status $badOrigin 403 'Foreign Origin'
$missingCsrf = Send-Json '/api/webauthn/register/options' $body @{Origin=$Origin}
Assert-Status $missingCsrf 403 'Missing CSRF'

# This is an anonymous session; authenticated logout is deliberately out of scope.
# Its short-lived server-side session expires normally.
Write-Output 'PASS: deployed HTTP and registration-option boundaries (no passkey/account created)'
