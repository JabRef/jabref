# Linux Mint Cinnamon VM

[Linux Mint](https://linuxmint.com/) with JabRef sources.

Uses <https://portal.cloud.hashicorp.com/vagrant/discover/aaronvonawesome/linux-mint-21-cinnamon>.

Start JabRef by following steps:

1. Open termminal
2. `cd jabref`
3. `./gradlew run`

## Alternatives

### Using `archman/linuxmint`

This image does not work with multiple monitors.

Issues

- [VirtualBox: Mouse pointer offset](https://forums.linuxmint.com/viewtopic.php?t=427855)
- [VirtualBox 3D acceleration: blank screen](https://forums.linuxmint.com/viewtopic.php?t=427853)

### Building an own image

We could have build our own image.
First creating an image using packer with <https://github.com/rmoesbergen/packer-linuxmint>.
Then, building a `Vagrantfile` on top of it.
Seemed to be too much issues for the users.

1. Install packer
2. `packer plugins install github.com/hashicorp/virtualbox`
3. `packer plugins install github.com/hashicorp/vagrant`
4. `packer build -var-file=mint-cinnamon-22.json core_template.json`
