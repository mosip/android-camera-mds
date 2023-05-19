package io.mosip.mock.sbi.utility;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.MGF1ParameterSpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author NPrime Technologies
 */


public class CryptoUtility {
    private static final BouncyCastleProvider provider;
    private static final String HASH_ALGO = "SHA-256";
    private static final String AES = "AES";
    private static final int AES_KEY_LENGTH = 256;
    private static final String SYMMETRIC_ALGORITHM = "AES/GCM/NoPadding"; //"AES/GCM/PKCS5Padding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final String asymmetricAlgorithm = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final String MGF1 = "MGF1";
    private static final Base64.Encoder urlSafeEncoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * Default UTC pattern.
     */
    private static final String UTC_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssX"; //"yyyy-MM-dd'T'HH:mm:ss'Z'";

    static {
        provider = init();
    }

    private static BouncyCastleProvider init() {
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        return provider;
    }

    public static String getTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(UTC_DATETIME_PATTERN);
        return formatter.format(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC));
    }

    public static byte[] generateHash(final byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGO);
        return messageDigest.digest(bytes);
    }

    public static byte[] decodeHex(String hexData) throws DecoderException {
        return Hex.decodeHex(hexData.toCharArray());

    }

    public static String toHex(byte[] bytes) {
        return new String(Hex.encodeHex(bytes)).toUpperCase();
    }

    public static String getBase64encodeString(String payLoad) {
        return encodeToURLSafeBase64(payLoad.getBytes());
    }

    public static String encodeToURLSafeBase64(byte[] data) {
        try {
            return data == null || data.length == 0 ? null : urlSafeEncoder.encodeToString(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public static Map<String, String> encrypt(PublicKey publicKey, byte[] dataBytes, String transactionId) {

        Map<String, String> result = new HashMap<>();
        try {
            String timestamp = getTimestamp();
            byte[] xorResult = getXOR(timestamp, transactionId);

            byte[] aadBytes = getLastBytes(xorResult, 16);
            byte[] ivBytes = getLastBytes(xorResult, 12);

            SecretKey secretKey = getSymmetricKey();
            final byte[] encryptedData = symmetricEncrypt(secretKey, dataBytes, ivBytes, aadBytes);
            final byte[] encryptedSymmetricKey = asymmetricEncrypt(publicKey, secretKey.getEncoded());

            result.put("ENC_SESSION_KEY", encodeToURLSafeBase64(encryptedSymmetricKey));
            result.put("ENC_DATA", encodeToURLSafeBase64(encryptedData));
            result.put("TIMESTAMP", timestamp);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    // Function to return the XOR
    // of the given strings
    private static byte[] getXOR(String a, String b) {
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        // Lengths of the given strings
        int aLen = aBytes.length;
        int bLen = bBytes.length;
        // Make both the strings of equal lengths
        // by inserting 0s in the beginning
        if (aLen > bLen) {
            bBytes = prependZeros(bBytes, aLen - bLen);
        } else if (bLen > aLen) {
            aBytes = prependZeros(aBytes, bLen - aLen);
        }
        // Updated length
        int len = Math.max(aLen, bLen);
        byte[] xorBytes = new byte[len];

        // To store the resultant XOR
        for (int i = 0; i < len; i++) {
            xorBytes[i] = (byte) (aBytes[i] ^ bBytes[i]);
        }
        return xorBytes;
    }

    // Function to insert n 0s in the
    // beginning of the given string
    static byte[] prependZeros(byte[] str, int n) {
        byte[] newBytes = new byte[str.length + n];
        int i = 0;
        for (; i < n; i++) {
            newBytes[i] = 0;
        }

        for (int j = 0; i < newBytes.length; i++, j++) {
            newBytes[i] = str[j];
        }

        return newBytes;
    }

    private static byte[] getLastBytes(byte[] xorBytes, int lastBytesNum) {
        assert (xorBytes.length >= lastBytesNum);
        return java.util.Arrays.copyOfRange(xorBytes, xorBytes.length - lastBytesNum, xorBytes.length);
    }

    public static SecretKey getSymmetricKey() throws NoSuchAlgorithmException {
        javax.crypto.KeyGenerator generator = KeyGenerator.getInstance(AES, provider);
        SecureRandom random = new SecureRandom();
        generator.init(AES_KEY_LENGTH, random);
        return generator.generateKey();
    }

    public static byte[] symmetricEncrypt(SecretKey secretKey, byte[] data, byte[] ivBytes, byte[] aadBytes) {
        try {
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM, provider);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            cipher.updateAAD(aadBytes);
            return cipher.doFinal(data);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] asymmetricEncrypt(PublicKey key, byte[] data) throws Exception {

        Cipher cipher = Cipher.getInstance(asymmetricAlgorithm);

        final OAEPParameterSpec oaepParams = new OAEPParameterSpec(HASH_ALGO, MGF1, MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.ENCRYPT_MODE, key, oaepParams);
        return doFinal(data, cipher);
    }

    private static byte[] doFinal(byte[] data, Cipher cipher) throws Exception {
        return cipher.doFinal(data);
    }
}
