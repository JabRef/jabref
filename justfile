set windows-shell := ["powershell"]

[unix]
checkout-pr pr-id:
    sh ./gg.cmd jbang https://github.com/JabRef/jabref/blob/main/.jbang/CheckoutPR.java {{pr-id}}

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
run-gui:
    .\gg.cmd gradle :jabgui:run

[windows]
run-jabkit *FLAGS:
    .\gg.cmd gradle :jabkit:run --args="{{FLAGS}}"

[windows]
run-jabsrv *FLAGS:
    .\gg.cmd gradle :jabsrv-cli:run --args="{{FLAGS}}"

run-pr pr-id:
    just checkout-pr {{pr-id}}
    just run-gui
