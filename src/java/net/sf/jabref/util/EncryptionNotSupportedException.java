package net.sf.jabref.util;

import java.io.IOException;

public class EncryptionNotSupportedException extends IOException {

	private static final long serialVersionUID = 3280233692527372333L;

	public EncryptionNotSupportedException(String string) {
		super(string);
	}

}
