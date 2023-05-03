package io.mosip.mock.sbi.utility.utility;

import android.provider.Settings;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.MessageDigest;
import java.security.Security;

import io.mosip.mock.sbi.mds.mds.MDServiceActivity;

/**
 * @author NPrime Technologies
 */

public class CommonDeviceAPI {

    public String getISOTimeStamp() {

        return CryptoUtility.getTimestamp();
    }

    public String getSerialNumber() {
        return Settings.Secure.getString(MDServiceActivity.applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public byte[] Sha256(byte[] inpData) {
        Security.addProvider(new BouncyCastleProvider());
        String algorithm = "SHA-256";
        byte[] hash = null;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
            digest.reset();
            hash = digest.digest(inpData);
        } catch (Exception e) {
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
