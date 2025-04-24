package org.jabref.logic.shared.security;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * {@link Password} contains methods which are useful to encrypt and decrypt passwords using symetric algorithms.
 */
public class Password {

    private final byte[] phrase;
    private final Cipher cipher;
    private final SecretKeySpec secretKey;
    private final IvParameterSpec ivSpec;

    /**
     * @param phrase Phrase which should be encrypted or decrypted
     * @param key    Key which is used to improve symmetric encryption
     */
    public Password(char[] phrase, String key) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this(new String(phrase), key);
    }

    public Password(String phrase, String key) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.phrase = phrase.getBytes();
        this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        this.secretKey = new SecretKeySpec(get128BitHash(key.getBytes()), "AES");
        this.ivSpec = new IvParameterSpec("ThisIsA128BitKey".getBytes());
    }

    /**
     * Encrypts the set phrase/password with a symmetric encryption algorithm.
     *
     * @return Encrypted phrase/password
     */
    public String encrypt() throws GeneralSecurityException, UnsupportedEncodingException {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        return new String(Base64.getEncoder().encode(cipher.doFinal(phrase)), StandardCharsets.UTF_8);
    }

    /**
     * Decrypts the set phrase/password which was encrypted via {@link Password#encrypt()}.
     *
     * @return Decrypted phrase/password
     */
    public String decrypt() throws GeneralSecurityException, UnsupportedEncodingException {
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        return new String(cipher.doFinal(Base64.getDecoder().decode(phrase)), StandardCharsets.UTF_8);
    }

    /**
     * Returns a 128 bit hash using SHA-256.
     */
    private byte[] get128BitHash(byte[] byteArrayToHash) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(byteArrayToHash);
        return Arrays.copyOf(messageDigest.digest(), 16); // return 128 bit
    }
}
