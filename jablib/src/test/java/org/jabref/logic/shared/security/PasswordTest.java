package org.jabref.logic.shared.security;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PasswordTest {

    @Test
    void passwordAESTest() throws GeneralSecurityException, UnsupportedEncodingException {
        String phrase = "Password";
        Password password = new Password(phrase, "someKey");
        String encryptedPassword = password.encrypt();
        assertNotEquals(phrase, encryptedPassword);
    }

    @Test
    void passwordAsCharTest() throws GeneralSecurityException, UnsupportedEncodingException {
        char[] charPhrase = "Password".toCharArray();
        Password charPassword = new Password(charPhrase, "someKey");
        String charEncryptedPassword = charPassword.encrypt();

        String stringPhrase = "Password";
        Password stringPassword = new Password(stringPhrase, "someKey");
        String stringEncryptedPassword = stringPassword.encrypt();

        assertEquals(charEncryptedPassword, stringEncryptedPassword);
    }
}
