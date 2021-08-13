package nprime.reg.sbi.mmc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmc.DataObjects.DeviceSnapshotResponse;
import com.mmc.DataObjects.MSBDeviceRegistrationResponse;
import com.mmc.DataObjects.MSBEncryptionCertResponse;
import com.mmc.DataObjects.MSBKeyRotationResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import nprime.reg.sbi.faceCaptureApi.DeviceIDDetails;
import nprime.reg.sbi.mds.DeviceMain;
import nprime.reg.sbi.utility.CommonDeviceAPI;
import nprime.reg.sbi.utility.DeviceConstants;
import nprime.reg.sbi.utility.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ManagementClient {
    static String retVal = "";
    MDServiceUtility rdServiceUtility;
    String MgmtSrvIpAddress = "";
    String serialNumber;
    CommonDeviceAPI deviceCommonDeviceAPI;
    MMCHelper mmcHelper;
    ObjectMapper oB;
    //String downloadedRDSVersion;

    private String webFolder;

    public ManagementClient(MDServiceUtility rdServiceUtility, String serialNumber)
    {
        //setTitle("Management Client");
        this.rdServiceUtility = rdServiceUtility;
        deviceCommonDeviceAPI = new CommonDeviceAPI();
        mmcHelper = new MMCHelper();
        this.serialNumber = serialNumber;

        try
        {

            if (null == oB){
                oB = new ObjectMapper();
            }
            //FileInputStream in = new FileInputStream(rdServiceUtility.getPropertiesPath());
            //Properties prop = new Properties();
            //prop.load(in);
            MgmtSrvIpAddress = DeviceConstants.mgmtSvrIpAddress; //prop.getProperty("mmcURL", "");

            webFolder = DeviceConstants.webFolder; //prop.getProperty("webFolder", "");

            //in.close();
        }
        catch(Exception ex)
        {

        }
    }

    @SuppressWarnings("unused")
    public boolean pingtoMgmtServer()
    {
        boolean status = false;
        String strServerUrl;
        Properties prop = new Properties();

        if (!DeviceMain.deviceMain.mgmtServerConnectivity){
            return true;
        }

        if (true == DeviceMain.deviceMain.serverStatus)
        {
            return true;
        }

        if(!MgmtSrvIpAddress.equals(""))
        {
            try
            {
                //String strUrl = "http://" + ipField.getText() + ":8080" + "/ManagementServer/ManagementServerServlet";
                String strMgmtServerUrl = MgmtSrvIpAddress + webFolder;

                //String tempURL = strMgmtServerUrl.toLowerCase();

                if (strMgmtServerUrl.contains("https")) {
                    Response httpResponse = null;
                    OkHttpClient httpclient = rdServiceUtility.getHttpClient();
                    Request request = new Request.Builder().addHeader("Client-Service","frontend-client").url(strMgmtServerUrl).build();
                    httpResponse = httpclient.newCall(request).execute();
                    int retCode = httpResponse.code();

                    if (200 == retCode) {
                        Logger.d(DeviceConstants.LOG_TAG, "Management Server is Reachable");
                        //rdServiceUtility.showNotification("Ping to Management Server is Successful");
                        status = true;
                    } else {
                        Logger.e(DeviceConstants.LOG_TAG, "Cannot Ping Server");
                        //InitActivity.msgUpdate("Cannot Ping Server");
                        //rdServiceUtility.showNotification("Cannot Ping Server");
                        status = false;
                    }
                } else {
                    URL obj = new URL(strMgmtServerUrl);
                    URLConnection con = (URLConnection) obj.openConnection();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    if (con != null) {
                        Logger.i(DeviceConstants.LOG_TAG, "Management Server is Reachable");
                        //rdServiceUtility.showNotification("Ping to Management Server is Successful");
                        status = true;
                    }
                }

            }
            catch(ConnectException ex)
            {
                ex.printStackTrace();
            }
            catch (Exception ex)
            {
                Logger.e(DeviceConstants.LOG_TAG, "Failed to Ping Management Server");
                ex.printStackTrace();
            }
        }
        else
        {
            try
            {
                Logger.d(DeviceConstants.LOG_TAG, "Failed to Ping Management Server");
                status = false;
            }
            catch(Exception ex)
            {
                //Logger.WriteLog(LOGTYPE.ERROR, "Face SBI :: " + ex.getMessage());
            }
        }
        return status;
    }

    public boolean fetchSnapshotDetails() {

        boolean isValidResponse = false;

        if (!DeviceMain.deviceMain.mgmtServerConnectivity){
            DeviceMain.deviceMain.gSnapshotResponse = new DeviceSnapshotResponse();
            DeviceMain.deviceMain.gSnapshotResponse.uniqueTransactionId = "0000-0000";
            DeviceMain.deviceMain.gSnapshotResponse.serialNumber = DeviceMain.deviceMain.mainSerialNumber;
            DeviceMain.deviceMain.gSnapshotResponse.deviceCodeUUID = DeviceMain.deviceMain.mainSerialNumber;
            DeviceMain.deviceMain.gSnapshotResponse.serverTimeStamp = deviceCommonDeviceAPI.getISOTimeStamp();
            DeviceMain.deviceMain.gSnapshotResponse.isWhiteListed = "true";
            DeviceMain.deviceMain.gSnapshotResponse.isDeviceValid = "true";
            DeviceMain.deviceMain.gSnapshotResponse.isDeviceRegistered = "true";
            DeviceMain.deviceMain.gSnapshotResponse.deviceServiceUpdateRequired = "false";
            DeviceMain.deviceMain.gSnapshotResponse.deviceServiceDownloadURL = "http://mosip.io/";
            DeviceMain.deviceMain.gSnapshotResponse.mandatoryKeyRotation = "false";
            DeviceMain.deviceMain.gSnapshotResponse.keyExpiryTimeStamp = "2026-02-26T08:53:33.217Z";
            DeviceMain.deviceMain.gSnapshotResponse.encryptionCertThumbprint = "";
            DeviceMain.deviceMain.gSnapshotResponse.snapshotValidity = "2026-02-26T08:53:33.217Z";
            DeviceMain.deviceMain.gSnapshotResponse.responseCode = "0";

            return true;
        }

        try{
            if (null == oB){
                oB = new ObjectMapper();
            }

            String responseBody = mmcHelper.getMsbDeviceSnapShot();

            isValidResponse = mmcHelper.evaluateMMCResponse(responseBody);

            if (isValidResponse){
                byte[] payLoadData = deviceCommonDeviceAPI.getPayloadBuffer(responseBody);
                //System.out.println(new String(payLoadData));
                deviceCommonDeviceAPI.WriteFiletoHost(DeviceIDDetails.SNAPSHOT_RESPONSE_INFO, responseBody.getBytes());

                DeviceMain.deviceMain.gSnapshotResponse = (DeviceSnapshotResponse) (oB.readValue(payLoadData, DeviceSnapshotResponse.class));
                Logger.d(DeviceConstants.LOG_TAG, "Snapshot details received from client successfully");
            }
            else{
                DeviceMain.deviceMain.gSnapshotResponse = null;
                Logger.d(DeviceConstants.LOG_TAG, "Failed to fetch Snapshot from client");
                Logger.d(DeviceConstants.LOG_TAG, "Response from Server: " + responseBody);
            }
        }
        catch(Exception ex){
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Error occurred while fetching snapshot details");
        }

        return isValidResponse;
    }

    public boolean readSnapshotDetails() {
        boolean status = false;

        if (!DeviceMain.deviceMain.mgmtServerConnectivity){
            DeviceMain.deviceMain.gSnapshotResponse = new DeviceSnapshotResponse();
            DeviceMain.deviceMain.gSnapshotResponse.uniqueTransactionId = "0000-0000";
            DeviceMain.deviceMain.gSnapshotResponse.serialNumber = DeviceMain.deviceMain.mainSerialNumber;
            DeviceMain.deviceMain.gSnapshotResponse.deviceCodeUUID = DeviceMain.deviceMain.mainSerialNumber;
            DeviceMain.deviceMain.gSnapshotResponse.serverTimeStamp = deviceCommonDeviceAPI.getISOTimeStamp();
            DeviceMain.deviceMain.gSnapshotResponse.isWhiteListed = "true";
            DeviceMain.deviceMain.gSnapshotResponse.isDeviceValid = "true";
            DeviceMain.deviceMain.gSnapshotResponse.isDeviceRegistered = "true";
            DeviceMain.deviceMain.gSnapshotResponse.deviceServiceUpdateRequired = "false";
            DeviceMain.deviceMain.gSnapshotResponse.deviceServiceDownloadURL = "http://mosip.io/";
            DeviceMain.deviceMain.gSnapshotResponse.mandatoryKeyRotation = "false";
            DeviceMain.deviceMain.gSnapshotResponse.keyExpiryTimeStamp = "2026-02-26T08:53:33.217Z";
            DeviceMain.deviceMain.gSnapshotResponse.encryptionCertThumbprint = "";
            DeviceMain.deviceMain.gSnapshotResponse.snapshotValidity = "2026-02-26T08:53:33.217Z";
            DeviceMain.deviceMain.gSnapshotResponse.responseCode = "0";

            return true;
        }

        try{
            if (null == oB){
                oB = new ObjectMapper();
            }

            byte[] responseBody = deviceCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.SNAPSHOT_RESPONSE_INFO);

            if (0 == responseBody.length){
                return false;
            }

            byte[] payLoadData = deviceCommonDeviceAPI.getPayloadBuffer(new String(responseBody));

            boolean isValidResponse = mmcHelper.evaluateMMCResponse(new String(responseBody));

            if (isValidResponse){
                //Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: " + new String(payLoadData));
                //payLoadData= new String(java.util.Base64.getUrlDecoder().decode(payLoadData.substring(1, payLoadData.length()-1)));
                DeviceMain.deviceMain.gSnapshotResponse = (DeviceSnapshotResponse) (oB.readValue(payLoadData, DeviceSnapshotResponse.class));
                /*ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                ObjectReader objectReader = objectMapper.reader(DeviceSnapshotResponse.class);
                DeviceMain.deviceMain.gSnapshotResponse = objectReader.readValue(payLoadData);*/
                /*try {
                    Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: unique transaction id" + DeviceMain.deviceMain.gSnapshotResponse.uniqueTransactionId);
                }catch (Exception e){
                    Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: unique transaction id fail" + e.getMessage());
                }*/

                status = true;
                Logger.d(DeviceConstants.LOG_TAG, "Snapshot details read successfully");
            }
            else{
                Logger.d(DeviceConstants.LOG_TAG, "Failed to read Snapshot");
            }

        }
        catch(Exception ex){
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Error occurred while reading snapshot details");
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: " + ex.getMessage());

            DeviceMain.deviceMain.gSnapshotResponse = null;
        }

        return status;
    }

    public boolean initiateKeyRotation() {
        boolean status = false;
        String responseBody = "";
        boolean isValidResponse = false;

        /*if (!DeviceMain.deviceMain.mgmtServerConnectivity){
            return DeviceMain.deviceKeystore.installpfx();
        }*/

        try{
            ////msbKeyRotation
            if (null == oB){
                oB = new ObjectMapper();
            }

            responseBody = mmcHelper.getmsbKeyRotation();

            isValidResponse = mmcHelper.evaluateMMCResponse(responseBody);
            Logger.d(DeviceConstants.LOG_TAG, "Signature Verification successful");

            if (isValidResponse){
                byte[] payLoadData = DeviceMain.deviceMain.deviceCommonDeviceAPI.getPayloadBuffer(responseBody);
                DeviceMain.deviceMain.gmsbKeyRotationResp = oB.readValue(payLoadData, MSBKeyRotationResponse.class);

                deviceCommonDeviceAPI.WriteFiletoHost(DeviceIDDetails.KEYROTATION_RESPONSE_INFO, responseBody.getBytes());

                status = true;//DeviceMain.deviceKeystore.updateKeystore(DeviceMain.deviceMain.gmsbKeyRotationResp.signedDeviceCertificate);
                Logger.i(DeviceConstants.LOG_TAG, "Key Rotation response received from server successfully");

            }
            else{
                DeviceMain.deviceMain.gmsbKeyRotationResp = null;
                Logger.e(DeviceConstants.LOG_TAG, "Failed to fetch key rotation response");
            }
            ////msbKeyRotation
        }
        catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        catch(Exception ex){

        }
        return status;
    }

    public boolean registerDevice() {
        boolean bStatus = false;

        if (!DeviceMain.deviceMain.mgmtServerConnectivity){
            return true;
        }

        try
        {
            if (true == DeviceMain.deviceMain.serverStatus)
            {
                if (!DeviceMain.deviceMain.isRegistered){
                    //Register Device
                    int returnValue = 0;//deviceCommonDeviceAPI.cleanDevice();

                    if (0 != returnValue)
                    {
                        Logger.e(DeviceConstants.LOG_TAG, "Registration check failure, Certificates cleaned...");
                    }

                    String responseBody = mmcHelper.getmsbDeviceRegistration();
                    boolean isValidResponse = mmcHelper.evaluateMMCResponse(responseBody);
                    if (isValidResponse){
                        byte[] payLoadData = deviceCommonDeviceAPI.getPayloadBuffer(responseBody);
                        DeviceMain.deviceMain.gmsbDeviceRegistrationResp = oB.readValue(payLoadData, MSBDeviceRegistrationResponse.class);

                        int retCode = Integer.valueOf(DeviceMain.deviceMain.gmsbDeviceRegistrationResp.responseCode);
                        if (0 == retCode){
                            DeviceMain.deviceMain.mainDeviceCode = DeviceMain.deviceMain.gmsbDeviceRegistrationResp.serialNumber; //DeviceMain.deviceMain.gmsbDeviceRegistrationResp.deviceCodeUUID;
                            //rdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE,  DeviceMain.errTable.getMessage("MDSRVMSG_0004"));
                            DeviceMain.deviceMain.sendInitMessage(DeviceMain.HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + DeviceMain.errTable.getMessage("MDSRVMSG_0004"));
                            Logger.i(DeviceConstants.LOG_TAG, DeviceMain.errTable.getMessage("MDSRVMSG_0004"));
                            bStatus = true;
                        }
                        else
                        {
                            //rdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, DeviceMain.errTable.getMessage("MDSRVMSG_0003"));
                            DeviceMain.deviceMain.sendInitMessage(DeviceMain.HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + DeviceMain.errTable.getMessage("MDSRVMSG_0003"));
                            Logger.i(DeviceConstants.LOG_TAG, DeviceMain.errTable.getMessage("MDSRVMSG_0003"));
                        }
                    }

                }
            }

        }
        catch(Exception ex)
        {

        }
        return bStatus;
    }

    public boolean readEncryptionCert() {
        boolean status = false;

        if (!DeviceMain.deviceMain.mgmtServerConnectivity){
            return true;
        }

        try{
            if (null == oB){
                oB = new ObjectMapper();
            }

            byte[] responseBody = deviceCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.ENCRYPTIONCERT_RESPONSE_INFO);

            if (0 == responseBody.length){
                return false;
            }

            byte[] payLoadData = deviceCommonDeviceAPI.getPayloadBuffer(new String(responseBody));

            boolean isValidResponse = mmcHelper.evaluateMMCResponse(new String(responseBody));

            if (isValidResponse){
                //payLoadData= new String(java.util.Base64.getUrlDecoder().decode(payLoadData.substring(1, payLoadData.length()-1)));
                DeviceMain.deviceMain.gmsbEncryptionCertResponse = (MSBEncryptionCertResponse) (oB.readValue(payLoadData, MSBEncryptionCertResponse.class));
                status = true;
                Logger.i(DeviceConstants.LOG_TAG, "Encryption details read successfully");
            }
            else{
                Logger.e(DeviceConstants.LOG_TAG, "Failed to read Encryption");
            }

        }
        catch(Exception ex){
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Error occurred while reading encryption cert details");
            DeviceMain.deviceMain.gmsbEncryptionCertResponse = null;
        }

        return status;
    }

    public boolean fetchEncrytionCert() {
        String responseBody = "";
        boolean isValidResponse = false;

        /*if (!DeviceMain.deviceMain.mgmtServerConnectivity){
            return DeviceMain.deviceKeystore.installpfx();
        }*/

        try{
            ////msbEncryptionCert
            if (null == oB){
                oB = new ObjectMapper();
            }

            responseBody = mmcHelper.getmsbEncryptionCert();

            isValidResponse = mmcHelper.evaluateMMCResponse(responseBody);
            Logger.d(DeviceConstants.LOG_TAG, "Signature Verification successful");

            if (isValidResponse){
                byte[] payLoadData = DeviceMain.deviceMain.deviceCommonDeviceAPI.getPayloadBuffer(responseBody);
                DeviceMain.deviceMain.gmsbEncryptionCertResponse = oB.readValue(payLoadData, MSBEncryptionCertResponse.class);

                deviceCommonDeviceAPI.WriteFiletoHost(DeviceIDDetails.ENCRYPTIONCERT_RESPONSE_INFO, responseBody.getBytes());

                Logger.i(DeviceConstants.LOG_TAG, "Encryption cert response received from server successfully");
            }
            else{
                DeviceMain.deviceMain.gmsbEncryptionCertResponse = null;
                Logger.e(DeviceConstants.LOG_TAG, "Failed to fetch encryption cert response");
            }
            ////msbEncryptionCert
        }
        catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        return isValidResponse;
    }
}
