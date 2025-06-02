set windows-shell := ["powershell"]

[unix]
checkout-pr pr-id:
    sh ./gg.cmd jbang https://github.com/JabRef/jabref/blob/main/.jbang/CheckoutPR.java {{pr-id}}

[unix]
run branch:
    sh ./gg.cmd jbang git@jbangdev checkout {{branch}}
    sh ./gg.cmd jbang git@jbangdev fetch origin
    sh ./gg.cmd jbang git@jbangdev merge origin/main
    just run-gui

[unix]
run-gui:
    sh ./gg.cmd gradle :jabgui:run

[unix]
run-jabkit *FLAGS:
    sh ./gg.cmd gradle :jabkit:run --args="{{FLAGS}}"

[unix]
run-jabsrv *FLAGS:
    sh ./gg.cmd gradle :jabsrv-cli:run --args="{{FLAGS}}"

[windows]
checkout-pr pr-id:
    .\gg.cmd jbang https://github.com/JabRef/jabref/blob/main/.jbang/CheckoutPR.java {{pr-id}}

[windows]
run branch:
    .\gg.cmd jbang git@jbangdev checkout {{branch}}
    .\gg.cmd jbang git@jbangdev fetch origin
    .\gg.cmd jbang git@jbangdev merge origin/main
    just run-gui

[windows]
run-gui:
    .\gg.cmd gradle :jabgui:run

[windows]
run-jabkit *FLAGS:
    .\gg.cmd gradle :jabkit:run --args="{{FLAGS}}"

[windows]
run-jabsrv *FLAGS:
    .\gg.cmd gradle :jabsrv-cli:run --args="{{FLAGS}}"

run-main:
    just run main

run-pr pr-id:
    just checkout-pr {{pr-id}}
    just run-gui
