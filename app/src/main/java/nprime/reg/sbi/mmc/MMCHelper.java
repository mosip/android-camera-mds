package nprime.reg.sbi.mmc;

import android.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmc.DataObjects.DeviceData;
import com.mmc.DataObjects.DeviceDataRequest;
import com.mmc.DataObjects.DeviceRegistration;
import com.mmc.DataObjects.DeviceSnapshot;
import com.mmc.DataObjects.MSBDeviceRegistration;
import com.mmc.DataObjects.MSBEncryptionCert;
import com.mmc.DataObjects.MSBKeyRotation;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import nprime.reg.sbi.faceCaptureApi.DeviceIDDetails;
import nprime.reg.sbi.mds.DeviceMain;
import nprime.reg.sbi.scanner.ResponseGenerator.ResponseGenHelper;
import nprime.reg.sbi.utility.CommonDeviceAPI;
import nprime.reg.sbi.utility.DeviceConstants;
import nprime.reg.sbi.utility.Logger;

public class MMCHelper {
    CommonDeviceAPI libCommonDeviceAPI;
    MMCServiceUtility mmcServiceUtility;

    ObjectMapper oB = null;

    public MMCHelper() {
        libCommonDeviceAPI = new CommonDeviceAPI();
        mmcServiceUtility = new MMCServiceUtility();

        oB = new ObjectMapper();
    }

    public String getMsbDeviceSnapShot() {
        String responseBody = "";
        CertificateFactory cf;
        byte[] headerBytes = null;
        byte[] payloadBytes = null;
        JSONObject data = new JSONObject();
        String msbDeviceSnapshotRequest = "";

        try {

            if (oB == null){
                oB = new ObjectMapper();
            }

            cf = CertificateFactory.getInstance("X.509");

            byte[] x509MOSIPCertByte = libCommonDeviceAPI.readFilefromHost((byte) DeviceIDDetails.HOST_CERT);
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509MOSIPCertByte));

            //HeaderData
            headerBytes = libCommonDeviceAPI.getHeader(certificate);

            //Payload
            payloadBytes = getDeviceSnapshotData();

            String encodedData = getJwtEncodedData(headerBytes, payloadBytes, certificate);

            data.put("msbDeviceSnapshot", encodedData);

            msbDeviceSnapshotRequest = data.toString();

            responseBody = mmcServiceUtility.getDataFromMMC(msbDeviceSnapshotRequest);
        } catch (CertificateException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return responseBody;
    }

    private String getJwtEncodedData(byte[] headerBytes, byte[] payloadBytes, X509Certificate certificate) {
        //HeaderData
        String headerBuffer = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(headerBytes);//Base64.encodeToString(headerBytes, Base64.NO_PADDING);

        //Payload
        String payloadBuffer = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes);//Base64.encodeToString(payloadBytes, Base64.NO_PADDING);

        String signString = headerBuffer + "." + payloadBuffer;

        //Signature
        String signatureBuffer = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                libCommonDeviceAPI.getSignatureMMCKey(signString.getBytes() , certificate));
                //Base64.encodeToString(libCommonDeviceAPI
                //.getSignatureMMCKey(signString.getBytes() , certificate), Base64.NO_PADDING);

        String encodedData = signString + "." + signatureBuffer;

        return encodedData;
    }

    private byte[] getDeviceSnapshotData() {
        byte[] deviceSnapshotData = null;
        try
        {

            DeviceSnapshot deviceSnapshotPayload = new DeviceSnapshot();
            deviceSnapshotPayload.uniqueTransactionId = getUniqueID();
            deviceSnapshotPayload.timeStamp = libCommonDeviceAPI.getISOTimeStamp();
            deviceSnapshotPayload.serialNumber = libCommonDeviceAPI.getSerialNumber();
            deviceSnapshotPayload.deviceCodeUUID = libCommonDeviceAPI.getSerialNumber();//DeviceMain.deviceMain.mainDeviceCode;
            deviceSnapshotPayload.modelID = DeviceConstants.DEVICEMODEL;
            deviceSnapshotPayload.deviceProviderId = DeviceConstants.PROVIDERID;
            deviceSnapshotPayload.bioType = "Face";
            deviceSnapshotPayload.certificationLevel = "L0";
            deviceSnapshotPayload.devicePurpose = DeviceConstants.PURPOSE;
            deviceSnapshotPayload.env = DeviceConstants.ENVIRONMENT;
            deviceSnapshotPayload.deviceServiceVersion = DeviceConstants.MDSVERSION;
            deviceSnapshotPayload.specVersion = DeviceConstants.REGSERVER_VERSION;
            deviceSnapshotPayload.hostos = DeviceConstants.OSTYPE;

            deviceSnapshotData =  oB.writeValueAsString(deviceSnapshotPayload).getBytes();

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }


        return deviceSnapshotData;
    }

    private String getUniqueID() {
        int trid = 10000000 + new Random().nextInt(90000000);
        return String.valueOf(trid);
    }

    public boolean evaluateMMCResponse(String responseData) {
        byte[] headerBuffer = null;
        byte[] signatureBuffer = null;
        String responseToken;
        boolean certVerifyStatus = false;
        try {

            if (0 == responseData.length()){
                return false;
            }

            JSONObject requestfromMDS = libCommonDeviceAPI.parseJsonRequest(new BufferedReader(new StringReader(responseData)));
            if(null == requestfromMDS){
                return false;
            }

            if (requestfromMDS.length() != 1) {

                Logger.d(DeviceConstants.LOG_TAG, "Face SBI :: " + requestfromMDS.get("description"));
                //libCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", requestfromMDS.get("description").toString());
                return false;
            }

            String requestType = requestfromMDS.names().getString(0);

            responseToken = requestfromMDS.get(requestType).toString();
            String []mdsResponseArray = responseToken.split("\\.");

            headerBuffer = java.util.Base64.getUrlDecoder().decode(mdsResponseArray[0]); //Base64.decode(mdsResponseArray[0], Base64.DEFAULT);

            String hostCert = new JSONObject(new String(headerBuffer)).getJSONArray("x5c").getString(0);

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            //InputStream stream = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(hostCert));
            InputStream stream = new ByteArrayInputStream(Base64.decode(hostCert, Base64.DEFAULT));
            X509Certificate hostX509Cert = (X509Certificate) certFactory.generateCertificate(stream);

            //Check Trust of Certificate if successful then verify Signature

            if (evaluateTrust(hostX509Cert)) {

                //Signature
                signatureBuffer = java.util.Base64.getUrlDecoder().decode(mdsResponseArray[2]); //Base64.decode(mdsResponseArray[2], Base64.DEFAULT);

                certVerifyStatus = libCommonDeviceAPI.verifySignature( (mdsResponseArray[0] + "." + mdsResponseArray[1]).getBytes(), signatureBuffer, hostX509Cert);
            }

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Logger.e(DeviceConstants.LOG_TAG, "Error while validating MMC response");
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return certVerifyStatus;
    }

    private boolean evaluateTrust(X509Certificate x509CertfromMMC) {
        boolean isValidCert = false;

        try {

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            byte[] x509CertByte = libCommonDeviceAPI.readFilefromHost((byte) DeviceIDDetails.TRUST_CERT);

            X509Certificate trustCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509CertByte));

            if (null != trustCert){
                PublicKey trustPublicKeyMDS = trustCert.getPublicKey();

                x509CertfromMMC.verify(trustPublicKeyMDS);

                Logger.d(DeviceConstants.LOG_TAG,"Face SBI :: TRUST CERT verification Success");
                isValidCert = true;
            }
            else {
                isValidCert = false;
            }

        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch (NoSuchProviderException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch (SignatureException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            Logger.e(DeviceConstants.LOG_TAG,"Face SBI :: TRUST CERT verification Failed");
        }


        return isValidCert;
    }

    public String getmsbKeyRotation() {
        CertificateFactory cf;
        byte[] headerBuffer = null;
        byte[] payloadBuffer = null;
        JSONObject data = new JSONObject();
        String responseBody = "";
        String msbKeyRotationRequest = "";

        try {

            if (oB == null){
                oB = new ObjectMapper();
            }

            cf = CertificateFactory.getInstance("X.509");

            byte[] x509MOSIPCertByte = libCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.HOST_CERT);
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509MOSIPCertByte));

            //HeaderData
            headerBuffer = libCommonDeviceAPI.getHeader(certificate);

            //Payload
            payloadBuffer = getmsbKeyRotationData();

            String encodedData = getJwtEncodedData(headerBuffer, payloadBuffer, certificate);

            data.put("msbKeyRotation", encodedData);

            msbKeyRotationRequest = data.toString();

            responseBody = mmcServiceUtility.getDataFromMMC(msbKeyRotationRequest);
        } catch (CertificateException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return responseBody;
    }

    private byte[] getmsbKeyRotationData() {
        byte[] msbKeyRotationData = null;
        try
        {
            MSBKeyRotation msbKeyRotationPayload = new MSBKeyRotation();
            msbKeyRotationPayload.uniqueTransactionId = String.valueOf(getUniqueID());
            msbKeyRotationPayload.timeStamp = libCommonDeviceAPI.getISOTimeStamp();
            msbKeyRotationPayload.serialNumber = libCommonDeviceAPI.getSerialNumber();//DeviceMain.deviceMain.mainSerialNumber;
            msbKeyRotationPayload.deviceCodeUUID = libCommonDeviceAPI.getSerialNumber();//DeviceMain.deviceMain.mainDeviceCode;
            msbKeyRotationPayload.modelID = DeviceConstants.DEVICEMODEL;
            msbKeyRotationPayload.deviceProviderId = DeviceConstants.PROVIDERID;
            msbKeyRotationPayload.bioType = "Face";
            msbKeyRotationPayload.certificationLevel = "L0";
            msbKeyRotationPayload.devicePurpose = DeviceConstants.PURPOSE;
            msbKeyRotationPayload.env = DeviceConstants.ENVIRONMENT;
            msbKeyRotationPayload.deviceServiceVersion = DeviceConstants.MDSVERSION;
            msbKeyRotationPayload.specVersion = DeviceConstants.REGSERVER_VERSION;
            msbKeyRotationPayload.hostos = DeviceConstants.OSTYPE;


            byte[] deviceCSR = libCommonDeviceAPI.generateCSR(DeviceMain.deviceKeystore);

            //msbKeyRotationPayload.csrData = DatatypeConverter.printBase64Binary(deviceCSR);
            msbKeyRotationPayload.csrData = java.util.Base64.getEncoder().encodeToString(deviceCSR);//Base64.encodeToString(deviceCSR, Base64.DEFAULT);

            msbKeyRotationData =  oB.writeValueAsString(msbKeyRotationPayload).getBytes();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }

        return msbKeyRotationData;
    }

    public String getmsbDeviceRegistration() {
        CertificateFactory cf;
        byte[] headerBuffer = null;
        byte[] payloadBuffer = null;
        JSONObject data = new JSONObject();
        String responseBody = "";
        String msbDeviceRegistationRequest = "";

        try {

            if (oB == null){
                oB = new ObjectMapper();
            }

            cf = CertificateFactory.getInstance("X.509");

            byte[] x509MOSIPCertByte = libCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.HOST_CERT);
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509MOSIPCertByte));

            //HeaderData
            headerBuffer = libCommonDeviceAPI.getHeader(certificate);

            //Payload
            payloadBuffer = getmsbDeviceRegistrationData();

            String encodedData = getJwtEncodedData(headerBuffer, payloadBuffer, certificate);

            data.put("msbDeviceRegistration", encodedData);

            msbDeviceRegistationRequest = data.toString();
            responseBody = mmcServiceUtility.getDataFromMMC(msbDeviceRegistationRequest);

        } catch (CertificateException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return responseBody;
    }

    private byte[] getmsbDeviceRegistrationData() {
        byte[] msbRegistrationData = null;
        try
        {
            MSBDeviceRegistration msbDeviceRegistrationPayload = new MSBDeviceRegistration();
            msbDeviceRegistrationPayload.uniqueTransactionId = String.valueOf(getUniqueID());
            msbDeviceRegistrationPayload.timeStamp = libCommonDeviceAPI.getISOTimeStamp();
            msbDeviceRegistrationPayload.serialNumber = libCommonDeviceAPI.getSerialNumber();
            msbDeviceRegistrationPayload.deviceCodeUUID = DeviceMain.deviceMain.mainDeviceCode;
            msbDeviceRegistrationPayload.modelID = DeviceConstants.DEVICEMODEL;
            msbDeviceRegistrationPayload.deviceProviderId = DeviceConstants.PROVIDERID;
            msbDeviceRegistrationPayload.bioType = "Face";
            msbDeviceRegistrationPayload.certificationLevel = "L0";
            msbDeviceRegistrationPayload.devicePurpose = DeviceConstants.PURPOSE;
            msbDeviceRegistrationPayload.env = DeviceConstants.ENVIRONMENT;
            msbDeviceRegistrationPayload.deviceServiceVersion = DeviceConstants.MDSVERSION;
            msbDeviceRegistrationPayload.specVersion = DeviceConstants.REGSERVER_VERSION;
            msbDeviceRegistrationPayload.hostos = DeviceConstants.OSTYPE;

            //Registration Request JSON
            String serialNumber = libCommonDeviceAPI.getSerialNumber();
            String regRequestData = getRegRequest(serialNumber);

            msbDeviceRegistrationPayload.deviceRegistrationRequest = regRequestData;

            msbRegistrationData = oB.writeValueAsString(msbDeviceRegistrationPayload).getBytes();

            //System.out.println("Registration Data : " + new String(msbRegistrationData));

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return msbRegistrationData;
    }

    private String getRegRequest(String serialNumber) {

        String finalResponse = "";
        String registrationRequest = "";
        JSONArray jsonDSubid = new JSONArray();
        DeviceData deviceData = new DeviceData();
        DeviceDataRequest deviceDataRequest = new DeviceDataRequest();
        com.mmc.DataObjects.DeviceInfo devInfo = new com.mmc.DataObjects.DeviceInfo();

        DeviceRegistration deviceRegistration = new DeviceRegistration();
        try{
            if (oB == null){
                oB = new ObjectMapper();
            }

            //digital ID
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            //String certStr = new String(libCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.DEVICE_CERT));

            X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();
            String certStr = java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());
            //Base64.encodeToString(certificate.getEncoded(), Base64.NO_WRAP);


            byte[] headerData = libCommonDeviceAPI.getHeaderfromCert(certStr);
            String enCodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(headerData);
            //Base64.encodeToString(headerData, Base64.NO_PADDING);

            String payLoad = ResponseGenHelper.getDigitalID(serialNumber, libCommonDeviceAPI.getISOTimeStamp());
            String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes());
            //Base64.encodeToString(payLoad.getBytes(), Base64.NO_PADDING);

            byte [] inputString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

            byte[] signature = "".getBytes();
            if (libCommonDeviceAPI.isValidDeviceforCertificate(DeviceMain.deviceMain.mainSerialNumber, DeviceMain.deviceKeystore)){
                signature = libCommonDeviceAPI.getSignature(DeviceMain.deviceKeystore, inputString);
            }
            String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
            //Base64.encodeToString(signature, Base64.NO_PADDING);


            jsonDSubid.put(0);
            devInfo.deviceSubId = "1";//jsonDSubid.toString();
            devInfo.certification = DeviceConstants.CERTIFICATIONLEVEL;
            devInfo.digitalId = enCodedHeader + "." + enCodedPayLoad + "." + enCodedSignature;
            devInfo.firmware = "1.0.9999";
            //devInfo.deviceExpiry = DeviceMain.deviceMain.gSnapshotResponse.keyExpiryTimeStamp;

            //yyyy-MM-dd'T'HH:mm:ss.SSSX
            /*DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, DeviceConstants.MDS_REGISTRATION_DEVICE_EXPIRY);
            String expiryForDevice = format.format(calendar.getTime());*/
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            String expiryForDevice = formatter.format(ZonedDateTime.now().plusYears(DeviceConstants.MDS_REGISTRATION_DEVICE_EXPIRY).withZoneSameInstant(ZoneOffset.UTC));
            devInfo.deviceExpiry = expiryForDevice;
            devInfo.timeStamp = libCommonDeviceAPI.getISOTimeStamp();

            byte[] deviceInfoPayload = oB.writeValueAsBytes(devInfo);
            String enCodedPayloadDeviceinfo = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(deviceInfoPayload); //Base64.encodeToString(deviceInfoPayload, Base64.NO_PADDING);
            byte[] inputStringDeviceinfo = (enCodedHeader + "." + enCodedPayloadDeviceinfo).getBytes();
            byte[] signatureDeviceInfo = "".getBytes();

            if (libCommonDeviceAPI.isValidDeviceforCertificate(DeviceMain.deviceMain.mainSerialNumber, DeviceMain.deviceKeystore)){
                signatureDeviceInfo = libCommonDeviceAPI.getSignature(DeviceMain.deviceKeystore, inputStringDeviceinfo);
            }

            String enCodedSignatureDeviceInfo = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signatureDeviceInfo); //Base64.encodeToString(signatureDeviceInfo, Base64.NO_PADDING);

            String deviceInfoData = enCodedHeader + "." + enCodedPayloadDeviceinfo + "." + enCodedSignatureDeviceInfo;


            deviceData.deviceId = DeviceMain.deviceMain.mainSerialNumber;
            deviceData.purpose = DeviceConstants.DeviceUsage.Registration.toString().toUpperCase();
            deviceData.deviceInfo = deviceInfoData;
            deviceData.foundationalTrustProviderId = "";


            byte[] deviceDataPayload = oB.writeValueAsBytes(deviceData);
            String enCodedPayloadDevicedata = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(deviceDataPayload); //Base64.encodeToString(deviceDataPayload, Base64.NO_PADDING);
            byte[] inputStringDevicedata = (enCodedHeader + "." + enCodedPayloadDevicedata).getBytes();

            byte[] signatureDevicedata = "".getBytes();
            if (libCommonDeviceAPI.isValidDeviceforCertificate(DeviceMain.deviceMain.mainSerialNumber, DeviceMain.deviceKeystore)){
                signatureDevicedata = libCommonDeviceAPI.getSignature(DeviceMain.deviceKeystore, inputStringDevicedata);
            }

            String enCodedSignatureDevicedata = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signatureDevicedata); //Base64.encodeToString(signatureDevicedata, Base64.NO_PADDING);

            String deviceDataString = enCodedHeader + "." + enCodedPayloadDevicedata + "." + enCodedSignatureDevicedata;

            deviceDataRequest.deviceData = deviceDataString;

            deviceRegistration.id = "io.mosip.deviceregister";

            //List <Map<String, Object>> requestList = new ArrayList<Map<String,Object>>();

            Map<String, String> requestMap = new HashMap<>();
            requestMap.put("deviceData", deviceDataString);

            deviceRegistration.requesttime = libCommonDeviceAPI.getISOTimeStamp();
            deviceRegistration.metadata = "";
            deviceRegistration.version = DeviceConstants.REGSERVER_VERSION;

            //registrationRequest = StringEscapeUtils.unescapeJava(new String(oB.writeValueAsBytes(deviceRegistration)));
            registrationRequest = StringEscapeUtils.unescapeJava(new String(oB.writeValueAsBytes(deviceRegistration)));

            finalResponse = (new JSONObject(registrationRequest).put("request", new JSONObject(requestMap))).toString();


            //Registration Request - JWT
			/*String enCodedPayloadRegRequestdata = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(finalResponse.getBytes());
			byte[] inputRegRequestdata = (enCodedHeader + "." + enCodedPayloadRegRequestdata).getBytes();

			byte[] signatureRegRequestdata = "".getBytes();
			if (libCommonDeviceAPI.isValidDeviceforCertificate(DeviceMain.deviceMain.mainSerialNumber, DeviceMain.deviceKeystore)){
				signatureRegRequestdata = libCommonDeviceAPI.getSignature(DeviceMain.deviceKeystore, inputRegRequestdata);
			}

			String enCodedSignatureRegRequestdata = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signatureRegRequestdata);
			finalResponse = enCodedHeader + "." + enCodedPayloadRegRequestdata + "." + enCodedSignatureRegRequestdata;*/

            //System.out.println(registrationJSON);
        }
        catch(Exception ex){
            finalResponse = "";
            ex.printStackTrace();
        }
        return finalResponse;
    }

    public String getmsbEncryptionCert() {
        CertificateFactory cf;
        byte[] headerBuffer = null;
        byte[] payloadBuffer = null;
        JSONObject data = new JSONObject();
        String responseBody = "";
        String msbEncryptionCertRequest = "";

        try {

            if (oB == null){
                oB = new ObjectMapper();
            }

            cf = CertificateFactory.getInstance("X.509");

            byte[] x509MOSIPCertByte = libCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.HOST_CERT);
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509MOSIPCertByte));

            //HeaderData
            headerBuffer = libCommonDeviceAPI.getHeader(certificate);

            //Payload
            payloadBuffer = getmsbEncryptCertData();

            String encodedData = getJwtEncodedData(headerBuffer, payloadBuffer, certificate);

            data.put("msbEncryptionCert", encodedData);

            msbEncryptionCertRequest = data.toString();

            responseBody = mmcServiceUtility.getDataFromMMC(msbEncryptionCertRequest);
        } catch (CertificateException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return responseBody;
    }

    private byte[] getmsbEncryptCertData() {
        byte[] msbEncryptionCertData = null;
        try
        {
            MSBEncryptionCert msbEncryptionCertPayload = new MSBEncryptionCert();
            msbEncryptionCertPayload.uniqueTransactionId = String.valueOf(getUniqueID());
            msbEncryptionCertPayload.timeStamp = libCommonDeviceAPI.getISOTimeStamp();
            msbEncryptionCertPayload.serialNumber = DeviceMain.deviceMain.mainSerialNumber;
            msbEncryptionCertPayload.deviceCodeUUID = DeviceMain.deviceMain.mainDeviceCode;//"50860dac-af49-4462-b1f4-e208da159eac";
            msbEncryptionCertPayload.modelID = DeviceConstants.DEVICEMODEL;
            msbEncryptionCertPayload.deviceProviderId = DeviceConstants.PROVIDERID;
            msbEncryptionCertPayload.bioType = "Face";
            msbEncryptionCertPayload.certificationLevel = "L0";
            msbEncryptionCertPayload.devicePurpose = DeviceConstants.PURPOSE;
            msbEncryptionCertPayload.env = DeviceConstants.ENVIRONMENT;
            msbEncryptionCertPayload.deviceServiceVersion = DeviceConstants.MDSVERSION;
            msbEncryptionCertPayload.specVersion = DeviceConstants.REGSERVER_VERSION;
            msbEncryptionCertPayload.hostos = DeviceConstants.OSTYPE;

            msbEncryptionCertData =  oB.writeValueAsString(msbEncryptionCertPayload).getBytes();

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }


        return msbEncryptionCertData;
    }
}
