package nprime.reg.sbi.utility;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import nprime.reg.sbi.faceCaptureApi.DeviceIDDetails;

//import org.jose4j.jws.JsonWebSignature;
//import org.jose4j.lang.JoseException;


public class JwtUtility {

	//TODO Need to be implement using properties 
	//@Value("${mosip.kernel.crypto.sign-algorithm-name:RS256}")
	private static String signAlgorithm="RS256";
	private static CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();

/*	public static String getJwt(byte[] data, PrivateKey privateKey, X509Certificate x509Certificate) {
		String jwsToken = null;
		JsonWebSignature jws = new JsonWebSignature();

		jws.setAlgorithmHeaderValue(signAlgorithm);
		jws.setHeader("typ", "jwt");

		if(x509Certificate != null) {
			List<X509Certificate> certList = new ArrayList<>();
			certList.add(x509Certificate);
			X509Certificate[] certArray = certList.toArray(new X509Certificate[] {});
			//jws.setCertificateChainHeaderValue(certArray);
			jws.setCertificateChainHeaderValue(certArray);
		}

		jws.setPayloadBytes(data);
		jws.setKey(privateKey);
		jws.setDoKeyValidation(false);
		try {
			jwsToken = jws.getCompactSerialization();
		} catch (JoseException e) {
			e.printStackTrace();
		}
		return jwsToken;

	}*/

	public static X509Certificate getCertificate() {

		try {						
			byte[] x509MOSIPCertByte = devCommonDeviceAPI.readFilefromHost((byte) DeviceIDDetails.MOSIP_CERT_STAGING);

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509MOSIPCertByte));

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static X509Certificate getProviderCertificate() {

		try {			
			byte[] x509MOSIPCertByte = devCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.MOSIP_CERT_STAGING);

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509MOSIPCertByte));


		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public PrivateKey getPrivateKey() {		
		try {
			byte[] p12Byte  = devCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.HOST_PRIVATEKEY); 
			InputStream p12File = new ByteArrayInputStream(p12Byte);

			return devCommonDeviceAPI.getPrivateKey(p12File, "rnpmds@123".toCharArray());

		} catch (Exception ex) {
			ex.printStackTrace();
			//throw new Exception("Failed to get private key");
		}
		return null;
	}

	public static PrivateKey getProviderPrivateKey() {		
		try {
			byte[] p12Byte  = devCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.HOST_PRIVATEKEY); 
			InputStream p12File = new ByteArrayInputStream(p12Byte);

			return devCommonDeviceAPI.getPrivateKey(p12File, "RNPmds@123".toCharArray());

		} catch (Exception ex) {
			ex.printStackTrace();
			//throw new Exception("Failed to get private key");
		}
		return null;
	}


	public static PublicKey getPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {		
		X509Certificate certificate = null;
		PublicKey pubKey = null;
		try {

			byte[] certDatainBytes = devCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.MOSIP_CERT_STAGING);
			//byte[] certDatainBytes = org.apache.commons.codec.binary.Base64.decodeBase64(DeviceMain.deviceMain.gmsbEncryptionCertResponse.encryptionCertData.getBytes());

			InputStream inpStream =	new ByteArrayInputStream(certDatainBytes);

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			certificate = (X509Certificate) cf.generateCertificate(inpStream);

			pubKey = certificate.getPublicKey();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Logger.e(DeviceConstants.LOG_TAG, "Invalid Certificate");
		}
		return pubKey;
	}

	/**
	 * Gets the file content.
	 *
	 * @param fis      the fis
	 * @param encoding the encoding
	 * @return the file content
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static String getFileContent(FileInputStream fis, String encoding) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(fis, encoding))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			return sb.toString();
		}
	}

	private static String trimBeginEnd(String pKey) {
		pKey = pKey.replaceAll("-*BEGIN([^-]*)-*(\r?\n)?", "");
		pKey = pKey.replaceAll("-*END([^-]*)-*(\r?\n)?", "");
		pKey = pKey.replaceAll("\\s", "");
		return pKey;
	}

}
