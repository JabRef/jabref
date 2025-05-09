Summary: APPLICATION_SUMMARY
Name: APPLICATION_PACKAGE
Version: APPLICATION_VERSION
Release: APPLICATION_RELEASE
License: APPLICATION_LICENSE_TYPE
Vendor: APPLICATION_VENDOR

%if "xAPPLICATION_URL" != "x"
URL: APPLICATION_URL
%endif

%if "xAPPLICATION_PREFIX" != "x"
Prefix: APPLICATION_PREFIX
%endif

Provides: APPLICATION_PACKAGE

%if "xAPPLICATION_GROUP" != "x"
Group: APPLICATION_GROUP
%endif

Autoprov: 0
Autoreq: 0
%if "xPACKAGE_DEFAULT_DEPENDENCIES" != "x" || "xPACKAGE_CUSTOM_DEPENDENCIES" != "x"
Requires: PACKAGE_DEFAULT_DEPENDENCIES PACKAGE_CUSTOM_DEPENDENCIES
%endif

#comment line below to enable effective jar compression
#it could easily get your package size from 40 to 15Mb but
#build time will substantially increase and it may require unpack200/system java to install
%define __jar_repack %{nil}

# on RHEL we got unwanted improved debugging enhancements
%define _build_id_links none

%define package_filelist %{_builddir}/%{name}.files
%define app_filelist %{_builddir}/%{name}.app.files
%define filesystem_filelist %{_builddir}/%{name}.filesystem.files

%define default_filesystem / /opt /usr /usr/bin /usr/lib /usr/local /usr/local/bin /usr/local/lib

%description
APPLICATION_DESCRIPTION

%global __os_install_post %{nil}

%prep

%build

%install
rm -rf %{buildroot}
install -d -m 755 %{buildroot}APPLICATION_DIRECTORY
cp -r %{_sourcedir}APPLICATION_DIRECTORY/* %{buildroot}APPLICATION_DIRECTORY
if [ "$(echo %{_sourcedir}/lib/systemd/system/*.service)" != '%{_sourcedir}/lib/systemd/system/*.service' ]; then
  install -d -m 755 %{buildroot}/lib/systemd/system
  cp %{_sourcedir}/lib/systemd/system/*.service %{buildroot}/lib/systemd/system
fi
%if "xAPPLICATION_LICENSE_FILE" != "x"
  %define license_install_file %{_defaultlicensedir}/%{name}-%{version}/%{basename:APPLICATION_LICENSE_FILE}
  install -d -m 755 "%{buildroot}%{dirname:%{license_install_file}}"
  install -m 644 "APPLICATION_LICENSE_FILE" "%{buildroot}%{license_install_file}"
%endif
(cd %{buildroot} && find . -path ./lib/systemd -prune -o -type d -print) | sed -e 's/^\.//' -e '/^$/d' | sort > %{app_filelist}
{ rpm -ql filesystem || echo %{default_filesystem}; } | sort > %{filesystem_filelist}
comm -23 %{app_filelist} %{filesystem_filelist} > %{package_filelist}
sed -i -e 's/.*/%dir "&"/' %{package_filelist}
(cd %{buildroot} && find . -not -type d) | sed -e 's/^\.//' -e 's/.*/"&"/' >> %{package_filelist}
%if "xAPPLICATION_LICENSE_FILE" != "x"
  sed -i -e 's|"%{license_install_file}"||' -e '/^$/d' %{package_filelist}
%endif

%files -f %{package_filelist}
%if "xAPPLICATION_LICENSE_FILE" != "x"
  %license "%{license_install_file}"
%endif

%post
package_type=rpm
LAUNCHER_AS_SERVICE_SCRIPTS
DESKTOP_COMMANDS_INSTALL
LAUNCHER_AS_SERVICE_COMMANDS_INSTALL
# Install the native-messaging host script for firefox/chrome/chromium
install -D -m0755 /opt/jabref/lib/native-messaging-host/firefox/org.jabref.jabref.json /usr/lib/mozilla/native-messaging-hosts/org.jabref.jabref.json
install -D -m0755 /opt/jabref/lib/native-messaging-host/chromium/org.jabref.jabref.json /etc/chromium/native-messaging-hosts/org.jabref.jabref.json
install -D -m0755 /opt/jabref/lib/native-messaging-host/chromium/org.jabref.jabref.json /etc/opt/chrome/native-messaging-hosts/org.jabref.jabref.json
# Trigger an auto-install of the browser addon for chrome/chromium browsers
install -D -m0644 /opt/jabref/lib/native-messaging-host/chromium/bifehkofibaamoeaopjglfkddgkijdlh.json /opt/google/chrome/extensions/bifehkofibaamoeaopjglfkddgkijdlh.json
install -D -m0644 /opt/jabref/lib/native-messaging-host/chromium/bifehkofibaamoeaopjglfkddgkijdlh.json /usr/share/google-chrome/extensions/bifehkofibaamoeaopjglfkddgkijdlh.json


%pre
package_type=rpm
COMMON_SCRIPTS
LAUNCHER_AS_SERVICE_SCRIPTS
if [ "$1" -gt 1 ]; then
  :; LAUNCHER_AS_SERVICE_COMMANDS_UNINSTALL
fi

%preun
package_type=rpm
COMMON_SCRIPTS
DESKTOP_SCRIPTS
LAUNCHER_AS_SERVICE_SCRIPTS
DESKTOP_COMMANDS_UNINSTALL
LAUNCHER_AS_SERVICE_COMMANDS_UNINSTALL
# Remove the native-messaging hosts script only if relative to the deb package
for NATIVE_MESSAGING_JSON in "/usr/lib/mozilla/native-messaging-hosts/org.jabref.jabref.json"\
                     "/etc/chromium/native-messaging-hosts/org.jabref.jabref.json"\
                     "/etc/opt/chrome/native-messaging-hosts/org.jabref.jabref.json"; do
    if [ -e $NATIVE_MESSAGING_JSON ] && grep --quiet '"path": "/opt' $NATIVE_MESSAGING_JSON; then
        rm $NATIVE_MESSAGING_JSON
    fi
done
# Remove the auto-install triggers of the browser addon for chrom/chromium
rm -f /opt/google/chrome/extensions/bifehkofibaamoeaopjglfkddgkijdlh.json || true
rm -f /usr/share/google-chrome/extensions/bifehkofibaamoeaopjglfkddgkijdlh.json || true


%clean
