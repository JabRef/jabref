# Linux Mint Cinnamon VM

[Linux Mint](https://linuxmint.com/) with JabRef snap and libreoffice-connection pre-installed.

Uses <https://app.vagrantup.com/archman/boxes/linuxmint/versions/22>.

Start JabRef by following steps:

- 1. Open termminal
- 2. `cd jabref`
- 3. `./gradlew run`

## Alternative

We could have build our own image.
First creating an image using packer with <https://github.com/rmoesbergen/packer-linuxmint>.
Then, building a `Vagrantfile` on top of it.
Seemed to be too much issues for the users.

1. Install packer
2. `packer plugins install github.com/hashicorp/virtualbox`
3. `packer plugins install github.com/hashicorp/vagrant`
4. `packer build -var-file=mint-cinnamon-22.json core_template.json`
