package nprime.reg.mocksbi.utility;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author NPrime Technologies
 */


public class CryptoUtility {

	private static final BouncyCastleProvider provider;
	
	private static final String MGF1 = "MGF1";
	private static final String HASH_ALGO = "SHA-256";

	static {
		provider = init();
	}
	
	private static BouncyCastleProvider init() {
		BouncyCastleProvider provider = new BouncyCastleProvider();
		Security.addProvider(provider);
		return provider;
	}

	public static String getTimestamp() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
		return formatter.format(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC));
	}
	
	public static byte[] getLastBytes(byte[] xorBytes, int lastBytesNum) {
		assert (xorBytes.length >= lastBytesNum);
		return java.util.Arrays.copyOfRange(xorBytes, xorBytes.length - lastBytesNum, xorBytes.length);
	}

	public static byte[] generateHash(final byte[] bytes) throws NoSuchAlgorithmException{
		MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGO);
		return messageDigest.digest(bytes);
	}

	public static byte[] decodeHex(String hexData) throws DecoderException {
		return Hex.decodeHex(hexData.toCharArray());

	}

	public static String toHex(byte[] bytes) {
		//return Hex.encodeHexString(bytes).toUpperCase();
		return new String(Hex.encodeHex(bytes)).toUpperCase();
	}
}
