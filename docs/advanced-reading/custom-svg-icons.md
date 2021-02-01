# Custom SVG icons

JabRef uses [Material Design Icons](https://materialdesignicons.com/) for most buttons on toolbars and panels. While most required icons are available, some specific ones cannot be found in the standard set, like Vim, Emacs, etc. Although custom icons might not fit the existing icons perfectly in style and level of detail, they will fit much better into JabRef than having a color pixel icon between all Material Design Icons.

![toolbar](http://i.imgur.com/KlyYrNn.png)

This tutorial aims to describe the process of adding missing icons created in a vector drawing tool like Adobe Illustrator and packing them into a _true type font_ \(TTF\) to fit seamlessly into the JabRef framework. Already existing custom icons will be published \(link is coming soon\) as they need to be repacked as well.

The process consists of 5 steps:

1. Download the template vector graphics from the Material Design Icons webpage. This gives you a set of existing underlying shapes that are typically used and the correct bounding boxes. You can design the missing icon based on this template and export it as an SVG file.
2. Pack the set of icons into a TTF with the help of the free IcoMoon tool.
3. Replace the existing `JabRefMaterialDesign.ttf` in the `src/main/resources/fonts` folder.
4. Adapt the class `org.jabref.gui.JabRefMaterialDesignIcon` to include all icons.
5. Adapt the class `org.jabref.gui.IconTheme` to make the new icons available in JabRef

## Step 1. Designing the icon

Good icon design requires years of experience and cannot be covered here. Adapting color icons with a high degree of detail to look good in the flat, one-colored setting is an even harder task. Therefore, only 3 tips: 1. Look up some tutorials on icon design, 2. reuse the provided basic shapes in the template, and 3. export your icon in the SVG format.

## Step 2. Packing the icons into a font

Use the [IcoMoon](https://icomoon.io) tool for packing the icons. Create a new set and import _all_ icons. Rearrange them so that they have the same order as in `org.jabref.gui.JabRefMaterialDesignIcon`. This will avoid that you have to change the code points for the existing glyphs. In the settings for your icon set, set the _Grid_ to 24. This is important to get the correct spacing. The name of the font is `JabRefMaterialDesign`. When your icon-set is ready, select all of them and download the font-package.

## Step 3. Replace the existing `JabRefMaterialDesign.ttf`

Unpack the downloaded font-package and copy the `.ttf` file under `fonts` to `src/main/resources/fonts/JabRefMaterialDesign.ttf`.

## Step 4. Adapt the class `org.jabref.gui.JabRefMaterialDesignIcon`

Inside the font-package will be a CSS file that specifies which icon \(glyph\) is at which code point. If you have ordered them correctly, you newly designed icon\(s\) will be at the end and you can simply append them to `org.jabref.gui.JabRefMaterialDesignIcon`:

```java
    TEX_STUDIO("\ue900"),
    TEX_MAKER("\ue901"),
    EMACS("\ue902"),
    OPEN_OFFICE("\ue903"),
    VIM("\ue904"),
    LYX("\ue905"),
    WINEDT("\ue906"),
    ARXIV("\ue907");
```

## Step 5. Adapt the class `org.jabref.gui.IconTheme`

If you added an icon that already existed \(but not as flat Material Design Icon\), then you need to change the appropriate line in `org.jabref.gui.IconTheme`, where the icon is assigned. If you created a new one, then you need to add a line. You can specify the icon like this:

```java
APPLICATION_EMACS(JabRefMaterialDesignIcon.EMACS)
```

