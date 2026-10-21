package org.jabref.logic.shared.security;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PasswordTest {

    @Test
    void decryptsLegacyCiphertextFromCharArray() throws GeneralSecurityException, UnsupportedEncodingException {
        char[] charPhrase = "xvrYKSvZj1U33vnvKk6b/A==".toCharArray();
        Password charPassword = new Password(charPhrase, "someKey");
        String charDecryptedPassword = charPassword.decrypt();
        assertEquals("Password", charDecryptedPassword);
    }

    @Test
    void decryptsLegacyCiphertext() throws GeneralSecurityException, UnsupportedEncodingException {
        Password password = new Password("xvrYKSvZj1U33vnvKk6b/A==", "someKey");
        assertEquals("Password", password.decrypt());
    }
}
