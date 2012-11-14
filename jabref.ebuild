#See also https://bugs.gentoo.org/show_bug.cgi?id=385751

#=========== jabref ebuild hacked from dmol's 2.8.1 ==============
# Copyright 1999-2012 Gentoo Foundation
# Distributed under the terms of the GNU General Public License v2
# $Header:  $

EAPI=4

WANT_ANT_TASKS="dev-java/jarbundler:0"
#docs aren't builded correctly?
JAVA_PKG_IUSE=""
inherit eutils java-pkg-2 java-ant-2

MY_PV="${PV/_beta/b}"

DESCRIPTION="GUI frontend for BibTeX, written in Java"
HOMEPAGE="http://jabref.sourceforge.net/"
SRC_URI="mirror://sourceforge/${PN}/JabRef-${MY_PV}-src.tar.bz2"

LICENSE="GPL-2"
SLOT="0"
KEYWORDS="~amd64 ~x86"
IUSE="mysql"

#do not include antlr-3, use shipped antlr-3.0b5.jar ?
#       >=dev-java/antlr-3.1.3:3[java]
CDEPEND="dev-java/spin:0
        dev-java/glazedlists:1.8
        dev-java/jempbox:1.7
        dev-java/pdfbox:1.7
        >=dev-java/antlr-2.7.3:0[java]
        >=dev-java/jgoodies-forms-1.1.0:0
        dev-java/jgoodies-looks:2.0
        >=dev-java/microba-0.4.3:0
        dev-java/commons-logging:0
        dev-java/jpf:1.5
        dev-java/jpfcodegen:0
        dev-java/jgoodies-forms:0
        mysql? ( dev-java/jdbc-mysql:0 )"

RDEPEND=">=virtual/jre-1.6
        ${CDEPEND}"

DEPEND=">=virtual/jdk-1.6
        ${CDEPEND}"

S="${WORKDIR}/${PN}-${MY_PV}"

JAVA_ANT_REWRITE_CLASSPATH="true"
#EANT_ANT_TASKS="jarbundler"
EANT_BUILD_TARGET="jars"
EANT_DOC_TARGET="docs"

#do not include antlr-3, use shipped antlr-3.0b5.jar
EANT_GENTOO_CLASSPATH="antlr,commons-logging,glazedlists-1.8,jempbox-1.7,jgoodies-forms,jgoodies-looks-2.0,jpf-1.5,microba,pdfbox-1.7,spin"
EANT_GENTOO_CLASSPATH_EXTRA="${S}/lib/antlr-3.0b5.jar"

src_install() {
        java-pkg_newjar build/lib/JabRef-${MY_PV}.jar
        java-pkg_dojar lib/antlr-3.0b5.jar
        java-pkg_dojar lib/plugin/JPFCodeGenerator-rt.jar

        #are not builded? Investigate why
        #use doc && java-pkg_dojavadoc build/docs/API
        dodoc src/txt/README

        java-pkg_dolauncher ${PN} \
                --main net.sf.jabref.JabRef

        dodir /usr/share/${PN}/lib/plugins
        keepdir /usr/share/${PN}/lib/plugins

        java-pkg_register-optional-dependency jdbc-mysql

        newicon src/images/JabRef-icon-48.png JabRef-icon.png || die
        make_desktop_entry ${PN} JabRef JabRef-icon Office
        echo "MimeType=text/x-bibtex;" >> "${D}/usr/share/applications/${PN}-${PN}.desktop"
}
