package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 *
 * The code was improved with the help of these great people:
 *
 *  Aljoscha Rittner
 *  C.J. Kent
 *  Christian Peter
 *  Christoph Wilhelms
 *  Eric Georges
 *  Gerald Bauer
 *  Ingo Kegel
 *  Jamie LaScolea
 *  Jens Niemeyer
 *  Jerason Banes
 *  Jim Wissner
 *  Johannes Ernst
 *  Jonas Kilian
 *  Julien Ponge
 *  Karsten Lentzsch
 *  Matthew Philips
 *  Romain Guy
 *  Sebastian Ferreyra
 *  Steve Varghese
 *  Taoufik Romdhane
 *  Timo Haberkern
 *
 */

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import com.incors.plaf.*;

/**
 * The main class for the Kunststoff Look&Feel.
 *
 */
public class KunststoffLookAndFeel extends MetalLookAndFeel {
  private static GradientTheme gradientTheme;
  private static boolean isInstalled = false;
  private static boolean themeHasBeenSet = false; // Thanks to Jonas Kilian for
                                                  // fixing the themes-bug

  public KunststoffLookAndFeel() {
    // the next line was removed by Jens Niemeyer, jens@jensn.de, because it would
    // cause a crash when using Sun Web Start
    // super();
    
    // install with the UIManager, if not done yet.
    if (!isInstalled) {
      UIManager.installLookAndFeel(new UIManager.LookAndFeelInfo("Kunststoff", "com.incors.plaf.kunststoff.KunststoffLookAndFeel"));
      isInstalled = true;
    }
  }


  public String getID() {
    return "Kunststoff";
  }


  public String getName() {
    return "Kunststoff";
  }


  public String getDescription() {
    return "Look and Feel giving a plastic effect. Developed by INCORS GmbH, 2001"
           + "and contributors. Published under the Lesser GNU Public Licence.";
  }


  public boolean isNativeLookAndFeel() {
    return false;
  }


  public boolean isSupportedLookAndFeel() {
    return true;
  }


  protected void initClassDefaults(UIDefaults table) {
    super.initClassDefaults(table);
    putDefault(table, "ButtonUI");
    putDefault(table, "ToggleButtonUI");
    putDefault(table, "ComboBoxUI");
    putDefault(table, "TabbedPaneUI");
    putDefault(table, "TextFieldUI");
    putDefault(table, "PasswordFieldUI");
    putDefault(table, "ListUI");
    putDefault(table, "TreeUI");
    putDefault(table, "ToolBarUI");
    putDefault(table, "MenuBarUI");
    putDefault(table, "MenuUI");
    putDefault(table, "ScrollBarUI");
    putDefault(table, "ProgressBarUI");
    putDefault(table, "TableHeaderUI");
    putDefault(table, "InternalFrameUI");
    // if you want a check box icon with gradients, just remove the comment from
    // the following lines. We prefer the standard icon.
    /*
    putDefault(table, "CheckBoxUI");
    try {
      String className = "com.incors.plaf.kunststoff.KunststoffCheckBoxIcon";
      table.put("CheckBox.icon", className);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    */
  }

  protected void putDefault(UIDefaults table, String uiKey) {
    try {
      String className = "com.incors.plaf.kunststoff.Kunststoff"+uiKey;
      table.put(uiKey, className);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  protected void createDefaultTheme() {
    if (!themeHasBeenSet) {
      setCurrentTheme(new KunststoffTheme());
    }
    if (gradientTheme==null) {
      gradientTheme = new KunststoffGradientTheme();
    }
  }

  /**
   * Sets the theme that defines the colors for gradients.
   */
  public static void setCurrentGradientTheme(GradientTheme theme) {
    if (theme == null) {
      throw new NullPointerException("Gradient Theme cannot have null value");
    }
    gradientTheme = theme;
  }

  /**
   * Sets the current color theme. This works exactly as with the MetalLookAndFeel.
   * Note that for customizing the gradients the method setCurrentGradientTheme()
   * must be used.
   */
  public static void setCurrentTheme(MetalTheme theme) {
    MetalLookAndFeel.setCurrentTheme(theme);
    themeHasBeenSet = true;
  }

  protected void initSystemColorDefaults(UIDefaults table) {
    super.initSystemColorDefaults(table);
    // we made the color a bit darker because the were complaints about the color
    // being very difficult to see
    table.put("textHighlight", KunststoffUtilities.getTranslucentColorUIResource(getTextHighlightColor(), 128));
  }

  protected void initComponentDefaults(UIDefaults table) {
    super.initComponentDefaults(table);
    table.put("SplitPane.dividerSize", new Integer(8)); // will result in only one row of bumps
  }




  // ******** getter methods for the gradient colors *********

  /**
   * Returns the reflection color for a standard component (such as JButton).
   */
  public static ColorUIResource getComponentGradientColorReflection() {
    return gradientTheme.getComponentGradientColorReflection();
  }

  /**
   * Returns the shadow color for a standard component (such as JButton).
   */
  public static ColorUIResource getComponentGradientColorShadow() {
    return gradientTheme.getComponentGradientColorShadow();
  }


  /**
   * Returns the reflection color for a text component (such as JTextField).
   */
  public static ColorUIResource getTextComponentGradientColorReflection() {
    return gradientTheme.getTextComponentGradientColorReflection();
  }

  /**
   * Returns the reflection color for a text component (such as JTextField).
   */
  public static ColorUIResource getTextComponentGradientColorShadow() {
    return gradientTheme.getTextComponentGradientColorShadow();
  }

  /**
   * Returns the background shadow color for JList. In future we might also
   * use this color for the background of JTree.
   */
  public static int getBackgroundGradientShadow() {
    return gradientTheme.getBackgroundGradientShadow();
  }


}