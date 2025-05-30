set windows-shell := ["powershell"]

[unix]
gui:
    sh ./gg.cmd gradle :jabgui:run

[unix]
checkout-pr pr-id:
    sh ./gg.cmd jbang https://github.com/JabRef/jabref/blob/main/.jbang/CheckoutPR.java {{pr-id}}

[windows]
gui:
    .\gg.cmd gradle :jabgui:run

[windows]
checkout-pr pr-id:
    .\gg.cmd jbang https://github.com/JabRef/jabref/blob/main/.jbang/CheckoutPR.java {{pr-id}}

run-pr pr-id:
    just checkout-pr {{pr-id}}
    just gui
