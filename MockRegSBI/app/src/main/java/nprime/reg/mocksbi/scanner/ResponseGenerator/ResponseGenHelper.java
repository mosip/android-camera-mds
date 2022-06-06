package nprime.reg.mocksbi.scanner.ResponseGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.DataObjects.*;

import nprime.reg.mocksbi.dto.CaptureDetail;
import nprime.reg.mocksbi.dto.CaptureResponse;
import nprime.reg.mocksbi.dto.DeviceInfoResponse;
import nprime.reg.mocksbi.dto.Error;
import org.bouncycastle.util.Strings;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import nprime.reg.mocksbi.faceCaptureApi.FaceCaptureResult;
import nprime.reg.mocksbi.faceCaptureApi.KeysNotFoundException;
import nprime.reg.mocksbi.secureLib.DeviceKeystore;
import nprime.reg.mocksbi.utility.CommonDeviceAPI;
import nprime.reg.mocksbi.utility.CryptoUtility;
import nprime.reg.mocksbi.utility.DeviceConstants;
import nprime.reg.mocksbi.utility.DeviceErrorCodes;
import nprime.reg.mocksbi.utility.Logger;

/**
 * @author NPrime Technologies
 */

public class ResponseGenHelper 
{

	static final String REGISTRATION = "Registration";


	static ObjectMapper oB = null;

	public static List<DeviceInfoResponse> getDeviceDriverInfo(
			DeviceConstants.ServiceStatus currentStatus,
			String szTimeStamp, String requestType, DeviceConstants.BioType bioType)
	{
		List<String> listOfModalities = Collections.singletonList("FAC");

		CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
		List<DeviceInfoResponse> infoList = new ArrayList<>();
		try
		{
			Error error = new Error("0", "Success");

			DeviceKeystore deviceKeystore = new DeviceKeystore();
			X509Certificate certificate = deviceKeystore.getX509Certificate();

			if (null == certificate){
				throw new KeysNotFoundException("Device Key not found");
			}

			String certStr = java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());

			byte[] headerData = devCommonDeviceAPI.getHeaderfromCert(certStr);

			listOfModalities.forEach(value -> {
				byte[] deviceInfoData = getDeviceInfo(deviceKeystore, currentStatus, szTimeStamp, requestType, bioType);

				String enCodedHeader =  java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(headerData);
				String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(deviceInfoData);

				byte [] inSignString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

				byte[] signature = "".getBytes();
				if (isValidDeviceforCertificate()){
					signature = deviceKeystore.getSignature(inSignString);
				}
				String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

				String encodedDeviceInfo = enCodedHeader + "." + enCodedPayLoad + "." + enCodedSignature;//.replace("=", "");

				infoList.add(new DeviceInfoResponse(encodedDeviceInfo, error));
			});
		}
		catch (KeysNotFoundException keyException){
			//DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Device Key not found, Please restart MDS.");
			Logger.e(DeviceConstants.LOG_TAG, "Device Key not found, Please restart MDS.");

			infoList.add(new DeviceInfoResponse(null, new Error(DeviceErrorCodes.MDS_DEVICEKEYS_NOT_FOUND,
					keyException.getMessage())));
		}		
		catch (CertificateException cex){
			//DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Invalid Device Certificate, Please restart MDS.");
			Logger.e(DeviceConstants.LOG_TAG, "Invalid Device Certificate, Please restart MDS.");

			infoList.add(new DeviceInfoResponse(null, new Error(DeviceErrorCodes.MDS_INVALID_CERTIFICATE,
					cex.getMessage())));
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			infoList.add(new DeviceInfoResponse(null, new Error("UNKNOWN",
					ex.getMessage())));
		}
		return  infoList;
	}

	public static List<DeviceInformation> getDeviceDiscovery(
			DeviceConstants.ServiceStatus currentStatus,
			String szTimeStamp, String requestType, DeviceConstants.BioType bioType) {
		List<DeviceInformation> list = new ArrayList();
		try
		{
			DeviceInformation jsonobject = new DeviceInformation();
			String serialNumber;
			CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
			serialNumber = devCommonDeviceAPI.getSerialNumber();
			jsonobject.deviceId = serialNumber;
			jsonobject.deviceStatus = currentStatus.getType();
			jsonobject.certification = DeviceConstants.CERTIFICATIONLEVEL;
			jsonobject.serviceVersion = DeviceConstants.MDSVERSION;

			switch (bioType) {
				case Face: jsonobject.deviceSubId = new int[]{0};break;
				case Finger: jsonobject.deviceSubId = new int[]{1,2,3}; break;
				case Iris: jsonobject.deviceSubId = new int[]{3};break;
			}

			jsonobject.callbackId = requestType;
			String payLoad = getDigitalID(serialNumber, szTimeStamp, bioType);
			String digID = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes()); //Base64.encodeToString(payLoad.getBytes(), Base64.NO_PADDING); //

			jsonobject.digitalId = digID;
			jsonobject.deviceCode = serialNumber;
			jsonobject.specVersion = new String[] {DeviceConstants.REGSERVER_VERSION};
			jsonobject.purpose = DeviceConstants.usageStage.getDeviceUsage();

			//jsonobject.error = new Error("0", "Success"));
			list.add(jsonobject);
		}
		catch(Exception ex)
		{
			Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: " + "Failed to process exception");
		}
		return list;
	}	

	private static byte[] getDeviceInfo(
			DeviceKeystore deviceKeystore,
			DeviceConstants.ServiceStatus currentStatus,
			String szTimeStamp, String requestType, DeviceConstants.BioType bioType)
	{
		byte[] deviceInfoData = null;
		try{
			if (oB == null){
				oB = new ObjectMapper();
			}

			byte[] fwVersion;
			fwVersion = DeviceConstants.FIRMWAREVER.getBytes();
			CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
			String serialNumber = devCommonDeviceAPI.getSerialNumber();

			DeviceInformation info = new DeviceInformation();
			info.callbackId = requestType.replace(".Info", "");
			info.certification = DeviceConstants.CERTIFICATIONLEVEL;
			info.deviceCode = serialNumber;
			info.deviceId = serialNumber;
			info.deviceStatus =  currentStatus.getType();
			info.deviceSubId = new int[]{0};

			//byte[] x509MOSIPCertByte = devCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.DEVICE_CERT);
			X509Certificate certificate = deviceKeystore.getX509Certificate();
			String certStr = java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());
			//Base64.encodeToString(certificate.getEncoded(), Base64.NO_WRAP);

			String enCodedHeader =  java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(devCommonDeviceAPI.getHeaderfromCert(certStr));//.replace("=", ""); //Base64.encodeToString(devCommonDeviceAPI.getHeaderfromCert(certStr), Base64.NO_PADDING);
			String payLoad = getDigitalID(serialNumber, szTimeStamp, bioType);
			String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes());//.replace("=", ""); //Base64.encodeToString(payLoad.getBytes(), Base64.NO_PADDING);
			byte [] inSignString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

			byte[] signature = "".getBytes();
			if (isValidDeviceforCertificate()){
				signature = deviceKeystore.getSignature(inSignString);
			}

			String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature); //Base64.encodeToString(signature, Base64.NO_PADDING);

			info.digitalId = enCodedHeader + "." + enCodedPayLoad + "." + enCodedSignature;//.replace("=", "");
			info.specVersion = new String[]{DeviceConstants.REGSERVER_VERSION};
			info.serviceVersion = DeviceConstants.MDSVERSION;
			info.purpose = DeviceConstants.usageStage.getDeviceUsage();
			info.firmware = new String(fwVersion).replaceAll("\0", "").trim();
			info.env = DeviceConstants.ENVIRONMENT; //DeviceConstants.Environment.Staging.getEnvironment();

			deviceInfoData =  oB.writeValueAsString(info).getBytes();

		} catch (IOException e) {
			e.printStackTrace();
		}
		catch(Exception ex)
		{
			deviceInfoData = "".getBytes();
			ex.printStackTrace();
		}

		return deviceInfoData;
	}


	public static String getDigitalID(String serialNumber, String szTS, DeviceConstants.BioType bioType) {
		String digiID;
		JSONObject jsonobject = new JSONObject();

		try{			
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
		}
		catch(Exception ex)
		{
			Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: " + "Error occurred while retreiving Digital ID ");
		}

		digiID = jsonobject.toString();
		return digiID;
	}

	/**
	 * Gets the file content.
	 *
	 * @param fis      the fis
	 * @param encoding the encoding
	 * @return the file content
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static String getFileContent(FileInputStream fis, String encoding) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(fis, encoding))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			return sb.toString();
		}
	}

	public static CaptureResponse getRCaptureBiometricsMOSIP(FaceCaptureResult fcResult,
													CaptureRequestDto captureRequestDto) {

		CaptureResponse responseMap = new CaptureResponse();
		try {

			if (oB == null)
				oB = new ObjectMapper();
			CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();

			//if (FaceCaptureResult.CAPTURE_SUCCESS == fcResult.getStatus()){
			if (DeviceConstants.environmentList.contains(captureRequestDto.env)
					&& captureRequestDto.purpose.equalsIgnoreCase(REGISTRATION)) {

				List<CaptureDetail> listOfBiometric = new ArrayList<>();
				//String previousHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(""));
				String previousHash = mdCommonDeviceAPI.digestAsPlainText(mdCommonDeviceAPI.Sha256("".getBytes()));

				for (CaptureRequestDeviceDetailDto bio : captureRequestDto.mosipBioRequest) {
					switch (bio.type.toLowerCase()) {
						case "face":
							NewBioAuthDto bioResponse = getBioResponse(mdCommonDeviceAPI, bio.type, "",
									captureRequestDto, fcResult, bio);
							CaptureDetail biometricData = getAuthMinimalResponse(
									captureRequestDto.specVersion, bioResponse, previousHash, fcResult);
							listOfBiometric.add(biometricData);
							previousHash = biometricData.hash;
							break;
						case "finger":
							switch (bio.deviceSubId) {
								case "1" :
									CaptureDetail left_index = getAuthMinimalResponse(
											captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
													"Left IndexFinger", captureRequestDto, fcResult, bio), previousHash, fcResult);
									listOfBiometric.add(left_index);
									previousHash = (String) left_index.hash;
									CaptureDetail left_middle = getAuthMinimalResponse(
											captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
													"Left MiddleFinger", captureRequestDto, fcResult, bio), previousHash, fcResult);
									listOfBiometric.add(left_middle);
									previousHash = (String) left_middle.hash;
									CaptureDetail left_ring = getAuthMinimalResponse(
											captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
													"Left RingFinger", captureRequestDto, fcResult, bio), previousHash, fcResult);
									listOfBiometric.add(left_ring);
									previousHash = (String) left_ring.hash;
									CaptureDetail left_little = getAuthMinimalResponse(
											captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
													"Left LittleFinger", captureRequestDto, fcResult, bio), previousHash, fcResult);
									listOfBiometric.add(left_little);
									previousHash = (String) left_little.hash;
									break;
								case "2":
									CaptureDetail right_index = getAuthMinimalResponse(
											captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
													"Right IndexFinger", captureRequestDto, fcResult, bio), previousHash, fcResult);
									listOfBiometric.add(right_index);
									previousHash = (String) right_index.hash;
									CaptureDetail right_middle = getAuthMinimalResponse(
											captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
													"Right MiddleFinger", captureRequestDto, fcResult, bio), previousHash, fcResult);
									listOfBiometric.add(right_middle);
									previousHash = (String) right_middle.hash;
									CaptureDetail right_ring = getAuthMinimalResponse(
											captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
													"Right RingFinger", captureRequestDto, fcResult, bio), previousHash, fcResult);
									listOfBiometric.add(right_ring);
									previousHash = (String) right_ring.hash;
									CaptureDetail right_little = getAuthMinimalResponse(
											captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
													"Right LittleFinger", captureRequestDto, fcResult, bio), previousHash, fcResult);
									listOfBiometric.add(right_little);
									previousHash = (String) right_little.hash;
									break;
								case "3":
									CaptureDetail left_thumb = getAuthMinimalResponse(
											captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
													"Left Thumb", captureRequestDto, fcResult, bio), previousHash, fcResult);
									listOfBiometric.add(left_thumb);
									previousHash = (String) left_thumb.hash;
									CaptureDetail right_thumb = getAuthMinimalResponse(
											captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
													"Right Thumb", captureRequestDto, fcResult, bio), previousHash, fcResult);
									listOfBiometric.add(right_thumb);
									previousHash = (String) right_thumb.hash;
									break;
							}
							break;
						case "iris":
							CaptureDetail left_iris = getAuthMinimalResponse(
									captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
											"Left", captureRequestDto, fcResult, bio), previousHash, fcResult);
							listOfBiometric.add(left_iris);
							previousHash = (String) left_iris.hash;
							CaptureDetail right_iris = getAuthMinimalResponse(
									captureRequestDto.specVersion, getBioResponse(mdCommonDeviceAPI, bio.type,
											"Right", captureRequestDto, fcResult, bio), previousHash, fcResult);
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
		}
		catch (KeysNotFoundException kexception){
			//DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Device Key not found, Please restart MDS.");
			Logger.e(DeviceConstants.LOG_TAG, "Device Key not found, Please restart MDS.");

			CaptureDetail captureDetail = new CaptureDetail();
			captureDetail.specVersion = DeviceConstants.REGSERVER_VERSION;
			captureDetail.error = new Error(DeviceErrorCodes.MDS_DEVICEKEYS_NOT_FOUND, kexception.getMessage());
			responseMap.biometrics = Arrays.asList(captureDetail);
		}
		catch (CertificateException cex){
			//DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Invalid Device Certificate, Please restart MDS.");
			Logger.e(DeviceConstants.LOG_TAG, "Invalid Device Certificate, Please restart MDS.");

			CaptureDetail captureDetail = new CaptureDetail();
			captureDetail.specVersion = DeviceConstants.REGSERVER_VERSION;
			captureDetail.error = new Error(DeviceErrorCodes.MDS_INVALID_CERTIFICATE, cex.getMessage());
			responseMap.biometrics = Arrays.asList(captureDetail);
		}
		catch (Exception exception) {
			CaptureDetail captureDetail = new CaptureDetail();
			captureDetail.specVersion = DeviceConstants.REGSERVER_VERSION;
			captureDetail.error = new Error("UNKNOWN", exception.getMessage());
		}
		return responseMap;
	}

	private static NewBioAuthDto getBioResponse(CommonDeviceAPI mdCommonDeviceAPI, String bioType, String bioSubType,
												CaptureRequestDto captureRequestDto, FaceCaptureResult captureResult,
												CaptureRequestDeviceDetailDto bioRequest) throws CertificateEncodingException, KeysNotFoundException {
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
		bioResponse.setQualityScore(String.valueOf(captureResult.getQualityScore()));
		bioResponse.setTransactionId(captureRequestDto.transactionId);

		DeviceKeystore deviceKeystore = new DeviceKeystore();
		X509Certificate certificate = deviceKeystore.getX509Certificate();

		if (null == certificate){
			throw new KeysNotFoundException("Device Key not found");
		}

		String certStr =  java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());
		String enCodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
				mdCommonDeviceAPI.getHeaderfromCert(certStr));

		String payLoad = getDigitalID(mdCommonDeviceAPI.getSerialNumber(), timestamp, DeviceConstants.BioType.Face);
		String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes());
		byte [] inSignString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

		byte[] signature = "".getBytes();
		if (isValidDeviceforCertificate()){
			signature = deviceKeystore.getSignature(inSignString);
		}
		String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

		String digitalID = enCodedHeader + "." + enCodedPayLoad + "." + enCodedSignature;
		bioResponse.setDigitalId(digitalID);

		bioResponse.setTimestamp(timestamp);
		//bioResponse.setCount("1");

		bioResponse.setBioValue(java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString((captureResult.getBiometricRecords().get(bioSubType) == null) ?
						"".getBytes(StandardCharsets.UTF_8) : captureResult.getBiometricRecords().get(bioSubType)));
		return bioResponse;
	}

	/*public static String getRCaptureBiometricsMOSIPTest(FaceCaptureResult fcResult,
													CaptureRequestDto captureRequestDto) {

		Map<String, Object> responseMap = new HashMap<>();
		String rCaptureResponse;
		String deviceCode;
		String serialNumber;
		try {

			if (oB == null)
				oB = new ObjectMapper();
			CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();
			serialNumber = mdCommonDeviceAPI.getSerialNumber();

			//if (FaceCaptureResult.CAPTURE_SUCCESS == fcResult.getStatus()){
			if (DeviceConstants.environmentList.contains(captureRequestDto.env)
					&& captureRequestDto.purpose.equalsIgnoreCase(REGISTRATION)) {

				List<Map<String, Object>> listOfBiometric = new ArrayList<>();
				//String previousHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(""));
				String previousHash = mdCommonDeviceAPI.digestAsPlainText(mdCommonDeviceAPI.Sha256("".getBytes()));

				for (CaptureRequestDeviceDetailDto bio : captureRequestDto.mosipBioRequest) {
					List<BioMetricsDataDto> list = new ArrayList<>();

					BioMetricsDataDto dataDto = new BioMetricsDataDto();
					deviceCode = serialNumber;
					dataDto.setDeviceCode(deviceCode);
					dataDto.setDeviceServiceVersion(DeviceConstants.MDSVERSION);
					dataDto.setBioType(DeviceConstants.BioType.Face.getType());
					dataDto.setBioSubType("");//For Face: No bioSubType
					dataDto.setPurpose(DeviceConstants.usageStage.getDeviceUsage());
					dataDto.setEnv(captureRequestDto.env);
					if (null != fcResult.getIsoFaceRecord()){
						dataDto.setBioExtract(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString((fcResult.getIsoFaceRecord())));
					}
					else{
						dataDto.setBioExtract(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(("".getBytes())));
					}
					//dataDto.setBioExtract(java.util.Base64.getUrlEncoder().encodeToString(isoFIRBytes));
					dataDto.setTransactionId(captureRequestDto.transactionId);
					dataDto.setTimestamp(CryptoUtility.getTimestamp());
					dataDto.setRequestedScore(String.valueOf(bio.requestedScore));
					dataDto.setQualityScore(String.valueOf(fcResult.getQualityScore()));

					list.add(dataDto);

					if (!list.isEmpty()) {

						for (BioMetricsDataDto dto : list) {
							NewBioAuthDto bioResponse = new NewBioAuthDto();
							bioResponse.setBioSubType(dto.getBioSubType());
							bioResponse.setBioType(dto.getBioType());
							bioResponse.setDeviceCode(dto.getDeviceCode());
							//Device service version should be read from file
							bioResponse.setDeviceServiceVersion(DeviceConstants.MDSVERSION);
							bioResponse.setEnv(dto.getEnv());
							bioResponse.setPurpose(dto.getPurpose());
							bioResponse.setRequestedScore(String.valueOf(bio.requestedScore));
							bioResponse.setQualityScore(dto.getQualityScore());
							bioResponse.setTransactionId(captureRequestDto.transactionId);

							DeviceKeystore deviceKeystore = new DeviceKeystore();
							X509Certificate certificate = deviceKeystore.getX509Certificate();

							if (null == certificate){
								throw new KeysNotFoundException("Device Key not found");
							}

							String certStr =  java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());
							String enCodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
									mdCommonDeviceAPI.getHeaderfromCert(certStr));

							String payLoad = getDigitalID(serialNumber, dto.getTimestamp(), DeviceConstants.BioType.Face);
							String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes());
							byte [] inSignString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

							byte[] signature = "".getBytes();
							if (isValidDeviceforCertificate()){
								signature = deviceKeystore.getSignature(inSignString);
							}
							String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

							String digitalID = enCodedHeader + "." + enCodedPayLoad + "." + enCodedSignature;
							bioResponse.setDigitalId(digitalID);

							bioResponse.setTimestamp(dto.getTimestamp());
							//bioResponse.setCount("1");
							bioResponse.setBioValue(dataDto.getBioExtract());

							Map<String, Object> biometricData = getAuthMinimalResponse(
									captureRequestDto.specVersion,
									bioResponse, previousHash, fcResult);
							listOfBiometric.add(biometricData);
							previousHash = (String) biometricData.get(HASH);
						}
					}else {
						Map<String, Object> errorCountMap = new LinkedHashMap<>();
						errorCountMap.put(errorCode, "102");
						errorCountMap.put(errorInfo, "Count Mismatch");
						listOfBiometric.add(errorCountMap);
					}
				}
				responseMap.put(BIOMETRICS, listOfBiometric);

			} else {
				Map<String, Object> errorMap = new LinkedHashMap<>();
				errorMap.put(errorCode, "101");
				errorMap.put(errorInfo, "Invalid Environment / Purpose");
				responseMap.put(error, errorMap);
			}
			//}
			rCaptureResponse = new JSONObject(responseMap).toString();
		}
		catch (KeysNotFoundException kexception){
			//DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Device Key not found, Please restart MDS.");
			Logger.e(DeviceConstants.LOG_TAG, "Device Key not found, Please restart MDS.");

			List<Map<String, Object>> listOfBioErrList = new ArrayList<>();
			Map<String, Object> biometricData = new LinkedHashMap<>();
			Map<String, Object> errorMap = new LinkedHashMap<>();

			errorMap.put(errorCode, DeviceErrorCodes.MDS_DEVICEKEYS_NOT_FOUND);
			errorMap.put(errorInfo, kexception.getMessage());

			biometricData.put(SPEC_VERSION, DeviceConstants.REGSERVER_VERSION);
			biometricData.put(DATA, "");
			biometricData.put(HASH, "");
			biometricData.put(error, errorMap);

			listOfBioErrList.add(biometricData);
			responseMap.put(BIOMETRICS, listOfBioErrList);

			rCaptureResponse = new JSONObject(responseMap).toString();
		}
		catch (CertificateException cex){
			//DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Invalid Device Certificate, Please restart MDS.");
			Logger.e(DeviceConstants.LOG_TAG, "Invalid Device Certificate, Please restart MDS.");

			List<Map<String, Object>> listOfBioErrList = new ArrayList<>();
			Map<String, Object> biometricData = new LinkedHashMap<>();

			Map<String, Object> errorMap = new LinkedHashMap<>();
			errorMap.put(errorCode, DeviceErrorCodes.MDS_INVALID_CERTIFICATE);
			errorMap.put(errorInfo, cex.getMessage());
			biometricData.put(SPEC_VERSION, DeviceConstants.REGSERVER_VERSION);
			biometricData.put(DATA, "");
			biometricData.put(HASH, "");
			biometricData.put(error, errorMap);

			listOfBioErrList.add(biometricData);
			responseMap.put(BIOMETRICS, listOfBioErrList);

			rCaptureResponse = new JSONObject(responseMap).toString();
		}
		catch (Exception exception) {
			List<Map<String, Object>> listOfBioErrList = new ArrayList<>();
			Map<String, Object> biometricData = new LinkedHashMap<>();

			Map<String, Object> errorMap = new LinkedHashMap<>();
			errorMap.put(errorCode, "UNKNOWN");
			errorMap.put(errorInfo, exception.getMessage());
			biometricData.put(SPEC_VERSION, DeviceConstants.REGSERVER_VERSION);
			biometricData.put(DATA, "");
			biometricData.put(HASH, "");
			biometricData.put(error, errorMap);

			listOfBioErrList.add(biometricData);
			responseMap.put(BIOMETRICS, listOfBioErrList);

			rCaptureResponse = new JSONObject(responseMap).toString();
		}
		return rCaptureResponse;
	}*/


	private static CaptureDetail getAuthMinimalResponse(String specVersion, NewBioAuthDto data,
														String previousHash, FaceCaptureResult fcResult) {
		CaptureDetail biometricData = new CaptureDetail();
		CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();
		try {

			if (oB == null)
				oB = new ObjectMapper();

			if (FaceCaptureResult.CAPTURE_SUCCESS == fcResult.getStatus()){
				biometricData.error = new Error("0", "Success");
			}
			else if (FaceCaptureResult.CAPTURE_TIMEOUT == fcResult.getStatus()){
				biometricData.error = new Error(DeviceErrorCodes.MDS_CAPTURE_TIMEOUT, "Capture Timeout");
			}
			else{
				biometricData.error = new Error(DeviceErrorCodes.MDS_CAPTURE_FAILED,  "Capture Failed");
			}

			biometricData.specVersion = specVersion;
			X509Certificate certificate = new DeviceKeystore().getX509Certificate();
			String certStr =  java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());

			String enCodedHeader =  java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mdCommonDeviceAPI.getHeaderfromCert(certStr));//.replace("=", "");
			String enCodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(oB.writeValueAsBytes(data));//.replace("=", "");
			byte [] inSignString = (enCodedHeader + "." + enCodedPayload).getBytes();

			byte[] signature = "".getBytes();
			if (isValidDeviceforCertificate()){
				signature = new DeviceKeystore().getSignature(inSignString);
			}
			String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);//.replace("=", "");

			String dataBlock = enCodedHeader + "." + enCodedPayload + "." + enCodedSignature;

			biometricData.data = dataBlock;
			byte[] previousBioDataHash;
			byte[] currentBioDataHash;

			currentBioDataHash = CryptoUtility.generateHash(java.util.Base64.getUrlDecoder().decode(data.getBioValue()));

			if (previousHash == null || previousHash.trim().length() == 0) {
				byte [] previousDataByteArr = Strings.toUTF8ByteArray("");
				previousBioDataHash = CryptoUtility.generateHash(previousDataByteArr);
			} else {
				previousBioDataHash = CryptoUtility.decodeHex(previousHash);
			}

			byte[] finalBioDataHash = new byte[previousBioDataHash.length + currentBioDataHash.length];
			System.arraycopy(previousBioDataHash, 0, finalBioDataHash, 0, previousBioDataHash.length);
			System.arraycopy(currentBioDataHash, 0, finalBioDataHash, previousBioDataHash.length, currentBioDataHash.length);
			String finalHashDataHex = CryptoUtility.toHex (CryptoUtility.generateHash(finalBioDataHash));

			biometricData.hash =  finalHashDataHex;

		} catch (Exception ex) {
			ex.printStackTrace();
			biometricData.error = new Error("UNKNOWN", ex.getMessage());
		}
		return biometricData;
	}
	public static String toHexString(byte[] hash) 
	{ 
		// Convert byte array into signum representation  
		BigInteger number = new BigInteger(1, hash);  

		// Convert message digest into hex value  
		StringBuilder hexString = new StringBuilder(number.toString(16));  

		// Pad with leading zeros 
		while (hexString.length() < 32)  
		{  
			hexString.insert(0, '0');  
		}  

		return hexString.toString().toUpperCase();  
	}

	public static boolean isValidDeviceforCertificate() {
		boolean status = false;
		try{
			DeviceKeystore deviceKeystore = new DeviceKeystore();
			X509Certificate certificate = deviceKeystore.getX509Certificate();
			if(null != certificate) {
				status = true;
			}

			//TODO check common name(CN) in cert have device serial number in it
		}
		catch(Exception ex){
			Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: While validating Certificate and connected device");
		}

		return status;
	}
}