package nprime.reg.mocksbi.mds;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nprime.reg.mocksbi.R;
import nprime.reg.mocksbi.camera.RCaptureActivity;
import nprime.reg.mocksbi.constants.ClientConstants;
import nprime.reg.mocksbi.dto.CaptureDetail;
import nprime.reg.mocksbi.dto.CaptureRequestDeviceDetailDto;
import nprime.reg.mocksbi.dto.CaptureRequestDto;
import nprime.reg.mocksbi.dto.CaptureResponse;
import nprime.reg.mocksbi.dto.DeviceDiscoveryRequestDetail;
import nprime.reg.mocksbi.dto.DeviceInfoResponse;
import nprime.reg.mocksbi.dto.DiscoverDto;
import nprime.reg.mocksbi.dto.Error;
import nprime.reg.mocksbi.faceCaptureApi.CaptureResult;
import nprime.reg.mocksbi.scanner.ResponseGenerator.ResponseGenHelper;
import nprime.reg.mocksbi.secureLib.DeviceKeystore;
import nprime.reg.mocksbi.utility.CommonDeviceAPI;
import nprime.reg.mocksbi.utility.DeviceConstants;
import nprime.reg.mocksbi.utility.Logger;

/**
 * @author NPrime Technologies
 */

public class MDServiceActivity extends AppCompatActivity {
    private static final int RequestCodeRCapture = 1;

    private static final int PERMISSION_CAMERA = 2;

    private final ObjectMapper ob;

    {
        ob = new ObjectMapper();
        ob.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, false);
        ob.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public static long lastInitTimestamp = 0;
    public static Context applicationContext;

    CaptureRequestDto captureRequestDto = null;
    DeviceConstants.ServiceStatus currentFaceStatus = DeviceConstants.ServiceStatus.READY;
    DeviceConstants.ServiceStatus currentFingerStatus = DeviceConstants.ServiceStatus.READY;
    DeviceConstants.ServiceStatus currentIrisStatus = DeviceConstants.ServiceStatus.READY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mdservice);

        if (ActivityCompat.checkSelfPermission(MDServiceActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MDServiceActivity.this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
            return;
        }
        applicationContext = this.getApplicationContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);

        currentFaceStatus = DeviceConstants.getDeviceStatusEnum(
                sharedPreferences.getString(ClientConstants.FACE_DEVICE_STATUS
                        , DeviceConstants.ServiceStatus.READY.toString()));
        currentFingerStatus = DeviceConstants.getDeviceStatusEnum(
                sharedPreferences.getString(ClientConstants.FINGER_DEVICE_STATUS
                        , DeviceConstants.ServiceStatus.READY.toString()));
        currentIrisStatus = DeviceConstants.getDeviceStatusEnum(
                sharedPreferences.getString(ClientConstants.IRIS_DEVICE_STATUS
                        , DeviceConstants.ServiceStatus.READY.toString()));

        handleIntents();
    }

    private void handleIntents() {
        String actionType = getIntent().getAction();
        switch (actionType) {
            case "io.sbi.device": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();
                    Object responseBody = null;
                    DeviceDiscoveryRequestDetail discoverRequestDto = null;
                    byte[] input = getIntent().getByteArrayExtra("input");

                    if (null != input) {
                        try {
                            discoverRequestDto = ob.readValue(input, DeviceDiscoveryRequestDetail.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (null != discoverRequestDto) {
                        switch (discoverRequestDto.type.toLowerCase()) {
                            case "face":
                                responseBody = discoverDevice(currentFaceStatus, szTs, "nprime.reg.mocksbi.face", DeviceConstants.BioType.Face);
                                break;
                            case "finger":
                                responseBody = discoverDevice(currentFingerStatus, szTs, "nprime.reg.mocksbi.finger",
                                        DeviceConstants.BioType.Finger);
                                break;
                            case "iris":
                                responseBody = discoverDevice(currentIrisStatus, szTs, "nprime.reg.mocksbi.iris",
                                        DeviceConstants.BioType.Iris);
                                break;
                            case "biometric device":
                                break;
                        }

                    } else {
                        responseBody = Arrays.asList(new DeviceInfoResponse(null, new Error("301", "Invalid type")));
                    }
                    generateResponse(responseBody, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /device. MOSIPDISC completed");
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.face.Info": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();

                    String requestType = "nprime.reg.mocksbi.face" + ".info";
                    List<DeviceInfoResponse> deviceInfo = getDeviceDriverInfo(currentFaceStatus, szTs, requestType, DeviceConstants.BioType.Face);

                    generateResponse(deviceInfo, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.finger.Info": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();

                    String requestType = "nprime.reg.mocksbi.finger" + ".info";
                    List<DeviceInfoResponse> deviceInfo = getDeviceDriverInfo(currentFingerStatus, szTs, requestType, DeviceConstants.BioType.Finger);

                    generateResponse(deviceInfo, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.iris.Info": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();

                    String requestType = "nprime.reg.mocksbi.iris" + ".info";
                    List<DeviceInfoResponse> deviceInfo = getDeviceDriverInfo(currentIrisStatus, szTs, requestType, DeviceConstants.BioType.Iris);

                    generateResponse(deviceInfo, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.face.rCapture": {
                new Thread(() -> {
                    cleanUriFileData();
                    byte[] input = getIntent().getByteArrayExtra("input");
                    if (null != input) {
                        rCapture(input, DeviceConstants.BioType.Face);
                    } else {
                        Map<String, Object> errorMap = new LinkedHashMap<>();
                        errorMap.put("errorCode", "101");
                        errorMap.put("errorInfo", "Invalid input");
                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("error", errorMap);
                        generateResponse(new JSONObject(responseMap), true);
                    }
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.finger.rCapture": {
                new Thread(() -> {
                    cleanUriFileData();
                    byte[] input = getIntent().getByteArrayExtra("input");
                    if (null != input) {
                        rCapture(input, DeviceConstants.BioType.Finger);
                    } else {
                        Map<String, Object> errorMap = new LinkedHashMap<>();
                        errorMap.put("errorCode", "101");
                        errorMap.put("errorInfo", "Invalid input");
                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("error", errorMap);
                        generateResponse(new JSONObject(responseMap), true);
                    }
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.iris.rCapture": {
                new Thread(() -> {
                    cleanUriFileData();
                    byte[] input = getIntent().getByteArrayExtra("input");
                    if (null != input) {
                        rCapture(input, DeviceConstants.BioType.Iris);
                    } else {
                        Map<String, Object> errorMap = new LinkedHashMap<>();
                        errorMap.put("errorCode", "101");
                        errorMap.put("errorInfo", "Invalid input");
                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("error", errorMap);
                        generateResponse(new JSONObject(responseMap), true);
                    }
                }).start();
                break;
            }
            default:
                setResult(Activity.RESULT_CANCELED);
                finish();
                break;
        }
    }


    //TODO clean shared files
    private void cleanUriFileData() {
        try {
            File outputPath = new File(this.getFilesDir(), "output/");
            if (outputPath.exists()) {
                String folder = getCallingPackage().replaceAll("\\.", "-");
                File clientFolderPath = new File(outputPath.getPath(), folder + "/");
                if (clientFolderPath.exists()) {
                    File[] files = clientFolderPath.listFiles();
                    if (null != files && files.length > 0) {
                        for (File file : files) {
                            file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rCapture(byte[] input, DeviceConstants.BioType bioType) {
        try {
            if (ActivityCompat.checkSelfPermission(MDServiceActivity.this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MDServiceActivity.this,
                        new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
                return;
            }
            captureRequestDto = ob.readValue(input, CaptureRequestDto.class);

            //Validations
            List<CaptureRequestDeviceDetailDto> mosipBioRequest = captureRequestDto.bio;

            int deviceSubId = Integer.parseInt(mosipBioRequest.get(0).deviceSubId);
            String[] bioException = mosipBioRequest.get(0).exception;// Bio exceptions
            int count = Integer.parseInt(mosipBioRequest.get(0).count);
            int exceptionCount = (bioException != null ? bioException.length : 0);
            int finalCount = count + exceptionCount;

            switch (bioType) {
                case Finger:
                    switch (deviceSubId) {
                        case DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_LEFT:
                        case DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_RIGHT:
                            // Max Count = 4 exception allowed
                            if (finalCount != 4) {
                                generateRCaptureResponse(getCaptureErrorResponse("109", "Count Mismatch"), false);
                                return;
                            }
                            break;
                        case DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_THUMB:
                            // Max Count = 2 exception allowed
                            if (finalCount != 2) {
                                generateRCaptureResponse(getCaptureErrorResponse("109", "Count Mismatch"), false);
                                return;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case Iris:
                    switch (deviceSubId) {
                        case DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_LEFT:
                        case DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_RIGHT:
                            // Max Count = 1 no exception allowed
                            if (count != 1 || exceptionCount != 0) {
                                generateRCaptureResponse(getCaptureErrorResponse("109", "Count Mismatch"), false);
                                return;
                            }
                            break;
                        case DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_BOTH:
                            // Max Count = 2 exception allowed
                            finalCount = count + exceptionCount;
                            if (finalCount != 2) {
                                generateRCaptureResponse(getCaptureErrorResponse("109", "Count Mismatch"), false);
                                return;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case Face:
                    // Max Face Count = 1 with or without exception
                    if (count != 1) {
                        generateRCaptureResponse(getCaptureErrorResponse("109", "Count Mismatch"), false);
                        return;
                    }
                    break;
                default:
                    break;
            }

            if (DeviceConstants.environmentList.contains(captureRequestDto.env)
                    && (captureRequestDto.purpose.equalsIgnoreCase(DeviceConstants.DeviceUsage.Registration.getDeviceUsage()))
                    || captureRequestDto.purpose.equalsIgnoreCase(DeviceConstants.DeviceUsage.Authentication.getDeviceUsage())) {
                startCameraActivityRCapture(captureRequestDto.timeout, captureRequestDto.bio.get(0), bioType);
            } else {
                generateRCaptureResponse(getCaptureErrorResponse("501", "Invalid Environment / Purpose"), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(DeviceConstants.LOG_TAG, "Failed to initiate capture");
            generateRCaptureResponse(getCaptureErrorResponse("101", "Invalid JSON Value"), false);
        }
    }

    private void startCameraActivityRCapture(int captureTimeout, CaptureRequestDeviceDetailDto bio, DeviceConstants.BioType bioType) {
        Intent intent = new Intent(this, RCaptureActivity.class);
        intent.putExtra("CaptureTimeout", captureTimeout);
        intent.putExtra("modality", bioType.getType());
        intent.putExtra("deviceSubId", bio.deviceSubId);
        intent.putExtra("bioSubType", bio.bioSubType);
        intent.putExtra("exception", bio.exception);
        startActivityForResult(intent, RequestCodeRCapture);
    }

    private void generateResponse(Object responseXml, boolean isError) {
        Intent intent = new Intent();
        try {
            byte[] responseBytes = ob.writeValueAsBytes(responseXml);
            intent.putExtra("response", responseBytes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (isError || (null == responseXml)) {
            setResult(Activity.RESULT_CANCELED, intent);
        } else {
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private CaptureResponse getCaptureErrorResponse(String errorCode, String exceptionMessage) {
        List<CaptureDetail> biometrics = new ArrayList<>();

        CaptureDetail biometric = new CaptureDetail();
        biometric.specVersion = DeviceConstants.MDSVERSION;
        biometric.data = "";
        biometric.hash = "";

        biometric.error = (new Error(errorCode, exceptionMessage));

        CaptureResponse captureResponse = new CaptureResponse();
        biometrics.add(biometric);
        captureResponse.biometrics = biometrics;

        return captureResponse;
    }

    private void generateRCaptureResponse(CaptureResponse responseXml, boolean isError) {
        Intent intent = new Intent();
        if (null != responseXml) {
            try {
                File outputPath = new File(this.getFilesDir(), "output/");
                if (!outputPath.exists()) {
                    outputPath.mkdir();
                }
                String folder = getCallingPackage().replaceAll("\\.", "-");
                File clientFolderPath = new File(outputPath.getPath(), folder + "/");
                if (!clientFolderPath.exists()) {
                    clientFolderPath.mkdir();
                }
                File file = new File(clientFolderPath, "resp_" + System.currentTimeMillis() + ".txt");
                final OutputStream os = new FileOutputStream(file);
                os.write(ob.writeValueAsBytes(responseXml));
                os.flush();
                os.close();
                Uri respUri = FileProvider.getUriForFile(MDServiceActivity.this, "nprime.reg.mocksbi.fileprovider", file);
                getApplicationContext().grantUriPermission(getCallingPackage(), respUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra("response", respUri);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (isError || (null == responseXml)) {
            setResult(Activity.RESULT_CANCELED, intent);
        } else {
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private List<DiscoverDto> discoverDevice(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType, DeviceConstants.BioType bioType) {
        return ResponseGenHelper.getDeviceDiscovery(currentStatus, szTimeStamp, requestType, bioType);
    }

    public List<DeviceInfoResponse> getDeviceDriverInfo(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType,
                                                        DeviceConstants.BioType bioType) {
        DeviceKeystore keystore = new DeviceKeystore(this);
        return ResponseGenHelper.getDeviceDriverInfo(currentStatus, szTimeStamp, requestType, bioType, keystore);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        DeviceKeystore keystore = new DeviceKeystore(this);
        if (RequestCodeRCapture == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                new Thread(() -> {
                    try {
                        CaptureResult captureResult = new CaptureResult();
                        captureResult.setModality(data.getStringExtra("modality"));
                        captureResult.setBioSubId(data.getStringExtra("bioSubId") != null ? data.getStringExtra("bioSubId") : "1");

                        List<String> segmentNames = data.getStringArrayListExtra("segmentNames");

                        segmentNames.forEach(segmentName -> {
                            try (InputStream is = getContentResolver().openInputStream(data.getParcelableExtra(segmentName))) {
                                captureResult.getBiometricRecords().put(segmentName, readBytes(is));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                        captureResult.setStatus(data.getIntExtra("Status", CaptureResult.CAPTURE_CANCELLED));
                        captureResult.setQualityScore(data.getIntExtra("Quality", 0));
                        CaptureResponse responseXml = ResponseGenHelper
                                .getRCaptureBiometricsMOSIP(captureResult, captureRequestDto, keystore);
                        generateRCaptureResponse(responseXml, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                CaptureResult captureResult = new CaptureResult();
                captureResult.setStatus(CaptureResult.CAPTURE_CANCELLED);
                captureResult.setQualityScore(0);
                if (null != data) {
                    captureResult.setStatus(data.getIntExtra("Status", CaptureResult.CAPTURE_CANCELLED));
                    captureResult.setQualityScore(data.getIntExtra("Quality", 0));
                }

                CaptureResponse responseXml = ResponseGenHelper
                        .getRCaptureBiometricsMOSIP(captureResult, captureRequestDto, keystore);
                generateRCaptureResponse(responseXml, false);
            }
        }
    }

    public byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }
}