package net.sf.jabref.exporter;

import java.nio.charset.Charset;

import com.google.common.base.Charsets;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

public class SavePreferences {



    public enum DatabaseSaveType {
        ALL,
        PLAIN_BIBTEX
    }

    boolean makeBackup;
    Charset encoding;
    DatabaseSaveType saveType;


    private final boolean isSaveOperation;

    public boolean isSaveInOriginalOrder() {
        return saveInOriginalOrder;
    }

    public void setSaveInOriginalOrder(boolean saveInOriginalOrder) {
        this.saveInOriginalOrder = saveInOriginalOrder;
    }

    private  boolean saveInOriginalOrder;
    public String pri;
    public String sec;
    public String ter;
    public boolean priD;
    public boolean secD;
    public boolean terD;

    public SavePreferences() {
        //this.makeBackup = false;
        //this.encoding = Charsets.UTF_8;
        this.isSaveOperation = true;
    }

    public SavePreferences(JabRefPreferences prefs) {
        this(prefs, true);
    }

    public SavePreferences(JabRefPreferences prefs, boolean isSaveOperation) {
        this.makeBackup = prefs.getBoolean(JabRefPreferences.BACKUP);
        this.encoding = prefs.getDefaultEncoding();
        this.isSaveOperation = isSaveOperation;
        this.saveType = DatabaseSaveType.ALL;
        if(isSaveOperation)
            this.saveInOriginalOrder = true;
        else
            this.saveInOriginalOrder = Globals.prefs.getBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER);

        if (!isSaveOperation && Globals.prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER)) {
            pri = prefs.get(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD);
            sec = prefs.get(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD);
            ter = prefs.get(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD);
            priD = prefs.getBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING);
            secD = prefs.getBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING);
            terD = prefs.getBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING);
        } else {
            // The setting is to save according to the current table order.
            pri = prefs.get(JabRefPreferences.TABLE_PRIMARY_SORT_FIELD);
            sec = prefs.get(JabRefPreferences.TABLE_SECONDARY_SORT_FIELD);
            ter = prefs.get(JabRefPreferences.TABLE_TERTIARY_SORT_FIELD);
            priD = prefs.getBoolean(JabRefPreferences.TABLE_PRIMARY_SORT_DESCENDING);
            secD = prefs.getBoolean(JabRefPreferences.TABLE_SECONDARY_SORT_DESCENDING);
            terD = prefs.getBoolean(JabRefPreferences.TABLE_TERTIARY_SORT_DESCENDING);
        }
    }

    public boolean getMakeBackup() {
        return makeBackup;
    }

    public void setMakeBackup(Boolean makeBackup) {
        this.makeBackup = makeBackup;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public boolean isSaveOperation() {
        return isSaveOperation;
    }

    public DatabaseSaveType getSaveType() {
        return saveType;
    }

    public void setSaveType(DatabaseSaveType saveType) {
        this.saveType = saveType;
    }
}
