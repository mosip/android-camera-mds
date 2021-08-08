package nprime.reg.sbi.mds;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

import com.mmc.DataObjects.DeviceSnapshotResponse;
import com.mmc.DataObjects.MSBDeviceRegistrationResponse;
import com.mmc.DataObjects.MSBEncryptionCertResponse;
import com.mmc.DataObjects.MSBKeyRotationResponse;

import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import nprime.reg.sbi.face.MainActivity;
import nprime.reg.sbi.face.R;
import nprime.reg.sbi.mmc.MDServiceUtility;
import nprime.reg.sbi.mmc.MMCHelper;
import nprime.reg.sbi.mmc.ManagementClient;
import nprime.reg.sbi.scanner.ResponseGenerator.ResponseGenHelper;
import nprime.reg.sbi.secureLib.DeviceKeystore;
import nprime.reg.sbi.utility.CommonDeviceAPI;
import nprime.reg.sbi.utility.DeviceConstants;
import nprime.reg.sbi.utility.ErrorList;
import nprime.reg.sbi.utility.Logger;

public class DeviceMain extends AppCompatActivity {

    public static final int HANDLER_STATUS = 1;
    public static final int HANDLER_TOAST = 2;

    public static MMCHelper mmcHelper;
    public static MDServiceUtility mdServiceUtility;
    public static DeviceMain deviceMain;
    public static DeviceKeystore deviceKeystore;
    public static ManagementClient managementClient = null;
    public static Context context;
    public static ErrorList errTable;

    public boolean mgmtServerConnectivity = true;
    public boolean serverStatus = false;
    public DeviceSnapshotResponse gSnapshotResponse;
    public String mainSerialNumber;
    public String mainDeviceCode;
    public MSBKeyRotationResponse gmsbKeyRotationResp;
    public CommonDeviceAPI deviceCommonDeviceAPI;
    public static long serverTimeStampinMillis = 0;
    public static boolean isSchedulerStarted = false;
    public boolean isRegistered = false;

    public static boolean isDeviceWhitelist = true;
    public static boolean isDeviceValidMDLicense = true;
    public static boolean isKeyRotationRequired = false;
    public static boolean isMDserviceStarted = false;
    public int advanceKeyRotationDays = 5;
    public MSBDeviceRegistrationResponse gmsbDeviceRegistrationResp;
    public MSBEncryptionCertResponse gmsbEncryptionCertResponse;

    TextView tvInitStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_main);

        tvInitStatus = findViewById(R.id.tv_init_status);

        context = this;
        //deviceHelper = new DeviceHelper();
        mmcHelper = new MMCHelper();
        mdServiceUtility = new MDServiceUtility();
        //deviceMain = new DeviceMain();
        deviceKeystore = new DeviceKeystore();
        deviceMain.deviceCommonDeviceAPI = new CommonDeviceAPI();
        errTable = new ErrorList();
        new Thread(){
            @Override
            public void run() {
                MainActivity.initSuccessful = init();
                if(MainActivity.initSuccessful) {
                    MainActivity.sharedPreferences.edit().putLong("LastInitTimeMills", new Date().getTime()).apply();
                    setResult(Activity.RESULT_OK);
                }else {
                    setResult(Activity.RESULT_CANCELED);
                }
                ((Activity)context).finish();
            }
        }.start();
    }

    public void sendInitMessage(int what, Object obj) {
        Message message = new Message();
        message.what = what;
        message.obj = obj;
        handler.sendMessage(message);
    }

    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case HANDLER_STATUS :
                    String text = (String)msg.obj;
                    tvInitStatus.setText(text);
                    break;
                case HANDLER_TOAST :
                    String toastText = (String)msg.obj;
                    Toast.makeText(DeviceMain.this, toastText, Toast.LENGTH_SHORT).show();
                    break;
                default:
            }

        }
    };
    public boolean init(){
        deviceMain.mainSerialNumber = deviceMain.deviceCommonDeviceAPI.getSerialNumber();

        deviceKeystore.initStore();
        if (null == managementClient){
            managementClient = new ManagementClient(mdServiceUtility, mainSerialNumber);
        }

        deviceMain.serverStatus = managementClient.pingtoMgmtServer();
        if(true == deviceMain.serverStatus){
            if (DeviceMain.deviceMain.mgmtServerConnectivity){
                //mdServiceUtility.showNotification(LOGTYPE.DEBUG, DeviceConstants.MSPMDSERVICE, "Ping to Management Client is Successful");
                sendInitMessage(HANDLER_STATUS, "Ping to Management Client is Successful");
            }
        }
        else
        {
            if (DeviceMain.deviceMain.mgmtServerConnectivity){
                //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, "Unable to reach Management Client");
                sendInitMessage(HANDLER_STATUS, "Unable to reach Management Client");
                Logger.w(DeviceConstants.LOG_TAG, "Unable to reach Management Client");
            }
        }

        boolean readStatus = managementClient.readSnapshotDetails();
        if(true == readStatus){
            if (DeviceMain.deviceMain.mgmtServerConnectivity){
                //mdServiceUtility.showNotification(LOGTYPE.DEBUG, DeviceConstants.MSPMDSERVICE, "Snapshot details read successfully");
                sendInitMessage(HANDLER_STATUS, "Snapshot details read successfully");
            }

            if(null == deviceMain.gSnapshotResponse || !deviceMain.mainSerialNumber.equals(deviceMain.gSnapshotResponse.serialNumber)) {
                Logger.w(DeviceConstants.LOG_TAG, "New Device connected.");
                if (DeviceMain.deviceMain.mgmtServerConnectivity){
                    //FileUtils.cleanDirectory(new File(deviceMain.appFolderPath));
                    deviceKeystore.removeKeys();
                }
                gmsbKeyRotationResp = null;
            }
            else if (null != deviceMain.gSnapshotResponse){
                deviceMain.mainDeviceCode = deviceMain.gSnapshotResponse.serialNumber; //gSnapshotResponse.deviceCodeUUID;
            }
        }

        //GetSnapshotDetails
        deviceMain.serverStatus = managementClient.fetchSnapshotDetails();
        if(true == deviceMain.serverStatus){
            if (DeviceMain.deviceMain.mgmtServerConnectivity){
                //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, "Snapshot details received successfully from server");
                sendInitMessage(HANDLER_STATUS, "Snapshot details received successfully from server");
            }

            int retCode = Integer.valueOf(DeviceMain.deviceMain.gSnapshotResponse.responseCode);
            if (retCode != 0){
                //deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face Auth SBI", retCode + " : " + DeviceMain.deviceMain.gSnapshotResponse.description);
                sendInitMessage(HANDLER_TOAST, retCode + " : " + DeviceMain.deviceMain.gSnapshotResponse.description);
                return false;
            }

            //System.out.println("From Snapshot	   : " + gSnapshotResponse.serverTimeStamp);
            //Checking Timestamp
            serverTimeStampinMillis = deviceMain.deviceCommonDeviceAPI.getMillisfromISO(DeviceMain.deviceMain.gSnapshotResponse.serverTimeStamp);

            //prompt MDS Download
            if (true == Boolean.valueOf(DeviceMain.deviceMain.gSnapshotResponse.deviceServiceUpdateRequired)){
                //deviceCommonDeviceAPI.showMessagewithLink("Face Auth SBI", "Please install latest MDS, link : ", gSnapshotResponse.deviceServiceDownloadURL);
                sendInitMessage(HANDLER_TOAST, "Face Auth SBI - Please install latest MDS,\n link : " + deviceMain.gSnapshotResponse.deviceServiceDownloadURL);
            }

            String currentTime = deviceMain.deviceCommonDeviceAPI.getISOTimeStamp();
            //System.out.println("Current Timestamp  : " + currentTime);
            Logger.i(DeviceConstants.LOG_TAG, "Current Timestamp : " + currentTime);

            long currentTimeinmillis = deviceMain.deviceCommonDeviceAPI.getMillisfromISO(currentTime);

            long timediff = Math.abs(serverTimeStampinMillis - currentTimeinmillis);

            if (timediff > DeviceConstants.MDS_TIMESYNC_TOLERANCE){
                Logger.e(DeviceConstants.LOG_TAG, "System time(" + currentTime +
                        ") is not in sync with server time(" + DeviceMain.deviceMain.gSnapshotResponse.serverTimeStamp + ")");
                if (DeviceMain.deviceMain.mgmtServerConnectivity){
                    //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, "Time Sync Failed");
                    sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + "Time Sync Failed");
                }
                if (DeviceMain.deviceMain.mgmtServerConnectivity){
                    //FileUtils.cleanDirectory(new File(deviceMain.appFolderPath));
                    deviceKeystore.removeKeys();
                }
                DeviceMain.deviceMain.gmsbKeyRotationResp = null;
                //deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face Auth SBI", "Time Sync Failed, Please correct system time and restart Face Auth SBI");
                sendInitMessage(HANDLER_TOAST, "Time Sync Failed, Please correct system time and restart Face Auth SBI");
                return false;
            }
        }
        else
        {
            if (DeviceMain.deviceMain.mgmtServerConnectivity){
                Logger.i(DeviceConstants.LOG_TAG, "Failed to fetch Snapshot details");
                //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, "Failed to fetch Snapshot details");
                sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + "Failed to fetch Snapshot details");
            }

            //readSnapshotDetails
            readStatus = managementClient.readSnapshotDetails();
            if(true == readStatus){
                if (DeviceMain.deviceMain.mgmtServerConnectivity){
                    //mdServiceUtility.showNotification(LOGTYPE.DEBUG, DeviceConstants.MSPMDSERVICE, "Snapshot details read successfully");
                    sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + "Snapshot details read successfully");
                }
            }
            else
            {
                if (DeviceMain.deviceMain.mgmtServerConnectivity){
                    //mdServiceUtility.showNotification(LOGTYPE.DEBUG, DeviceConstants.MSPMDSERVICE, "Failed to read snapshot from filesystem");
                    //deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face Auth SBI", "Snapshot not available, Please connect to internet and restart Face Auth SBI");
                    sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + "Failed to read snapshot from filesystem");
                    sendInitMessage(HANDLER_TOAST, "Face Auth SBI" + "Snapshot not available, Please connect to internet and restart Face Auth SBI");
                }
                return false;
            }
        }
        if (false == mdServiceUtility.checkSnapshotValidity())
        {
            return false;
        }

        //Time Sync Scheduler
        if (false == isSchedulerStarted){
            boolean schedulerStatus = mdServiceUtility.snapshotValidityScheduler();
            if (schedulerStatus){
                isSchedulerStarted = true;
                //Logger.WriteLog(LOGTYPE.INFO, "Snapshot validity scheduler started");
            }
            else{
                isSchedulerStarted = false;
                //Logger.WriteLog(LOGTYPE.ERROR, "Snapshot validity scheduler failed to start");
                return false;
            }
        }

        //updating current uuid
        if (null != deviceMain.gSnapshotResponse){
            deviceMain.mainDeviceCode = deviceMain.gSnapshotResponse.serialNumber; //gSnapshotResponse.deviceCodeUUID;
            isRegistered = Boolean.valueOf(deviceMain.gSnapshotResponse.isDeviceRegistered);
            isDeviceWhitelist = Boolean.valueOf(deviceMain.gSnapshotResponse.isWhiteListed);
            isDeviceValidMDLicense = Boolean.valueOf(deviceMain.gSnapshotResponse.isDeviceValid);
        }

        if ((!isDeviceValidMDLicense)){
            //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, errTable.getMessage("MDSRVMSG_0020"));
            sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + errTable.getMessage("MDSRVMSG_0020"));
            return false;
        }

        if ((!isDeviceWhitelist)){
            //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, errTable.getMessage("MDSRVMSG_0013"));
            sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + errTable.getMessage("MDSRVMSG_0013"));
            //TBD : Dialog BOX with Serial number
            return false;
        }

        //TBD : Compute Key Validity in days and update deviceKeyValidityDays
        if (!isRegistered){
            isRegistered = managementClient.registerDevice();
            if (isRegistered){
                deviceMain.serverStatus = managementClient.fetchSnapshotDetails();
                if(deviceMain.serverStatus){
                    Logger.i(DeviceConstants.LOG_TAG, "Device Snapshot updated");
                }

                mainDeviceCode = deviceMain.gmsbDeviceRegistrationResp.serialNumber;//deviceMain.gmsbDeviceRegistrationResp.deviceCodeUUID;

                deviceMain.gSnapshotResponse.isDeviceRegistered = String.valueOf(isRegistered);
                deviceMain.gSnapshotResponse.deviceCodeUUID = deviceMain.gmsbDeviceRegistrationResp.serialNumber;//deviceMain.gmsbDeviceRegistrationResp.deviceCodeUUID;
            }
        }


        //get encryption certificate only if thumbprint is not matching
        //Fetch encryption cert
        /*if (DeviceMain.deviceMain.mgmtServerConnectivity){
            readStatus = managementClient.readEncryptionCert();
            if(true == readStatus){
                if (DeviceMain.deviceMain.mgmtServerConnectivity){
                    Logger.i(DeviceConstants.LOG_TAG, "Encryption cert read successfully");
                    //mdServiceUtility.showNotification(LOGTYPE.DEBUG, DeviceConstants.MSPMDSERVICE, "Encryption cert read successfully");
                    sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + "Encryption cert read successfully");
                }
            }

            if (null == DeviceMain.deviceMain.gmsbEncryptionCertResponse ||
                    null == DeviceMain.deviceMain.gmsbEncryptionCertResponse.encryptionCertData ||
                    DeviceMain.deviceMain.gmsbEncryptionCertResponse.encryptionCertData.isEmpty()){
                deviceMain.serverStatus = managementClient.fetchEncrytionCert();
                if(true == deviceMain.serverStatus){
                    if (DeviceMain.deviceMain.mgmtServerConnectivity){
                        Logger.i(DeviceConstants.LOG_TAG, "Encryption cert received successfully from server");
                        //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, "Encryption cert received successfully from server");
                        sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + "Encryption cert received successfully from server");
                    }
                }
            }
            else{
                try {
                    byte []encryptionCert = org.apache.commons.codec.binary.Base64.decodeBase64(DeviceMain.deviceMain.gmsbEncryptionCertResponse.encryptionCertData.getBytes());
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509Certificate x509Cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(encryptionCert));
                    String encryptThumbprint = deviceMain.deviceCommonDeviceAPI.getThumbprint(x509Cert);

                    if (false == DeviceMain.deviceMain.gSnapshotResponse.encryptionCertThumbprint.equalsIgnoreCase(encryptThumbprint)){
                        deviceMain.serverStatus = managementClient.fetchEncrytionCert();
                        if(true == deviceMain.serverStatus){
                            if (DeviceMain.deviceMain.mgmtServerConnectivity){
                                Logger.i(DeviceConstants.LOG_TAG, "Encryption cert received successfully from server");
                                //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, "Encryption cert received successfully from server");
                                sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + "Encryption cert received successfully from server");
                            }
                        }
                    }
                }catch (CertificateException e){
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        }*/
        //Fetch encryption cert

        boolean bValidCert = false;

        bValidCert = mdServiceUtility.CheckValidCertificate();
        if (false == bValidCert) {
            //Remove existing Keys
            if (DeviceMain.deviceMain.mgmtServerConnectivity){
                deviceKeystore.removeKeys();
            }
            gmsbKeyRotationResp = null;
            //Generate keypair at local keystore and retrieve Device Public Key
            deviceMain.serverStatus = managementClient.initiateKeyRotation();
            if(true == deviceMain.serverStatus){

                Logger.i(DeviceConstants.LOG_TAG, "Key is installed");
                managementClient.fetchSnapshotDetails();

                bValidCert = mdServiceUtility.CheckValidCertificate();

                if (false == bValidCert){
                    Logger.i(DeviceConstants.LOG_TAG, "Key Expired or does not exist");
                    return false;
                }
                //String serialNumber = DeviceMain.deviceMain.deviceCommonDeviceAPI.getSerialNumber();
                //deviceCommonDeviceAPI.WriteFiletoHost((byte)DeviceIDDetails.RESERVED_MOSIP_CERT_1, serialNumber.getBytes());

                if (DeviceMain.deviceMain.mgmtServerConnectivity){
                    //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, errTable.getMessage("MDSRVMSG_0014"));
                    sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + errTable.getMessage("MDSRVMSG_0014"));
                    Logger.i(DeviceConstants.LOG_TAG, "Key Rotation completed");
                }
            }
            else {
                Logger.i(DeviceConstants.LOG_TAG, "Key installation failed");
                if (DeviceMain.deviceMain.mgmtServerConnectivity){
                    //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, "Key Expired and Rotation Failed");
                    sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + "Key Expired and Rotation Failed");
                    Logger.e(DeviceConstants.LOG_TAG, "Key expired and Rotation Failed");
                }
                return false;
            }
        }
        else {
            if (!mdServiceUtility.checkAdvanceKeyRotation()){
                deviceMain.serverStatus = managementClient.initiateKeyRotation();
                if(true == deviceMain.serverStatus){
                    Logger.i(DeviceConstants.LOG_TAG, "Key is installed");
                    deviceMain.serverStatus = managementClient.fetchSnapshotDetails();

                    bValidCert = mdServiceUtility.CheckValidCertificate();

                    if (!bValidCert){
                        Logger.i(DeviceConstants.LOG_TAG, "Key Expired");
                        return false;
                    }
                    //String serialNumber = DeviceMain.deviceMain.deviceCommonDeviceAPI.getSerialNumber();
                    //deviceCommonDeviceAPI.WriteFiletoHost((byte)DeviceIDDetails.RESERVED_MOSIP_CERT_1, serialNumber.getBytes());

                    if (DeviceMain.deviceMain.mgmtServerConnectivity){
                        //mdServiceUtility.showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, errTable.getMessage("MDSRVMSG_0014"));
                        sendInitMessage(HANDLER_STATUS, DeviceConstants.MSPMDSERVICE + errTable.getMessage("MDSRVMSG_0014"));
                        Logger.i(DeviceConstants.LOG_TAG, "Key Rotation completed...");
                    }
                }
            }
        }



        //TBD : Compute Key Validity in days and update deviceKeyValidityDays
       /* if (!isRegistered){
            isRegistered = managementClient.registerDevice();
            if (isRegistered){
                deviceMain.serverStatus = managementClient.fetchSnapshotDetails();
                if(true == deviceMain.serverStatus){
                    Logger.i(DeviceConstants.LOG_TAG, "Device Snapshot updated");
                }

                deviceMain.mainDeviceCode = gSnapshotResponse.serialNumber; //deviceMain.gmsbDeviceRegistrationResp.deviceCodeUUID;

                deviceMain.gSnapshotResponse.isDeviceRegistered = String.valueOf(isRegistered);
                deviceMain.gSnapshotResponse.deviceCodeUUID = deviceMain.gmsbDeviceRegistrationResp.deviceCodeUUID;

                //deviceCommonDeviceAPI.WriteFiletoHost((byte)DeviceIDDetails.RESERVED_MOSIP_CERT_2, mainDeviceCode.getBytes());

                //String serialNumber = DeviceMain.deviceMain.deviceCommonDeviceAPI.getSerialNumber();
                //deviceCommonDeviceAPI.WriteFiletoHost((byte)DeviceIDDetails.RESERVED_MOSIP_CERT_1, serialNumber.getBytes());
            }
        }*/
        return true;
    }

    public String getDeviceDriverInfo(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType) {
        return ResponseGenHelper.getDeviceDriverInfo(currentStatus, szTimeStamp, requestType);
    }

    public String discoverDevice(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType) {
        return ResponseGenHelper.getDeviceDiscovery(currentStatus, szTimeStamp, requestType);
    }
}