package nprime.reg.mocksbi.scanner.ResponseGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;

import nprime.reg.mocksbi.dto.CaptureDetail;
import nprime.reg.mocksbi.dto.CaptureRequestDeviceDetailDto;
import nprime.reg.mocksbi.dto.CaptureRequestDto;
import nprime.reg.mocksbi.dto.CaptureResponse;
import nprime.reg.mocksbi.dto.DeviceInfo;
import nprime.reg.mocksbi.dto.DeviceInfoResponse;
import nprime.reg.mocksbi.dto.Error;

import org.bouncycastle.util.Strings;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import nprime.reg.mocksbi.dto.NewBioAuthDto;
import nprime.reg.mocksbi.faceCaptureApi.FaceCaptureResult;
import nprime.reg.mocksbi.secureLib.DeviceKeystore;
import nprime.reg.mocksbi.utility.CommonDeviceAPI;
import nprime.reg.mocksbi.utility.CryptoUtility;
import nprime.reg.mocksbi.utility.DeviceConstants;
import nprime.reg.mocksbi.utility.DeviceErrorCodes;
import nprime.reg.mocksbi.utility.Logger;

/**
 * @author NPrime Technologies
 */

public class ResponseGenHelper {

    static final String REGISTRATION = "Registration";

    static ObjectMapper oB = null;

    static {
        oB = new ObjectMapper();
    }

    public static List<DeviceInfoResponse> getDeviceDriverInfo(
            DeviceConstants.ServiceStatus currentStatus,
            String szTimeStamp, String requestType, DeviceConstants.BioType bioType, DeviceKeystore keystore) {
        List<String> listOfModalities = Collections.singletonList("FAC");

        CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
        List<DeviceInfoResponse> infoList = new ArrayList<>();
        try {
            Error error = new Error("0", "Success");

            listOfModalities.forEach(value -> {
                byte[] deviceInfoData = getDeviceInfo(keystore, currentStatus, szTimeStamp, requestType, bioType);
                String encodedDeviceInfo = keystore.getJwt(deviceInfoData);
                infoList.add(new DeviceInfoResponse(encodedDeviceInfo, error));
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            infoList.add(new DeviceInfoResponse(null, new Error("UNKNOWN",
                    ex.getMessage())));
        }
        return infoList;
    }

    public static List<DeviceInfo> getDeviceDiscovery(
            DeviceConstants.ServiceStatus currentStatus,
            String szTimeStamp, String requestType, DeviceConstants.BioType bioType) {
        List<DeviceInfo> list = new ArrayList();
        try {
            DeviceInfo jsonobject = new DeviceInfo();
            String serialNumber;
            CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
            serialNumber = devCommonDeviceAPI.getSerialNumber();
            jsonobject.deviceId = serialNumber;
            jsonobject.deviceStatus = currentStatus.getType();
            jsonobject.certification = DeviceConstants.CERTIFICATIONLEVEL;
            jsonobject.serviceVersion = DeviceConstants.MDSVERSION;

            switch (bioType) {
                case Face:
                    jsonobject.deviceSubId = new String[]{"0"};
                    break;
                case Finger:
                    jsonobject.deviceSubId = new String[]{"1", "2", "3"};
                    break;
                case Iris:
                    jsonobject.deviceSubId = new String[]{"3"};
                    break;
            }

            jsonobject.callbackId = requestType;
            String payLoad = getDigitalID(serialNumber, szTimeStamp, bioType);
            String digID = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes()); //Base64.encodeToString(payLoad.getBytes(), Base64.NO_PADDING); //

            jsonobject.digitalId = digID;
            jsonobject.deviceCode = serialNumber;
            jsonobject.specVersion = new String[]{DeviceConstants.REGSERVER_VERSION};
            jsonobject.purpose = DeviceConstants.usageStage.getDeviceUsage();

            //jsonobject.error = new Error("0", "Success"));
            list.add(jsonobject);
        } catch (Exception ex) {
            Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: " + "Failed to process exception");
        }
        return list;
    }

    private static byte[] getDeviceInfo(
            DeviceKeystore deviceKeystore,
            DeviceConstants.ServiceStatus currentStatus,
            String szTimeStamp, String requestType, DeviceConstants.BioType bioType) {
        byte[] deviceInfoData = null;
        try {
            byte[] fwVersion;
            fwVersion = DeviceConstants.FIRMWAREVER.getBytes();
            CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
            String serialNumber = devCommonDeviceAPI.getSerialNumber();

            DeviceInfo info = new DeviceInfo();
            info.callbackId = requestType.replace(".Info", "");
            info.certification = DeviceConstants.CERTIFICATIONLEVEL;
            info.deviceCode = serialNumber;
            info.deviceId = serialNumber;
            info.deviceStatus = currentStatus.getType();
            info.deviceSubId = new String[]{"0"};
            String payLoad = getDigitalID(serialNumber, szTimeStamp, bioType);
            info.digitalId = deviceKeystore.getJwt(payLoad.getBytes());
            info.specVersion = new String[]{DeviceConstants.REGSERVER_VERSION};
            info.serviceVersion = DeviceConstants.MDSVERSION;
            info.purpose = DeviceConstants.usageStage.getDeviceUsage();
            info.firmware = new String(fwVersion).replaceAll("\0", "").trim();
            info.env = DeviceConstants.ENVIRONMENT; //DeviceConstants.Environment.Staging.getEnvironment();

            deviceInfoData = oB.writeValueAsString(info).getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            deviceInfoData = "".getBytes();
            ex.printStackTrace();
        }

        return deviceInfoData;
    }


    public static String getDigitalID(String serialNumber, String szTS, DeviceConstants.BioType bioType) {
        String digiID;
        JSONObject jsonobject = new JSONObject();

        try {
            jsonobject.put("serialNo", serialNumber);
            jsonobject.put("make", DeviceConstants.DEVICEMAKE);
            jsonobject.put("model", DeviceConstants.DEVICEMODEL);
            switch (bioType) {
                case Face:
                    jsonobject.put("type", DeviceConstants.BioType.Face.getType());
                    jsonobject.put("deviceSubType", DeviceConstants.FACE_DEVICESUBTYPE);
                    break;
                case Finger:
                    jsonobject.put("type", DeviceConstants.BioType.Finger.getType());
                    jsonobject.put("deviceSubType", DeviceConstants.FINGER_DEVICESUBTYPE);
                    break;
                case Iris:
                    jsonobject.put("type", DeviceConstants.BioType.Iris.getType());
                    jsonobject.put("deviceSubType", DeviceConstants.IRIS_DEVICESUBTYPE);
                    break;
            }
            jsonobject.put("deviceProvider", DeviceConstants.PROVIDERNAME);
            jsonobject.put("deviceProviderId", DeviceConstants.PROVIDERID);
            jsonobject.put("dateTime", szTS);
        } catch (Exception ex) {
            Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: " + "Error occurred while retreiving Digital ID ");
        }

        digiID = jsonobject.toString();
        return digiID;
    }


    public static CaptureResponse getRCaptureBiometricsMOSIP(FaceCaptureResult fcResult,
                                                             CaptureRequestDto captureRequestDto, DeviceKeystore keystore) {

        CaptureResponse responseMap = new CaptureResponse();
        try {
            CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();

            //if (FaceCaptureResult.CAPTURE_SUCCESS == fcResult.getStatus()){
            if (DeviceConstants.environmentList.contains(captureRequestDto.env)
                    && (captureRequestDto.purpose.equalsIgnoreCase(REGISTRATION) || captureRequestDto.purpose.equalsIgnoreCase("Auth"))) {

                List<CaptureDetail> listOfBiometric = new ArrayList<>();
                //String previousHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(""));
                String previousHash = mdCommonDeviceAPI.digestAsPlainText(mdCommonDeviceAPI.Sha256("".getBytes()));

                for (CaptureRequestDeviceDetailDto bio : captureRequestDto.bio) {
                    switch (bio.type.toLowerCase()) {
                        case "face":
                            NewBioAuthDto bioResponse = getBioResponse(mdCommonDeviceAPI, bio.type, "",
                                    captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Face);
                            bioResponse.setBioSubType(null);
                            CaptureDetail biometricData = getAuthMinimalResponse(
                                    captureRequestDto.specVersion, bioResponse, previousHash, fcResult, keystore);
                            listOfBiometric.add(biometricData);
                            previousHash = biometricData.hash;
                            break;
                        case "finger":
                            switch (bio.deviceSubId) {
                                case "1":
                                    CaptureDetail left_index = getAuthMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                                    "Left IndexFinger", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, fcResult, keystore);
                                    listOfBiometric.add(left_index);
                                    previousHash = (String) left_index.hash;
                                    CaptureDetail left_middle = getAuthMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                                    "Left MiddleFinger", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, fcResult, keystore);
                                    listOfBiometric.add(left_middle);
                                    previousHash = (String) left_middle.hash;
                                    CaptureDetail left_ring = getAuthMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                                    "Left RingFinger", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, fcResult, keystore);
                                    listOfBiometric.add(left_ring);
                                    previousHash = (String) left_ring.hash;
                                    CaptureDetail left_little = getAuthMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                                    "Left LittleFinger", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, fcResult, keystore);
                                    listOfBiometric.add(left_little);
                                    previousHash = (String) left_little.hash;
                                    break;
                                case "2":
                                    CaptureDetail right_index = getAuthMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                                    "Right IndexFinger", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, fcResult, keystore);
                                    listOfBiometric.add(right_index);
                                    previousHash = (String) right_index.hash;
                                    CaptureDetail right_middle = getAuthMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                                    "Right MiddleFinger", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, fcResult, keystore);
                                    listOfBiometric.add(right_middle);
                                    previousHash = (String) right_middle.hash;
                                    CaptureDetail right_ring = getAuthMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                                    "Right RingFinger", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, fcResult, keystore);
                                    listOfBiometric.add(right_ring);
                                    previousHash = (String) right_ring.hash;
                                    CaptureDetail right_little = getAuthMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                                    "Right LittleFinger", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, fcResult, keystore);
                                    listOfBiometric.add(right_little);
                                    previousHash = (String) right_little.hash;
                                    break;
                                case "3":
                                    CaptureDetail left_thumb = getAuthMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                                    "Left Thumb", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, fcResult, keystore);
                                    listOfBiometric.add(left_thumb);
                                    previousHash = (String) left_thumb.hash;
                                    CaptureDetail right_thumb = getAuthMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                                    "Right Thumb", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, fcResult, keystore);
                                    listOfBiometric.add(right_thumb);
                                    previousHash = (String) right_thumb.hash;
                                    break;
                            }
                            break;
                        case "iris":
                            CaptureDetail left_iris = getAuthMinimalResponse(
                                    captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                            "Left", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Iris),
                                    previousHash, fcResult, keystore);
                            listOfBiometric.add(left_iris);
                            previousHash = (String) left_iris.hash;
                            CaptureDetail right_iris = getAuthMinimalResponse(
                                    captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
                                            "Right", captureRequestDto, fcResult, bio, keystore, DeviceConstants.BioType.Iris),
                                    previousHash, fcResult, keystore);
                            listOfBiometric.add(right_iris);
                            previousHash = (String) right_iris.hash;
                            break;
                    }
                }
                responseMap.biometrics = listOfBiometric;

            } else {
                CaptureDetail captureDetail = new CaptureDetail();
                captureDetail.error = new Error("101", "Invalid Environment / Purpose");
                responseMap.biometrics = Arrays.asList(captureDetail);
            }
        } catch (Exception exception) {
            CaptureDetail captureDetail = new CaptureDetail();
            captureDetail.specVersion = DeviceConstants.REGSERVER_VERSION;
            captureDetail.error = new Error("UNKNOWN", exception.getMessage());
        }
        return responseMap;
    }

    private static NewBioAuthDto getBioResponse(CommonDeviceAPI mdCommonDeviceAPI, String bioType, String bioSubType,
                                                CaptureRequestDto captureRequestDto, FaceCaptureResult captureResult,
                                                CaptureRequestDeviceDetailDto bioRequest, DeviceKeystore keystore,
                                                DeviceConstants.BioType bioTypeAtt) {

        List<String> exceptions = Arrays.asList(captureRequestDto.bio.get(0).exception == null ?
                new String[]{} : captureRequestDto.bio.get(0).exception);
        boolean isMarkedAsException = exceptions.contains(bioSubType);

        String timestamp = CryptoUtility.getTimestamp();
        NewBioAuthDto bioResponse = new NewBioAuthDto();
        bioResponse.setBioSubType(bioSubType);
        bioResponse.setBioType(bioType);
        bioResponse.setDeviceCode(mdCommonDeviceAPI.getSerialNumber());
        //Device service version should be read from file
        bioResponse.setDeviceServiceVersion(DeviceConstants.MDSVERSION);
        bioResponse.setEnv(captureRequestDto.env);
        bioResponse.setPurpose(DeviceConstants.usageStage.getDeviceUsage());
        bioResponse.setRequestedScore(String.valueOf(bioRequest.requestedScore));
        bioResponse.setQualityScore(isMarkedAsException ? "0" : String.valueOf(captureResult.getQualityScore()));
        bioResponse.setTransactionId(captureRequestDto.transactionId);
        String payLoad = getDigitalID(mdCommonDeviceAPI.getSerialNumber(), timestamp, bioTypeAtt);
        String digitalID = keystore.getJwt(payLoad.getBytes(StandardCharsets.UTF_8));
        bioResponse.setDigitalId(digitalID);

        bioResponse.setTimestamp(timestamp);
        //bioResponse.setCount("1");

        bioResponse.setBioValue(java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString((captureResult.getBiometricRecords().get(bioSubType) == null || isMarkedAsException) ?
                        "".getBytes(StandardCharsets.UTF_8) : captureResult.getBiometricRecords().get(bioSubType)));
        return bioResponse;
    }


    private static CaptureDetail getAuthMinimalResponse(String specVersion, NewBioAuthDto data,
                                                        String previousHash, FaceCaptureResult fcResult, DeviceKeystore keystore) {
        CaptureDetail biometricData = new CaptureDetail();
        try {
            if (FaceCaptureResult.CAPTURE_SUCCESS == fcResult.getStatus()) {
                biometricData.error = new Error("0", "Success");
            } else if (FaceCaptureResult.CAPTURE_TIMEOUT == fcResult.getStatus()) {
                biometricData.error = new Error(DeviceErrorCodes.MDS_CAPTURE_TIMEOUT, "Capture Timeout");
            } else {
                biometricData.error = new Error(DeviceErrorCodes.MDS_CAPTURE_FAILED, "Capture Failed");
            }

            biometricData.specVersion = specVersion;
            String dataBlock = keystore.getJwt(oB.writeValueAsBytes(data));

            biometricData.data = dataBlock;
            byte[] previousBioDataHash;
            byte[] currentBioDataHash;

            currentBioDataHash = CryptoUtility.generateHash(java.util.Base64.getUrlDecoder().decode(data.getBioValue()));

            if (previousHash == null || previousHash.trim().length() == 0) {
                byte[] previousDataByteArr = Strings.toUTF8ByteArray("");
                previousBioDataHash = CryptoUtility.generateHash(previousDataByteArr);
            } else {
                previousBioDataHash = CryptoUtility.decodeHex(previousHash);
            }

            byte[] finalBioDataHash = new byte[previousBioDataHash.length + currentBioDataHash.length];
            System.arraycopy(previousBioDataHash, 0, finalBioDataHash, 0, previousBioDataHash.length);
            System.arraycopy(currentBioDataHash, 0, finalBioDataHash, previousBioDataHash.length, currentBioDataHash.length);
            String finalHashDataHex = CryptoUtility.toHex(CryptoUtility.generateHash(finalBioDataHash));

            biometricData.hash = finalHashDataHex;

        } catch (Exception ex) {
            ex.printStackTrace();
            biometricData.error = new Error("UNKNOWN", ex.getMessage());
        }
        return biometricData;
    }
}