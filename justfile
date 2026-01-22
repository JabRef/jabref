set windows-shell := ["powershell"]

[unix]
ensure-gg-cmd:
    [ ! -f gg.cmd ] && ( echo "gg.cmd not found — downloading..."; ( command -v wget >/dev/null 2>&1 && wget -O gg.cmd https://ggcmd.io/gg.cmd ) || ( command -v curl >/dev/null 2>&1 && curl -L https://ggcmd.io/gg.cmd -o gg.cmd ) || { echo "Error: neither wget nor curl is installed." >&2; exit 1; } )

[unix]
checkout-pr pr-id: ensure-gg-cmd
    if command -v gh >/dev/null 2>&1; then gh pr checkout {{pr-id}}; else sh ./gg.cmd jbang https://github.com/JabRef/jabref/blob/main/.jbang/CheckoutPR.java {{pr-id}}; fi

[unix]
run-brach branch: ensure-gg-cmd
    sh ./gg.cmd jbang git@jbangdev checkout {{branch}}
    sh ./gg.cmd jbang git@jbangdev fetch origin
    sh ./gg.cmd jbang git@jbangdev merge origin/main
    just run

[unix]
run: ensure-gg-cmd
    sh ./gg.cmd gradle :jabgui:run

[unix]
run-jabkit *FLAGS: ensure-gg-cmd
    sh ./gg.cmd gradle :jabkit:run --args="{{FLAGS}}"

[unix]
run-jabsrv *FLAGS: ensure-gg-cmd
    sh ./gg.cmd gradle :jabsrv-cli:run --args="{{FLAGS}}"

[windows]
ensure-gg-cmd:
    if (-not (Test-Path 'gg.cmd')) { Write-Host 'gg.cmd not found — downloading...'; Invoke-WebRequest 'https://ggcmd.io/gg.cmd' -OutFile 'gg.cmd' }

[windows]
checkout-pr pr-id: ensure-gg-cmd
    if (Get-Command gh -ErrorAction SilentlyContinue) { gh pr checkout {{pr-id}} } else { .\gg.cmd jbang https://github.com/JabRef/jabref/blob/main/.jbang/CheckoutPR.java {{pr-id}} }

[windows]
run-branch branch: ensure-gg-cmd
    .\gg.cmd jbang git@jbangdev checkout {{branch}}
    .\gg.cmd jbang git@jbangdev fetch origin
    .\gg.cmd jbang git@jbangdev merge origin/main
    just run

[windows]
run: ensure-gg-cmd
    .\gg.cmd gradle :jabgui:run

[windows]
run-jabkit *FLAGS: ensure-gg-cmd
    .\gg.cmd gradle :jabkit:run --args="{{FLAGS}}"

[windows]
run-jabsrv *FLAGS: ensure-gg-cmd
    .\gg.cmd gradle :jabsrv-cli:run --args="{{FLAGS}}"

run-main:
    just run-branch main

run-pr pr-id:
    just checkout-pr {{pr-id}}
    just run
