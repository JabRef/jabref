package com.incors.plaf.kunststoff;

import javax.swing.plaf.*;

import com.incors.plaf.*;

public class KunststoffGradientTheme implements GradientTheme {

  // gradient colors

  private final ColorUIResource componentGradientColorReflection = new ColorUIResource2(255, 255, 255, 96);

  private final ColorUIResource componentGradientColorShadow = new ColorUIResource2(0, 0, 0, 48);

  private final ColorUIResource textComponentGradientColorReflection = new ColorUIResource2(0, 0, 0, 32);

  private final ColorUIResource textComponentGradientColorShadow = null;

  private final int backgroundGradientShadow = 32;

  // methods

  public String getName() { return "Default Kunststoff Gradient Theme"; }


  // methods for getting gradient colors

  public ColorUIResource getComponentGradientColorReflection() {
    return componentGradientColorReflection;
  }

  public ColorUIResource getComponentGradientColorShadow() {
    return componentGradientColorShadow;
  }

  public ColorUIResource getTextComponentGradientColorReflection() {
    return textComponentGradientColorReflection;
  }

  public ColorUIResource getTextComponentGradientColorShadow() {
    return textComponentGradientColorShadow;
  }

  public int getBackgroundGradientShadow() {
    return backgroundGradientShadow;
  }

}