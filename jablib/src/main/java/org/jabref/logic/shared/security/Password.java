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

/// [Password] only decrypts legacy values (values encrypted by older JabRef versions) so that they can be migrated.
/// The fixed IV is kept to retain compatibility with existing ciphertexts.
/// Not to be used to encrypt new values.
public class Password {

    private final byte[] phrase;
    private final SecretKeySpec secretKey;
    private final IvParameterSpec ivSpec;

    /// @param phrase Phrase which should be encrypted or decrypted
    /// @param key    Key which is used to improve symmetric encryption
    public Password(char[] phrase, String key) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this(new String(phrase), key);
    }

    public Password(String phrase, String key) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.phrase = phrase.getBytes();
        this.secretKey = new SecretKeySpec(get128BitHash(key.getBytes()), "AES");
        this.ivSpec = new IvParameterSpec("ThisIsA128BitKey".getBytes());
    }

    /// Decrypts the set phrase/password which was encrypted via [Password#encrypt()].
    ///
    /// @return Decrypted phrase/password
    public String decrypt() throws GeneralSecurityException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        return new String(cipher.doFinal(Base64.getDecoder().decode(phrase)), StandardCharsets.UTF_8);
    }

    /// Returns a 128 bit hash using SHA-256.
    private byte[] get128BitHash(byte[] byteArrayToHash) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(byteArrayToHash);
        return Arrays.copyOf(messageDigest.digest(), 16); // return 128 bit
    }
}
