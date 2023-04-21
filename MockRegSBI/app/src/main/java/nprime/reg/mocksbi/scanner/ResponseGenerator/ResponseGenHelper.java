package nprime.reg.mocksbi.scanner.ResponseGenerator;

import static nprime.reg.mocksbi.utility.DeviceConstants.CERTIFICATION_L1;
import static nprime.reg.mocksbi.utility.DeviceConstants.ServiceStatus.NOT_REGISTERED;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.bouncycastle.util.Strings;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
                    break;
                case BUSY:
                    error = new Error("111", "Device busy");
                    break;
                case NOT_REGISTERED:
                    error = new Error("100", "Device not registered");
                    break;
                default:
                    error = new Error("0", "Success");
                    break;
            }
            //Purpose for Not Registered device should be empty
            String purpose = currentStatus != NOT_REGISTERED ? deviceUtil.DEVICE_USAGE.getDeviceUsage() : "";

            listOfModalities.forEach(value -> {
                byte[] deviceInfoData = getDeviceInfo(keystore, currentStatus, szTimeStamp, requestType, bioType, purpose);
                String encodedDeviceInfo = keystore.getJwt(deviceInfoData, false);
                infoList.add(new DeviceInfoResponse(encodedDeviceInfo, error));
            });
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

            discoverDto.digitalId = CryptoUtility.getBase64encodeString(payLoad);
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
            info.digitalId = deviceKeystore.getJwt(payLoad.getBytes(), deviceUtil.CERTIFICATION_LEVEL.equals(CERTIFICATION_L1));
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

    public CaptureResponse getCaptureBiometricsMOSIP(CaptureResult captureResult,
                                                     CaptureRequestDto captureRequestDto, DeviceKeystore keystore) {
        CaptureResponse captureResponse = new CaptureResponse();
        try {
            CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();

            List<CaptureDetail> listOfBiometric = new ArrayList<>();
            String previousHash = mdCommonDeviceAPI.digestAsPlainText(mdCommonDeviceAPI.Sha256("".getBytes()));

            for (CaptureRequestDeviceDetailDto bio : captureRequestDto.bio) {
                int captureStatus = captureResult.getStatus();
                int qualityScore = captureResult.getQualityScore();

                DeviceConstants.BioType bioType = DeviceConstants.BioType.getBioType(bio.type);

                if (bioType == null || bioType == DeviceConstants.BioType.BioDevice) {
                    continue;
                }

                for (String bioSubType : captureResult.getBiometricRecords().keySet()) {
                    byte[] bioValue = captureResult.getBiometricRecords().get(bioSubType);
                    CaptureDetail biometricData = getCaptureDetail(mdCommonDeviceAPI.getSerialNumber(), bio.type, bioSubType, captureRequestDto,
                            bioValue, bio.requestedScore, keystore, bioType, previousHash
                            , captureStatus, qualityScore);
                    listOfBiometric.add(biometricData);
                    previousHash = biometricData.hash;
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

    private CaptureDetail getCaptureDetail(String deviceSerialNumber, String bioType, String bioSubType,
                                           CaptureRequestDto captureRequestDto, byte[] captureBioValue,
                                           int requestedScore, DeviceKeystore keystore,
                                           DeviceConstants.BioType bioTypeAtt, String previousHash,
                                           int captureStatus, int capturedQualityScore) throws CertificateException {

        String domainUri;
        String bioValueString;
        String sessionKey;
        String timeStamp;
        String thumbprint;
        String digitalID;
        if (deviceUtil.DEVICE_USAGE == DeviceConstants.DeviceUsage.Authentication) {
            Certificate certificate = keystore.getCertificateToEncryptCaptureBioValue();
            PublicKey publicKey = certificate.getPublicKey();
            Map<String, String> cryptoResult = CryptoUtility.encrypt(publicKey, captureBioValue, captureRequestDto.transactionId);
            byte[] crt = DeviceKeystore.getCertificateThumbprint(certificate);

            bioValueString = cryptoResult.getOrDefault("ENC_DATA", null);
            sessionKey = cryptoResult.getOrDefault("ENC_SESSION_KEY", null);
            timeStamp = cryptoResult.getOrDefault("TIMESTAMP", CryptoUtility.getTimestamp());
            domainUri = captureRequestDto.domainUri == null ? "" : captureRequestDto.domainUri;
            thumbprint = CryptoUtility.toHex(crt).replace("-", "").toUpperCase();

            String payLoad = getDigitalID(deviceSerialNumber, timeStamp, bioTypeAtt);
            digitalID = keystore.getJwt(payLoad.getBytes(StandardCharsets.UTF_8), true);
        } else {
            bioValueString = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(captureBioValue);
            sessionKey = null;
            timeStamp = CryptoUtility.getTimestamp();
            domainUri = null;
            thumbprint = null;

            String payLoad = getDigitalID(deviceSerialNumber, timeStamp, bioTypeAtt);
            digitalID = keystore.getJwt(payLoad.getBytes(StandardCharsets.UTF_8), false);
        }

        //bioSubType should be null for face
        bioSubType = "face".equalsIgnoreCase(bioType) ? null : bioSubType;

        NewBioDto bioDto = getBioResponse(deviceSerialNumber, bioType, bioSubType, captureRequestDto, capturedQualityScore,
                requestedScore, domainUri, bioValueString, timeStamp, digitalID);
        return getMinimalResponse(captureRequestDto.specVersion, bioDto,
                previousHash, captureStatus, keystore, sessionKey, thumbprint);
    }

    private NewBioDto getBioResponse(String deviceSerialNumber, String bioType, String bioSubType,
                                     CaptureRequestDto captureRequestDto, int capturedQualityScore, int requestedScore,
                                     String domainUri, String bioValueStr, String timeStamp, String digitalID) {
        NewBioDto bioResponse = new NewBioDto();
        bioResponse.setBioSubType(bioSubType);
        bioResponse.setBioType(bioType);
        bioResponse.setDeviceCode(deviceSerialNumber);
        //Device service version should be read from file
        bioResponse.setDeviceServiceVersion(DeviceConstants.MDS_VERSION);
        bioResponse.setEnv(captureRequestDto.env);
        bioResponse.setPurpose(deviceUtil.DEVICE_USAGE.getDeviceUsage());
        bioResponse.setRequestedScore(String.valueOf(requestedScore));
        bioResponse.setQualityScore(String.valueOf(capturedQualityScore));
        bioResponse.setTransactionId(captureRequestDto.transactionId);
        bioResponse.setDigitalId(digitalID);
        bioResponse.setTimestamp(timeStamp);
        bioResponse.setDomainUri(domainUri);
        bioResponse.setBioValue(bioValueStr);

        return bioResponse;
    }

    private CaptureDetail getMinimalResponse(String specVersion, NewBioDto data, String previousHash,
                                             int captureStatus, DeviceKeystore keystore,
                                             String sessionKey, String thumbprint) {
        CaptureDetail biometricData = new CaptureDetail();
        try {
            if (CaptureResult.CAPTURE_SUCCESS == captureStatus) {
                biometricData.error = new Error("0", "Success");
            } else if (CaptureResult.CAPTURE_TIMEOUT == captureStatus) {
                biometricData.error = new Error(DeviceErrorCodes.MDS_CAPTURE_TIMEOUT, "Capture Timeout");
            } else {
                biometricData.error = new Error(DeviceErrorCodes.MDS_CAPTURE_FAILED, "Capture Failed");
            }

            biometricData.specVersion = specVersion;

            biometricData.data = keystore.getJwt(oB.writeValueAsBytes(data), false);
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
            biometricData.sessionKey = sessionKey;
            biometricData.thumbprint = thumbprint;
        } catch (Exception ex) {
            ex.printStackTrace();
            biometricData.error = new Error("UNKNOWN", ex.getMessage());
        }
        return biometricData;
    }
}