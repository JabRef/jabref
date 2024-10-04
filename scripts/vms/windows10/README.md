# Windows 10 VM

A Windows-based VM to test JabRef.
As user, you need to ensure to have the proper Windows license to use this VM.

In case you have many CPU cores, you can adapt `vb.cpus` in `Vagrantfile` to a higher number.

One has to install the [JabRef Browser Extension](https://addons.mozilla.org/en-US/firefox/addon/jabref/) manually.

## Troubleshooting

### "Waiting for machine to reboot..."

In case Vagrant reports "Waiting for machine to reboot..." and nothing happens, one has to "power off" the machine, execute `vagrant destory`, and then run `vagrant up` again.

### `fatal: early EOF`

```console
jabref-windows-sandbox: Cloning into 'jabref'...
jabref-windows-sandbox: error: RPC failed; curl 92 HTTP/2 stream 5 was not closed cleanly: CANCEL (err 8)
jabref-windows-sandbox: error: 6846 bytes of body are still expected
jabref-windows-sandbox: fetch-pack: unexpected disconnect while reading sideband packet
jabref-windows-sandbox: fatal: early EOF
jabref-windows-sandbox: fatal: fetch-pack: invalid index-pack output
```

The `git clone` command did not work.

Login, open `cmd` and then execute following commands:

```cmd
git clone --recurse-submodules https://github.com/JabRef/jabref.git
cd jabref
gradlew run
```

## Background

`Vagrantfile` is based on [SeisoLLC/windows-sandbox](https://github.com/SeisoLLC/windows-sandbox/tree/main).

The most use image seems to be the [Windows 10 image by `gusztavvargadr`](https://portal.cloud.hashicorp.com/vagrant/discover/gusztavvargadr/windows-10).
List of all images at <https://portal.cloud.hashicorp.com/vagrant/discover/gusztavvargadr>.

[Chocolatey](https://chocolatey.org/) is used instead of [winget-cli](https://learn.microsoft.com/en-us/windows/package-manager/), because Chocolatey installation does not hit GitHub's rate limits during unattended installation.

## Atlernatives

- Atlernative Vagrant images: <https://app.vagrantup.com/boxes/search?q=windows+10&utf8=%E2%9C%93>.
- [Windows Sandbox](https://learn.microsoft.com/en-us/windows/security/application-security/application-isolation/windows-sandbox/windows-sandbox-overview)
