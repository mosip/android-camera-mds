package io.mosip.mock.sbi.secureLib;

import static io.mosip.mock.sbi.constants.ClientConstants.CERTIFICATE_TO_ENCRYPT_BIO;
import static io.mosip.mock.sbi.utility.CryptoUtility.getTimestamp;
import static io.mosip.mock.sbi.utility.DeviceConstants.DEFAULT_MOSIP_AUTH_APPID;
import static io.mosip.mock.sbi.utility.DeviceConstants.DEFAULT_MOSIP_AUTH_CLIENTID;
import static io.mosip.mock.sbi.utility.DeviceConstants.DEFAULT_MOSIP_AUTH_SECRETKEY;
import static io.mosip.mock.sbi.utility.DeviceConstants.DEFAULT_MOSIP_AUTH_SERVER_URL;
import static io.mosip.mock.sbi.utility.DeviceConstants.DEFAULT_MOSIP_IDA_SERVER_URL;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.apache.commons.codec.digest.DigestUtils;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import io.mosip.mock.sbi.constants.ClientConstants;
import io.mosip.mock.sbi.utility.DeviceConstants;
import io.mosip.mock.sbi.utility.Logger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author NPrime Technologies
 */

public class DeviceKeystore {

    private final Context context;
    private static final String signAlgorithm = "RS256";
    private final String device_keystorePwd;
    private final String device_keyAlias;
    private final String ftm_keystorePwd;
    private final String ftm_keyAlias;
    private final String mosipAuthAppId;
    private final String mosipAuthClientId;
    private final String mosipAuthSecretKey;
    private final String mosipIdaServerUrl;
    private final String mosipAuthServerUrl;
    SharedPreferences sharedPreferences;

    private final static String AUTH_REQ_TEMPLATE = "{ \"id\": \"string\",\"metadata\": {},\"request\": { \"appId\": \"%s\", \"clientId\": \"%s\", \"secretKey\": \"%s\" }, \"requesttime\": \"%s\", \"version\": \"string\"}";

    public DeviceKeystore(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        device_keyAlias = sharedPreferences.getString(ClientConstants.DEVICE_KEY_ALIAS, "");
        device_keystorePwd = sharedPreferences.getString(ClientConstants.DEVICE_KEY_STORE_PASSWORD, "");
        ftm_keyAlias = sharedPreferences.getString(ClientConstants.FTM_KEY_ALIAS, "");
        ftm_keystorePwd = sharedPreferences.getString(ClientConstants.FTM_KEY_STORE_PASSWORD, "");
        mosipAuthAppId = sharedPreferences.getString(ClientConstants.MOSIP_AUTH_APPID, DEFAULT_MOSIP_AUTH_APPID);
        mosipAuthClientId = sharedPreferences.getString(ClientConstants.MOSIP_AUTH_CLIENTID, DEFAULT_MOSIP_AUTH_CLIENTID);
        mosipAuthSecretKey = sharedPreferences.getString(ClientConstants.MOSIP_AUTH_SECRETKEY, DEFAULT_MOSIP_AUTH_SECRETKEY);
        mosipAuthServerUrl = sharedPreferences.getString(ClientConstants.MOSIP_AUTH_SERVER_URL, DEFAULT_MOSIP_AUTH_SERVER_URL);
        mosipIdaServerUrl = sharedPreferences.getString(ClientConstants.MOSIP_IDA_SERVER_URL, DEFAULT_MOSIP_IDA_SERVER_URL);
    }

    public String getJwt(byte[] data, boolean signWithFTMKey) {
        String fileName;
        String keyAlias;
        String keystorePwd;
        if (signWithFTMKey) {
            fileName = ClientConstants.FTM_P12_FILE_NAME;
            keyAlias = ftm_keyAlias;
            keystorePwd = ftm_keystorePwd;
        } else {
            fileName = ClientConstants.DEVICE_P12_FILE_NAME;
            keyAlias = device_keyAlias;
            keystorePwd = device_keystorePwd;
        }
        PrivateKey privateKey;
        Certificate x509Certificate;

        File file = new File(context.getFilesDir(), fileName);

        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(inputStream, keystorePwd.toCharArray());
            privateKey = (PrivateKey) keystore.getKey(keyAlias, keystorePwd.toCharArray());
            x509Certificate = keystore.getCertificate(keyAlias);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return getJwt(data, privateKey, (X509Certificate) x509Certificate);
    }

    private String getJwt(byte[] data, PrivateKey privateKey, X509Certificate x509Certificate) {
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
            Logger.e(DeviceConstants.LOG_TAG, "checkCertificateCredentials: " + e.getMessage());
        }
        return jwsToken;
    }

    public boolean checkCertificateCredentials(String fileName, String keyAlias, String keystorePwd) {
        PrivateKey privateKey;
        Certificate x509Certificate;

        File file = new File(context.getFilesDir(), fileName);

        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(inputStream, keystorePwd.toCharArray());
            privateKey = (PrivateKey) keystore.getKey(keyAlias, keystorePwd.toCharArray());
            x509Certificate = keystore.getCertificate(keyAlias);
            return privateKey != null && x509Certificate != null;
        } catch (Exception e) {
            Logger.e(DeviceConstants.LOG_TAG, "checkCertificateCredentials: " + e.getMessage());
            return false;
        }
    }

    public Certificate getCertificateToEncryptCaptureBioValue() throws CertificateException {
        String certificateStr = sharedPreferences.getString(ClientConstants.CERTIFICATE_TO_ENCRYPT_BIO, "");

        if (certificateStr.equals("")) {
            throw new RuntimeException("Fail to fetch Certificate to Encrypt Captured BIO");
        }

        certificateStr = trimBeginEnd(certificateStr);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(
                new ByteArrayInputStream(Base64.getDecoder().decode(certificateStr)));
    }

    public void loadCertificateFromIDA(Runnable onLoadCompleted) {
        new Thread(() -> {
            String certificateStr = getCertificateFromIDA();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(CERTIFICATE_TO_ENCRYPT_BIO, certificateStr);
            editor.apply();
            onLoadCompleted.run();
        }).start();
    }

    private String getCertificateFromIDA() {
        try {
            OkHttpClient client = new OkHttpClient();
            String requestBody = String.format(AUTH_REQ_TEMPLATE,
                    mosipAuthAppId,
                    mosipAuthClientId,
                    mosipAuthSecretKey,
                    getTimestamp());

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(mediaType, requestBody);
            Request request = new Request.Builder()
                    .url(mosipAuthServerUrl)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String authToken = response.header("authorization");
                Request idaRequest = new Request.Builder()
                        .header("cookie", "Authorization=" + authToken)
                        .url(mosipIdaServerUrl)
                        .get()
                        .build();

                Response idaResponse = new OkHttpClient().newCall(idaRequest).execute();
                if (idaResponse.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(idaResponse.body().string());
                    jsonObject = jsonObject.getJSONObject("response");
                    return jsonObject.getString("certificate");
                }
            }
        } catch (Exception e) {
            Logger.e(DeviceConstants.LOG_TAG, "checkCertificateCredentials: " + e.getMessage());
        }
        return "";
    }

    private static String trimBeginEnd(String pKey) {
        pKey = pKey.replaceAll("-*BEGIN([^-]*)-*(\r?\n)?", "");
        pKey = pKey.replaceAll("-*END([^-]*)-*(\r?\n)?", "");
        pKey = pKey.replaceAll("\\s", "");
        return pKey;
    }

    public static byte[] getCertificateThumbprint(Certificate cert) throws CertificateEncodingException {
        return DigestUtils.sha256(cert.getEncoded());
    }
}
