# JabRef browser-extension fulltext native-messaging host — Windows mirror.
#
# Same protocol as jabext_host.py (req~bxf.*~1); on Windows the native-messaging
# host is PowerShell, launched via jabext_host.bat. Drivable by the same e2e
# harness (e2e_test.py) as the Python host.
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
$StatusFor = @{ 'no-pdf-found'=404; 'no-adapter'=404; 'auth-required'=403;
                'not-reachable'=502; 'timeout'=504; 'busy'=503; 'bad-request'=400;
                'internal-error'=500 }

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

$Gone = @{ error = 'not-reachable'; message = 'browser extension disconnected (native-messaging stdin closed)' }

function Round-Trip([hashtable]$msg, [int]$timeoutMs) {
    $Sync.Seq++                                     # only the single-threaded HTTP loop calls this
    $rid = "r$($Sync.Seq)"
    $msg['requestId'] = $rid
    $ev = New-Object System.Threading.ManualResetEventSlim $false
    $Sync.Pending[$rid] = @{ Event = $ev; Reply = $null }
    try {
        if ($Sync.Done) { return $Gone }
        try { Write-Frame $msg } catch { return $Gone }   # stdout closed: browser gone
        if (-not $ev.Wait($timeoutMs)) {
            return @{ error = 'timeout'; message = 'provider fetch exceeded internal timeout' }
        }
        $reply = $Sync.Pending[$rid].Reply
        if ($null -eq $reply) { return $Gone }          # woken by the reader on stdin EOF, no reply
        return $reply
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
        if (Reject-Bearer $ctx) { return }   # spec: every request carries the bearer token, health included
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
            if (-not $r.path -or -not (Test-Path -LiteralPath $r.path -PathType Leaf) -or (Get-Item -LiteralPath $r.path).Length -eq 0) {
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
# separate host (JabRefHost.ps1 / org.jabref.jabref, see ADR 0071), so messages
# without a requestId are ignored here.
$ReaderScript = {
    param($Sync)
    $stdin = [Console]::OpenStandardInput()
    function ReadFrame($s) {
        $head = New-Object byte[] 4; $n = 0
        while ($n -lt 4) { $r = $s.Read($head, $n, 4 - $n); if ($r -le 0) { return $null }; $n += $r }
        $len = [BitConverter]::ToInt32($head, 0); if ($len -le 0 -or $len -gt 1048576) { return $null }  # cap at 1 MiB
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
    # stdin EOF: browser/extension gone. Wake every HTTP thread parked in
    # Round-Trip so it answers 'not-reachable' now instead of after its own
    # (up to 5 min) timeout. If nothing is pending the main thread is blocked
    # in GetContext(); Stop() throws there and ends the loop. If something is
    # pending, the main loop sees Done after replying and stops the listener
    # itself (stopping it here would abort the in-flight response).
    $Sync.Done = $true
    foreach ($slot in @($Sync.Pending.Values)) { try { $slot.Event.Set() } catch {} }
    if ($Sync.Pending.Count -eq 0 -and $Sync.Listener) { try { $Sync.Listener.Stop() } catch {} }
}

function Main {
    $tf, $Sync.Bearer = Ensure-Token
    $Sync.Out = [Console]::OpenStandardOutput()
    # JABEXT_PORT (user env var) pins the port; only needed if HTTP.sys demands a
    # URL-ACL reservation (see below), which requires a fixed port to reserve.
    $port = if ($env:JABEXT_PORT) { [int]$env:JABEXT_PORT } else { Get-FreePort }

    # [FIDDLY] HttpListener goes through HTTP.sys, which may refuse Start() with
    # "Access is denied" when the prefix is not reserved for this user (netsh
    # http add urlacl). Verified on Windows 10 22H2: a plain, non-elevated user
    # binds http://127.0.0.1:<port>/ without any reservation (loopback prefixes
    # are exempt), so this is only a guard. The Java bridge sidesteps HTTP.sys
    # entirely (com.sun.net.httpserver binds a raw socket); a PS host could do
    # the same over a TcpListener if the guard ever fires in practice.
    $prefix = "http://127.0.0.1:$port/"
    $listener = [System.Net.HttpListener]::new()
    $listener.Prefixes.Add($prefix)
    try { $listener.Start() }
    catch [System.Net.HttpListenerException] {
        [Console]::Error.WriteLine("jabext_host: cannot listen on $prefix : $($_.Exception.Message)")
        if ($_.Exception.ErrorCode -eq 5) {            # ERROR_ACCESS_DENIED: URL-ACL missing
            [Console]::Error.WriteLine("jabext_host: HTTP.sys refused the prefix for this user. Pin a port and reserve it once (elevated prompt):")
            [Console]::Error.WriteLine("jabext_host:   setx JABEXT_PORT $port")
            [Console]::Error.WriteLine("jabext_host:   netsh http add urlacl url=http://127.0.0.1:$port/ user=$env:USERDOMAIN\$env:USERNAME")
            [Console]::Error.WriteLine("jabext_host: then restart the browser. See browser-bridge/README.md.")
        }
        exit 1
    }
    $Sync.Listener = $listener

    # start the NM reader on its own thread/runspace (BeginInvoke is async)
    $ps = [PowerShell]::Create()
    $ps.AddScript($ReaderScript).AddArgument($Sync) | Out-Null
    $async = $ps.BeginInvoke()

    New-Item -ItemType Directory -Force -Path $DiscoveryDir | Out-Null
    # Per-instance filename (keyed by port): concurrent hosts coexist as separate
    # providers instead of clobbering one shared file (JabRef enumerates every *.json).
    $discovery = Join-Path $DiscoveryDir "$ProviderName.$port.json"
    # Publish atomically (write temp, then replace) so JabRef never reads a partial file.
    $discoveryTmp = "$discovery.$PID.tmp"
    @{ name = $ProviderName; displayName = $DisplayName; port = $port
       tokenFile = $tf; protocolVersion = $ProtocolVersion } |
        ConvertTo-Json -Compress | Set-Content $discoveryTmp -NoNewline
    Move-Item -Force -Path $discoveryTmp -Destination $discovery

    try {
        while (-not $Sync.Done) {
            try { $ctx = $listener.GetContext() }   # blocks; Listener.Stop() throws here on shutdown
            catch { break }
            try { Handle-Context $ctx }
            catch { try { Send-Err $ctx 500 'internal-error' "$($_.Exception.Message)" } catch {} }
        }
    } finally {
        # The discovery file is unique to this instance (keyed by port), so removing it
        # on exit cannot strand another concurrently-running host.
        Remove-Item $discovery -ErrorAction SilentlyContinue
        if ($listener.IsListening) { $listener.Stop() }
        $ps.EndInvoke($async); $ps.Dispose()
    }
}

Main
