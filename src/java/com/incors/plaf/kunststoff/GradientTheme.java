package com.incors.plaf.kunststoff;

/*
 * This code was developed by INCORS GmbH (www.incors.com).
 * It is published under the terms of the GNU Lesser General Public License.
 */

import java.awt.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

/**
 * Interface that provides methods for getting the colors for all gradients
 * in the Kunststoff Look&Feel. This interface can be implemented by subclasses
 * of <code>javax.swing.plaf.metal.MetalTheme</code> to have a theme that provides
 * standard colors as well as gradient colors.
 */
public interface GradientTheme {

  /**
   * Returns the upper gradient color for components like JButton, JMenuBar,
   * and JProgressBar.
   * Will return <code>null</code> if upper gradient should not be painted.
   */
  public ColorUIResource getComponentGradientColorReflection();

  /**
   * Returns the lower gradient color for components like JButton, JMenuBar,
   * and JProgressBar.
   * Will return <code>null</code> if lower gradient should not be painted.
   */
  public ColorUIResource getComponentGradientColorShadow();

  /**
   * Returns the upper gradient color for text components like JTextField and
   * JPasswordField.
   * Will return <code>null</code> if upper gradient should not be painted.
   */
  public ColorUIResource getTextComponentGradientColorReflection();

  /**
   * Returns the lower gradient color for text components like JTextField and
   * JPasswordField.
   * Will return <code>null</code> if lower gradient should not be painted.
   */
  public ColorUIResource getTextComponentGradientColorShadow();

  public int getBackgroundGradientShadow();

}