package net.sf.jabref.shared.security;

import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *   {@link Password} contains methods which are useful to encrypt and decrypt phrases using symetric algorithms.
 */
public class Password {

    private static final Log LOGGER = LogFactory.getLog(Password.class);

    private static final String ALGORITHM = "AES";
    private byte[] key;

    private final String phrase;


    /**
     * @param phrase Phrase which should be encrypted or decrypted
     * @param key Secret key which is used to improve symmetric encryption
     */
    public Password(String phrase, String key) {
        this.phrase = phrase;
        // normalize to maximum AES key length (16) if too long
        this.key = key.substring(0, Math.min(16, key.length())).getBytes();

        byte[] operand = "ThisIsA128bitKey".getBytes();

        // increase key complexity using XOR
        for (int i = 0; i < this.key.length; i++) {
            operand[i] = (byte) (operand[i] ^ this.key[i]);
        }
        this.key = operand;
    }

    /**
     *  Encrypts the set phrase/password with strong symmetric encryption algorithms.
     *
     *  @return Encrypted phrase/password
     */
    public String encrypt() {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALGORITHM));
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
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALGORITHM));
            return new String(cipher.doFinal(Base64.getDecoder().decode(phrase)));
        } catch (GeneralSecurityException e) {
            LOGGER.error("Decryption error", e);
            return "";
        }
    }
}
