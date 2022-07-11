package nprime.reg.mocksbi.secureLib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import nprime.reg.mocksbi.constants.ClientConstants;

/**
 * @author NPrime Technologies
 */

public class DeviceKeystore {

    private final Context context;
    private static final String signAlgorithm = "RS256";
    private final String keystorePwd;
    private final String keyAlias;

    public DeviceKeystore(Context context) {
        this.context = context;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        keyAlias = sharedPreferences.getString(ClientConstants.KEY_ALIAS, "");
        keystorePwd = sharedPreferences.getString(ClientConstants.KEY_STORE_PASSWORD, "");

    }

    public String getJwt(byte[] data) {
        PrivateKey privateKey = null;
        Certificate x509Certificate = null;

        File file = new File(context.getFilesDir(), ClientConstants.P12_FILE_NAME);

        try (InputStream inputStream = new FileInputStream(file)) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(inputStream, keystorePwd.toCharArray());
            privateKey = (PrivateKey) keystore.getKey(keyAlias, keystorePwd.toCharArray());
            x509Certificate = keystore.getCertificate(keyAlias);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return getJwt(data, privateKey, (X509Certificate) x509Certificate);
    }

    private static String getJwt(byte[] data, PrivateKey privateKey, X509Certificate x509Certificate) {
        String jwsToken = null;
        JsonWebSignature jws = new JsonWebSignature();

        if (x509Certificate != null) {
            List<X509Certificate> certList = new ArrayList<>();
            certList.add(x509Certificate);
            X509Certificate[] certArray = certList.toArray(new X509Certificate[]{});
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

    public boolean checkCertificateCredentials() {
        PrivateKey privateKey = null;
        Certificate x509Certificate = null;

        File file = new File(context.getFilesDir(), ClientConstants.P12_FILE_NAME);

        try (InputStream inputStream = new FileInputStream(file)) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(inputStream, keystorePwd.toCharArray());
            privateKey = (PrivateKey) keystore.getKey(keyAlias, keystorePwd.toCharArray());
            x509Certificate = keystore.getCertificate(keyAlias);
            return privateKey != null && x509Certificate != null;
        } catch (Exception e) {
            return false;
        }
    }
}
