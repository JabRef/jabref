# Debian 12

Uses <https://github.com/alvistack/vagrant-debian>.

Reproducer for <https://github.com/JabRef/jabref/issues/10731#issuecomment-2302373288>.

<!-- Installs [Just Perfection GNOME Shell Extension](https://gitlab.gnome.org/jrahmatzadeh/just-perfection). -->

After `vagrant up`:

1. `vagrant ssh`
2. You are logged into the VM
3. `sudo -s`
4. `passwd vagrant`
5. Enter `vagrant` as password
6. `exit`
7. `exit`
8. In the graphical VirtualBox window, you can now login
9. Enter `startx` to see IceWM.
10. Open termminal
11. `cd jabref`
12. `./gradlew :jabgui:run`
