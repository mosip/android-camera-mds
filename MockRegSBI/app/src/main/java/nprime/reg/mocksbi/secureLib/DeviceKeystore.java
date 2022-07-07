package nprime.reg.mocksbi.secureLib;

import android.content.Context;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import  java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import nprime.reg.mocksbi.R;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

/**
 * @author NPrime Technologies
 */

public class DeviceKeystore {

	private Context context;
	private static String signAlgorithm="RS256";

	public DeviceKeystore(Context context) {
		this.context = context;
	}

	public String getJwt(byte[] data) {
		String keystorePwd = "mosipface";
		String keyAlias = "Device";
		PrivateKey privateKey = null;
		Certificate x509Certificate = null;

		try(InputStream inputStream = context.getResources().openRawResource(R.raw.deviceqa4)) {
			KeyStore keystore = KeyStore.getInstance("PKCS12");
			keystore.load(inputStream, keystorePwd.toCharArray());
			privateKey = (PrivateKey)keystore.getKey(keyAlias, keystorePwd.toCharArray());
			x509Certificate = keystore.getCertificate(keyAlias);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return getJwt(data, privateKey, (X509Certificate)x509Certificate);
	}

	private static String getJwt(byte[] data, PrivateKey privateKey, X509Certificate x509Certificate) {
		String jwsToken = null;
		JsonWebSignature jws = new JsonWebSignature();

		if(x509Certificate != null) {
			List<X509Certificate> certList = new ArrayList<>();
			certList.add(x509Certificate);
			X509Certificate[] certArray = certList.toArray(new X509Certificate[] {});
			jws.setCertificateChainHeaderValue(certArray);
		}

		jws.setPayloadBytes(data);
		jws.setAlgorithmHeaderValue(signAlgorithm);
		jws.setHeader(org.jose4j.jwx.HeaderParameterNames.TYPE, "JWT");
		jws.setKey(privateKey);
		jws.setDoKeyValidation(false);
		try {
			jwsToken = jws.getCompactSerialization();
		} catch (JoseException e) {
			e.printStackTrace();
		}
		return jwsToken;
	}
}
