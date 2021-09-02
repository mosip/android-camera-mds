package nprime.reg.sbi.secureLib;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmc.DataObjects.MSBKeyRotationResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;

import javax.security.auth.x500.X500Principal;

import nprime.reg.sbi.faceCaptureApi.DeviceIDDetails;
import nprime.reg.sbi.faceCaptureApi.KeysNotFoundException;
import nprime.reg.sbi.mds.DeviceMain;
import nprime.reg.sbi.mmc.MDServiceUtility;
import nprime.reg.sbi.mmc.MMCHelper;
import nprime.reg.sbi.utility.CommonDeviceAPI;
import nprime.reg.sbi.utility.DeviceConstants;
import nprime.reg.sbi.utility.Logger;
import sun.security.pkcs.PKCS10;
import sun.security.x509.X500Name;
import sun.security.x509.X500Signer;

public class DeviceKeystore {

	private static final String KEYSTORENAME = "AndroidKeyStore";
	private static String KEYSTOREALIAS = "Reg.Face.SBI";
	//private KeyStore keyStore;
	//private KeyStore.ProtectionParameter protParam;
	//private PrivateKey privateKey;

	private static CommonDeviceAPI commDeviceAPI;
	private static MMCHelper mmcHelper;
	private static MDServiceUtility mdServiceUtility;
	ObjectMapper oB = null;
	String aliasName = KEYSTOREALIAS;

	public DeviceKeystore() {
		commDeviceAPI = new CommonDeviceAPI();
		mmcHelper = new MMCHelper();
		mdServiceUtility = new MDServiceUtility();
		oB = new ObjectMapper();

		if (DeviceMain.deviceMain.mgmtServerConnectivity){
			aliasName = commDeviceAPI.getSerialNumber();

			if (!aliasName.isEmpty())
			{
				KEYSTOREALIAS = aliasName;
			}

			if (null == aliasName || aliasName.isEmpty())
			{
				aliasName = KEYSTOREALIAS;
			}
		}
	}

	public void initStore(){

		try{
			String aliasName =  KEYSTOREALIAS;



			KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
			keyStore.load(null);
			/*if (null == keyStore){
				keyStore = KeyStore.getInstance(KEYSTORENAME);
				keyStore.load(null, null);  // Load keystore
			}*/

			if (DeviceMain.deviceMain.mgmtServerConnectivity){
				aliasName = commDeviceAPI.getSerialNumber();

				if (!aliasName.isEmpty())
				{
					KEYSTOREALIAS = aliasName;
				}

				if (null == aliasName || aliasName.isEmpty())
				{
					aliasName = KEYSTOREALIAS;
				}
			}

			/*protParam = new KeyStore.PasswordProtection(aliasName.toCharArray());

			if( keyStore.isKeyEntry(KEYSTOREALIAS)){
				PrivateKeyEntry pkEntry = (PrivateKeyEntry)keyStore.getEntry(KEYSTOREALIAS, protParam);
				if (null != pkEntry){
					privateKey = pkEntry.getPrivateKey();
				}
			}*/
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: Error Loading KeyStore");
		}
	}

	/*public void reloadStore(){
		try {
			keyStore = KeyStore.getInstance(KEYSTORENAME);
			keyStore.load(null, null);  // Load keystore
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	/*public boolean installpfx()
	{
		boolean installStatus = false;
		try {
			X509Certificate cert = extractCertFromPfx();
			X509Certificate[] chain = new X509Certificate[]{cert};

			PrivateKey privateKey = extractPrivateKeyFromPfx();

			PrivateKeyEntry keyEntry = new PrivateKeyEntry(privateKey, chain);

			keyStore.setEntry(KEYSTOREALIAS, keyEntry, protParam);
			keyStore.store(null, null);
			
			installStatus = true;

		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		return installStatus;
	}*/


	public byte[] GenerateCSR(String cnNameforCSR)
	{
		byte[] deviceCSR = null;

		try {
			//String cnNameforCSR = commDeviceAPI.getSerialNumber() + "." + DeviceConstants.DEVICEMODEL;
			String szDN = "CN=" + cnNameforCSR + ", OU=NPRTech, O=NPrime Technologies Private Limited, L=Hyderabad, S=Telangana, C=IN";
			Calendar end = Calendar.getInstance();
			end.add(Calendar.YEAR, 10);
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.
					getInstance("RSA",KEYSTORENAME);

			keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(KEYSTOREALIAS, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
					.setKeyValidityStart(new GregorianCalendar().getTime())
					.setKeyValidityEnd(end.getTime())
					.setKeySize(2048)
					//.setAlgorithmParameterSpec(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
					.setDigests(KeyProperties.DIGEST_SHA256)
					.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
					.setCertificateSubject(new X500Principal(szDN))
					.setCertificateSerialNumber(BigInteger.valueOf(1337))
					.build());
			KeyPair keyPair = keyPairGenerator.generateKeyPair();

			PrivateKey privateKey = keyPair.getPrivate();

			KeyStore ks = KeyStore.getInstance(KEYSTORENAME);
			ks.load(null);
			java.security.cert.Certificate certificate = null;
			Enumeration enumeration = ks.aliases();
			while (enumeration.hasMoreElements()){
				String alias = (String)enumeration.nextElement();
				if(alias.equalsIgnoreCase(KEYSTOREALIAS)){
					certificate = ks.getCertificate(alias);
					privateKey = (PrivateKey)ks.getKey(alias, null);
					break;
				}
			}

			PKCS10 pkcs10 = new PKCS10(certificate.getPublicKey());

			Signature signature = Signature.getInstance("SHA256WithRSA");
			signature.initSign(privateKey);
			X500Name x500name = new X500Name(((X509Certificate)certificate).getSubjectDN().toString());
			X500Signer signer = new X500Signer(signature, x500name);
			pkcs10.encodeAndSign(signer);

			deviceCSR = pkcs10.getEncoded();
			//devicePublicKey = Base64EncodeNDecode.base64Encode(pkcs10.getEncoded());

			/*CertAndKeyGen certAndKeyGen = new CertAndKeyGen("RSA", "SHA256withRSA");
			certAndKeyGen.generate(2048);

			privateKey = certAndKeyGen.getPrivateKey();

			PKCS10 csrReq = certAndKeyGen.getCertRequest(new X500Name("CN=" + cnNameforCSR + ", OU=RNP" +
					", O=Registre public de population, L=Rabat" +
					", S=Rabat, C=MA"));
			deviceCSR = csrReq.getEncoded();*/

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return deviceCSR;
	}

	public byte[] getSignature(byte[] inputData) {

		byte[] signatureBytes = null;
		try {

			KeyStore keyStore = KeyStore.getInstance(KEYSTORENAME);
			keyStore.load(null);
			PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEYSTOREALIAS, null);
			if (null != privateKey) {
				Signature sig = Signature.getInstance("SHA256WithRSA");
				sig.initSign(privateKey);
				sig.update(inputData);

				signatureBytes = sig.sign();

				//Verification
				sig.initVerify(getX509Certificate().getPublicKey());
				sig.update(inputData);

				if (false == sig.verify(signatureBytes)){
					signatureBytes = null;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			signatureBytes = null;
		}

		return signatureBytes;
	}

	/*public boolean updateKeystore(String signedDeviceCertificate) {

		boolean status = false;
		try {

			if (null == signedDeviceCertificate || signedDeviceCertificate.isEmpty()){
				return status;
			}

			byte [] decoded = Base64.decode(signedDeviceCertificate, Base64.DEFAULT);

			X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decoded));
			X509Certificate[] chain = new X509Certificate[]{cert};

			if (null != keyStore){
				if(keyStore.isKeyEntry(KEYSTOREALIAS)){
					keyStore.deleteEntry(KEYSTOREALIAS);
				}

				//PrivateKeyEntry keyEntry = new PrivateKeyEntry(privateKey, chain);
				//keyStore.setEntry(KEYSTOREALIAS, keyEntry, protParam);
				keyStore.setKeyEntry(KEYSTOREALIAS, privateKey, KEYSTOREALIAS.toCharArray(), chain);
				keyStore.store(null, null);

				//initStore();
				status = true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return status;
	}*/

	public X509Certificate getX509Certificate() throws KeysNotFoundException, CertificateException{

		X509Certificate  x509Cert = null;
		X509Certificate x509Certlocal = null;
		byte[] payLoadData = "".getBytes();
		boolean isValidResponse = false;
		byte[] responseBody = "".getBytes();
		try {

			if (null == oB){
				oB = new ObjectMapper();
			}

			//reloadStore();
			KeyStore keyStore = KeyStore.getInstance(KEYSTORENAME);
			keyStore.load(null);
			if (null != keyStore){
				x509Cert = (X509Certificate)keyStore.getCertificate(KEYSTOREALIAS);

				//Verification against Appdata Key and Keystore Key using Thumbprint
				//1. JWT verification from file while loading MDService
				//2. Thumbprint matching
				//3. return X509Certificate else null

				//Reading certificate from local
				if (DeviceMain.deviceMain.mgmtServerConnectivity){
					responseBody = commDeviceAPI.readFilefromHost((byte) DeviceIDDetails.KEYROTATION_RESPONSE_INFO);
					if (0 == responseBody.length){
						Logger.w(DeviceConstants.LOG_TAG, "Failed to read key rotation response from filesystem");
						x509Cert = null;
					}
					else{
						payLoadData = commDeviceAPI.getPayloadBuffer(new String(responseBody));
						isValidResponse = mmcHelper.evaluateMMCResponse(new String(responseBody));

						if (!isValidResponse){
							Logger.w(DeviceConstants.LOG_TAG, "Failed to verify stored response from filesystem");
							x509Cert = null;
						}
						else{
							DeviceMain.deviceMain.gmsbKeyRotationResp = (MSBKeyRotationResponse) (oB.readValue(payLoadData, MSBKeyRotationResponse.class));

							byte [] decoded = java.util.Base64.getDecoder().decode(DeviceMain.deviceMain.gmsbKeyRotationResp.signedDeviceCertificate);
							//Base64.decode(DeviceMain.deviceMain.gmsbKeyRotationResp.signedDeviceCertificate, Base64.NO_WRAP);

							x509Certlocal = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decoded));

							if (null == x509Cert || null == x509Certlocal){
								throw new KeysNotFoundException("Device Key not found");
							}

							if (null != x509Cert)
							{
								Date date = new Date();
								x509Cert.checkValidity(date);
							}

							String keystoreCertThumbprint = commDeviceAPI.getThumbprint(x509Cert);
							String localCertThumbprint = commDeviceAPI.getThumbprint(x509Certlocal);

							if (keystoreCertThumbprint.equals(localCertThumbprint)){
								return x509Cert;
							}
						}
					}
				}
			}

		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (null == x509Cert){
			throw new KeysNotFoundException("Device Key not found");
		}
		return x509Cert;
	}

	public void removeKeys() {

		try{
			//if(null != keyStore /*&& keyStore.isKeyEntry(KEYSTOREALIAS)*/){
			KeyStore keyStore = KeyStore.getInstance(KEYSTORENAME);
			keyStore.load(null);
			keyStore.deleteEntry(KEYSTOREALIAS);
			//}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*private X509Certificate extractCertFromPfx() throws Exception {

		X509Certificate X509certificate;
		try {
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			KeyStore ks = KeyStore.getInstance("PKCS12");
			char[] password = "rnp@123".toCharArray();
			byte [] decodedpfx = Base64.decode(DeviceConstants.KEYPFXCONTENT, Base64.DEFAULT);
			ks.load(new ByteArrayInputStream(decodedpfx), password);
			kmf.init(ks, password);
			ks.getKey(KEYSTOREALIAS, password);
			Certificate[] cc = ks.getCertificateChain(KEYSTOREALIAS);
			X509certificate = (X509Certificate)cc[0];
		} catch (Exception var6) {
			throw new Exception("Error while extracting Certificate.");
		}
		return X509certificate;
	}*/

	/*private PrivateKey extractPrivateKeyFromPfx() throws KeyException, FileSystemException {
		KeyStore ks;
		try {
			ks = KeyStore.getInstance("pkcs12", "SunJSSE");
		} catch (NoSuchProviderException | KeyStoreException var10) {
			throw new KeyException();
		}

		try {
			char[] password = "rnp@123".toCharArray();
			byte [] decodedpfx = Base64.decode(DeviceConstants.KEYPFXCONTENT, Base64.NO_WRAP);
			ks.load(new ByteArrayInputStream(decodedpfx), password);
		} catch (NoSuchAlgorithmException | CertificateException | IOException var9) {
			var9.printStackTrace();
		}

		Enumeration es = null;

		try {
			es = ks.aliases();
		} catch (KeyStoreException var8) {
			var8.printStackTrace();
		}

		String alias = "";
		boolean isAliasWithPrivateKey = false;

		while(true) {
			assert es != null;

			if (!es.hasMoreElements()) {
				break;
			}

			alias = (String)es.nextElement();

			try {
				if (isAliasWithPrivateKey = ks.isKeyEntry(alias)) {
					break;
				}
			} catch (KeyStoreException var11) {
				throw new KeyException();
			}
		}

		if (!isAliasWithPrivateKey) {
			return null;
		} else {
			PrivateKeyEntry pkEntry = null;

			try {
				KeyStore.ProtectionParameter pfxprotParam;
				pfxprotParam = new KeyStore.PasswordProtection("rnp@123".toCharArray());
				pkEntry = (PrivateKeyEntry)ks.getEntry(alias, pfxprotParam);
			} catch (UnrecoverableEntryException | KeyStoreException | NoSuchAlgorithmException var7) {
				var7.printStackTrace();
			}

			assert pkEntry != null;
			return pkEntry.getPrivateKey();
		}
	}*/

}
