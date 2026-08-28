# Merged JabRef native-messaging host — Windows mirror.
#
# Same protocol as jabext_host.py (req~bxf.*~1). This is the "second
# implementation to maintain" the Python route costs on Windows, where the
# NM host is PowerShell (JabRefHost.bat -> .ps1), not Python. Drivable by the
# same e2e harness (e2e_test.py) as the Python host.
#
# Structure: the HTTP listener loop runs on the main thread (it needs the rich
# helpers); a small background runspace owns the blocking stdin native-messaging
# read loop and delivers replies into a shared, synchronized correlation map.
# The three [FIDDLY] spots are where PowerShell is bulkier than Python:
#   - ephemeral port (HttpListener has no port-0 auto-assign)  -> Get-FreePort
#   - cross-runspace shared state (reader runspace <-> main)   -> $Sync
#   - manual stdio byte framing                                -> Read/Write-Frame
$ErrorActionPreference = 'Stop'

$ProviderName    = 'jabext-bridge'
$DisplayName     = 'JabRef Browser Extension (experimental)'
$ProtocolVersion = 1
$FetchTimeoutMs  = 300000
$MathTimeoutMs   = 10000

# test/CI hook via $env:JABEXT_CONFIG_BASE; else the real per-user JabRef config
$ConfigBase = if ($env:JABEXT_CONFIG_BASE) { $env:JABEXT_CONFIG_BASE } else { Join-Path $env:APPDATA 'JabRef' }
$DiscoveryDir = Join-Path $ConfigBase 'fulltext-providers'
$TokenDir     = Join-Path $ConfigBase 'fulltext-providers-state'
$StatusFor = @{ 'no-pdf-found'=404; 'no-adapter'=404; 'auth-required'=404;
                'not-reachable'=404; 'timeout'=504; 'busy'=503; 'bad-request'=400 }

function Ensure-Token {
    New-Item -ItemType Directory -Force -Path $TokenDir | Out-Null
    $f = Join-Path $TokenDir "$ProviderName.token"
    if ((Test-Path $f) -and (Get-Item $f).Length -gt 0) { return @($f, (Get-Content $f -Raw).Trim()) }
    $bytes = New-Object byte[] 32; [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    $tok = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_')
    Set-Content -Path $f -Value $tok -NoNewline    # %APPDATA% is already user-only ACL
    return @($f, $tok)
}

# [FIDDLY] shared, thread-safe state reachable from the reader runspace.
# In Python this is just module globals + a Lock.
$Sync = [hashtable]::Synchronized(@{
    Pending = [hashtable]::Synchronized(@{}); Bearer = ''; Seq = 0
    Out = $null; OutLock = (New-Object object); Listener = $null; Done = $false
})

function Write-Frame($obj) {                        # [FIDDLY] manual 4-byte len + json
    $json = ($obj | ConvertTo-Json -Compress -Depth 6)
    $data = [Text.Encoding]::UTF8.GetBytes($json)
    [System.Threading.Monitor]::Enter($Sync.OutLock)
    try {
        $Sync.Out.Write([BitConverter]::GetBytes([int]$data.Length), 0, 4)
        $Sync.Out.Write($data, 0, $data.Length); $Sync.Out.Flush()
    } finally { [System.Threading.Monitor]::Exit($Sync.OutLock) }
}

function Round-Trip([hashtable]$msg, [int]$timeoutMs) {
    $Sync.Seq++                                     # only the single-threaded HTTP loop calls this
    $rid = "r$($Sync.Seq)"
    $msg['requestId'] = $rid
    $ev = New-Object System.Threading.ManualResetEventSlim $false
    $Sync.Pending[$rid] = @{ Event = $ev; Reply = $null }
    try {
        Write-Frame $msg
        if (-not $ev.Wait($timeoutMs)) {
            return @{ error = 'timeout'; message = 'provider fetch exceeded internal timeout' }
        }
        return $Sync.Pending[$rid].Reply
    } finally { $Sync.Pending.Remove($rid) }
}

function Get-FreePort {                              # [FIDDLY] HttpListener has no port 0
    $l = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $l.Start(); $p = $l.LocalEndpoint.Port; $l.Stop(); return $p
}

# --- HTTP helpers (mirror the Python Handler one-for-one) ---
# Explicit (no ?? operator) so this also runs on Windows PowerShell 5.1, which
# JabRefHost.bat falls back to when pwsh.exe is absent.
function Status-For($code) { if ($StatusFor.ContainsKey($code)) { $StatusFor[$code] } else { 500 } }
function Msg-Of($r)        { if ($r.message) { $r.message } else { $r.error } }

function Send-Json($ctx, [int]$status, $obj) {
    $data = [Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Compress -Depth 6))
    $ctx.Response.StatusCode = $status
    $ctx.Response.ContentType = 'application/json'
    $ctx.Response.ContentLength64 = $data.Length
    $ctx.Response.OutputStream.Write($data, 0, $data.Length)
    $ctx.Response.OutputStream.Close()
}
function Send-Err($ctx, [int]$status, $code, $msg) { Send-Json $ctx $status @{ error = $code; message = $msg } }

function Reject-Origin($ctx) {
    $o = $ctx.Request.Headers['Origin']
    if ([string]::IsNullOrEmpty($o) -or $o -eq 'null') { return $false }
    Send-Err $ctx 403 'bad-request' 'Origin header rejected'; return $true
}
function Reject-Bearer($ctx) {
    $a = [string]$ctx.Request.Headers['Authorization']
    if (-not $a.StartsWith('Bearer ') -or ($a.Substring(7).Trim() -ne $Sync.Bearer)) {  # constant-time in prod
        Send-Err $ctx 401 'bad-request' 'Missing or invalid bearer token'; return $true
    }
    return $false
}
function Read-Body($ctx) {
    $sr = New-Object IO.StreamReader($ctx.Request.InputStream, [Text.Encoding]::UTF8)
    try { $t = $sr.ReadToEnd() } finally { $sr.Close() }
    if ([string]::IsNullOrWhiteSpace($t)) { return @{} }
    return ($t | ConvertFrom-Json)
}

function Handle-Context($ctx) {
    $path = $ctx.Request.Url.AbsolutePath
    $method = $ctx.Request.HttpMethod

    if ($method -eq 'GET' -and $path -eq '/v1/health') {
        if (Reject-Origin $ctx) { return }
        Send-Json $ctx 200 @{ ok = $true; name = $ProviderName; protocolVersion = $ProtocolVersion }; return
    }
    if ($method -ne 'POST') { Send-Err $ctx 404 'bad-request' 'unknown endpoint'; return }
    if (Reject-Origin $ctx) { return }
    if (Reject-Bearer $ctx) { return }

    try { $body = Read-Body $ctx } catch { Send-Err $ctx 400 'bad-request' 'Malformed request body'; return }

    switch ($path) {
        '/v1/fulltext' {
            $doi = $body.doi; $url = $body.url
            if (-not $doi -and -not $url) { Send-Err $ctx 400 'bad-request' 'At least one of doi or url is required'; return }
            $r = Round-Trip @{ type = 'fetchFulltext'; doi = $doi; url = $url } $FetchTimeoutMs
            if ($r.error) { Send-Err $ctx (Status-For $r.error) $r.error (Msg-Of $r); return }
            if (-not $r.path -or -not (Test-Path -LiteralPath $r.path -PathType Leaf)) {
                Send-Err $ctx 404 'no-pdf-found' 'Provider returned no readable PDF path'; return
            }
            $out = @{ id = $r.id; path = $r.path }
            if ($r.sourceUrl) { $out['sourceUrl'] = $r.sourceUrl }
            Send-Json $ctx 200 $out; return
        }
        '/v1/mathscinet/open' {
            if (-not $body.mrNumber) { Send-Err $ctx 400 'bad-request' 'mrNumber is required'; return }
            $r = Round-Trip @{ type = 'openMathSciNet'; mrNumber = $body.mrNumber } $MathTimeoutMs
            if ($r.error) { Send-Err $ctx (Status-For $r.error) $r.error (Msg-Of $r); return }
            if (-not $r.action -or $null -eq $r.tabId) { Send-Err $ctx 500 'internal-error' 'Provider returned no tab action'; return }
            Send-Json $ctx 200 @{ action = $r.action; tabId = [int]$r.tabId }; return
        }
        default { Send-Err $ctx 404 'bad-request' 'unknown endpoint' }
    }
}

# Background runspace: blocking native-messaging read loop. Reads frames and
# delivers fulltext replies (correlated by requestId). Import is served by a
# separate host (JabRefHost.ps1 / org.jabref.jabref, see ADR 0070), so messages
# without a requestId are ignored here.
$ReaderScript = {
    param($Sync)
    $stdin = [Console]::OpenStandardInput()
    function ReadFrame($s) {
        $head = New-Object byte[] 4; $n = 0
        while ($n -lt 4) { $r = $s.Read($head, $n, 4 - $n); if ($r -le 0) { return $null }; $n += $r }
        $len = [BitConverter]::ToInt32($head, 0); if ($len -le 0) { return $null }
        $buf = New-Object byte[] $len; $n = 0
        while ($n -lt $len) { $r = $s.Read($buf, $n, $len - $n); if ($r -le 0) { return $null }; $n += $r }
        return [Text.Encoding]::UTF8.GetString($buf) | ConvertFrom-Json
    }
    while ($true) {
        $msg = ReadFrame $stdin
        if ($null -eq $msg) { break }               # stdin EOF: browser/extension gone
        if ($msg.PSObject.Properties.Name -contains 'requestId') {
            $slot = $Sync.Pending[$msg.requestId]
            if ($slot) { $slot.Reply = $msg; $slot.Event.Set() }
        }
    }
    $Sync.Done = $true
    if ($Sync.Listener) { $Sync.Listener.Stop() }   # unblock the main GetContext()
}

function Main {
    $tf, $Sync.Bearer = Ensure-Token
    $Sync.Out = [Console]::OpenStandardOutput()
    $port = Get-FreePort

    # [FIDDLY] HttpListener as a non-admin user needs a one-time URL-ACL
    # reservation (netsh http add urlacl url=http://127.0.0.1:PORT/ user=...),
    # or it throws "Access is denied" on Start(). The Java bridge sidesteps this
    # entirely: com.sun.net.httpserver binds a raw socket, no ACL. A fully
    # robust PS host would likewise hand-roll HTTP over a TcpListener — more code.
    $listener = [System.Net.HttpListener]::new()
    $listener.Prefixes.Add("http://127.0.0.1:$port/")
    $listener.Start()
    $Sync.Listener = $listener

    # start the NM reader on its own thread/runspace (BeginInvoke is async)
    $ps = [PowerShell]::Create()
    $ps.AddScript($ReaderScript).AddArgument($Sync) | Out-Null
    $async = $ps.BeginInvoke()

    New-Item -ItemType Directory -Force -Path $DiscoveryDir | Out-Null
    $discovery = Join-Path $DiscoveryDir "$ProviderName.json"
    @{ name = $ProviderName; displayName = $DisplayName; port = $port
       tokenFile = $tf; protocolVersion = $ProtocolVersion } |
        ConvertTo-Json -Compress | Set-Content $discovery -NoNewline

    try {
        while (-not $Sync.Done) {
            try { $ctx = $listener.GetContext() }   # blocks; Listener.Stop() throws here on shutdown
            catch { break }
            Handle-Context $ctx
        }
    } finally {
        Remove-Item $discovery -ErrorAction SilentlyContinue
        if ($listener.IsListening) { $listener.Stop() }
        $ps.EndInvoke($async); $ps.Dispose()
    }
}

Main
