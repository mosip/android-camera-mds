package nprime.reg.mocksbi.device;

import static nprime.reg.mocksbi.utility.DeviceConstants.DEFAULT_TIME_DELAY;
import static nprime.reg.mocksbi.utility.DeviceConstants.DEVICE_FINGER_SINGLE_SUB_TYPE_ID;
import static nprime.reg.mocksbi.utility.DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_LEFT;
import static nprime.reg.mocksbi.utility.DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_RIGHT;
import static nprime.reg.mocksbi.utility.DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_THUMB;
import static nprime.reg.mocksbi.utility.DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_BOTH;
import static nprime.reg.mocksbi.utility.DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_LEFT;
import static nprime.reg.mocksbi.utility.DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_RIGHT;
import static nprime.reg.mocksbi.utility.DeviceConstants.DEVICE_IRIS_SINGLE_SUB_TYPE_ID;
import static nprime.reg.mocksbi.utility.DeviceConstants.DeviceUsage;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_FACE;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_INDEX;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_IRIS;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_LITTLE;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_MIDDLE;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_RING;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_THUMB;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_INDEX;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_IRIS;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_LITTLE;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_MIDDLE;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_RING;
import static nprime.reg.mocksbi.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_THUMB;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import nprime.reg.mocksbi.R;
import nprime.reg.mocksbi.constants.ClientConstants;
import nprime.reg.mocksbi.faceCaptureApi.CaptureResult;
import nprime.reg.mocksbi.utility.DeviceConstants;

/**
 * @author NPrime Technologies
 */

public class AuthCaptureActivity extends AppCompatActivity {
    private int faceQualityScore;
    private int fingerQualityScore;
    private int irisQualityScore;

    private Map<String, String> segmentUriMapping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcapture);
        String modality = getIntent().getStringExtra("modality");
        int deviceSubId = getIntent().getIntExtra("deviceSubId", 1);
        int captureTimeout = getIntent().getIntExtra("CaptureTimeout", Integer.MAX_VALUE);
        String[] bioSubType = getIntent().getStringArrayExtra("bioSubType");
        String[] exception = getIntent().getStringArrayExtra("exception");

        segmentUriMapping = new HashMap<>();
        segmentUriMapping.put("", PROFILE_BIO_FILE_NAME_FACE);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_INDEX, PROFILE_BIO_FILE_NAME_LEFT_INDEX);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_MIDDLE, PROFILE_BIO_FILE_NAME_LEFT_MIDDLE);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_RING, PROFILE_BIO_FILE_NAME_LEFT_RING);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_LITTLE, PROFILE_BIO_FILE_NAME_LEFT_LITTLE);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_INDEX, PROFILE_BIO_FILE_NAME_RIGHT_INDEX);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_MIDDLE, PROFILE_BIO_FILE_NAME_RIGHT_MIDDLE);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_RING, PROFILE_BIO_FILE_NAME_RIGHT_RING);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_LITTLE, PROFILE_BIO_FILE_NAME_RIGHT_LITTLE);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_THUMB, PROFILE_BIO_FILE_NAME_LEFT_THUMB);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_THUMB, PROFILE_BIO_FILE_NAME_RIGHT_THUMB);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_IRIS, PROFILE_BIO_FILE_NAME_LEFT_IRIS);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_IRIS, PROFILE_BIO_FILE_NAME_RIGHT_IRIS);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        faceQualityScore = sharedPreferences.getInt(ClientConstants.FACE_SCORE, 30);
        fingerQualityScore = sharedPreferences.getInt(ClientConstants.FINGER_SCORE, 30);
        irisQualityScore = sharedPreferences.getInt(ClientConstants.IRIS_SCORE, 30);

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
                        uris = captureFaceModality();
                        qualityScore = faceQualityScore;
                        break;
                    case "finger":
                        uris = captureFingersModality(deviceSubId, bioSubType, exception);
                        qualityScore = fingerQualityScore;
                        break;
                    case "iris":
                        uris = captureIrisModality(deviceSubId, bioSubType, exception);
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

    private Map<String, Uri> captureIrisModality(int deviceSubId, String[] bioSubType, String[] exception) {
        ((ImageView) findViewById(R.id.img)).setImageResource(R.drawable.iris);
        List<String> segmentsToCapture = null;
        switch (deviceSubId) {
            case DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_LEFT:
            case DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_RIGHT:
            case DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_BOTH:
                break; // double not implemented for auth
            case DEVICE_IRIS_SINGLE_SUB_TYPE_ID:
                segmentsToCapture = getSegmentsToCapture(Arrays.asList(
                                DeviceConstants.BIO_NAME_LEFT_IRIS,
                                DeviceConstants.BIO_NAME_RIGHT_IRIS),
                        bioSubType == null ? null : Arrays.asList(bioSubType),
                        exception == null ? null : Arrays.asList(exception));
                break;
        }
        Map<String, Uri> uris = new HashMap<>();
        if (segmentsToCapture == null || segmentsToCapture.isEmpty()) {
            return uris;
        }

        segmentsToCapture.forEach(segment -> uris.put(segment,
                getBioAttributeURI(segmentUriMapping.get(segment))));
        return uris;
    }

    private Map<String, Uri> captureFingersModality(int deviceSubId, String[] bioSubType, String[] exception) {
        List<String> segmentsToCapture = null;
        switch (deviceSubId) {
            case DEVICE_FINGER_SLAP_SUB_TYPE_ID_LEFT: // left
            case DEVICE_FINGER_SLAP_SUB_TYPE_ID_RIGHT: // right
            case DEVICE_FINGER_SLAP_SUB_TYPE_ID_THUMB: // thumbs
                break; // double not implemented for auth
            case DEVICE_FINGER_SINGLE_SUB_TYPE_ID:
                ((ImageView) findViewById(R.id.img)).setImageResource(R.drawable.right);
                segmentsToCapture = getSegmentsToCapture(
                        Arrays.asList(
                                DeviceConstants.BIO_NAME_RIGHT_INDEX,
                                DeviceConstants.BIO_NAME_RIGHT_MIDDLE,
                                DeviceConstants.BIO_NAME_RIGHT_RING,
                                DeviceConstants.BIO_NAME_RIGHT_LITTLE,
                                DeviceConstants.BIO_NAME_LEFT_INDEX,
                                DeviceConstants.BIO_NAME_LEFT_MIDDLE,
                                DeviceConstants.BIO_NAME_LEFT_RING,
                                DeviceConstants.BIO_NAME_LEFT_LITTLE,
                                DeviceConstants.BIO_NAME_LEFT_THUMB,
                                DeviceConstants.BIO_NAME_RIGHT_THUMB),
                        bioSubType == null ? null : Arrays.asList(bioSubType),
                        exception == null ? null : Arrays.asList(exception));
                break;
        }

        Map<String, Uri> uris = new HashMap<>();
        if (segmentsToCapture == null || segmentsToCapture.isEmpty()) {
            return uris;
        }

        segmentsToCapture.forEach(segment -> uris.put(segment,
                getBioAttributeURI(segmentUriMapping.get(segment))));
        return uris;
    }

    private Map<String, Uri> captureFaceModality() {
        ((ImageView) findViewById(R.id.img)).setImageResource(R.drawable.face);
        Map<String, Uri> uris = new HashMap<>();
        uris.put("", getBioAttributeURI(segmentUriMapping.get("")));
        return uris;
    }

    private List<String> getSegmentsToCapture(List<String> defaultSubTypes, List<String> bioSubTypes, List<String> exceptions) {
        List<String> localCopy = new ArrayList<>(defaultSubTypes);
        if (exceptions != null) {
            localCopy.removeAll(exceptions);
        }

        List<String> segmentsToCapture = new ArrayList<>();
        if (bioSubTypes == null || bioSubTypes.isEmpty()) {
            segmentsToCapture.addAll(localCopy);
            return segmentsToCapture;
        } else {
            Random rand = new Random();
            for (String bioSubType : bioSubTypes) {
                if (localCopy.contains(bioSubType)) {
                    segmentsToCapture.add(bioSubType);
                } else if ("UNKNOWN".equals(bioSubType)) {
                    String randSubType = defaultSubTypes.get(rand.nextInt(defaultSubTypes.size()));
                    while (bioSubTypes.contains(randSubType) && bioSubTypes.size() <= localCopy.size()) {
                        randSubType = defaultSubTypes.get(rand.nextInt(defaultSubTypes.size()));
                    }
                    segmentsToCapture.add(randSubType);
                }
            }
        }
        return segmentsToCapture;
    }

    private Uri getBioAttributeURI(String file) {
        byte[] isoRecord = getIsoDataFromAssets(DeviceUsage.Authentication.getDeviceUsage() + "/" + file);
        Uri isoUri = Uri.fromFile(getTempFile(AuthCaptureActivity.this));
        saveByteArray(isoRecord, isoUri);
        return isoUri;
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

    public File getTempFile(Context context) {
        try {
            return File.createTempFile("isoRecord_", ".dat", context.getCacheDir());
        } catch (final IOException e) {
            throw new RuntimeException("Unable to create photo file.");
        }
    }

    public static void saveByteArray(final byte[] data, final Uri uri) {
        final File file = new File(uri.getPath());

        try {
            final OutputStream os = Files.newOutputStream(file.toPath());
            os.write(data);
            os.flush();
            os.close();
        } catch (final Exception e) {
            throw new RuntimeException("Unable to store data.", e);
        }
    }

    private byte[] getIsoDataFromAssets(String assetFileName) {
        try (InputStream in = getAssets().open(assetFileName)) {
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}