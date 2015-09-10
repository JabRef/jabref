package net.sf.jabref;

import java.awt.event.ActionEvent;

public interface AutoCompleteFormater<E> { 
    String formatItemToString(E item);
}