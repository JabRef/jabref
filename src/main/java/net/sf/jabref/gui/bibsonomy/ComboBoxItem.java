package net.sf.jabref.gui.bibsonomy;

/**
 * {@link ComboBoxItem} is a simple class to represent a key-value store in a combo box
 *
 * @param <K> Type of the key
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
class ComboBoxItem<K> {

    private K key;
    private String value;

    public void setKey(K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    ComboBoxItem(K key, String value) {

        setKey(key);
        setValue(value);
    }

    @Override
    public String toString() {
        return getValue();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ComboBoxItem<?>) {
            ComboBoxItem<?> cbi = (ComboBoxItem<?>) obj;
            return (cbi.getValue().equals(this.getValue()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.getValue() != null ? this.getValue().hashCode() : 0);
        return hash;
    }
}
