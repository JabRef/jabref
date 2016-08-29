package net.sf.jabref.shared.security;

import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 *   {@link Password} contains methods which are useful to encrypt and decrypt passwords using symetric algorithms.
 */
public class Password {

    private static final String ALGORITHM = "AES";
    private static final String STATIC_KEY = "ThisIsA128bitKey";
    private final byte[] key;
    private final String phrase;


    /**
     * @param phrase Phrase which should be encrypted or decrypted
     * @param key Secret key which is used to improve symmetric encryption
     */
    public Password(String phrase, String key) {
        this.phrase = phrase;
        this.key = convertToAESKey(key);
    }

    /**
     *  Encrypts the set phrase/password with strong symmetric encryption algorithms.
     *
     *  @return Encrypted phrase/password
     */
    public String encrypt() throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALGORITHM));
        return Base64.getEncoder().encodeToString(cipher.doFinal(phrase.getBytes()));
    }

    /**
     *  Decrypts the set phrase/password which was encrypted via {@link Password#encrypt()}.
     *
     *  @return Decrypted phrase/password
     */
    public String decrypt() throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALGORITHM));
        return new String(cipher.doFinal(Base64.getDecoder().decode(phrase)));
    }

    /**
     * Converts the given String to the distinct 128 bit AES key.
     * @param keyToConvert Key to convert
     * @return normalized 128 bit AES key
     */
    private byte[] convertToAESKey(String keyToConvert) {
        // normalize to maximum AES key length (16) if too long
        byte[] convertedKey = keyToConvert.substring(0, Math.min(16, keyToConvert.length())).getBytes();

        byte[] operand = STATIC_KEY.getBytes();

        // increase key complexity using XOR
        for (int i = 0; i < convertedKey.length; i++) {
            operand[i] = (byte) (operand[i] ^ convertedKey[i]);
        }
        return operand;
    }
}