package com.SelfHome;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String SECRET_KEY = "1234567890abcdef";

    public static String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec secretKey = new SecretKeySpec("1234567890abcdef".getBytes(), "AES");
            cipher.init(1, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting", e);
        }
    }

    public static String decrypt(String encryptedValue) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec secretKey = new SecretKeySpec("1234567890abcdef".getBytes(), "AES");
            cipher.init(2, secretKey);
            byte[] decodedValue = Base64.getDecoder().decode(encryptedValue);
            return new String(cipher.doFinal(decodedValue));
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting", e);
        }
    }
}
