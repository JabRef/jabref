# Virtual Machines for testing JabRef

This folder contains directories making use of [Vagrant](https://www.vagrantup.com/) to install virtual machines on [VirtualBox](https://www.virtualbox.org/)

## Usage

### Prerequisites

1. [Install VirtualBox](https://www.virtualbox.org/wiki/Downloads)
    - Windows: `winget install -e --id Oracle.VirtualBox`
2. [Install Vagrant](https://developer.hashicorp.com/vagrant/install?product_intent=vagrant)
    - Windows: `winget install  -e --id Hashicorp.Vagrant`

### Setup VM

1. `cd` into `{vmdir}`, e..g, `cd ubuntu`
2. Start the vm `vagrant up`
3. Linux machines: Store ssh configuration: `vagrant ssh-config > default`

### Use VM

You can use the UI offered by the VirtualBox client.
On Linux, you can also do `ssh -Y -F vagrant-ssh default` to SSH into the machine.

If asked for a password, this is `vagrant`.

### Remove VM

Execute `vagrant destroy`.
Then, everything is removed.

## Available VMs

| VM                                              | JabRef  | Browser | LibreOffice |
|-------------------------------------------------|---------|---------|-------------|
| [`fedora`](fedora/)                             | source  | --      | --          |
| [`Linux Mint (Cinnamon)`](linux-mint-cinnamon/) | source  | Firefox | yes         |
| [`ubuntu`](ubuntu/)                             | snap    | Firefox | yes         |
| [`windows`](windows/)                           | source  | Edge    | --          |

## Troubleshooting

> VBoxManage.exe: error: Could not rename the directory '`C:\Users\$username\VirtualBox VMs\output-ubuntu_source_1720167378145_42641_1720548095320_67904`' to '`C:\Users\$username\VirtualBox VMs\jabref-ubuntu`' to save the settings file (`VERR_ALREADY_EXISTS`)

Solution: Delete folder `C:\Users\$username\VirtualBox VMs\jabref-ubuntu`

> How to use another JabRef snap image?

Solution: `snap refresh --edge jabref` (or `--stable`, ...).
More info on snaps is available at <https://snapcraft.io/docs/quickstart-tour>.
