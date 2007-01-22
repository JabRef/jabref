
JabRef Installer Documentation
------------------------------

How to build the installer:

  * Create a build.properties file in root-directory and set the
    nsis.executable variable to point to makensis.exe
  * Run the win.installer.build target from the ant-file in root-directory.

What is here?

  * dist
    * Should contain everything that is supposed to be shipped to the user.
  * launcher.nsi - Will create the exe-wrapper.
  * setup.nsi - Will create the installer.
  * fileassoc.nsh - Helper script to set file-associations

Requirements + Current Status

	Exe-Wrapper:
	  * Pass arguments to JabRef [done]
	  * JabRef-Ico 16x16, 32x32, 48x48 [done]
	
	Installer:
	  * Display GPL [done]
	  * Add file association for .bib (if desired) [done]
	  * Install into custom start-menu location (if desired) [done]
	  * Can only run as administrator [done, Uwe]
	  * Install onto desktop (if desired)
	  * Install onto quicklaunch (if desired)
	
	Uninstaller:
	  * Uninstaller
	    * Allow to keep configuration
	    
	All:
	  * Localization
	  
Credits

	Uwe Stöhr rewrote the installer and fixed a lot of issues with the old one.
		  