package nprime.reg.mocksbi.utility;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author NPrime Technologies
 */


public class CryptoUtility {
    private static final String HASH_ALGO = "SHA-256";

    public static String getTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
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
}
