package nprime.reg.mocksbi.device;

import static nprime.reg.mocksbi.utility.DeviceConstants.*;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nprime.reg.mocksbi.R;
import nprime.reg.mocksbi.constants.ClientConstants;
import nprime.reg.mocksbi.faceCaptureApi.CaptureResult;
import nprime.reg.mocksbi.utility.DeviceConstants;

/**
 * @author NPrime Technologies
 */

public class CaptureActivity extends AppCompatActivity {
    private int faceQualityScore;
    private int fingerQualityScore;
    private int irisQualityScore;
    private BioDevice bioDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcapture);

        String modality = getIntent().getStringExtra("modality");
        int deviceSubId = getIntent().getIntExtra("deviceSubId", 1);
        int captureTimeout = getIntent().getIntExtra("CaptureTimeout", Integer.MAX_VALUE);
        String[] bioSubType = getIntent().getStringArrayExtra("bioSubType");
        String[] exception = getIntent().getStringArrayExtra("exception");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        faceQualityScore = sharedPreferences.getInt(ClientConstants.FACE_SCORE, 30);
        fingerQualityScore = sharedPreferences.getInt(ClientConstants.FINGER_SCORE, 30);
        irisQualityScore = sharedPreferences.getInt(ClientConstants.IRIS_SCORE, 30);

        String deviceUsage = sharedPreferences.getString(ClientConstants.DEVICE_USAGE
                , DeviceConstants.DeviceUsage.Registration.getDeviceUsage());
        if (DeviceUsage.Authentication.getDeviceUsage().equals(deviceUsage)) {
            bioDevice = new AuthBioDevice(this);
        } else {
            bioDevice = new RegBioDevice(this);
        }

        long responseDelay;
        switch (modality.toLowerCase()) {
            case "face":
                responseDelay = sharedPreferences.getInt(ClientConstants.FACE_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
                break;
            case "finger":
                responseDelay = sharedPreferences.getInt(ClientConstants.FINGER_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
                break;
            case "iris":
                responseDelay = sharedPreferences.getInt(ClientConstants.IRIS_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
                break;
            default:
                responseDelay = DEFAULT_TIME_DELAY;
        }

        if (captureTimeout < responseDelay) {
            new Handler().postDelayed(() -> {
                captureFailed(CaptureResult.CAPTURE_TIMEOUT, "");
            }, captureTimeout);
            return;
        }

        new Handler().postDelayed(() -> {
            try {
                int qualityScore = 30;
                Map<String, Uri> uris;
                switch (modality.toLowerCase()) {
                    case "face":
                        ((ImageView) findViewById(R.id.img)).setImageResource(R.drawable.face);
                        uris = bioDevice.captureFaceModality();
                        qualityScore = faceQualityScore;
                        break;
                    case "finger":
                        ((ImageView) findViewById(R.id.img)).setImageResource(R.drawable.left);
                        uris = bioDevice.captureFingersModality(deviceSubId, bioSubType, exception);
                        qualityScore = fingerQualityScore;
                        break;
                    case "iris":
                        ((ImageView) findViewById(R.id.img)).setImageResource(R.drawable.iris);
                        uris = bioDevice.captureIrisModality(deviceSubId, bioSubType, exception);
                        qualityScore = irisQualityScore;
                        break;
                    default:
                        uris = new HashMap<>();
                        break;
                }
                captureSuccessful(uris, qualityScore);
            } catch (Exception e) {
                e.printStackTrace();
                captureFailed(-301, e.getMessage());
            }
        }, responseDelay);
    }

    public void captureSuccessful(Map<String, Uri> uris, int quality) {
        Intent intent = new Intent();
        ArrayList<String> segmentNames = new ArrayList<>();

        for (String attribute : uris.keySet()) {
            segmentNames.add(attribute);
            intent.putExtra(attribute, uris.get(attribute));
        }

        intent.putExtra("segmentNames", segmentNames);
        intent.putExtra("Status", CaptureResult.CAPTURE_SUCCESS);
        intent.putExtra("Quality", quality);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public void captureFailed(int status, String msg) {
        Intent intent = new Intent();
        intent.putExtra("Status", status);
        intent.putExtra("msg", msg);
        intent.putExtra("Quality", 0);
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }
}