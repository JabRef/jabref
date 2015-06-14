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
package net.sf.jabref;

import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import net.sf.jabref.help.HelpAction;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Preferences tab for file options. These options were moved out from GeneralTab to
 * resolve the space issue.
 */
public class FileTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    JabRefFrame _frame;

    private JCheckBox backup, openLast, autoDoubleBraces, autoSave,
            promptBeforeUsingAutoSave, includeEmptyFields, camelCase, sameColumn;
    private JComboBox<String> valueDelimiter, newlineSeparator;
    private JRadioButton
        resolveStringsStandard, resolveStringsAll;
    private JTextField bracesAroundCapitalsFields, nonWrappableFields,
            doNotResolveStringsFor;
    private JSpinner autoSaveInterval;
    private boolean origAutoSaveSetting = false;
    
    //for LWang_AdjustableFieldOrder 
//    private JRadioButton sortFieldInAlphabetaOrder,unSortFieldStyle,orderAsUserDefined;
    private ButtonGroup bgFieldOrderStyle;
//    int fieldOrderStyle;
    private JTextField userDefinedFieldOrder;
    
    private JCheckBox wrapFieldLine;

    public FileTab(JabRefFrame frame, JabRefPreferences prefs) {
        _prefs = prefs;
        _frame = frame;

        HelpAction autosaveHelp = new HelpAction(frame.helpDiag, GUIGlobals.autosaveHelp, "Help",
                GUIGlobals.getIconUrl("helpSmall"));
        openLast = new JCheckBox(Globals.lang("Open last edited databases at startup"));
        backup = new JCheckBox(Globals.lang("Backup old file when saving"));
        autoSave = new JCheckBox(Globals.lang("Autosave"));
        promptBeforeUsingAutoSave = new JCheckBox(Globals.lang("Prompt before recovering a database from an autosave file"));
        autoSaveInterval = new JSpinner(new SpinnerNumberModel(1, 1, 60, 1));
        valueDelimiter = new JComboBox<String>(new String[]{
                Globals.lang("Quotes") + ": \", \"",
                Globals.lang("Curly Brackets") + ": {, }" });
        includeEmptyFields = new JCheckBox(Globals.lang("Include empty fields"));
        sameColumn = new JCheckBox(Globals.lang("Start field contents in same column"));
        camelCase = new JCheckBox(Globals.lang("Use camel case for field names (e.g., \"HowPublished\" instead of \"howpublished\")"));
        resolveStringsAll = new JRadioButton(Globals.lang("Resolve strings for all fields except")+":");
        resolveStringsStandard = new JRadioButton(Globals.lang("Resolve strings for standard BibTeX fields only"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(resolveStringsAll);
        bg.add(resolveStringsStandard);
        
        //for LWang_AdjustableFieldOrder
//        ButtonGroup bgFieldOrderStyle=new ButtonGroup();
//        sortFieldInAlphabetaOrder = new JRadioButton(Globals.lang("Sort fields in alphabeta order (as ver >= 2.10)"));
//        unSortFieldStyle = new JRadioButton(Globals.lang("Do not sort fields (as ver<=2.9.2)"));
//        orderAsUserDefined= new JRadioButton(Globals.lang("Save fields as user defined order"));
//        bgFieldOrderStyle.add(sortFieldInAlphabetaOrder);
//        bgFieldOrderStyle.add(unSortFieldStyle);
//        bgFieldOrderStyle.add(orderAsUserDefined);
        
        userDefinedFieldOrder=new JTextField(_prefs.get(JabRefPreferences.WRITEFIELD_USERDEFINEDORDER)); //need to use JcomboBox in the future
        
        
        // This is sort of a quick hack
        newlineSeparator = new JComboBox<String>(new String[] {"CR", "CR/LF", "LF"});

        bracesAroundCapitalsFields = new JTextField(25);
        nonWrappableFields = new JTextField(25);
        doNotResolveStringsFor = new JTextField(30);
        autoDoubleBraces = new JCheckBox(
                //+ Globals.lang("Store fields with double braces, and remove extra braces when loading.<BR>"
                //+ "Double braces signal that BibTeX should preserve character case.") + "</HTML>");
                Globals.lang("Remove double braces around BibTeX fields when loading."));

        autoSave.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                autoSaveInterval.setEnabled(autoSave.isSelected());
                promptBeforeUsingAutoSave.setEnabled(autoSave.isSelected());
            }
        });

        FormLayout layout = new FormLayout("left:pref, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Globals.lang("General"));
        builder.nextLine();
        builder.append(openLast, 3);
        builder.nextLine();
        builder.append(backup, 3);
        builder.nextLine();
        builder.append(autoDoubleBraces, 3);
        builder.nextLine();

        JLabel label = new JLabel(Globals.lang("Store the following fields with braces around capital letters")+":");
        builder.append(label);
        builder.append(bracesAroundCapitalsFields);
        builder.nextLine();
        label = new JLabel(Globals.lang("Do not wrap the following fields when saving")+":");
        builder.append(label);
        builder.append(nonWrappableFields);
        builder.nextLine();
        builder.append(resolveStringsStandard, 3);
        builder.nextLine();
        builder.append(resolveStringsAll);
        builder.append(doNotResolveStringsFor);
        builder.nextLine();
        
        JLabel lab = new JLabel(Globals.lang("Newline separator") + ":");
        builder.append(lab);
        builder.append(newlineSeparator);
        builder.nextLine();
        
        builder.appendSeparator(Globals.lang("Autosave"));
        builder.append(autoSave, 1);
        JButton hlp = new JButton(autosaveHelp);
        hlp.setText(null);
        hlp.setPreferredSize(new Dimension(24, 24));
        JPanel hPan = new JPanel();
        hPan.setLayout(new BorderLayout());
        hPan.add(hlp, BorderLayout.EAST);
        builder.append(hPan);
        builder.nextLine();
        builder.append(Globals.lang("Autosave interval (minutes)")+":");
        builder.append(autoSaveInterval);
        builder.nextLine();
        builder.append(promptBeforeUsingAutoSave);
        builder.nextLine();
        builder.appendSeparator(Globals.lang("Field saving options"));
        builder.nextLine();
        builder.append(camelCase);
        builder.nextLine();
        builder.append(sameColumn);
        /*FormLayout layout2 = new FormLayout(
                "left:pref, 8dlu, fill:pref", "");
        DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
    	builder2.append(new JLabel(Globals.lang("Field value delimiter. E.g., \"author={x}\" or \"author='x'\"") + ":"));
        builder2.append(valueDelimiter);
        builder.nextLine();
        builder.append(builder2.getPanel());*/
        builder.append(new JPanel());
        builder.nextLine();
        builder.append(includeEmptyFields);
        builder.append(new JPanel());
        builder.nextLine();
        
        
        wrapFieldLine = new JCheckBox(Globals.lang("Wrap fields as ver 2.9.2"));
        builder.append(wrapFieldLine);
        builder.nextLine();
        //for LWang_AdjustableFieldOrder
        String [] _rbs0={"Sort fields in alphabeta order (as ver 2.10)", "Sort fields in old fasion (as ver 2.9.2)","Save fields as user defined order"};
        ArrayList<String> _rbs = new ArrayList<String>();
        for (String _rb:_rbs0){
            _rbs.add(Globals.lang(_rb));
        }
        bgFieldOrderStyle=createRadioBg(_rbs);
        userDefinedFieldOrder=new JTextField(_prefs.get(JabRefPreferences.WRITEFIELD_USERDEFINEDORDER)); //need to use JcomboBox in the future
        createAdFieldOrderBg(builder, bgFieldOrderStyle, userDefinedFieldOrder);
//        builder.append(sortFieldInAlphabetaOrder);
//        builder.nextLine();
//        builder.append(unSortFieldStyle);
//        builder.nextLine();
//        builder.append(orderAsUserDefined);
//        builder.append(userDefinedFieldOrder);
//        builder.nextLine();
        

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());
        add(pan, BorderLayout.CENTER);
    }
    
    private ButtonGroup createRadioBg(Iterable<String> radioButtonLabels){
        ButtonGroup _bg=new ButtonGroup();
        for (String _s: radioButtonLabels){
            JRadioButton _rb=new JRadioButton(_s);
            _bg.add(_rb);
            
        }
        return _bg;
    }
    
    private int getBgValue(ButtonGroup bg){
        int _i=0;
        for (Enumeration<AbstractButton> _it = bg.getElements(); _it.hasMoreElements();){
            if (_it.nextElement().isSelected()){
                return _i;
            }
            _i++;
            
        }
        return 0;
        
    }
    private void setBgSelected(ButtonGroup bg,int x){
        int _i=0;
        
        for (Enumeration<AbstractButton> _it = bg.getElements(); _it.hasMoreElements();){
            if (_i==x){
                _it.nextElement().setSelected(true);
                
            } else {
                _it.nextElement().setSelected(false);
                
            }
            _i++;
            
        }
        
    }
    
    
//    private void setValueFieldOrderStyle(){
//        fieldOrderStyle=getBgValue(bgFieldOrderStyle);
//    }
    
    private void createAdFieldOrderBg(DefaultFormBuilder builder, ButtonGroup bg, JTextField jtf){
        //for LWang_AdjustableFieldOrder

        
        
        for (Enumeration<AbstractButton> _it = bg.getElements(); _it.hasMoreElements();){
            builder.append(_it.nextElement());
            builder.nextLine();
        }
        builder.append(jtf);
        builder.nextLine();
    }
    
    

    public void setValues() {
        openLast.setSelected(_prefs.getBoolean("openLastEdited"));
        backup.setSelected(_prefs.getBoolean("backup"));

        String newline = _prefs.get(net.sf.jabref.JabRefPreferences.NEWLINE);
        if ("\r".equals(newline)) {
            newlineSeparator.setSelectedIndex(0);
        } else if ("\n".equals(newline)) {
            newlineSeparator.setSelectedIndex(2);
        } else {
            // fallback: windows standard
            newlineSeparator.setSelectedIndex(1);
        }
        
        //preserveFormatting.setSelected(_prefs.getBoolean("preserveFieldFormatting"));
        wrapFieldLine.setSelected(_prefs.getBoolean(JabRefPreferences.WRITEFIELD_WRAPFIELD));
        autoDoubleBraces.setSelected(_prefs.getBoolean("autoDoubleBraces"));
        resolveStringsAll.setSelected(_prefs.getBoolean("resolveStringsAllFields"));
        resolveStringsStandard.setSelected(!resolveStringsAll.isSelected());
        doNotResolveStringsFor.setText(_prefs.get("doNotResolveStringsFor"));
        bracesAroundCapitalsFields.setText(_prefs.get("putBracesAroundCapitals"));
        nonWrappableFields.setText(_prefs.get("nonWrappableFields"));

        autoSave.setSelected(_prefs.getBoolean("autoSave"));
        promptBeforeUsingAutoSave.setSelected(_prefs.getBoolean("promptBeforeUsingAutosave"));
        autoSaveInterval.setValue(_prefs.getInt("autoSaveInterval"));
        origAutoSaveSetting = autoSave.isSelected();
        valueDelimiter.setSelectedIndex(_prefs.getInt("valueDelimiters"));
        includeEmptyFields.setSelected(_prefs.getBoolean("includeEmptyFields"));
        camelCase.setSelected(_prefs.getBoolean(JabRefPreferences.WRITEFIELD_CAMELCASENAME));
        sameColumn.setSelected(_prefs.getBoolean(JabRefPreferences.WRITEFIELD_ADDSPACES));
        
        //for LWang_AdjustableFieldOrder
        setBgSelected(bgFieldOrderStyle, _prefs.getInt(JabRefPreferences.WRITEFIELD_SORTSTYLE));
        userDefinedFieldOrder.setText(_prefs.get(JabRefPreferences.WRITEFIELD_USERDEFINEDORDER));
        
    }

    public void storeSettings() {
        String newline;
        switch (newlineSeparator.getSelectedIndex()) {
        case 0:
            newline = "\r";
            break;
        case 2:
            newline = "\n";
            break;
        default:
            newline = "\r\n";
        }
        _prefs.put(net.sf.jabref.JabRefPreferences.NEWLINE, newline);
        // we also have to change Globals variable as globals is not a getter, but a constant
        Globals.NEWLINE = newline;
        Globals.NEWLINE_LENGTH = newline.length();

        _prefs.putBoolean("backup", backup.isSelected());
        _prefs.putBoolean("openLastEdited", openLast.isSelected());
        _prefs.putBoolean("autoDoubleBraces", autoDoubleBraces.isSelected());
        _prefs.putBoolean("resolveStringsAllFields", resolveStringsAll.isSelected());
        _prefs.put("doNotResolveStringsFor", doNotResolveStringsFor.getText().trim());
        _prefs.putBoolean("autoSave", autoSave.isSelected());
        _prefs.putBoolean("promptBeforeUsingAutosave", promptBeforeUsingAutoSave.isSelected());
        _prefs.putInt("autoSaveInterval", (Integer)autoSaveInterval.getValue());
        _prefs.putInt("valueDelimiters", valueDelimiter.getSelectedIndex());
        _prefs.putBoolean("includeEmptyFields", includeEmptyFields.isSelected());
        _prefs.putBoolean(JabRefPreferences.WRITEFIELD_CAMELCASENAME, camelCase.isSelected());
        _prefs.putBoolean(JabRefPreferences.WRITEFIELD_ADDSPACES, sameColumn.isSelected());
        doNotResolveStringsFor.setText(_prefs.get("doNotResolveStringsFor"));
        
        //for LWang_AdjustableFieldOrder
        _prefs.putInt(JabRefPreferences.WRITEFIELD_SORTSTYLE,getBgValue(bgFieldOrderStyle));
        _prefs.put(JabRefPreferences.WRITEFIELD_USERDEFINEDORDER,userDefinedFieldOrder.getText().trim());
        _prefs.putBoolean(JabRefPreferences.WRITEFIELD_WRAPFIELD,wrapFieldLine.isSelected());
        
        
        boolean updateSpecialFields = false;
        if (!bracesAroundCapitalsFields.getText().trim().equals(_prefs.get("putBracesAroundCapitals"))) {
            _prefs.put("putBracesAroundCapitals", bracesAroundCapitalsFields.getText());
            updateSpecialFields = true;
        }
        if (!nonWrappableFields.getText().trim().equals(_prefs.get("nonWrappableFields"))) {
            _prefs.put("nonWrappableFields", nonWrappableFields.getText());
            updateSpecialFields = true;
        }
        // If either of the two last entries were changed, run the update for special field handling:
        if (updateSpecialFields)
                _prefs.updateSpecialFieldHandling();

        // See if we should start or stop the auto save manager:
        if (!origAutoSaveSetting && autoSave.isSelected()) {
            Globals.startAutoSaveManager(_frame);
        }
        else if (origAutoSaveSetting && !autoSave.isSelected()) {
            Globals.stopAutoSaveManager();
        }

    }

    public boolean readyToClose() {
        return true;
    }

	public String getTabName() {
		return Globals.lang("File");
	}


}
