package net.sf.jabref.config;

import java.util.Vector;

/**
 * Stores the save order config from MetaData
 * 
 * Format: <choice>, pair of field + ascending (boolean)
 */
public class SaveOrderConfig {

    public boolean saveInOriginalOrder, saveInSpecifiedOrder;

    // quick hack for outside modifications
    public final SortCriterion[] sortCriteria = new SortCriterion[3];


    public static class SortCriterion {

        public String field;
        public boolean descending = false;


        public SortCriterion() {
            this.field = "";
        }

        public SortCriterion(String field, String descending) {
            this.field = field;
            this.descending = Boolean.parseBoolean(descending);
        }
    }


    public SaveOrderConfig() {
        // fill default values
        setSaveInOriginalOrder();
        sortCriteria[0] = new SortCriterion();
        sortCriteria[1] = new SortCriterion();
        sortCriteria[2] = new SortCriterion();
    }

    public SaveOrderConfig(Vector<String> data) {
        if (data == null) {
            throw new NullPointerException();
        }
        if (data.isEmpty()) {
            throw new IllegalArgumentException();
        }

        String choice = data.elementAt(0);
        if ("original".equals(choice)) {
            setSaveInOriginalOrder();
        } else {
            setSaveInSpecifiedOrder();
        }

        if (data.size() >= 3) {
            sortCriteria[0] = new SortCriterion(data.elementAt(1), data.elementAt(2));
        } else {
            sortCriteria[0] = new SortCriterion();
        }
        if (data.size() >= 5) {
            sortCriteria[1] = new SortCriterion(data.elementAt(3), data.elementAt(4));
        } else {
            sortCriteria[1] = new SortCriterion();
        }
        if (data.size() >= 7) {
            sortCriteria[2] = new SortCriterion(data.elementAt(5), data.elementAt(6));
        } else {
            sortCriteria[2] = new SortCriterion();
        }
    }

    public void setSaveInOriginalOrder() {
        this.saveInOriginalOrder = true;
        this.saveInSpecifiedOrder = false;
    }

    public void setSaveInSpecifiedOrder() {
        this.saveInOriginalOrder = false;
        this.saveInSpecifiedOrder = true;
    }

    /**
     * Outputs the current configuration to be consumed later by the constructor
     */
    public Vector<String> getVector() {
        Vector<String> res = new Vector<String>(7);
        if (saveInOriginalOrder) {
            res.insertElementAt("original", 0);
        } else {
            assert (saveInSpecifiedOrder);
            res.insertElementAt("specified", 0);
        }

        res.insertElementAt(sortCriteria[0].field, 1);
        res.insertElementAt(Boolean.toString(sortCriteria[0].descending), 2);
        res.insertElementAt(sortCriteria[1].field, 3);
        res.insertElementAt(Boolean.toString(sortCriteria[1].descending), 4);
        res.insertElementAt(sortCriteria[2].field, 5);
        res.insertElementAt(Boolean.toString(sortCriteria[2].descending), 6);

        return res;
    }

}
