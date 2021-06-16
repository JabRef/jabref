Summary: APPLICATION_SUMMARY
Name: APPLICATION_PACKAGE
Version: APPLICATION_VERSION
Release: APPLICATION_RELEASE
License: APPLICATION_LICENSE_TYPE
Vendor: APPLICATION_VENDOR
Prefix: %{dirname:APPLICATION_DIRECTORY}
Provides: APPLICATION_PACKAGE
%if "xAPPLICATION_GROUP" != x
Group: APPLICATION_GROUP
%endif

Autoprov: 0
Autoreq: 0
%if "xPACKAGE_DEFAULT_DEPENDENCIES" != x || "xPACKAGE_CUSTOM_DEPENDENCIES" != x
Requires: PACKAGE_DEFAULT_DEPENDENCIES PACKAGE_CUSTOM_DEPENDENCIES
%endif

#avoid ARCH subfolder
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.%%{ARCH}.rpm

#comment line below to enable effective jar compression
#it could easily get your package size from 40 to 15Mb but
#build time will substantially increase and it may require unpack200/system java to install
%define __jar_repack %{nil}

%description
APPLICATION_DESCRIPTION

%prep

%build

%install
rm -rf %{buildroot}
install -d -m 755 %{buildroot}APPLICATION_DIRECTORY
cp -r %{_sourcedir}APPLICATION_DIRECTORY/* %{buildroot}APPLICATION_DIRECTORY
%if "xAPPLICATION_LICENSE_FILE" != x
  %define license_install_file %{_defaultlicensedir}/%{name}-%{version}/%{basename:APPLICATION_LICENSE_FILE}
  install -d -m 755 %{buildroot}%{dirname:%{license_install_file}}
  install -m 644 APPLICATION_LICENSE_FILE %{buildroot}%{license_install_file}
%endif

%files
%if "xAPPLICATION_LICENSE_FILE" != x
  %license %{license_install_file}
  %{dirname:%{license_install_file}}
%endif
# If installation directory for the application is /a/b/c, we want only root
# component of the path (/a) in the spec file to make sure all subdirectories
# are owned by the package.
%(echo APPLICATION_DIRECTORY | sed -e "s|\(^/[^/]\{1,\}\).*$|\1|")

%post
# Install the native-messaging host script for firefox/chrome/chromium
install -D -m0755 /opt/jabref/lib/native-messaging-host/firefox/org.jabref.jabref.json /usr/lib/mozilla/native-messaging-hosts/org.jabref.jabref.json
install -D -m0755 /opt/jabref/lib/native-messaging-host/chromium/org.jabref.jabref.json /etc/chromium/native-messaging-hosts/org.jabref.jabref.json
install -D -m0755 /opt/jabref/lib/native-messaging-host/chromium/org.jabref.jabref.json /etc/opt/chrome/native-messaging-hosts/org.jabref.jabref.json
# Trigger an auto-install of the browser addon for chrome/chromium browsers
install -D -m0644 /opt/jabref/lib/native-messaging-host/chromium/bifehkofibaamoeaopjglfkddgkijdlh.json /opt/google/chrome/extensions/bifehkofibaamoeaopjglfkddgkijdlh.json
install -D -m0644 /opt/jabref/lib/native-messaging-host/chromium/bifehkofibaamoeaopjglfkddgkijdlh.json /usr/share/google-chrome/extensions/bifehkofibaamoeaopjglfkddgkijdlh.json
DESKTOP_COMMANDS_INSTALL

%preun
# Remove the native-messaging hosts script only if relative to the deb package
for NATIVE_MESSAGING_JSON in "/usr/lib/mozilla/native-messaging-hosts/org.jabref.jabref.json"\
                     "/etc/chromium/native-messaging-hosts/org.jabref.jabref.json"\
                     "/etc/opt/chrome/native-messaging-hosts/org.jabref.jabref.json"; do
    if [ -e $NATIVE_MESSAGING_JSON ] && grep --quiet '"path": "/opt' $NATIVE_MESSAGING_JSON; then
        rm $NATIVE_MESSAGING_JSON
    fi
done
# Remove the auto-install triggers of the browser addon for chrom/chromium
rm /opt/google/chrome/extensions/bifehkofibaamoeaopjglfkddgkijdlh.json
rm /usr/share/google-chrome/extensions/bifehkofibaamoeaopjglfkddgkijdlh.json
UTILITY_SCRIPTS
DESKTOP_COMMANDS_UNINSTALL

%clean
