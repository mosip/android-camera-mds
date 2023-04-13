package nprime.reg.mocksbi.scanner.ResponseGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.bouncycastle.util.Strings;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nprime.reg.mocksbi.dto.CaptureDetail;
import nprime.reg.mocksbi.dto.CaptureRequestDeviceDetailDto;
import nprime.reg.mocksbi.dto.CaptureRequestDto;
import nprime.reg.mocksbi.dto.CaptureResponse;
import nprime.reg.mocksbi.dto.DeviceInfo;
import nprime.reg.mocksbi.dto.DeviceInfoResponse;
import nprime.reg.mocksbi.dto.DiscoverDto;
import nprime.reg.mocksbi.dto.Error;
import nprime.reg.mocksbi.dto.NewBioDto;
import nprime.reg.mocksbi.faceCaptureApi.CaptureResult;
import nprime.reg.mocksbi.secureLib.DeviceKeystore;
import nprime.reg.mocksbi.utility.CommonDeviceAPI;
import nprime.reg.mocksbi.utility.CryptoUtility;
import nprime.reg.mocksbi.utility.DeviceConstants;
import nprime.reg.mocksbi.utility.DeviceErrorCodes;
import nprime.reg.mocksbi.utility.DeviceUtil;
import nprime.reg.mocksbi.utility.Logger;

/**
 * @author NPrime Technologies
 */

public class ResponseGenHelper {
    ObjectMapper oB;
    DeviceUtil deviceUtil;

    public ResponseGenHelper(DeviceUtil _deviceUtil) {
        oB = new ObjectMapper();
        deviceUtil = _deviceUtil;
    }

    public List<DeviceInfoResponse> getDeviceDriverInfo(DeviceConstants.ServiceStatus currentStatus,
                                                        String szTimeStamp, String requestType,
                                                        DeviceConstants.BioType bioType, DeviceKeystore keystore) {
        List<String> listOfModalities = Collections.singletonList("FAC");

        List<DeviceInfoResponse> infoList = new ArrayList<>();
        try {
            Error error;
            switch (currentStatus) {
                case NOT_READY:
                    error = new Error("110", "Device not ready");
                    listOfModalities.forEach(value -> {
                        byte[] deviceInfoData = getDeviceInfo(keystore, currentStatus, szTimeStamp, requestType, bioType, deviceUtil.DEVICE_USAGE.getDeviceUsage());
                        String encodedDeviceInfo = keystore.getJwt(deviceInfoData);
                        infoList.add(new DeviceInfoResponse(encodedDeviceInfo, error));
                    });
                    break;
                case BUSY:
                    error = new Error("111", "Device busy");
                    listOfModalities.forEach(value -> {
                        byte[] deviceInfoData = getDeviceInfo(keystore, currentStatus, szTimeStamp, requestType, bioType, deviceUtil.DEVICE_USAGE.getDeviceUsage());
                        String encodedDeviceInfo = keystore.getJwt(deviceInfoData);
                        infoList.add(new DeviceInfoResponse(encodedDeviceInfo, error));
                    });
                    break;
                case NOT_REGISTERED:
                    error = new Error("100", "Device not registered");
                    listOfModalities.forEach(value -> {
                        byte[] deviceInfoData = getDeviceInfo(keystore, currentStatus, szTimeStamp, requestType, bioType, "");
                        String encodedDeviceInfo = keystore.getJwt(deviceInfoData);
                        infoList.add(new DeviceInfoResponse(encodedDeviceInfo, error));
                    });
                    break;
                default:
                    error = new Error("0", "Success");
                    listOfModalities.forEach(value -> {
                        byte[] deviceInfoData = getDeviceInfo(keystore, currentStatus, szTimeStamp, requestType, bioType, deviceUtil.DEVICE_USAGE.getDeviceUsage());
                        String encodedDeviceInfo = keystore.getJwt(deviceInfoData);
                        infoList.add(new DeviceInfoResponse(encodedDeviceInfo, error));
                    });
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            infoList.add(new DeviceInfoResponse(null, new Error("UNKNOWN",
                    ex.getMessage())));
        }
        return infoList;
    }

    public List<DiscoverDto> getDeviceDiscovery(
            DeviceConstants.ServiceStatus currentStatus,
            String szTimeStamp, String requestType, DeviceConstants.BioType bioType) {
        List<DiscoverDto> list = new ArrayList<>();
        try {
            DiscoverDto discoverDto = new DiscoverDto();
            CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
            String serialNumber = devCommonDeviceAPI.getSerialNumber();
            discoverDto.deviceId = serialNumber;
            discoverDto.deviceStatus = currentStatus.getStatus();
            discoverDto.certification = deviceUtil.CERTIFICATION_LEVEL;
            discoverDto.serviceVersion = DeviceConstants.MDS_VERSION;

            switch (bioType) {
                case Face:
                    discoverDto.deviceSubId = new String[]{"0"};
                    break;
                case Finger:
                    discoverDto.deviceSubId = new String[]{"1", "2", "3"};
                    break;
                case Iris:
                    discoverDto.deviceSubId = new String[]{"3"};
                    break;
            }

            switch (currentStatus) {
                case NOT_READY:
                    discoverDto.error = new Error("110", "Device not ready");
                    break;
                case BUSY:
                    discoverDto.error = new Error("111", "Device busy");
                    break;
                case NOT_REGISTERED:
                    discoverDto.deviceId = "";
                    discoverDto.deviceCode = "";
                    discoverDto.purpose = "";
                    discoverDto.error = new Error("100", "Device not registered");
                    break;
                default:
                    discoverDto.error = new Error("0", "Success");
                    break;
            }

            discoverDto.callbackId = requestType;
            String payLoad = getDigitalID(serialNumber, szTimeStamp, bioType);

            discoverDto.digitalId = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes());
            discoverDto.deviceCode = serialNumber;
            discoverDto.specVersion = new String[]{DeviceConstants.REG_SERVER_VERSION};
            discoverDto.purpose = deviceUtil.DEVICE_USAGE.getDeviceUsage();
            discoverDto.error = new Error("0", "Success");

            list.add(discoverDto);
        } catch (Exception ex) {
            Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: " + "Failed to process exception");
        }
        return list;
    }

    private byte[] getDeviceInfo(
            DeviceKeystore deviceKeystore,
            DeviceConstants.ServiceStatus currentStatus,
            String szTimeStamp, String requestType, DeviceConstants.BioType bioType,
            String deviceUsage) {
        byte[] deviceInfoData = null;
        try {
            byte[] fwVersion;
            fwVersion = DeviceConstants.FIRMWARE_VER.getBytes();
            CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
            String serialNumber = devCommonDeviceAPI.getSerialNumber();

            DeviceInfo info = new DeviceInfo();
            info.callbackId = requestType.replace(".Info", "");
            info.certification = deviceUtil.CERTIFICATION_LEVEL;
            info.deviceCode = serialNumber;
            info.deviceId = serialNumber;
            info.deviceStatus = currentStatus.getStatus();
            info.deviceSubId = new String[]{"0"};
            String payLoad = getDigitalID(serialNumber, szTimeStamp, bioType);
            info.digitalId = deviceKeystore.getJwt(payLoad.getBytes());
            info.specVersion = new String[]{DeviceConstants.REG_SERVER_VERSION};
            info.serviceVersion = DeviceConstants.MDS_VERSION;
            info.purpose = deviceUsage;
            info.firmware = new String(fwVersion).replaceAll("\0", "").trim();
            info.env = DeviceConstants.ENVIRONMENT;

            deviceInfoData = oB.writeValueAsString(info).getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            deviceInfoData = "".getBytes();
            ex.printStackTrace();
        }

        return deviceInfoData;
    }

    public String getDigitalID(String serialNumber, String szTS, DeviceConstants.BioType bioType) {
        String digiID;
        JSONObject jsonobject = new JSONObject();

        try {
            jsonobject.put("serialNo", serialNumber);
            jsonobject.put("make", DeviceConstants.DEVICE_MAKE);
            jsonobject.put("model", DeviceConstants.DEVICE_MODEL);
            switch (bioType) {
                case Face:
                    jsonobject.put("type", DeviceConstants.BioType.Face.getBioType());
                    jsonobject.put("deviceSubType", deviceUtil.FACE_DEVICE_SUBTYPE);
                    break;
                case Finger:
                    jsonobject.put("type", DeviceConstants.BioType.Finger.getBioType());
                    jsonobject.put("deviceSubType", deviceUtil.FINGER_DEVICE_SUBTYPE);
                    break;
                case Iris:
                    jsonobject.put("type", DeviceConstants.BioType.Iris.getBioType());
                    jsonobject.put("deviceSubType", deviceUtil.IRIS_DEVICE_SUBTYPE);
                    break;
            }
            jsonobject.put("deviceProvider", DeviceConstants.PROVIDER_NAME);
            jsonobject.put("deviceProviderId", DeviceConstants.PROVIDER_ID);
            jsonobject.put("dateTime", szTS);
        } catch (Exception ex) {
            Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: " + "Error occurred while retreiving Digital ID ");
        }

        digiID = jsonobject.toString();
        return digiID;
    }

    public CaptureResponse getRCaptureBiometricsMOSIP(CaptureResult captureResult,
                                                      CaptureRequestDto captureRequestDto, DeviceKeystore keystore) {

        CaptureResponse captureResponse = new CaptureResponse();
        try {
            CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();

            List<CaptureDetail> listOfBiometric = new ArrayList<>();
            String previousHash = mdCommonDeviceAPI.digestAsPlainText(mdCommonDeviceAPI.Sha256("".getBytes()));

            for (CaptureRequestDeviceDetailDto bio : captureRequestDto.bio) {
                List<String> exceptions = Arrays.asList(bio.exception == null ?
                        new String[]{} : bio.exception);

                switch (bio.type.toLowerCase()) {
                    case "face":
                        NewBioDto bioResponse = getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type, "",
                                captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Face);
                        bioResponse.setBioSubType(null);
                        CaptureDetail biometricData = getMinimalResponse(
                                captureRequestDto.specVersion, bioResponse, previousHash, captureResult, keystore);
                        listOfBiometric.add(biometricData);
                        previousHash = biometricData.hash;
                        break;
                    case "finger":
                        switch (bio.deviceSubId) {
                            case "1":
                                if (!exceptions.contains(DeviceConstants.BIO_NAME_LEFT_INDEX)) {
                                    CaptureDetail left_index = getMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                                    DeviceConstants.BIO_NAME_LEFT_INDEX, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, captureResult, keystore);
                                    listOfBiometric.add(left_index);
                                    previousHash = left_index.hash;
                                }
                                if (!exceptions.contains(DeviceConstants.BIO_NAME_LEFT_MIDDLE)) {
                                    CaptureDetail left_middle = getMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                                    DeviceConstants.BIO_NAME_LEFT_MIDDLE, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, captureResult, keystore);
                                    listOfBiometric.add(left_middle);
                                    previousHash = left_middle.hash;
                                }
                                if (!exceptions.contains(DeviceConstants.BIO_NAME_LEFT_RING)) {
                                    CaptureDetail left_ring = getMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                                    DeviceConstants.BIO_NAME_LEFT_RING, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, captureResult, keystore);
                                    listOfBiometric.add(left_ring);
                                    previousHash = left_ring.hash;
                                }
                                if (!exceptions.contains(DeviceConstants.BIO_NAME_LEFT_LITTLE)) {
                                    CaptureDetail left_little = getMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                                    DeviceConstants.BIO_NAME_LEFT_LITTLE, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, captureResult, keystore);
                                    listOfBiometric.add(left_little);
                                    previousHash = left_little.hash;
                                }
                                break;
                            case "2":
                                if (!exceptions.contains(DeviceConstants.BIO_NAME_RIGHT_INDEX)) {
                                    CaptureDetail right_index = getMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                                    DeviceConstants.BIO_NAME_RIGHT_INDEX, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, captureResult, keystore);
                                    listOfBiometric.add(right_index);
                                    previousHash = right_index.hash;
                                }
                                if (!exceptions.contains(DeviceConstants.BIO_NAME_RIGHT_MIDDLE)) {
                                    CaptureDetail right_middle = getMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                                    DeviceConstants.BIO_NAME_RIGHT_MIDDLE, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, captureResult, keystore);
                                    listOfBiometric.add(right_middle);
                                    previousHash = right_middle.hash;
                                }
                                if (!exceptions.contains(DeviceConstants.BIO_NAME_RIGHT_RING)) {
                                    CaptureDetail right_ring = getMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                                    DeviceConstants.BIO_NAME_RIGHT_RING, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, captureResult, keystore);
                                    listOfBiometric.add(right_ring);
                                    previousHash = right_ring.hash;
                                }
                                if (!exceptions.contains(DeviceConstants.BIO_NAME_RIGHT_LITTLE)) {
                                    CaptureDetail right_little = getMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                                    DeviceConstants.BIO_NAME_RIGHT_LITTLE, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, captureResult, keystore);
                                    listOfBiometric.add(right_little);
                                    previousHash = right_little.hash;
                                }
                                break;
                            case "3":
                                if (!exceptions.contains(DeviceConstants.BIO_NAME_LEFT_THUMB)) {
                                    CaptureDetail left_thumb = getMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                                    DeviceConstants.BIO_NAME_LEFT_THUMB, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, captureResult, keystore);
                                    listOfBiometric.add(left_thumb);
                                    previousHash = left_thumb.hash;
                                }
                                if (!exceptions.contains(DeviceConstants.BIO_NAME_RIGHT_THUMB)) {
                                    CaptureDetail right_thumb = getMinimalResponse(
                                            captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                                    DeviceConstants.BIO_NAME_RIGHT_THUMB, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Finger),
                                            previousHash, captureResult, keystore);
                                    listOfBiometric.add(right_thumb);
                                    previousHash = right_thumb.hash;
                                }
                                break;
                        }
                        break;
                    case "iris":
                        if (!exceptions.contains(DeviceConstants.BIO_NAME_LEFT_IRIS)) {
                            CaptureDetail left_iris = getMinimalResponse(
                                    captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                            DeviceConstants.BIO_NAME_LEFT_IRIS, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Iris),
                                    previousHash, captureResult, keystore);
                            listOfBiometric.add(left_iris);
                            previousHash = left_iris.hash;
                        }
                        if (!exceptions.contains(DeviceConstants.BIO_NAME_RIGHT_IRIS)) {
                            CaptureDetail right_iris = getMinimalResponse(
                                    captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI.getSerialNumber(), bio.type,
                                            DeviceConstants.BIO_NAME_RIGHT_IRIS, captureRequestDto, captureResult, bio.requestedScore, keystore, DeviceConstants.BioType.Iris),
                                    previousHash, captureResult, keystore);
                            listOfBiometric.add(right_iris);
                            previousHash = right_iris.hash;
                        }
                        break;
                }
            }
            captureResponse.biometrics = listOfBiometric;
        } catch (Exception exception) {
            CaptureDetail captureDetail = new CaptureDetail();
            captureDetail.specVersion = DeviceConstants.REG_SERVER_VERSION;
            captureDetail.error = new Error("UNKNOWN", exception.getMessage());
        }
        return captureResponse;
    }

    private NewBioDto getBioResponse(String deviceSerialNumber, String bioType, String bioSubType,
                                     CaptureRequestDto captureRequestDto, CaptureResult captureResult,
                                     int requestedScore, DeviceKeystore keystore,
                                     DeviceConstants.BioType bioTypeAtt) {
        String timestamp = CryptoUtility.getTimestamp();
        NewBioDto bioResponse = new NewBioDto();
        bioResponse.setBioSubType(bioSubType);
        bioResponse.setBioType(bioType);
        bioResponse.setDeviceCode(deviceSerialNumber);
        //Device service version should be read from file
        bioResponse.setDeviceServiceVersion(DeviceConstants.MDS_VERSION);
        bioResponse.setEnv(captureRequestDto.env);
        bioResponse.setPurpose(deviceUtil.DEVICE_USAGE.getDeviceUsage());
        bioResponse.setRequestedScore(String.valueOf(requestedScore));
        bioResponse.setQualityScore(String.valueOf(captureResult.getQualityScore()));
        bioResponse.setTransactionId(captureRequestDto.transactionId);
        String payLoad = getDigitalID(deviceSerialNumber, timestamp, bioTypeAtt);
        String digitalID = keystore.getJwt(payLoad.getBytes(StandardCharsets.UTF_8));
        bioResponse.setDigitalId(digitalID);

        bioResponse.setTimestamp(timestamp);

        bioResponse.setBioValue(java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString((captureResult.getBiometricRecords().get(bioSubType) == null) ?
                        "".getBytes(StandardCharsets.UTF_8) : captureResult.getBiometricRecords().get(bioSubType)));
        return bioResponse;
    }

    private CaptureDetail getMinimalResponse(String specVersion, NewBioDto data,
                                             String previousHash, CaptureResult fcResult, DeviceKeystore keystore) {
        CaptureDetail biometricData = new CaptureDetail();
        try {
            if (CaptureResult.CAPTURE_SUCCESS == fcResult.getStatus()) {
                biometricData.error = new Error("0", "Success");
            } else if (CaptureResult.CAPTURE_TIMEOUT == fcResult.getStatus()) {
                biometricData.error = new Error(DeviceErrorCodes.MDS_CAPTURE_TIMEOUT, "Capture Timeout");
            } else {
                biometricData.error = new Error(DeviceErrorCodes.MDS_CAPTURE_FAILED, "Capture Failed");
            }

            biometricData.specVersion = specVersion;

            biometricData.data = keystore.getJwt(oB.writeValueAsBytes(data));
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

            biometricData.hash = CryptoUtility.toHex(CryptoUtility.generateHash(finalBioDataHash));

        } catch (Exception ex) {
            ex.printStackTrace();
            biometricData.error = new Error("UNKNOWN", ex.getMessage());
        }
        return biometricData;
    }
}