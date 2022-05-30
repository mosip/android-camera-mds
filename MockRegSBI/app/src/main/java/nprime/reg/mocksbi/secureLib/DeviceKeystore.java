package nprime.reg.mocksbi.secureLib;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import nprime.reg.mocksbi.utility.DeviceConstants;

/**
 * @author NPrime Technologies
 */

public class DeviceKeystore {

	public DeviceKeystore() {
	}

	public byte[] getSignature(byte[] inputData) {

		byte[] signatureBytes = null;
		try {

			InputStream inputStream = new ByteArrayInputStream((Base64.decode(DeviceConstants.MOCK_MDS_KEYSTORE,Base64.DEFAULT)));
			KeyStore trustStore = KeyStore.getInstance("BKS");
			trustStore.load(inputStream,"mock@123".toCharArray());
			PrivateKey privateKey = (PrivateKey) trustStore.getKey("nprime", "mock@123".toCharArray());
			if (null != privateKey) {
				Signature sig = Signature.getInstance("SHA256WithRSA");
				sig.initSign(privateKey);
				sig.update(inputData);

				signatureBytes = sig.sign();

				//Verification
				sig.initVerify(getX509Certificate().getPublicKey());
				sig.update(inputData);

				if (!sig.verify(signatureBytes)){
					signatureBytes = null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			signatureBytes = null;
		}

		return signatureBytes;
	}

	public X509Certificate getX509Certificate(){

		X509Certificate  x509Cert = null;
		try {

			//******************
			InputStream inputStream = new ByteArrayInputStream((Base64.decode(DeviceConstants.MOCK_MDS_KEYSTORE,Base64.DEFAULT)));
			KeyStore trustStore = KeyStore.getInstance("BKS");
			trustStore.load(inputStream,"mock@123".toCharArray());
			x509Cert = (X509Certificate)trustStore.getCertificate("nprime");

			//TODO get cert from keyrotation info and check thumbprints of both certs
		} catch (KeyStoreException | CertificateException |
				NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}
		return x509Cert;
	}
}
