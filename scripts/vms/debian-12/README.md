# Debian 12

Uses <https://github.com/alvistack/vagrant-debian>.

Reproducer for <https://github.com/JabRef/jabref/issues/10731#issuecomment-2302373288>.

Installs [Just Perfection GNOME Shell Extension](https://gitlab.gnome.org/jrahmatzadeh/just-perfection).

After `vagrant up`:

1. Terminate the VM.
2. Open settings of the VM.
3. Reconfigure the Virtual Box display to "VMSVGA", "Enable 3D Acceleration", use 32 MB of Video RAM.
4. Power on.
5. Log in.

Then, start JabRef by following steps:

1. Open termminal
2. `cd jabref`
3. `./gradlew run`
