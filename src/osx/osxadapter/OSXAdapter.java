/*
  File:    OSXAdapter.java

  Description:A single class with clear, static entry points for hooking existing preferences,
        about, quit functionality from an existing Java app into handlers for the Mac OS X
        application menu.  Useful for developers looking to support
        multiple platforms with a single codebase, and support Mac OS X features with
        minimal impact.

        This sample is designed for Java 1.4.1 and later on Mac OS X.

  Author:    mdrance

  Copyright:   © Copyright 2003 Apple Computer, Inc. All rights reserved.

  Disclaimer:  IMPORTANT:  This Apple software is supplied to you by Apple Computer, Inc.
        ("Apple") in consideration of your agreement to the following terms, and your
        use, installation, modification or redistribution of this Apple software
        constitutes acceptance of these terms.  If you do not agree with these terms,
        please do not use, install, modify or redistribute this Apple software.

        In consideration of your agreement to abide by the following terms, and subject
        to these terms, Apple grants you a personal, non-exclusive license, under Apple's
        copyrights in this original Apple software (the "Apple Software"), to use,
        reproduce, modify and redistribute the Apple Software, with or without
        modifications, in source and/or binary forms; provided that if you redistribute
        the Apple Software in its entirety and without modifications, you must retain
        this notice and the following text and disclaimers in all such redistributions of
        the Apple Software.  Neither the name, trademarks, service marks or logos of
        Apple Computer, Inc. may be used to endorse or promote products derived from the
        Apple Software without specific prior written permission from Apple.  Except as
        expressly stated in this notice, no other rights or licenses, express or implied,
        are granted by Apple herein, including but not limited to any patent rights that
        may be infringed by your derivative works or by other works in which the Apple
        Software may be incorporated.

        The Apple Software is provided by Apple on an "AS IS" basis.  APPLE MAKES NO
        WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION THE IMPLIED
        WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR
        PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND OPERATION ALONE OR IN
        COMBINATION WITH YOUR PRODUCTS.

        IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL OR
        CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
        GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
        ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION, MODIFICATION AND/OR DISTRIBUTION
        OF THE APPLE SOFTWARE, HOWEVER CAUSED AND WHETHER UNDER THEORY OF CONTRACT, TORT
        (INCLUDING NEGLIGENCE), STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN
        ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

  Change History (most recent first):
    04092003  mdrance    added comments

*/
package osxadapter;

import net.sf.jabref.JabRefFrame;
import com.apple.eawt.*;

public class OSXAdapter extends ApplicationAdapter {

  // pseudo-singleton model; no point in making multiple instances
  // of the EAWT application or our adapter
  private static OSXAdapter            theAdapter;
  private static com.apple.eawt.Application    theApplication;

  // reference to the app where the existing quit, about, prefs code is
  private JabRefFrame mainApp;

  private OSXAdapter (JabRefFrame inApp) {
    mainApp = inApp;
  }

  // implemented handler methods.  These are basically hooks into existing
  // functionality from the main app, as if it came over from another platform.
  public void handleAbout(ApplicationEvent ae) {
    if (mainApp != null) {
      ae.setHandled(true);
      mainApp.about();
    } else {
      throw new IllegalStateException("handleAbout: MyApp instance detached from listener");
    }
  }

  public void handlePreferences(ApplicationEvent ae) {
    if (mainApp != null) {
      mainApp.preferences();
      ae.setHandled(true);
    } else {
      throw new IllegalStateException("handlePreferences: MyApp instance detached from listener");
    }
  }

  public void handleQuit(ApplicationEvent ae) {
    if (mainApp != null) {
      /*
      /  You MUST setHandled(false) if you want to delay or cancel the quit.
      /  This is important for cross-platform development -- have a universal quit
      /  routine that chooses whether or not to quit, so the functionality is identical
      /  on all platforms.  This example simply cancels the AppleEvent-based quit and
      /  defers to that universal method.
      */
      ae.setHandled(false);
      mainApp.quit();
    } else {
      throw new IllegalStateException("handleQuit: MyApp instance detached from listener");
    }
  }


  // The main entry-point for this functionality.  This is the only method
  // that needs to be called at runtime, and it can easily be done using
  // reflection (see MyApp.java)
  public static void registerMacOSXApplication(JabRefFrame inApp) {
    if (theApplication == null) {
      theApplication = new com.apple.eawt.Application();
    }

    if (theAdapter == null) {
      theAdapter = new OSXAdapter(inApp);
    }
    theApplication.addApplicationListener(theAdapter);
  }

  // Another static entry point for EAWT functionality.  Enables the
  // "Preferences..." menu item in the application menu.
  public static void enablePrefs(boolean enabled) {
    if (theApplication == null) {
      theApplication = new com.apple.eawt.Application();
    }
    theApplication.setEnabledPreferencesMenu(enabled);
  }
}
