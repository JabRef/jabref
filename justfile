set windows-shell := ["powershell"]

[unix]
ensure-gg-cmd:
    [ ! -f gg.cmd ] && ( echo "gg.cmd not found — downloading..."; ( command -v wget >/dev/null 2>&1 && wget -O gg.cmd https://ggcmd.io/gg.cmd ) || ( command -v curl >/dev/null 2>&1 && curl -L https://ggcmd.io/gg.cmd -o gg.cmd ) || { echo "Error: neither wget nor curl is installed." >&2; exit 1; } )

[unix]
checkout-pr pr-id: ensure-gg-cmd
    if command -v gh >/dev/null 2>&1; then gh pr checkout {{pr-id}}; else sh ./gg.cmd jbang https://github.com/JabRef/jabref/blob/main/.jbang/CheckoutPR.java {{pr-id}}; fi

[unix]
run-branch branch: ensure-gg-cmd
    sh ./gg.cmd jbang git@jbangdev checkout {{branch}}
    sh ./gg.cmd jbang git@jbangdev fetch origin
    sh ./gg.cmd jbang git@jbangdev merge origin/main
    just whats-new
    just run

# Show the CHANGELOG.md entries that landed since the previous run, by others and by me. "Cancel run" stops the recipe.
[unix]
whats-new *FLAGS: ensure-gg-cmd
    sh ./gg.cmd jbang .jbang/WhatsNewLauncher.java {{FLAGS}}

# Run JabRef from the checkout until it is really quit: "Restart to update" in its "What's new" window pulls, rebuilds and starts it again.
[unix]
run-loop: ensure-gg-cmd
    #!/usr/bin/env sh
    while :; do
        git pull --no-rebase
        just whats-new
        sh ./gg.cmd gradle :jabgui:run
        marker="$(git rev-parse --absolute-git-dir)/restart-requested"
        [ -f "$marker" ] || break
        rm -f "$marker"
    done

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
    just whats-new
    just run

[windows]
whats-new *FLAGS: ensure-gg-cmd
    .\gg.cmd jbang .jbang\WhatsNewLauncher.java {{FLAGS}}

[windows]
run-loop: ensure-gg-cmd
    while ($true) { git pull --no-rebase; just whats-new; .\gg.cmd gradle :jabgui:run; $marker = "$(git rev-parse --absolute-git-dir)/restart-requested"; if (-not (Test-Path $marker)) { break }; Remove-Item $marker }

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
