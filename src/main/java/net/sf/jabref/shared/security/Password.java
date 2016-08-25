package net.sf.jabref.shared.security;

import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import net.sf.jabref.shared.DBMSConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *   {@link Password} contains methods which are useful to encrypt and decrypt phrases using symetric algorithms.
 */
public class Password {

    private static final Log LOGGER = LogFactory.getLog(DBMSConnector.class);

    private static final String ALGORITHM = "AES";
    private static final String KEY = "ThisIsA128bitKey";

    private final String phrase;


    public Password(String phrase) {
        this.phrase = phrase;
    }

    /**
     *  Encrypts the set phrase/password with strong symmetric encryption algorithms.
     *
     *  @return Encrypted phrase/password
     */
    public String encrypt() {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY.getBytes(), ALGORITHM));
            return Base64.getEncoder().encodeToString(cipher.doFinal(phrase.getBytes()));
        } catch (GeneralSecurityException e) {
            LOGGER.error("Encryption error", e);
            return "";
        }
    }

    /**
     *  Decrypts the set phrase/password which was encrypted via {@link Password#encrypt()}.
     *
     *  @return Decrypted phrase/password
     */
    public String decrypt() {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY.getBytes(), ALGORITHM));
            return new String(cipher.doFinal(Base64.getDecoder().decode(phrase)));
        } catch (GeneralSecurityException e) {
            LOGGER.error("Decryption error", e);
            return "";
        }
    }
}
