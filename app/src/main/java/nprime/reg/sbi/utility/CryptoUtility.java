package nprime.reg.sbi.utility;

import com.mdm.DataObjects.BioMetricsDataDto;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.MGF1ParameterSpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource.PSpecified;
import javax.crypto.spec.SecretKeySpec;


public class CryptoUtility {

	private static BouncyCastleProvider provider;
	private static final String asymmetricAlgorithm = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
	private static final String SYMMETRIC_ALGORITHM = "AES/GCM/NoPadding";//"AES/GCM/PKCS5Padding";
	private static final int GCM_TAG_LENGTH = 128;
	private static final String RSA_ECB_NO_PADDING = "RSA/ECB/NoPadding";
	
	private static final String MGF1 = "MGF1";
	private static final String HASH_ALGO = "SHA-256";
	private static final int asymmetricKeyLength = 2048;

	static {
		provider = init();
	}
	
	private static BouncyCastleProvider init() {
		BouncyCastleProvider provider = new BouncyCastleProvider();
		Security.addProvider(provider);
		return provider;
	}

	public static String getTimestamp() {
		//DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
		//return formatter.format(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
		return formatter.format(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC));
	}
	
	/*public static String getTimestamp() {
		//DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    	return formatter.format(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC));
		//return formatter.format(ZonedDateTime.now());
	}*/
	
	// Function to insert n 0s in the
	// beginning of the given string
	static byte[] prependZeros(byte[] str, int n) {
		byte[] newBytes = new byte[str.length + n];
		int i = 0;
		for (; i < n; i++) {
			newBytes[i] = 0;
		}

		for(int j = 0;i < newBytes.length; i++, j++) {
			newBytes[i] = str[j];
		}

		return newBytes;
	}   
	
	// Function to return the XOR
	// of the given strings
	public static byte[] getXOR(String a, String b) {
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
			xorBytes[i] = (byte)(aBytes[i] ^ bBytes[i]);
		}
		return xorBytes;
	}
	
	public static byte[] getLastBytes(byte[] xorBytes, int lastBytesNum) {
		assert(xorBytes.length >= lastBytesNum);
		return java.util.Arrays.copyOfRange(xorBytes, xorBytes.length - lastBytesNum, xorBytes.length);
	}	
		
	public static Map<String, String>  encrypt(PublicKey publicKey, String data, BioMetricsDataDto bioDto) {
		Map<String, String> result = new HashMap<>();
		try {	
			
			byte[] computedTimestamp = getXOR(bioDto.getTimestamp(), bioDto.getTransactionId());
						
			byte[] dataBytes = java.util.Base64.getDecoder().decode(data); //Base64.decode(data, Base64.NO_WRAP); //
									
			byte[] aadBytes = getLastBytes(computedTimestamp, 16);
			byte[] ivBytes = getLastBytes(computedTimestamp, 12);
		
			SecretKey secretKey = getSymmetricKey();
			final byte[] encryptedData = symmetricEncrypt(secretKey, dataBytes, ivBytes, aadBytes);
			final byte[] encryptedSymmetricKey =  asymmetricEncrypt(publicKey, secretKey.getEncoded());
					
			result.put("ENC_SESSION_KEY", java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedSymmetricKey)); //Base64.encodeToString(encryptedSymmetricKey, Base64.NO_PADDING)
			result.put("ENC_DATA", java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedData)); //Base64.encodeToString(encryptedData, Base64.NO_PADDING));
			result.put("TIMESTAMP", bioDto.getTimestamp());
			
		} catch(Exception ex) {
			Logger.e(DeviceConstants.LOG_TAG, "error in encrypt : " + ex.getMessage());
			ex.printStackTrace();
		}
		return result;
	}

	
	public static String decrypt(PrivateKey privateKey, String sessionKey, String data, String timestamp) {
		try {
			
			timestamp = timestamp.trim();
			byte[] aadBytes = timestamp.substring(timestamp.length() - 16).getBytes();
			byte[] ivBytes = timestamp.substring(timestamp.length() - 12).getBytes();
			
			byte[] decodedSessionKey =  java.util.Base64.getUrlDecoder().decode(sessionKey); //Base64.decode(sessionKey, Base64.NO_WRAP);
			final byte[] symmetricKey = asymmetricDecrypt(privateKey, decodedSessionKey);		
			SecretKeySpec secretKeySpec = new SecretKeySpec(symmetricKey, "AES");
			
			byte[] decodedData =  java.util.Base64.getUrlDecoder().decode(data); //Base64.decode(data, Base64.NO_WRAP);
			final byte[] decryptedData = symmetricDecrypt(secretKeySpec, decodedData, ivBytes, aadBytes);
			return new String(decryptedData);
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	public static byte[] symmetricDecrypt(SecretKeySpec secretKeySpec, byte[] dataBytes, byte[] ivBytes, byte[] aadBytes) {
		try {			
			Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);			
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
			cipher.updateAAD(aadBytes);
			return cipher.doFinal(dataBytes);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	
	
	public static byte[] symmetricEncrypt(SecretKey secretKey, byte[] data, byte[] ivBytes, byte[] aadBytes) {
		try {			
			Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
			//SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
			cipher.updateAAD(aadBytes);
			return cipher.doFinal(data);
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	
	
	public static SecretKey getSymmetricKey() throws NoSuchAlgorithmException {
		KeyGenerator generator = KeyGenerator.getInstance("AES", provider);
		SecureRandom random = new SecureRandom();
		generator.init(256, random);
		return generator.generateKey();
	}
	
	public static byte[] asymmetricEncrypt(PublicKey key, byte[] data) throws Exception {
		
		Cipher cipher = Cipher.getInstance(asymmetricAlgorithm);
		
		final OAEPParameterSpec oaepParams = new OAEPParameterSpec(HASH_ALGO, MGF1, MGF1ParameterSpec.SHA256,
				PSpecified.DEFAULT);
		cipher.init(Cipher.ENCRYPT_MODE, key, oaepParams);
		return doFinal(data, cipher);
	}
 
	public static byte[] asymmetricDecrypt(PrivateKey key, byte[] data)  throws Exception {
	
		Cipher cipher = Cipher.getInstance(RSA_ECB_NO_PADDING);
		cipher.init(Cipher.DECRYPT_MODE, key);

		return doFinal(data, cipher);
		/*byte[] paddedPlainText = doFinal(data, cipher);
		if (paddedPlainText.length < asymmetricKeyLength / 8) {
			byte[] tempPipe = new byte[asymmetricKeyLength / 8];
			System.arraycopy(paddedPlainText, 0, tempPipe, tempPipe.length - paddedPlainText.length,
					paddedPlainText.length);
			paddedPlainText = tempPipe;
		}
		final OAEPParameterSpec oaepParams = new OAEPParameterSpec(HASH_ALGO, MGF1, MGF1ParameterSpec.SHA256,
				PSpecified.DEFAULT);
		return unpadOEAPPadding(paddedPlainText, oaepParams);*/
	}

	
	/*private static byte[] unpadOEAPPadding(byte[] paddedPlainText, OAEPParameterSpec paramSpec) throws Exception{
		byte[] unpaddedData = null;
		sun.security.rsa.RSAPadding padding = sun.security.rsa.RSAPadding.getInstance(
				sun.security.rsa.RSAPadding.PAD_OAEP_MGF1, asymmetricKeyLength / 8, new SecureRandom(), paramSpec);
		unpaddedData = padding.unpad(paddedPlainText);
		return unpaddedData;
	}*/

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

	private static byte[] doFinal(byte[] data, Cipher cipher) throws Exception {
		return cipher.doFinal(data);
	}
	
	/*public static void main(String[] args) throws Exception {
		String data = "this is my test";
		
		KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
		gen.initialize(2048);
		KeyPair pair = gen.generateKeyPair();
		
		String timestamp =  getTimestamp();
		
		byte[] aadBytes = timestamp.substring(timestamp.length() - 16).getBytes();
		byte[] ivBytes = timestamp.substring(timestamp.length() - 12).getBytes();
		byte[] dataBytes = data.getBytes();
	
		SecretKey secretKey = getSymmetricKey();
		final byte[] encryptedData = symmetricEncrypt(secretKey, dataBytes, ivBytes, aadBytes);			
		final byte[] encryptedSymmetricKey =  asymmetricEncrypt(pair.getPublic(), secretKey.getEncoded());
		
		String bioValue = java.util.Base64.getUrlEncoder().encodeToString(encryptedData);
		String sessionKey = java.util.Base64.getUrlEncoder().encodeToString(encryptedSymmetricKey);
				
		byte[] decodedSessionKey =  java.util.Base64.getUrlDecoder().decode(sessionKey);		
		final byte[] symmetricKey = asymmetricDecrypt(pair.getPrivate(), decodedSessionKey);		
		SecretKeySpec secretKeySpec = new SecretKeySpec(symmetricKey, "AES");
		
		byte[] decodedBioValue =  java.util.Base64.getUrlDecoder().decode(bioValue);
		final byte[] decryptedData = symmetricDecrypt(secretKeySpec, decodedBioValue, ivBytes, aadBytes);
		System.out.println(new String(decryptedData));
	}*/
}
