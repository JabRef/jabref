package net.sf.jabref.event;

/**
 * Represents an interface of a medium which can be used
 * to pass objects on event occurrence.
 */
public interface Event {

    public Object getObject();

    public void setObject(Object object);
}
