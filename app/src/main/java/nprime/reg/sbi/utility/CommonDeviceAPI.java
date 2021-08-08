package nprime.reg.sbi.utility;

import android.provider.Settings;
import android.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmc.DataObjects.DataHeader;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

import nprime.reg.sbi.face.MainActivity;
import nprime.reg.sbi.faceCaptureApi.DeviceIDDetails;
import nprime.reg.sbi.mds.DeviceMain;
import nprime.reg.sbi.secureLib.DeviceKeystore;

public class CommonDeviceAPI {

    ObjectMapper oB = null;

    public String getISOTimeStamp() {

        return CryptoUtility.getTimestamp();
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            return formatter.format(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC));
        }else {
            Calendar calendar = GregorianCalendar.getInstance();
            long currentTimeinSeconds = calendar.getTimeInMillis();
            calendar.setTimeInMillis(currentTimeinSeconds);

            int month = (calendar.get(Calendar.MONTH) + 1);

            TimeZone tz = calendar.getTimeZone();

            String resultTz = "";
            long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
            long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset())
                    - TimeUnit.HOURS.toMinutes(hours);
            if (hours > 0) {
                resultTz = String.format("+%02d:%02d", hours, minutes);
            } else {
                resultTz = String.format("%02d:%02d", hours, minutes);
            }

            String szTs = String.format("%d-%02d-%02dT%02d:%02d:%02d.%03d%s", calendar.get(Calendar.YEAR), month, calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
                    calendar.get(Calendar.MILLISECOND), resultTz);
            return szTs;
        }*/
    }

    public byte[] getHeader(X509Certificate certificate) {
        byte[] headerData = null;
        DataHeader header = new DataHeader();

        try {
            if (oB == null)
                oB = new ObjectMapper();

            //header.x5c.add(Base64.encodeToString(certificate.getEncoded(), Base64.NO_WRAP));
            header.x5c.add(java.util.Base64.getEncoder().encodeToString(certificate.getEncoded()));
            //header.x5c.add(DatatypeConverter.printBase64Binary(certificate.getEncoded()));
            headerData = oB.writeValueAsString(header).getBytes();
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
        return headerData;
    }

    public String getSerialNumber() {
        return Settings.Secure.getString(MainActivity.context.getContentResolver(), Settings.Secure.ANDROID_ID);
        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
        //return Build.getSerial().toString();
        /*}else {
            return Build.SERIAL;
        }*/
    }

    public byte[] getSignatureMMCKey(byte[] signdata, X509Certificate certificate) {
        byte[] jwt = null;
        try {
            String passKey = "";
            PrivateKey privateKey = null;

            byte[] p12Bytes = readFilefromHost((byte) DeviceIDDetails.HOST_PRIVATEKEY);
            passKey = "npr@123";

            InputStream p12File= new ByteArrayInputStream(Base64.decode(p12Bytes, Base64.NO_WRAP));

            privateKey = getPrivateKey(p12File, passKey.toCharArray());
            jwt = sign(signdata, privateKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return jwt;
    }

    private byte[] sign(byte[] data, PrivateKey privateKey) {
        byte[] signatureBytes = null;

        try {
            Signature sig = Signature.getInstance("SHA256WithRSA");
            sig.initSign(privateKey);
            sig.update(data);

            signatureBytes = sig.sign();

        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SignatureException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return signatureBytes;
    }

    public PrivateKey getPrivateKey(InputStream p12File, char[] ksPwd) {
        KeyStore kStore = null;
        try {
            kStore = KeyStore.getInstance("PKCS12");
            kStore.load(p12File, ksPwd);
            Enumeration<String> aliases = kStore.aliases();
            String alias = aliases.nextElement();
            p12File.close();
            return (PrivateKey) kStore.getKey(alias, ksPwd);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getPayloadBuffer(String responseBody) {
        byte[] payLoad = null;
        String requestType;

        try {
            JSONObject requestfromMDS = parseJsonRequest(new BufferedReader(new StringReader(responseBody)));
            if(null == requestfromMDS){
                return "".getBytes();
            }

            if (requestfromMDS.length() != 1) {
                return "".getBytes();
            }

            requestType = requestfromMDS.names().getString(0);

            String responseToken = requestfromMDS.get(requestType).toString();
            String []mdsResponseArray = responseToken.split("\\.");

            //Payload
            //System.out.println(mdsResponseArray[1]);
            payLoad = java.util.Base64.getUrlDecoder().decode(mdsResponseArray[1]);//Base64.decode(mdsResponseArray[1], Base64.DEFAULT); //
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return payLoad;
    }

    public JSONObject parseJsonRequest(BufferedReader requestBuffer) {
        JSONObject respObject = null;
        try {
            StringBuffer jb = new StringBuffer();
            String line = null;

            while ((line = requestBuffer.readLine()) != null){
                jb.append(line);
            }
            respObject =  new JSONObject(jb.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return respObject;
    }

    public boolean verifySignature(byte[] payloadData, byte[] signature, X509Certificate certificate) {
        boolean verifyStatus = false;
        try {
            Signature sig = Signature.getInstance("SHA256WithRSA");

            //Verification
            sig.initVerify(certificate.getPublicKey());
            sig.update(payloadData);

            if (true == sig.verify(signature)){
                verifyStatus = true;
            }
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SignatureException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return verifyStatus;
    }

    public String getThumbprint(X509Certificate cert) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String digestHex = "";
        try{
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            digestHex = java.util.Base64.getEncoder().encodeToString(digest).toUpperCase();
            //Base64.encodeToString(digest, Base64.NO_WRAP).toUpperCase();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        return digestHex.toUpperCase();
    }

    public byte[] generateCSR(DeviceKeystore devKeyStore) {
        byte[] deviceCSR = null;

        try{
            String cnNameforCSR = getSerialNumber() + "." + DeviceConstants.DEVICEMODEL;
            deviceCSR = devKeyStore.GenerateCSR(cnNameforCSR);
        }
        catch(Exception ex) {
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Error while generating CSR");
        }
        return deviceCSR;
    }

    public long getMillisfromISO(String isoTimeStamp) {
        long currentTimeinMillis = 0l;

        if (null != isoTimeStamp && !isoTimeStamp.isEmpty()){
            try{
                /*DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.getDefault());
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.YEAR, DeviceConstants.MDS_REGISTRATION_DEVICE_EXPIRY);
                currentTimeinMillis = format.parse(isoTimeStamp).getTime();*/

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                    currentTimeinMillis = ZonedDateTime.parse(isoTimeStamp, formatter).toInstant().toEpochMilli();
                }
            }
            catch(Exception ex)
            {
                //ex.printStackTrace();
                //System.out.println("Face Auth SBI :: ISO timestamp parse error");
            }
        }

        return currentTimeinMillis;
    }

    public byte[] getHeaderfromCert(String certData) {
        byte[] headerData = null;
        DataHeader header = new DataHeader();

        try
        {
            if (oB == null)
                oB = new ObjectMapper();
            header.x5c.add(certData.replaceAll("-----BEGIN CERTIFICATE-----\n", "")
                    .replaceAll("-----END CERTIFICATE-----", "").replaceAll("\r\n", ""));
            //header.x5c.add(certData.replaceAll(X509Factory.BEGIN_CERT, "").replaceAll(X509Factory.END_CERT, "").replaceAll("\r\n", ""));
            headerData =  oB.writeValueAsString(header).getBytes();
        }
        catch(Exception ex)
        {
            //ex.printStackTrace();
        }
        return headerData;
    }

    public boolean isValidDeviceforCertificate(String serialNumber, DeviceKeystore devKeystore) {
        boolean status = false;
        String commonName = "";
        try{
            X509Certificate certificate = devKeystore.getX509Certificate();

            commonName = getCommonName(certificate);

            if (commonName.startsWith(serialNumber)){
                status = true;
            }
            else{
                status = false;
            }
        }
        catch(Exception ex){
            status = false;
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: While validating Certificate and connected device");
        }

        return status;
    }

    private String getCommonName(X509Certificate c) {
        for (String each : c.getSubjectDN().getName().split(",\\s*")) {
            if (each.startsWith("CN=")) {
                String result = each.substring(3);
                return result;
            }
        }
        throw new IllegalStateException("Missed CN in Subject DN: "
                + c.getSubjectDN());
    }

    public byte[] getSignature(DeviceKeystore devKeystore, byte[] data) {
        byte[] signedData = null;
        try {
            //Native Signing
            signedData = devKeystore.getSignature(data);

        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Error while signing with Device Key");
        }
        return signedData;
    }

    public byte[] Sha256(byte[] inpData) {
        Security.addProvider(new BouncyCastleProvider());
        String algorithm = "SHA-256";
        String SECURITY_PROVIDER = "BC";
        byte[] hash = null;
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance(algorithm);
            digest.reset();
            hash = digest.digest(inpData);
        }
        catch (Exception e)
        {
            Logger.e(DeviceConstants.LOG_TAG, "Error while generating SHA");
        }
        return hash;
    }

    public String digestAsPlainText(byte[] digest) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : digest) {
            stringBuilder.append(String.format("%02X", b & 0xff));
        }

        return stringBuilder.toString().toUpperCase();
    }

    private byte[] readInputStream(InputStream in) {
        if (in == null) {
            return null;
        }

        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream(in.available());
            byte[] buffer = new byte[16 * 1024];
            int bytesRead = in.read(buffer);
            while (bytesRead >= 0) {
                out.write(buffer, 0, bytesRead);
                bytesRead = in.read(buffer);
            }
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] readFilefromHost(byte fileCode) {
        byte[] keyfromDevice = null;
        String certPath = "";

        int nRetry = 3;

        try
        {
            for(int nRetryAttempt = 1; nRetryAttempt <= nRetry; nRetryAttempt++ )
            {
                Logger.d(DeviceConstants.LOG_TAG, "Face Auth SBI :: Reading File from Host, Attempt # " + nRetryAttempt);

                if (DeviceIDDetails.MOSIP_CERT_PROD == fileCode) {
                    InputStream in = DeviceMain.context.getAssets().open("mosipprod.cer");
                    keyfromDevice = readInputStream(in);
                    if(null == keyfromDevice){
                        keyfromDevice = "".getBytes();
                    }
                }
                else if (DeviceIDDetails.MOSIP_CERT_PRE_PROD == fileCode) {
                    InputStream in = DeviceMain.context.getAssets().open("mosippreprod.cer");
                    keyfromDevice = readInputStream(in);
                    if(null == keyfromDevice){
                        keyfromDevice = "".getBytes();
                    }
                }
                else if (DeviceIDDetails.MOSIP_CERT_STAGING == fileCode) {
                    InputStream in = DeviceMain.context.getAssets().open("mosip-public-key.cer");
                    keyfromDevice = readInputStream(in);
                    if(null == keyfromDevice){
                        keyfromDevice = "".getBytes();
                    }
                }
                else if (DeviceIDDetails.MOSIP_CERT_DEVELOPER == fileCode) {
                    InputStream in = DeviceMain.context.getAssets().open("mosipdeveloper.cer");
                    keyfromDevice = readInputStream(in);
                    if(null == keyfromDevice){
                        keyfromDevice = "".getBytes();
                    }
                }
				/*else if (DeviceIDDetails.DEVICE_CERT == fileCode)	{
					certPath = DeviceMain.deviceMain.appFolderPath + "\\deviceKey.cer";
					if (true == Files.exists(Paths.get(certPath))) {
						File crtFile = new File(certPath);
						int crtFileLength = (int)crtFile.length();
						keyfromDevice = new byte[crtFileLength];
						if (crtFile.isFile() && crtFile.canRead()) {
							FileInputStream in = new FileInputStream(crtFile);
							in.read(keyfromDevice, 0, crtFileLength);
							in.close();
							break;
						}
					}
					else
					{
						keyfromDevice = "".getBytes();
					}
					break;
				}*/
                else if (DeviceIDDetails.HOST_CERT == fileCode)	{
                    InputStream in = DeviceMain.context.getAssets().open("SBIHostKeyRoot.cer");
                    keyfromDevice = readInputStream(in);
                    if(null == keyfromDevice){
                        keyfromDevice = "".getBytes();
                    }
                }
                else if (DeviceIDDetails.TRUST_CERT == fileCode)	{
                    keyfromDevice = DeviceConstants.TRUSTCERT.getBytes();
                    break;
					/*String currentAppPath = new File(".").getCanonicalPath();
					certPath = currentAppPath + "//certs//mmcTrust.cer";
					if (true == Files.exists(Paths.get(certPath))) {
						File crtFile = new File(certPath);
						int crtFileLength = (int)crtFile.length();
						keyfromDevice = new byte[crtFileLength];
						if (crtFile.isFile() && crtFile.canRead()) {
							FileInputStream in = new FileInputStream(crtFile);
							in.read(keyfromDevice, 0, crtFileLength);
							in.close();
							break;
						}
					}
					else
					{
						keyfromDevice = "".getBytes();
					}*/
                }
                else if (DeviceIDDetails.HOST_PRIVATEKEY == fileCode)	{
                    keyfromDevice = DeviceConstants.HOSTPRIVATEKEYCONTENT.getBytes();
                    break;
					/*String currentAppPath = new File(".").getCanonicalPath();
					certPath = currentAppPath + "//certs//FaceSBIHost.p12";
					if (true == Files.exists(Paths.get(certPath))) {
						File crtFile = new File(certPath);
						int crtFileLength = (int)crtFile.length();
						keyfromDevice = new byte[crtFileLength];
						if (crtFile.isFile() && crtFile.canRead()) {
							FileInputStream in = new FileInputStream(crtFile);
							in.read(keyfromDevice, 0, crtFileLength);
							in.close();
							break;
						}
					}
					else
					{
						keyfromDevice = "".getBytes();
					}*/
                }
				/*else  if (DeviceIDDetails.RESERVED_MOSIP_CERT_1 == fileCode)	{ //serialno
					certPath = DeviceMain.deviceMain.appFolderPath + "\\device.sno";
					if (true == Files.exists(Paths.get(certPath))) {
						File crtFile = new File(certPath);
						int crtFileLength = (int)crtFile.length();
						keyfromDevice = new byte[crtFileLength];
						if (crtFile.isFile() && crtFile.canRead()) {
							FileInputStream in = new FileInputStream(crtFile);
							in.read(keyfromDevice, 0, crtFileLength);
							in.close();
							break;
						}
					}
					else
					{
						keyfromDevice = "".getBytes();
					}
				}
				else  if (DeviceIDDetails.RESERVED_MOSIP_CERT_2 == fileCode)	{ //devicecode
					certPath = DeviceMain.deviceMain.appFolderPath + "\\device.uuid";
					if (true == Files.exists(Paths.get(certPath))) {
						File crtFile = new File(certPath);
						int crtFileLength = (int)crtFile.length();
						keyfromDevice = new byte[crtFileLength];
						if (crtFile.isFile() && crtFile.canRead()) {
							FileInputStream in = new FileInputStream(crtFile);
							in.read(keyfromDevice, 0, crtFileLength);
							in.close();
							break;
						}
					}
					else
					{
						keyfromDevice = "".getBytes();
					}
				}*/
                else if (DeviceIDDetails.SNAPSHOT_RESPONSE_INFO == fileCode) {
                    String encodedData = MainActivity.sharedPreferences.getString("SNAPSHOT_INFO", null);
                    if(null == encodedData){
                        keyfromDevice = "".getBytes();
                    }else {
                        keyfromDevice = Base64.decode(encodedData, Base64.DEFAULT);
                    }

                    /*InputStream in = DeviceMain.context.getAssets().open("snapShot.info");
                    keyfromDevice = readInputStream(in);
                    if(null == keyfromDevice){
                        keyfromDevice = "".getBytes();
                    }*/
                    break;
                }
                else if (DeviceIDDetails.KEYROTATION_RESPONSE_INFO == fileCode) {
                    String encodedData = MainActivity.sharedPreferences.getString("KEY_ROTATION_INFO", null);
                    if(null == encodedData){
                        keyfromDevice = "".getBytes();
                    }else {
                        keyfromDevice = Base64.decode(encodedData, Base64.DEFAULT);
                    }
                    /*InputStream in = DeviceMain.context.getAssets().open("keyRotation.info");
                    keyfromDevice = readInputStream(in);
                    if(null == keyfromDevice){
                        keyfromDevice = "".getBytes();
                    }*/
                    break;
                }
                else if (DeviceIDDetails.ENCRYPTIONCERT_RESPONSE_INFO == fileCode) {
                    String encodedData = MainActivity.sharedPreferences.getString("ENCRYPTION_CERT_INFO", null);
                    if(null == encodedData){
                        keyfromDevice = "".getBytes();
                    }else {
                        keyfromDevice = Base64.decode(encodedData, Base64.DEFAULT);
                    }

                    /*certPath = DeviceMain.deviceMain.appFolderPath + "\\encryptionCert.info";
                    if (true == Files.exists(Paths.get(certPath))) {
                        keyfromDevice = Files.readAllBytes(Paths.get(certPath));
                    }
                    else{
                        keyfromDevice = "".getBytes();
                    }*/
                    break;
                }
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }


        return keyfromDevice;
    }

    public void WriteFiletoHost(int fileCode, byte[] fileContent) {
        if (DeviceIDDetails.SNAPSHOT_RESPONSE_INFO == fileCode) {
            MainActivity.sharedPreferences.edit().putString("SNAPSHOT_INFO", Base64.encodeToString(fileContent, Base64.DEFAULT)).apply();
        }
        else if (DeviceIDDetails.KEYROTATION_RESPONSE_INFO == fileCode) {
            MainActivity.sharedPreferences.edit().putString("KEY_ROTATION_INFO", Base64.encodeToString(fileContent, Base64.DEFAULT)).apply();
        }
        else if (DeviceIDDetails.ENCRYPTIONCERT_RESPONSE_INFO == fileCode) {
            MainActivity.sharedPreferences.edit().putString("ENCRYPTION_CERT_INFO", Base64.encodeToString(fileContent, Base64.DEFAULT)).apply();
        }
    }
}
