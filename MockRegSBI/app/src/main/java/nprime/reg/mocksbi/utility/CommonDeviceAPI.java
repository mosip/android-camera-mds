package nprime.reg.mocksbi.utility;

import android.provider.Settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmc.DataObjects.DataHeader;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.Security;

import nprime.reg.mocksbi.mds.MDServiceActivity;

/**
 * @author NPrime Technologies
 */

public class CommonDeviceAPI {

    ObjectMapper oB = null;

    public String getISOTimeStamp() {

        return CryptoUtility.getTimestamp();
    }

    public String getSerialNumber() {
        return Settings.Secure.getString(MDServiceActivity.applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
        //return Build.getSerial().toString();
        /*}else {
            return Build.SERIAL;
        }*/
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
            StringBuilder jb = new StringBuilder();
            String line;

            while ((line = requestBuffer.readLine()) != null){
                jb.append(line);
            }
            respObject =  new JSONObject(jb.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return respObject;
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

    public byte[] Sha256(byte[] inpData) {
        Security.addProvider(new BouncyCastleProvider());
        String algorithm = "SHA-256";
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
}
