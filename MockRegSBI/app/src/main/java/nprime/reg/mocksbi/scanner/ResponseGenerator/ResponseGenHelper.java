package nprime.reg.mocksbi.scanner.ResponseGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.DataObjects.BioMetricsDataDto;
import com.mdm.DataObjects.CaptureRequestDeviceDetailDto;
import com.mdm.DataObjects.CaptureRequestDto;
import com.mdm.DataObjects.DeviceInformation;
import com.mdm.DataObjects.NewBioAuthDto;

import org.bouncycastle.util.Strings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	public static int nErrorCode = 0;

	static final String REGISTRATION = "Registration";
	static final String SPEC_VERSION = "specVersion";
	static final String DATA = "data";
	static final String HASH = "hash";
	static final String BIOMETRICS = "biometrics";
	static final String errorCode = "errorCode";
	static final String errorInfo = "errorInfo";
	static final String error = "error";


	static ObjectMapper oB = null;

	public static String getDeviceDriverInfo(
			DeviceConstants.ServiceStatus currentStatus,
			String szTimeStamp, String requestType)
	{
		String outputDeviceInfo;
		List<String> listOfModalities = Collections.singletonList("FAC");

		CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();

		Map<String, Object> statusMap = new HashMap<>();
		List<Map<String, Object>> infoList = new ArrayList<>();
		try
		{

			statusMap.put("errorCode", "0");
			statusMap.put("errorInfo", "Success");

			DeviceKeystore deviceKeystore = new DeviceKeystore();
			X509Certificate certificate = deviceKeystore.getX509Certificate();

			if (null == certificate){
				throw new KeysNotFoundException("Device Key not found");
			}

			String certStr = java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());

			byte[] headerData = devCommonDeviceAPI.getHeaderfromCert(certStr);

			listOfModalities.forEach(value -> {
				Map<String, Object> data = new HashMap<>();
				byte[] deviceInfoData = getDeviceInfo(deviceKeystore, currentStatus, szTimeStamp, requestType);

				String enCodedHeader =  java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(headerData);
				String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(deviceInfoData);

				byte [] inSignString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

				byte[] signature = "".getBytes();
				if (isValidDeviceforCertificate()){
					signature = deviceKeystore.getSignature(inSignString);
				}
				String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

				String encodedDeviceInfo = enCodedHeader + "." + enCodedPayLoad + "." + enCodedSignature;//.replace("=", "");

				data.put("deviceInfo", encodedDeviceInfo);
				data.put("error", statusMap);
				infoList.add(data);
			});
			outputDeviceInfo = new JSONArray(infoList).toString();
		}
		catch (KeysNotFoundException keyException){
			//DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Device Key not found, Please restart MDS.");
			Logger.e(DeviceConstants.LOG_TAG, "Device Key not found, Please restart MDS.");

			Map<String, Object> errorMap = new LinkedHashMap<>();
			errorMap.put(errorCode, DeviceErrorCodes.MDS_DEVICEKEYS_NOT_FOUND);
			errorMap.put(errorInfo, keyException.getMessage());
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put(error, errorMap);
			infoList.add(responseMap);
			outputDeviceInfo = new JSONArray(infoList).toString();
		}		
		catch (CertificateException cex){
			//DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Invalid Device Certificate, Please restart MDS.");
			Logger.e(DeviceConstants.LOG_TAG, "Invalid Device Certificate, Please restart MDS.");

			Map<String, Object> errorMap = new LinkedHashMap<>();
			errorMap.put(errorCode, DeviceErrorCodes.MDS_INVALID_CERTIFICATE);
			errorMap.put(errorInfo, cex.getMessage());
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put(error, errorMap);
			infoList.add(responseMap);
			outputDeviceInfo = new JSONArray(infoList).toString();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Map<String, Object> errorMap = new LinkedHashMap<>();
			errorMap.put(errorCode, "UNKNOWN");
			errorMap.put(errorInfo, ex.getMessage());
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put(error, errorMap);
			infoList.add(responseMap);
			outputDeviceInfo = new JSONArray(infoList).toString();
		}
		return  outputDeviceInfo;
	}

	public static String getDeviceDiscovery(
			DeviceConstants.ServiceStatus currentStatus,
			String szTimeStamp, String requestType) {
		String discoveryData = "";

		try
		{
			JSONObject jsonobject = new JSONObject();
			JSONArray jsonarray = new JSONArray();
			JSONArray jsonDSubid = new JSONArray();
			String serialNumber;
			CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
			serialNumber = devCommonDeviceAPI.getSerialNumber();
			jsonobject.put("deviceId",  serialNumber);
			jsonobject.put("deviceStatus",  currentStatus.getType());
			jsonobject.put("certification", DeviceConstants.CERTIFICATIONLEVEL);
			jsonobject.put("serviceVersion", DeviceConstants.MDSVERSION);

			jsonDSubid.put(0);		
			jsonobject.put("deviceSubId", jsonDSubid);			
			jsonobject.put("callbackId", requestType);

			String payLoad = getDigitalID(serialNumber, szTimeStamp);
			String digID = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes()); //Base64.encodeToString(payLoad.getBytes(), Base64.NO_PADDING); //

			jsonobject.put("digitalId", digID);
			jsonobject.put("deviceCode", serialNumber);

			jsonarray.put(DeviceConstants.REGSERVER_VERSION);
			jsonobject.put("specVersion", jsonarray);

			jsonobject.put("purpose", DeviceConstants.usageStage.getDeviceUsage());

			JSONObject jsonerror = new JSONObject();
			jsonerror.put("errorCode", "0");
			jsonerror.put("errorInfo", "Success");
			jsonobject.put("error", jsonerror);



			discoveryData = jsonobject.toString();  
		}
		catch(JSONException ex)
		{
			Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: " + "Failed to process JSON");
		}
		catch(Exception ex)
		{
			Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: " + "Failed to process exception");
		}

		return "[" + discoveryData + "]";
	}	

	private static byte[] getDeviceInfo(
			DeviceKeystore deviceKeystore,
			DeviceConstants.ServiceStatus currentStatus,
			String szTimeStamp, String requestType)
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
			info.callbackId = requestType;
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
			String payLoad = getDigitalID(serialNumber, szTimeStamp);
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


	public static String getDigitalID(String serialNumber, String szTS) {		
		String digiID;
		JSONObject jsonobject = new JSONObject();

		try{			
			jsonobject.put("serialNo", serialNumber);
			jsonobject.put("make", DeviceConstants.DEVICEMAKE);
			jsonobject.put("model", DeviceConstants.DEVICEMODEL);
			jsonobject.put("type", DeviceConstants.BioType.Face.getType());
			jsonobject.put("deviceSubType", DeviceConstants.DEVICESUBTYPE);
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

	public static String getRCaptureBiometricsMOSIP(FaceCaptureResult fcResult,
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

							String payLoad = getDigitalID(serialNumber, dto.getTimestamp());
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
	}


	private static Map<String, Object>
	getAuthMinimalResponse(String specVersion, NewBioAuthDto data,
						   String previousHash, FaceCaptureResult fcResult) {
		Map<String, Object> biometricData = new LinkedHashMap<>();
		Map<String, Object> errorMap = new HashMap<>();
		CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();
		try {

			if (oB == null)
				oB = new ObjectMapper();

			if (FaceCaptureResult.CAPTURE_SUCCESS == fcResult.getStatus()){
				errorMap.put("errorCode", "0");
				errorMap.put("errorInfo", "Success");
			}
			else if (FaceCaptureResult.CAPTURE_TIMEOUT == fcResult.getStatus()){
				errorMap.put(errorCode, DeviceErrorCodes.MDS_CAPTURE_TIMEOUT);
				errorMap.put(errorInfo, "Capture Timeout");
			}
			else{
				errorMap.put(errorCode, DeviceErrorCodes.MDS_CAPTURE_FAILED);
				errorMap.put(errorInfo, "Capture Failed");
			}

			biometricData.put(SPEC_VERSION, specVersion);
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

			biometricData.put(DATA, dataBlock);
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

			biometricData.put(HASH, finalHashDataHex);
			biometricData.put(error, errorMap);

		} catch (Exception ex) {
			ex.printStackTrace();
			Map<String, String> map = new HashMap<>();
			map.put(errorCode, "UNKNOWN");
			map.put(errorInfo, ex.getMessage());
			biometricData.put(error, map);
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