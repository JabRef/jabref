/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.TreeSet;

import javax.swing.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Aug 23, 2005
 * Time: 11:30:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class FieldWeightDialog extends JDialog {

    private final JabRefFrame frame;
    private final HashMap<JSlider, SliderInfo> sliders = new HashMap<JSlider, SliderInfo>();
    private final JButton ok = new JButton(Localization.lang("Ok"));
    private final JButton cancel = new JButton(Localization.lang("Cancel"));

    private FieldWeightDialog(JabRefFrame frame) {
        this.frame = frame;
        JPanel main = buildMainPanel();
        main.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        getContentPane().add(main, BorderLayout.CENTER);
        getContentPane().add(buildButtonPanel(), BorderLayout.SOUTH);
        pack();
    }

    private JPanel buildMainPanel() {
        FormLayout layout = new FormLayout
                ("right:pref, 4dlu, fill:pref, 8dlu, right:pref, 4dlu, fill:pref", // 4dlu, left:pref, 4dlu",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Localization.lang("Field sizes"));

        // We use this list to build an alphabetical list of field names:
        TreeSet<String> fields = new TreeSet<String>();
        // We use this map to remember which slider represents which field name:
        sliders.clear();
        for (int i = 0, len = BibtexFields.numberOfPublicFields(); i < len; i++)
        {
            fields.add(BibtexFields.getFieldName(i));
        }
        fields.remove("bibtexkey"); // bibtex key doesn't need weight.
        // Here is the place to add other fields:

        // --------------

        for (String field : fields) {
            builder.append(field);
            int weight = (int) (100 * BibtexFields.getFieldWeight(field) / GUIGlobals.MAX_FIELD_WEIGHT);
            //System.out.println(weight);
            JSlider slider = new JSlider(0, 100, weight);//,);
            sliders.put(slider, new SliderInfo(field, weight));
            builder.append(slider);
        }
        builder.appendSeparator();

        return builder.getPanel();

    }

    private JPanel buildButtonPanel() {

        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                storeSettings();
                dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });

        ButtonBarBuilder builder = new ButtonBarBuilder();
        builder.addGlue();
        builder.addButton(ok);
        builder.addButton(cancel);
        builder.addGlue();
        return builder.getPanel();
    }

    private void storeSettings() {
        for (JSlider slider : sliders.keySet()) {
            SliderInfo sInfo = sliders.get(slider);
            // Only list the value if it has changed:
            if (sInfo.originalValue != slider.getValue()) {
                double weight = GUIGlobals.MAX_FIELD_WEIGHT * slider.getValue() / 100d;
                BibtexFields.setFieldWeight(sInfo.fieldName, weight);
            }
        }
        frame.removeCachedEntryEditors();
    }


    /**
     * "Struct" class to hold the necessary info about one of our JSliders:
     * which field it represents, and what value it started out with.
     */
    static class SliderInfo {

        final String fieldName;
        final int originalValue;


        public SliderInfo(String fieldName, int originalValue) {
            this.fieldName = fieldName;
            this.originalValue = originalValue;
        }
    }
}
