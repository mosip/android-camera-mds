package io.mosip.mock.sbi.mds;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.mosip.mock.sbi.R;
import io.mosip.mock.sbi.device.CaptureActivity;
import io.mosip.mock.sbi.constants.ClientConstants;
import io.mosip.mock.sbi.dto.CaptureDetail;
import io.mosip.mock.sbi.dto.CaptureRequestDeviceDetailDto;
import io.mosip.mock.sbi.dto.CaptureRequestDto;
import io.mosip.mock.sbi.dto.CaptureResponse;
import io.mosip.mock.sbi.dto.DeviceDiscoveryRequestDetail;
import io.mosip.mock.sbi.dto.DeviceInfoResponse;
import io.mosip.mock.sbi.dto.DiscoverDto;
import io.mosip.mock.sbi.dto.Error;
import io.mosip.mock.sbi.faceCaptureApi.CaptureResult;
import io.mosip.mock.sbi.scanner.ResponseGenerator.ResponseGenHelper;
import io.mosip.mock.sbi.secureLib.DeviceKeystore;
import io.mosip.mock.sbi.utility.CommonDeviceAPI;
import io.mosip.mock.sbi.utility.DeviceConstants;
import io.mosip.mock.sbi.utility.DeviceUtil;
import io.mosip.mock.sbi.utility.Logger;

/**
 * @author NPrime Technologies
 */

public class MDServiceActivity extends AppCompatActivity {
    private static final int RequestCodeCapture = 1;

    private static final int PERMISSION_CAMERA = 2;

    private final ObjectMapper ob;

    {
        ob = new ObjectMapper();
        ob.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, false);
        ob.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public static Context applicationContext;

    CaptureRequestDto captureRequestDto = null;
    DeviceConstants.ServiceStatus currentFaceStatus = DeviceConstants.ServiceStatus.READY;
    DeviceConstants.ServiceStatus currentFingerStatus = DeviceConstants.ServiceStatus.READY;
    DeviceConstants.ServiceStatus currentIrisStatus = DeviceConstants.ServiceStatus.READY;

    ResponseGenHelper responseGenHelper;
    DeviceUtil deviceUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mdservice);

        if (ActivityCompat.checkSelfPermission(MDServiceActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MDServiceActivity.this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
            generateResponse(null, true);
            return;
        }
        applicationContext = this.getApplicationContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);

        currentFaceStatus = DeviceConstants.getDeviceStatusEnum(
                sharedPreferences.getString(ClientConstants.FACE_DEVICE_STATUS
                        , DeviceConstants.ServiceStatus.READY.getStatus()));
        currentFingerStatus = DeviceConstants.getDeviceStatusEnum(
                sharedPreferences.getString(ClientConstants.FINGER_DEVICE_STATUS
                        , DeviceConstants.ServiceStatus.READY.getStatus()));
        currentIrisStatus = DeviceConstants.getDeviceStatusEnum(
                sharedPreferences.getString(ClientConstants.IRIS_DEVICE_STATUS
                        , DeviceConstants.ServiceStatus.READY.getStatus()));

        String deviceUsage = sharedPreferences.getString(ClientConstants.DEVICE_USAGE
                , DeviceConstants.DeviceUsage.Registration.getDeviceUsage());

        deviceUtil = new DeviceUtil(deviceUsage);
        responseGenHelper = new ResponseGenHelper(deviceUtil);
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
                        switch (discoverRequestDto.type) {
                            case "Face":
                                responseBody = discoverDevice(currentFaceStatus, szTs, "io.mosip.mock.sbi.face", DeviceConstants.BioType.Face);
                                break;
                            case "Finger":
                                responseBody = discoverDevice(currentFingerStatus, szTs, "io.mosip.mock.sbi.finger",
                                        DeviceConstants.BioType.Finger);
                                break;
                            case "Iris":
                                responseBody = discoverDevice(currentIrisStatus, szTs, "io.mosip.mock.sbi.iris",
                                        DeviceConstants.BioType.Iris);
                                break;
                            case "Biometric Device":
                                break;
                            default:
                                responseBody = Arrays.asList(new DeviceInfoResponse(null, new Error("501", "Invalid Type Value in Device Discovery Request")));
                                break;
                        }

                    } else {
                        responseBody = Arrays.asList(new DeviceInfoResponse(null, new Error("501", "Invalid Type Value in Device Discovery Request")));
                    }
                    generateResponse(responseBody, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /device. MOSIPDISC completed");
                }).start();
                break;
            }
            case "io.mosip.mock.sbi.face.Info": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();

                    String requestType = actionType.replace(".Info", ".info") + ".info";
                    List<DeviceInfoResponse> deviceInfo = getDeviceDriverInfo(currentFaceStatus, szTs, requestType, DeviceConstants.BioType.Face);

                    generateResponse(deviceInfo, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                }).start();
                break;
            }
            case "io.mosip.mock.sbi.finger.Info": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();

                    String requestType = "io.mosip.mock.sbi.finger" + ".info";
                    List<DeviceInfoResponse> deviceInfo = getDeviceDriverInfo(currentFingerStatus, szTs, requestType, DeviceConstants.BioType.Finger);

                    generateResponse(deviceInfo, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                }).start();
                break;
            }
            case "io.mosip.mock.sbi.iris.Info": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();

                    String requestType = "io.mosip.mock.sbi.iris" + ".info";
                    List<DeviceInfoResponse> deviceInfo = getDeviceDriverInfo(currentIrisStatus, szTs, requestType, DeviceConstants.BioType.Iris);

                    generateResponse(deviceInfo, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                }).start();
                break;
            }
            case "io.mosip.mock.sbi.face.rCapture":
            case "io.mosip.mock.sbi.face.Capture": {
                new Thread(() -> {
                    cleanUriFileData();
                    byte[] input = getIntent().getByteArrayExtra("input");
                    if (null != input) {
                        capture(input, DeviceConstants.BioType.Face);
                    } else {
                        generateCaptureResponse(getCaptureErrorResponse("101", "Invalid input"), false);
                    }
                }).start();
                break;
            }
            case "io.mosip.mock.sbi.finger.rCapture":
            case "io.mosip.mock.sbi.finger.Capture": {
                new Thread(() -> {
                    cleanUriFileData();
                    byte[] input = getIntent().getByteArrayExtra("input");
                    if (null != input) {
                        capture(input, DeviceConstants.BioType.Finger);
                    } else {
                        generateCaptureResponse(getCaptureErrorResponse("101", "Invalid input"), false);
                    }
                }).start();
                break;
            }
            case "io.mosip.mock.sbi.iris.rCapture":
            case "io.mosip.mock.sbi.iris.Capture": {
                new Thread(() -> {
                    cleanUriFileData();
                    byte[] input = getIntent().getByteArrayExtra("input");
                    if (null != input) {
                        capture(input, DeviceConstants.BioType.Iris);
                    } else {
                        generateCaptureResponse(getCaptureErrorResponse("101", "Invalid input"), false);
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

    private void capture(byte[] input, DeviceConstants.BioType bioType) {
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
            int count = Integer.parseInt(mosipBioRequest.get(0).count);

            if (deviceUtil.DEVICE_USAGE == DeviceConstants.DeviceUsage.Registration) {
                if (!validateBioCountReg(mosipBioRequest.get(0), bioType)) {
                    generateCaptureResponse(getCaptureErrorResponse("109", "Count Mismatch"), false);
                    return;
                }
            } else {
                if (!validateBioCountAuth(bioType, count)) {
                    generateCaptureResponse(getCaptureErrorResponse("109", "Count Mismatch"), false);
                    return;
                }
            }

            if (DeviceConstants.environmentList.contains(captureRequestDto.env)
                    && (captureRequestDto.purpose.equalsIgnoreCase(DeviceConstants.DeviceUsage.Registration.getDeviceUsage()))
                    || captureRequestDto.purpose.equalsIgnoreCase(DeviceConstants.DeviceUsage.Authentication.getDeviceUsage())) {
                startCameraActivityCapture(captureRequestDto.timeout, captureRequestDto.bio.get(0), bioType, deviceSubId);
            } else {
                generateCaptureResponse(getCaptureErrorResponse("501", "Invalid Environment / Purpose"), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(DeviceConstants.LOG_TAG, "Failed to initiate capture");
            generateCaptureResponse(getCaptureErrorResponse("101", "Invalid JSON Value"), false);
        }
    }

    private boolean validateBioCountAuth(DeviceConstants.BioType bioType, int bioCount) {
        switch (bioType) {
            case Finger:
                if (bioCount < 0 || bioCount > 10)
                    return false;
                break;
            case Iris:
                if (bioCount < 0 || bioCount > 2)
                    return false;
                break;
            case Face:
                if (bioCount < 0 || bioCount > 1)
                    return false;
                break;
        }
        return true;
    }

    private boolean validateBioCountReg(CaptureRequestDeviceDetailDto bioRequest, DeviceConstants.BioType bioType) {
        int deviceSubId = Integer.parseInt(bioRequest.deviceSubId);
        String[] bioException = bioRequest.exception;// Bio exceptions
        int count = Integer.parseInt(bioRequest.count);
        int exceptionCount = (bioException != null ? bioException.length : 0);
        int finalCount = count + exceptionCount;

        switch (bioType) {
            case Finger:
                switch (deviceSubId) {
                    case DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_LEFT:
                    case DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_RIGHT:
                        // Max Count = 4 exception allowed
                        if (finalCount != 4) {
                            return false;
                        }
                        break;
                    case DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_THUMB:
                        // Max Count = 2 exception allowed
                        if (finalCount != 2) {
                            return false;
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
                            return false;
                        }
                        break;
                    case DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_BOTH:
                        // Max Count = 2 exception allowed
                        finalCount = count + exceptionCount;
                        if (finalCount != 2) {
                            return false;
                        }
                        break;
                    default:
                        break;
                }
                break;
            case Face:
                // Max Face Count = 1 with or without exception
                if (count != 1) {
                    return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    private void startCameraActivityCapture(int captureTimeout, CaptureRequestDeviceDetailDto bio, DeviceConstants.BioType bioType, int deviceSubId) {
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.putExtra("CaptureTimeout", captureTimeout);
        intent.putExtra("modality", bioType.getBioType());
        intent.putExtra("deviceSubId", deviceSubId);
        intent.putExtra("bioSubType", bio.bioSubType);
        intent.putExtra("exception", bio.exception);
        startActivityForResult(intent, RequestCodeCapture);
    }

    private void generateResponse(Object responseXml, boolean isError) {
        Intent intent = new Intent();
        try {
            byte[] responseBytes = ob.writeValueAsBytes(responseXml);
            intent.putExtra("response", responseBytes);
        } catch (JsonProcessingException e) {
            Logger.e(DeviceConstants.LOG_TAG, "generateResponse: " + e.getMessage());
            setResult(Activity.RESULT_CANCELED, intent);
            finish();
            return;
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
        biometric.specVersion = DeviceConstants.MDS_VERSION;
        biometric.data = "";
        biometric.hash = "";

        biometric.error = (new Error(errorCode, exceptionMessage));

        CaptureResponse captureResponse = new CaptureResponse();
        biometrics.add(biometric);
        captureResponse.biometrics = biometrics;

        return captureResponse;
    }

    private void generateCaptureResponse(CaptureResponse captureResponse, boolean isError) {
        Intent intent = new Intent();
        if (null != captureResponse) {
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
                final OutputStream os = Files.newOutputStream(file.toPath());
                os.write(ob.writeValueAsBytes(captureResponse));
                os.flush();
                os.close();
                Uri respUri = FileProvider.getUriForFile(MDServiceActivity.this, "io.mosip.mock.sbi.fileprovider", file);
                getApplicationContext().grantUriPermission(getCallingPackage(), respUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra("response", respUri);
            } catch (final Exception e) {
                Logger.e(DeviceConstants.LOG_TAG, "generateCaptureResponse: " + e.getMessage());
            }
        }

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (isError || (null == captureResponse)) {
            setResult(Activity.RESULT_CANCELED, intent);
        } else {
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private List<DiscoverDto> discoverDevice(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType, DeviceConstants.BioType bioType) {
        return responseGenHelper.getDeviceDiscovery(currentStatus, szTimeStamp, requestType, bioType);
    }

    public List<DeviceInfoResponse> getDeviceDriverInfo(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType,
                                                        DeviceConstants.BioType bioType) {
        DeviceKeystore keystore = new DeviceKeystore(this);
        return responseGenHelper.getDeviceDriverInfo(currentStatus, szTimeStamp, requestType, bioType, keystore);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        DeviceKeystore keystore = new DeviceKeystore(this);
        if (RequestCodeCapture == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                new Thread(() -> {
                    try {
                        CaptureResult captureResult = new CaptureResult();

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
                        CaptureResponse captureResponse = responseGenHelper
                                .getCaptureBiometricsMOSIP(captureResult, captureRequestDto, keystore);
                        generateCaptureResponse(captureResponse, false);
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

                CaptureResponse captureResponse = responseGenHelper
                        .getCaptureBiometricsMOSIP(captureResult, captureRequestDto, keystore);
                generateCaptureResponse(captureResponse, false);
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