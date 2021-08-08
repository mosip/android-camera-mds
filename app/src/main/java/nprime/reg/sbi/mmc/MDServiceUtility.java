package nprime.reg.sbi.mmc;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import nprime.reg.sbi.face.MainActivity;
import nprime.reg.sbi.face.R;
import nprime.reg.sbi.faceCaptureApi.KeysNotFoundException;
import nprime.reg.sbi.mds.DeviceMain;
import nprime.reg.sbi.utility.CommonDeviceAPI;
import nprime.reg.sbi.utility.DeviceConstants;
import nprime.reg.sbi.utility.Logger;
import okhttp3.OkHttpClient;

public class MDServiceUtility {

    CommonDeviceAPI devCommonDeviceAPI;
    public long lastSnapshotValidityCheck = 0L;

    public MDServiceUtility() {
        devCommonDeviceAPI = new CommonDeviceAPI();
    }

    public OkHttpClient getHttpClient() {
        OkHttpClient testClient = null;
        try {
            /*javax.security.cert.X509Certificate cert = javax.security.cert.X509Certificate.getInstance(DeviceConstants.sslCert.getBytes());
            String shaValue = Base64.encodeToString(sha256Digest(cert.getPublicKey().getEncoded()), 0);

            CertificatePinner certificatePinner = new CertificatePinner.Builder()
                    .add("mmc.nprime.in", "sha256/" + shaValue)
                    .build();*/
            //javax.security.cert.X509Certificate x509Certificate = javax.security.cert.X509Certificate.getInstance(DeviceConstants.sslCert.getBytes());
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            InputStream is = DeviceMain.context.getResources().openRawResource(R.raw.sslcert);//new ByteArrayInputStream(x509Certificate.getEncoded());
            Certificate cert = factory.generateCertificate(is);

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", cert);

            // creating a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // creating an SSLSocketFactory that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            /*KeyStore keyStore = SslUtils.getKeyStore(context, fileName);
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);*/
            //sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
            testClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory())
                    //.certificatePinner(certificatePinner)
                    .build();
        }catch (Exception e){
            e.printStackTrace();
        }
        return testClient;
    }
    private byte[] sha256Digest(byte[] data) {
        MessageDigest mdSha256 = null;
        try {
            mdSha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
            //System.out.println("Error initializing SHA1 message digest");
        }
        mdSha256.update(data);
        byte[] sha256hash = mdSha256.digest();
        return sha256hash;
    }

    public boolean checkSnapshotValidity() {
        boolean validityStatus = true;
        long snapshotValidity = devCommonDeviceAPI.getMillisfromISO(
                DeviceMain.deviceMain.gSnapshotResponse.snapshotValidity);

        long currentTimeinmillis = devCommonDeviceAPI.getMillisfromISO(
                DeviceMain.deviceMain.deviceCommonDeviceAPI.getISOTimeStamp());

        if (snapshotValidity > 0 && currentTimeinmillis > 0){
            long timediff = snapshotValidity - currentTimeinmillis;

            if (timediff <= 0){//Already expired case
                Logger.w(DeviceConstants.LOG_TAG, "Timesync validity is expired (" + DeviceMain.deviceMain.gSnapshotResponse.snapshotValidity
                        + "), Connect to Internet and Restart Face Auth SBI" );
                //devCommonDeviceAPI.showAlwaysOnTopMessage("Face Auth SBI", "Timesync validity is expired, Connect to Internet and Restart Face Auth SBI");
                //new File(DeviceMain.deviceMain.appFolderPath + "\\snapShot.info").delete();
                DeviceMain.deviceMain.gSnapshotResponse = null;
                validityStatus = false;
            }
        }
        return validityStatus;
    }

    public boolean snapshotValidityScheduler() {
        boolean status = false;
        try{
            Timer time = new Timer();
            status = true;
            TimeSyncScheduler tSyncScheduler = new TimeSyncScheduler();
            time.scheduleAtFixedRate(tSyncScheduler, 0, DeviceConstants.MDS_SCHEDULER_INTERVAL);
        }
        catch(Exception ex){
            status = false;
        }
        return status;
    }

    public boolean CheckValidCertificate() {

        boolean bStatus = false;
        try {
            if (true == Boolean.valueOf(DeviceMain.deviceMain.gSnapshotResponse.mandatoryKeyRotation)){
                Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Mandatory key rotation enabled, please initiate key rotation");
                return false;
            }

            if (false == isValidDeviceforCertificate()){
                Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Device serial does not match with device certificate");
                return false;
            }


            X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();

            if (null != certificate)
            {
                Date date = new Date();
                certificate.checkValidity(date);
                bStatus = true;
                Logger.i(DeviceConstants.LOG_TAG, "Face Auth SBI :: Device has valid certificate!!!");
            }
        }
        catch(CertificateExpiredException | CertificateNotYetValidException e){
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Certificate is expired or will be expired shortly");
            bStatus = false;
        }
        catch(Exception e){
            bStatus = false;
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Error occurred while checking certficate");
        }
        return bStatus;
    }

    private boolean isValidDeviceforCertificate() {
        boolean status = false;
        String serialNo = DeviceMain.deviceMain.mainSerialNumber;
        String commonName = "";

        if (!DeviceMain.deviceMain.mgmtServerConnectivity){
            return true;
        }

        try{
            X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();

            if (null == certificate){
                status = false;
            }
            else{
                commonName = getCommonName(certificate);

                if (commonName.startsWith(serialNo)){
                    status = true;
                }
                else{
                    status = false;
                }
            }
        }
        catch(KeysNotFoundException ex){
            status = false;
            Logger.w(DeviceConstants.LOG_TAG, "Face Auth SBI :: Device key not found");
        }
        catch(Exception ex){
            status = false;
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: While validating Certificate of connected device");
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

    public boolean checkAdvanceKeyRotation() {
        boolean bStatus = false;

        try {
            X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();

            if (null != certificate)
            {
                Date date = new Date();
                Calendar calender = Calendar.getInstance();
                calender.setTime(date);
                calender.add(Calendar.DATE, DeviceMain.deviceMain.advanceKeyRotationDays);

                certificate.checkValidity(calender.getTime());
                bStatus = true;
            }
        }
        catch(CertificateExpiredException | CertificateNotYetValidException e){
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Certificate is expired or will be expired shortly");
            bStatus = false;
        }
        catch(Exception e){
            Logger.e(DeviceConstants.LOG_TAG, "Face Auth SBI :: Error while checking Advance Key Rotation");
            bStatus = false;
        }

        return bStatus;
    }

    public boolean checkSnapshotValidityAndInit() {
        boolean validityStatus = true;

        if (!DeviceMain.deviceMain.mgmtServerConnectivity){
            return true;
        }

        if(null == DeviceMain.deviceMain.gSnapshotResponse){
            MDServiceUtility mdServiceUtility = new MDServiceUtility();
            CommonDeviceAPI commonDeviceAPI = new CommonDeviceAPI();
            ManagementClient managementClient = new ManagementClient(mdServiceUtility, commonDeviceAPI.getSerialNumber());
            managementClient.readSnapshotDetails();
        }
        long snapshotValidity = devCommonDeviceAPI.getMillisfromISO(
                DeviceMain.deviceMain.gSnapshotResponse.snapshotValidity);

        long currentTimeinmillis = devCommonDeviceAPI.getMillisfromISO(
                devCommonDeviceAPI.getISOTimeStamp());

        if (snapshotValidity > 0 && currentTimeinmillis > 0){
            long timediff = snapshotValidity - currentTimeinmillis;

            if (timediff <= 0){//Already expired case
                Logger.w(DeviceConstants.LOG_TAG, "Timesync validity is expired (" + DeviceMain.deviceMain.gSnapshotResponse.snapshotValidity
                        + "), Trying to reach Management Server" );
                //devCommonDeviceAPI.showAlwaysOnTopMessage("Face Auth SBI", "Timesync validity is expired, Trying to reach Management Server");
                //new File(DeviceMain.deviceMain.appFolderPath + "\\snapShot.info").delete();
                DeviceMain.deviceMain.gSnapshotResponse = null;
                validityStatus = false;
                MainActivity.initSuccessful = false;
                MainActivity.sharedPreferences.edit().putLong("LastInitTimeMills", 0).apply();
                //doInitDataInThread();
            }
        }
        return validityStatus;
    }

    public class TimeSyncScheduler extends TimerTask
    {
        public TimeSyncScheduler(){
        }

        public void run()
        {
            long currentTime = (new Date()).getTime();
            if ((currentTime - lastSnapshotValidityCheck) < DeviceConstants.MDS_SCHEDULER_INTERVAL/2){
                return;
            }
            performSnapshotValidityCheck();
            lastSnapshotValidityCheck = (new Date()).getTime();
        }
    }

    private void performSnapshotValidityCheck() {
        if (!DeviceMain.deviceMain.mgmtServerConnectivity){
            return;
        }

        Logger.i(DeviceConstants.LOG_TAG, "Starting Snapshot Validity check" );

        long snapshotValidity = DeviceMain.deviceMain.deviceCommonDeviceAPI.getMillisfromISO(
                DeviceMain.deviceMain.gSnapshotResponse.snapshotValidity);

        long currentTimeinmillis = DeviceMain.deviceMain.deviceCommonDeviceAPI.getMillisfromISO(
                DeviceMain.deviceMain.deviceCommonDeviceAPI.getISOTimeStamp());

        if (snapshotValidity > 0 && currentTimeinmillis > 0){
            long timediff = snapshotValidity - currentTimeinmillis;

            if (timediff < DeviceConstants.MDS_SNAPSHOTVALIDITY_WARNING){
                DeviceMain.deviceMain.init();
            }

            snapshotValidity = devCommonDeviceAPI.getMillisfromISO(
                    DeviceMain.deviceMain.gSnapshotResponse.snapshotValidity);

            timediff = snapshotValidity - currentTimeinmillis;

            if (timediff <= 0){//Already expired case
                Logger.i(DeviceConstants.LOG_TAG, "Timesync validity is expired (" + DeviceMain.deviceMain.gSnapshotResponse.snapshotValidity
                        + "), Please connect to internet and restart Face Auth SBI" );
                //devCommonDeviceAPI.showAlwaysOnTopMessage("Face Auth SBI", "Timesync validity is expired, Please connect to internet and restart Face Auth SBI...");
                //new File(DeviceMain.deviceMain.appFolderPath + "\\snapShot.info").delete();
                DeviceMain.deviceMain.gSnapshotResponse = null;
            }
            else if (timediff < DeviceConstants.MDS_SNAPSHOTVALIDITY_WARNING){
                int hrs = (int)(timediff/3600000); // 1 hr (millisecond) - 3600000
                if (hrs > 0){
                    //showNotification(LOGTYPE.INFO, DeviceConstants.MSPMDSERVICE, "Connect to Internet in " + hrs + " hour(s) and restart Face Auth SBI");
                    Logger.i(DeviceConstants.LOG_TAG, "Connect to Internet in " + hrs + " hour(s) and restart Face Auth SBI" );
                }
                else{
                    //devCommonDeviceAPI.showAlwaysOnTopMessage("Face Auth SBI", "Connect to Internet now and restart Face Auth SBI");
                    Logger.i(DeviceConstants.LOG_TAG, "Connect to Internet now and restart Face Auth SBI" );
                }
            }
        }
    }

}
