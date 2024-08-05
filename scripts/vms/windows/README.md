# Windows VM

The initial setup of [WinGet](https://learn.microsoft.com/en-us/windows/package-manager/) might take long.

In case Vagrant reports "Waiting for machine to reboot..." and nothing happens, one has to "power off" the machine, execute `vagrant destory`, and then run `vagrant up` again.

One has to install the [JabRef Browser Extension](https://addons.mozilla.org/en-US/firefox/addon/jabref/) manually.

## Troubleshooting

> Repair-WinGetPackageManager : API rate limit exceeded for 194.35.188.229. (But here's the good news: Authenticated
> requests get a higher rate limit. Check out the documentation for more details.)
>
> `CategoryInfo          : NotSpecified: (:) [Repair-WinGetPackageManager], RateLimitExceededException`

This is a GitHub API rate limit. Either try again at a later point in time or modify `Vagrantfile` to include some GitHub API token (Based on <https://stackoverflow.com/q/33655700/873282>).

---

Slow downloading

```
 Writing web request
    Writing request stream... (Number of bytes written: 32768)
 ```

 This downloads winget-cli from GitHub. It seems that are GitHub rate limiters in place.

 One can try to execute `Install-Module -Name WingetTools` and then `Install-WinGet` manually. This downloads an older winget-cli version.

## Background

`Vagrantfile` is based on [SeisoLLC/windows-sandbox](https://github.com/SeisoLLC/windows-sandbox/tree/main).

The most use image seems to be the [Windows 10 image by `gusztavvargadr`](https://portal.cloud.hashicorp.com/vagrant/discover/gusztavvargadr/windows-10).
List of all images at <https://portal.cloud.hashicorp.com/vagrant/discover/gusztavvargadr>.

## Atlernatives

- Atlernative Vagrant images: <https://app.vagrantup.com/boxes/search?q=windows+10&utf8=%E2%9C%93>.
- [Windows Sandbox](https://learn.microsoft.com/en-us/windows/security/application-security/application-isolation/windows-sandbox/windows-sandbox-overview)
