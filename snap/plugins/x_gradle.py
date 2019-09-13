# -*- Mode:Python; indent-tabs-mode:nil; tab-width:4 -*-
#
# Copyright (C) 2016, 2018 Canonical Ltd
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 3 as
# published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

"""This plugin is useful for building parts that use gradle.

The gradle build system is commonly used to build Java projects.
The plugin requires a pom.xml in the root of the source tree.

This plugin uses the common plugin keywords as well as those for "sources".
For more information check the 'plugins' topic for the former and the
'sources' topic for the latter.

Additionally, this plugin uses the following plugin-specific keywords:

    - gradle-options:
      (list of strings)
      Flags to pass to the build using the gradle semantics for parameters.
      The 'jar' option is always passed in as the last parameter.

    - gradle-output-dir:
      (string; default: 'build/libs')
      The output directory where the resulting jar or war files from gradle[w]
      are generated.

    - gradle-version:
      (string)
      The version of gradle you want to use to build the source artifacts.
      Defaults to the current release downloadable from
      https://services.gradle.org/distributions/
      The entry is ignored if gradlew is found.

    - gradle-version-checksum:
      (string)
      The checksum for gradle-version in the form of <digest-type>/<digest>.
      As an example "sha512/2a803f578f341e164f6753e410413d16ab60fab...".

    - gradle-openjdk-version:
      (string)
      openjdk version available to the base to use. If not set the latest
      version available to the base will be used.
"""

import logging
import os
import urllib.parse
from glob import glob
from typing import Sequence

import snapcraft
from snapcraft import file_utils, formatting_utils
from snapcraft.internal import errors, sources

logger = logging.getLogger(__name__)


_DEFAULT_GRADLE_VERSION = "4.10.2"
_DEFAULT_GRADLE_CHECKSUM = (
    "sha256/b49c6da1b2cb67a0caf6c7480630b51c70a11ca2016ff2f555eaeda863143a29"
)
_GRADLE_URL = "https://services.gradle.org/distributions/gradle-{version}-bin.zip"


class UnsupportedJDKVersionError(errors.SnapcraftError):

    fmt = (
        "The gradle-openjdk-version plugin property was set to {version!r}.\n"
        "Valid values for the {base!r} base are: {valid_versions}."
    )

    def __init__(
        self, *, base: str, version: str, valid_versions: Sequence[str]
    ) -> None:
        super().__init__(
            base=base,
            version=version,
            valid_versions=formatting_utils.humanize_list(
                valid_versions, conjunction="or"
            ),
        )


class GradlePlugin(snapcraft.BasePlugin):
    @classmethod
    def schema(cls):
        schema = super().schema()
        schema["properties"]["gradle-options"] = {
            "type": "array",
            "minitems": 1,
            "uniqueItems": True,
            "items": {"type": "string"},
            "default": [],
        }
        schema["properties"]["gradle-output-dir"] = {
            "type": "string",
            "default": "build/libs",
        }

        schema["properties"]["gradle-version"] = {"type": "string"}

        schema["properties"]["gradle-version-checksum"] = {"type": "string"}

        schema["properties"]["gradle-openjdk-version"] = {
            "type": "string",
            "default": "",
        }

        schema["required"] = ["source"]

        return schema

    @classmethod
    def get_pull_properties(cls):
        # Inform Snapcraft of the properties associated with pulling. If these
        # change in the YAML Snapcraft will consider the pull step dirty.
        return ["gradle-version", "gradle-version-checksum", "gradle-openjdk-version"]

    @classmethod
    def get_build_properties(cls):
        # Inform Snapcraft of the properties associated with building. If these
        # change in the YAML Snapcraft will consider the build step dirty.
        return super().get_build_properties() + ["gradle-options", "gradle-output-dir"]

    @property
    def _gradle_tar(self):
        if self._gradle_tar_handle is None:
            gradle_uri = _GRADLE_URL.format(version=self._gradle_version)
            self._gradle_tar_handle = sources.Zip(
                gradle_uri, self._gradle_dir, source_checksum=self._gradle_checksum
            )
        return self._gradle_tar_handle

    def __init__(self, name, options, project):
        super().__init__(name, options, project)

        self._setup_gradle()
        self._setup_base_tools(project.info.get_build_base())

    def _setup_base_tools(self, base):
        if base not in ("core", "core16", "core18"):
            raise errors.PluginBaseError(
                part_name=self.name, base=self.project.info.get_build_base()
            )

        if base in ("core", "core16"):
            valid_versions = ["8", "9"]
        elif base == "core18":
            valid_versions = ["8", "11"]

        version = self.options.gradle_openjdk_version
        if not version:
            version = valid_versions[-1]
        elif version not in valid_versions:
            raise UnsupportedJDKVersionError(
                version=version, base=base, valid_versions=valid_versions
            )

        self.stage_packages.append("openjdk-{}-jre-headless".format(version))
        self.build_packages.append("openjdk-{}-jdk-headless".format(version))
        self.build_packages.append("ca-certificates-java")
        self._java_version = version

    def _using_gradlew(self) -> bool:
        return os.path.isfile(os.path.join(self.sourcedir, "gradlew"))

    def _setup_gradle(self):
        self._gradle_tar_handle = None
        self._gradle_dir = os.path.join(self.partdir, "gradle")
        if self.options.gradle_version:
            self._gradle_version = self.options.gradle_version
            self._gradle_checksum = self.options.gradle_version_checksum
        else:
            self._gradle_version = _DEFAULT_GRADLE_VERSION
            self._gradle_checksum = _DEFAULT_GRADLE_CHECKSUM

    def pull(self):
        super().pull()

        if self._using_gradlew():
            logger.info("Found gradlew, skipping gradle setup.")
            return

        os.makedirs(self._gradle_dir, exist_ok=True)
        self._gradle_tar.download()

    def build(self):
        super().build()

        if self._using_gradlew():
            gradle_cmd = ["./gradlew"]
        else:
            self._gradle_tar.provision(self._gradle_dir, keep_zip=True)
            gradle_cmd = ["gradle"]
        self.run(
            gradle_cmd
            + self._get_proxy_options()
            + self.options.gradle_options,
            rootdir=self.builddir,
        )

        src = os.path.join(self.builddir, self.options.gradle_output_dir)
        basedir = "jabref"
        # jarfiles = glob(os.path.join(src, "*.jar"))
        # warfiles = glob(os.path.join(src, "*.war"))

        # if len(jarfiles) > 0:
        #     basedir = "jar"
        # elif len(warfiles) > 0:
        #     basedir = "war"
        #     jarfiles = warfiles
        # else:
        #     raise RuntimeError("Could not find any built jar files for part")

        file_utils.link_or_copy_tree(
            src,
            os.path.join(self.installdir, basedir),
            copy_function=lambda src, dst: file_utils.link_or_copy(
                src, dst, self.installdir
            ),
        )

        self._create_symlinks()

    def _create_symlinks(self):
        if self.project.info.get_build_base() not in ("core18", "core16", "core"):
            raise errors.PluginBaseError(
                part_name=self.name, base=self.project.info.get_build_base()
            )

        os.makedirs(os.path.join(self.installdir, "bin"), exist_ok=True)
        java_bin = glob(
            os.path.join(
                self.installdir,
                "usr",
                "lib",
                "jvm",
                "java-{}-openjdk-*".format(self._java_version),
                "bin",
                "java",
            )
        )[0]
        os.symlink(
            os.path.relpath(java_bin, os.path.join(self.installdir, "bin")),
            os.path.join(self.installdir, "bin", "java"),
        )

    def run(self, cmd, rootdir):
        super().run(cmd, cwd=rootdir, env=self._build_environment())

    def _build_environment(self):
        if self._using_gradlew():
            return

        env = os.environ.copy()
        gradle_bin = os.path.join(
            self._gradle_dir, "gradle-{}".format(self._gradle_version), "bin"
        )
        print(gradle_bin)

        if env.get("PATH"):
            new_path = "{}:{}".format(gradle_bin, env.get("PATH"))
        else:
            new_path = gradle_bin

        env["PATH"] = new_path
        return env

    def _get_proxy_options(self):
        proxy_options = []
        for var in ("http", "https"):
            proxy = os.environ.get("{}_proxy".format(var), False)
            if proxy:
                parsed_url = urllib.parse.urlparse(proxy)
                proxy_options.append(
                    "-D{}.proxyHost={}".format(var, parsed_url.hostname)
                )
                if parsed_url.port:
                    proxy_options.append(
                        "-D{}.proxyPort={}".format(var, parsed_url.port)
                    )
                if parsed_url.username:
                    proxy_options.append(
                        "-D{}.proxyUser={}".format(var, parsed_url.username)
                    )
                if parsed_url.password:
                    proxy_options.append(
                        "-D{}.proxyPassword={}".format(var, parsed_url.password)
                    )
        return proxy_options
