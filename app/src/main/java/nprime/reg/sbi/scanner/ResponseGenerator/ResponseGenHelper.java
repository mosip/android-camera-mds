package nprime.reg.sbi.scanner.ResponseGenerator;

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
import java.security.Key;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;

import nprime.reg.sbi.face.MainActivity;
import nprime.reg.sbi.faceCaptureApi.FaceCaptureResult;
import nprime.reg.sbi.faceCaptureApi.KeysNotFoundException;
import nprime.reg.sbi.mds.DeviceMain;
import nprime.reg.sbi.secureLib.DeviceKeystore;
import nprime.reg.sbi.utility.CommonDeviceAPI;
import nprime.reg.sbi.utility.CryptoUtility;
import nprime.reg.sbi.utility.DeviceConstants;
import nprime.reg.sbi.utility.DeviceErrorCodes;
import nprime.reg.sbi.utility.Logger;

//import javax.security.cert.X509Certificate;

public class ResponseGenHelper 
{
	//static Properties prop;
	public static int nErrorCode = 0;
	//static XMLGregorianCalendar calendar;
	//static String deviceCode = "";
	private static int nmPoint;
	private static int nfiq;

	static final String SHA_TYPE = "SHA-256";
	static final String SHA_DSA = "SHA256withRSA";
	static final int IV_SIZE_BITS = 96;  
	static final int AES_KEY_SIZE_BITS = 256;
	static final String SYMMETRIC_ALGO = "AES";
	static final String JCE_PROVIDER = "BC"; 
	static final int AAD_SIZE_BITS = 128;
	static final int AUTH_TAG_SIZE_BITS = 128;
	static final String ASYMMETRIC_ALGO = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";//"RSA/ECB/PKCS1Padding";
	static final String AUTH = "Auth";	
	static final String REGISTRATION = "Registration";
	static final String FINGER = "Finger";
	static final String SPEC_VERSION = "specVersion";
	static final String DATA = "data";
	static final String HASH = "hash";
	static final String SESSION_KEY = "sessionKey";
	static final String THUMB_PRINT = "thumbprint";
	static final String BIOMETRICS = "biometrics";
	static final String errorCode = "errorCode";
	static final String errorInfo = "errorInfo";
	static final String error = "error";


	static ObjectMapper oB = null;

	public static byte[] encrypt(byte[] content, String algorithm, Key key) throws Exception 
	{
		//Cipher pkCipher = Cipher.getInstance(algorithm, JCE_PROVIDER);
		Cipher pkCipher = Cipher.getInstance(algorithm);
		pkCipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] encodedBytes = pkCipher.doFinal(content);
		return encodedBytes;
	}

	public static String getDeviceDriverInfo(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType)
	{
		String outputDeviceInfo = null;
		List<String> listOfModalities = Arrays.asList("FAC");

		CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();

		Map<String, Object> statusMap = new HashMap<>();
		List<Map<String, Object>> infoList = new ArrayList<Map<String, Object>>();
		try
		{

			statusMap.put("errorCode", "0");
			statusMap.put("errorInfo", "Success");

			if(null == DeviceMain.deviceKeystore){
				DeviceMain.deviceKeystore = new DeviceKeystore();
			}
			X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();

			if (null == certificate){
				throw new KeysNotFoundException("Device Key not found");
			}

			String certStr = java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());
			//Base64.encodeToString(certificate.getEncoded(), Base64.NO_WRAP);

			byte[] headerData = devCommonDeviceAPI.getHeaderfromCert(certStr);

			listOfModalities.forEach(value -> {
				Map<String, Object> data = new HashMap<>();
				byte[] deviceInfoData = getDeviceInfo(currentStatus, szTimeStamp, requestType);

				String enCodedHeader =  java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(headerData);
				String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(deviceInfoData);

				byte [] inSignString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

				byte[] signature = "".getBytes();
				if (isValidDeviceforCertificate()){
					signature = DeviceMain.deviceKeystore.getSignature(inSignString);
				}
				String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

				String encodedDeviceInfo = enCodedHeader + "." + enCodedPayLoad + "." + enCodedSignature;//.replace("=", "");

				data.put("deviceInfo", encodedDeviceInfo);
				data.put("error", statusMap);
				infoList.add(data);
			});
			outputDeviceInfo = new JSONArray(infoList).toString();
		}
		catch (KeysNotFoundException kexception){
			//DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Device Key not found, Please restart MDS.");
			Logger.e(DeviceConstants.LOG_TAG, "Device Key not found, Please restart MDS.");

			Map<String, Object> errorMap = new LinkedHashMap<>();
			errorMap.put(errorCode, DeviceErrorCodes.MDS_DEVICEKEYS_NOT_FOUND);
			errorMap.put(errorInfo, kexception.getMessage());
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put(error, errorMap);
			infoList.add(responseMap);
			outputDeviceInfo = new JSONArray(infoList).toString();
			MainActivity.sharedPreferences.edit().putLong("LastInitTimeMills", 0).apply();
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
			MainActivity.sharedPreferences.edit().putLong("LastInitTimeMills", 0).apply();
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
			MainActivity.sharedPreferences.edit().putLong("LastInitTimeMills", 0).apply();
		}
		return  outputDeviceInfo;
	}

	public static String getDeviceDiscovery(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType) {
		String discoveryData = "";

		try
		{
			JSONObject jsonobject = new JSONObject();
			JSONArray jsonarray = new JSONArray();
			JSONArray jsonDSubid = new JSONArray();
			String serialNumber = "";
			String deviceCode = "";
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
			jsonobject.put("deviceCode", DeviceMain.deviceMain.mainDeviceCode);

			jsonarray.put(DeviceConstants.REGSERVER_VERSION);
			jsonobject.put("specVersion", jsonarray);

			jsonobject.put("purpose", DeviceConstants.usageStage.getDeviceUsage());

			JSONObject jsonerror = new JSONObject();
			//jsonerror.put("errorCode", "101");
			//jsonerror.put("errorInfo", "Invalid JSON Value Type For Discovery..");
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

	private static byte[] getDeviceInfo(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType)
	{
		byte[] deviceInfoData = null;
		try{
			if (oB == null){
				oB = new ObjectMapper();
			}

			byte[] fwVersion = new byte[50];
			fwVersion = DeviceConstants.FIRMWAREVER.getBytes();
			CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
			String serialNumber = devCommonDeviceAPI.getSerialNumber();

			DeviceInformation info = new DeviceInformation();
			info.callbackId = requestType;
			info.certification = DeviceConstants.CERTIFICATIONLEVEL;
			info.deviceCode = DeviceMain.deviceMain.mainDeviceCode;
			info.deviceId = serialNumber;
			info.deviceStatus =  currentStatus.getType().toString(); 
			info.deviceSubId = new int[]{0};

			//byte[] x509MOSIPCertByte = devCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.DEVICE_CERT);
			X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();
			String certStr = java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());
			//Base64.encodeToString(certificate.getEncoded(), Base64.NO_WRAP);

			String enCodedHeader =  java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(devCommonDeviceAPI.getHeaderfromCert(certStr));//.replace("=", ""); //Base64.encodeToString(devCommonDeviceAPI.getHeaderfromCert(certStr), Base64.NO_PADDING);
			String payLoad = getDigitalID(serialNumber, szTimeStamp);
			String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes());//.replace("=", ""); //Base64.encodeToString(payLoad.getBytes(), Base64.NO_PADDING);
			byte [] inSignString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

			byte[] signature = "".getBytes();
			if (isValidDeviceforCertificate()){				
				signature = DeviceMain.deviceKeystore.getSignature(inSignString);
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
			// TODO Auto-generated catch block
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
		String digiID = "";
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

	public static String getRCaptureBiometricsMOSIP(FaceCaptureResult fcResult, CaptureRequestDto captureRequestDto) {

		Map<String, Object> responseMap = new HashMap<>();
		String rCaptureResponse = "";
		String deviceCode = "";
		String serialNumber = "";
		try {

			if (oB == null)
				oB = new ObjectMapper();
			CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();
			serialNumber = DeviceMain.deviceMain.mainSerialNumber;

			//if (FaceCaptureResult.CAPTURE_SUCCESS == fcResult.getStatus()){
			if (DeviceConstants.environmentList.contains(captureRequestDto.env)
					&& captureRequestDto.purpose.equalsIgnoreCase(REGISTRATION)) {

				List<Map<String, Object>> listOfBiometric = new ArrayList<>();
				//String previousHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(""));
				String previousHash = mdCommonDeviceAPI.digestAsPlainText(mdCommonDeviceAPI.Sha256("".getBytes()));

				for (CaptureRequestDeviceDetailDto bio : captureRequestDto.mosipBioRequest) {
					List<BioMetricsDataDto> list = new ArrayList<>();

					BioMetricsDataDto dataDto = new BioMetricsDataDto();
					deviceCode = DeviceMain.deviceMain.mainDeviceCode;
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
							/*Map<String, String> result = CryptoUtility.encrypt(JwtUtility.getPublicKey(),
									dataDto.getBioExtract());*/

							//NewBioAuthDto data = buildAuthNewBioDto(dto, bio.type, bio.requestedScore,
							//		captureRequestDto.transactionId/*, result*/);

							NewBioAuthDto bioResponse = new NewBioAuthDto();
							bioResponse.setBioSubType(dto.getBioSubType());
							bioResponse.setBioType(dto.getBioType());
							bioResponse.setDeviceCode(dto.getDeviceCode());
							//TODO Device service version should be read from file
							bioResponse.setDeviceServiceVersion(DeviceConstants.MDSVERSION);
							bioResponse.setEnv(dto.getEnv());
							bioResponse.setPurpose(dto.getPurpose());
							bioResponse.setRequestedScore(String.valueOf(bio.requestedScore));
							bioResponse.setQualityScore(dto.getQualityScore());
							bioResponse.setTransactionId(captureRequestDto.transactionId);

							//byte[] x509MOSIPCertByte = mdCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.DEVICE_CERT);
							X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();

							if (null == certificate){
								throw new KeysNotFoundException("Device Key not found");
							}

							String certStr =  java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());

							//X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509MOSIPCertByte));
							String enCodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
									mdCommonDeviceAPI.getHeaderfromCert(certStr));//.replace("=", "");

							String payLoad = getDigitalID(serialNumber, dto.getTimestamp());
							String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes());//.replace("=", "");
							byte [] inSignString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

							byte[] signature = "".getBytes();
							if (isValidDeviceforCertificate()){
								signature = DeviceMain.deviceKeystore.getSignature(inSignString);
							}
							String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);//.replace("=", "");

							String digitalID = enCodedHeader + "." + enCodedPayLoad + "." + enCodedSignature;
							bioResponse.setDigitalId(digitalID);

							bioResponse.setTimestamp(dto.getTimestamp());
							//bioResponse.setCount("1");
							bioResponse.setBioValue(dataDto.getBioExtract());

							Map<String, Object> biometricData = getAuthMinimalResponse(captureRequestDto.specVersion,
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
			MainActivity.sharedPreferences.edit().putLong("LastInitTimeMills", 0).apply();
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
			MainActivity.sharedPreferences.edit().putLong("LastInitTimeMills", 0).apply();
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
			MainActivity.sharedPreferences.edit().putLong("LastInitTimeMills", 0).apply();
		}
		return rCaptureResponse;
	}
/*	public static String getFaceCaptureBiometricsMOSIP(FaceCaptureResult fcResult, CaptureRequestDto captureRequestDto){

		Map<String, Object> responseMap = new HashMap<>();
		String captureResponse = "";
		String deviceCode = "";
		String connectedDeviceSerialNumber = "";		
		try {

			if (oB == null)
				oB = new ObjectMapper();

			byte[] fwVersion = new byte[50];
			fwVersion = DeviceConstants.FIRMWAREVER.getBytes();

			CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();
			connectedDeviceSerialNumber = mdCommonDeviceAPI.getSerialNumber();
			String serialNumber = captureRequestDto.mosipBioRequest.get(0).deviceId;

			if (FaceCaptureResult.CAPTURE_SUCCESS == fcResult.getStatus()){
				if (connectedDeviceSerialNumber.equals(serialNumber)){
					if (DeviceConstants.environmentList.contains(captureRequestDto.env)
							&& captureRequestDto.purpose.equalsIgnoreCase(AUTH)) {

						List<Map<String, Object>> listOfBiometric = new ArrayList<>();
						String previousHash;

						for (CaptureRequestDeviceDetailDto bio : captureRequestDto.mosipBioRequest) {
							List<BioMetricsDataDto> list = new ArrayList<>();
							previousHash = bio.previousHash;


							BioMetricsDataDto dataDto = new BioMetricsDataDto();
							deviceCode = DeviceMain.deviceMain.mainDeviceCode;
							dataDto.deviceCode = deviceCode;
							dataDto.deviceServiceVersion = DeviceConstants.MDSVERSION;
							dataDto.bioType = DeviceConstants.BioType.Face.getType();
							dataDto.bioSubType = "";
							dataDto.purpose = DeviceConstants.usageStage.getDeviceUsage();
							dataDto.env = captureRequestDto.env;
							dataDto.domainUri = captureRequestDto.domainUri;				
							dataDto.bioExtract = java.util.Base64.getEncoder().encodeToString(fcResult.getIsoFaceRecord()); //Base64.encodeToString(fcResult.getIsoFaceRecord(), Base64.DEFAULT);
							dataDto.transactionId = captureRequestDto.transactionId;
							dataDto.timestamp = mdCommonDeviceAPI.getISOTimeStamp();
							dataDto.requestedScore = String.valueOf(bio.requestedScore);
							dataDto.qualityScore = "60";

							list.add(dataDto);

							if (!list.isEmpty()) {

								PublicKey encryptPublicKey = null;
								if (DeviceMain.deviceMain.mgmtServerConnectivity){
									byte []encryptionCert = org.apache.commons.codec.binary.Base64.decodeBase64(DeviceMain.deviceMain.gmsbEncryptionCertResponse.encryptionCertData.getBytes());
									CertificateFactory cf = CertificateFactory.getInstance("X.509");
									X509Certificate x509Cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(encryptionCert));
									encryptPublicKey = x509Cert.getPublicKey();
								}
								else {
									encryptPublicKey = JwtUtility.getPublicKey();
								}

								if (null == encryptPublicKey){
									Map<String, Object> errorMap = new LinkedHashMap<>();
									errorMap.put(errorCode, "106");
									errorMap.put(errorInfo, "Invalid encryption certificate format or certificate not exists");
									responseMap.put(error, errorMap);
								}
								else{
									for (BioMetricsDataDto dto : list) {
										Map<String, String> result = CryptoUtility.encrypt(encryptPublicKey,
												dto.bioExtract, dto);

										NewBioAuthDto data = buildAuthNewBioDto(dto, dto.bioType, dto.requestedScore,
												captureRequestDto.transactionId, result);							

										Map<String, Object> biometricData = getAuthMinimalResponse(captureRequestDto.specVersion,
												data, previousHash, result, fcResult.getStatus());
										listOfBiometric.add(biometricData);
										previousHash = (String) biometricData.get(HASH);
									}
									responseMap.put(BIOMETRICS, listOfBiometric);
								}
							}else {
								Map<String, Object> errorCountMap = new LinkedHashMap<>();
								errorCountMap.put(errorCode, "102");
								errorCountMap.put(errorInfo, "Count Mismatch");
								listOfBiometric.add(errorCountMap);
								responseMap.put(BIOMETRICS, listOfBiometric);
							}
						}
					} else {
						Map<String, Object> errorMap = new LinkedHashMap<>();
						errorMap.put(errorCode, "101");
						errorMap.put(errorInfo, "Invalid Environment / Purpose");
						responseMap.put(error, errorMap);
					}
				}
				else{
					Map<String, Object> errorMap = new LinkedHashMap<>();
					errorMap.put(errorCode, "103");
					errorMap.put(errorInfo, "Serial number check failed");
					responseMap.put(error, errorMap);				
				}
			}
			else{
				Map<String, Object> errorMap = new LinkedHashMap<>();
				errorMap.put(errorCode, "105");
				errorMap.put(errorInfo, "Capture failed");
				responseMap.put(error, errorMap);				
			}
			captureResponse = new JSONObject(responseMap).toString();
			//System.out.println("Response --- " +captureResponse);
		} catch (Exception exception) {

			exception.printStackTrace();

			Map<String, Object> errorMap = new LinkedHashMap<>();
			errorMap.put(errorCode, "UNKNOWN");
			errorMap.put(errorInfo, exception.getMessage());
			responseMap.put(error, errorMap);
			captureResponse = new JSONObject(responseMap).toString();
		}
		return captureResponse;

	}	*/

	private static NewBioAuthDto buildAuthNewBioDto(BioMetricsDataDto bioMetricsData, String bioType, String requestedScore, String transactionId, 
			Map<String, String> cryptoResult) throws Exception {	


		byte[] fwVersion = new byte[50];
		fwVersion = DeviceConstants.DEVICEMODEL.getBytes();
		CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();

		String serialNumber = devCommonDeviceAPI.getSerialNumber();
		NewBioAuthDto bioResponse = new NewBioAuthDto();

		X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();
		if (null == certificate){
			throw new KeysNotFoundException("Device Key not found");
		}

		String certStr = java.util.Base64.getEncoder().encodeToString(certificate.getEncoded()); //Base64.encodeToString(certificate.getEncoded(), Base64.NO_WRAP);

		String enCodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(devCommonDeviceAPI.getHeaderfromCert(certStr));
		//Base64.encodeToString(devCommonDeviceAPI.getHeaderfromCert(certStr), Base64.NO_PADDING);

		String payLoad = getDigitalID(serialNumber, bioMetricsData.getTimestamp());
		String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes()); //Base64.encodeToString(payLoad.getBytes(), Base64.NO_PADDING);
		byte [] inSignString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

		byte[] signature = "".getBytes();
		if (isValidDeviceforCertificate()){
			signature = DeviceMain.deviceKeystore.getSignature(inSignString);
		}
		String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature); //Base64.encodeToString(signature, Base64.NO_PADDING);
		String digitalID = enCodedHeader + "." + enCodedPayLoad + "." + enCodedSignature;

		bioResponse.setBioSubType(bioMetricsData.getBioSubType());
		bioResponse.setBioType(bioType);
		bioResponse.setDeviceCode(bioMetricsData.getDeviceCode());
		//TODO Device service version should be read from file
		bioResponse.setDeviceServiceVersion(DeviceConstants.MDSVERSION);
		bioResponse.setEnv(bioMetricsData.getEnv());
		//TODO - need to change, should handle based on deviceId
		bioResponse.setDigitalId(digitalID);
		bioResponse.setPurpose(bioMetricsData.getPurpose());
		bioResponse.setRequestedScore(requestedScore);
		bioResponse.setQualityScore(bioMetricsData.getQualityScore());
		bioResponse.setTransactionId(bioMetricsData.getTransactionId());
		//TODO Domain URL need to be set
		//bioResponse.setDomainUri(bioMetricsData.domainUri);

		//bioResponse.setTimestamp(cryptoResult.get("TIMESTAMP"));
		bioResponse.setTimestamp(bioMetricsData.getTimestamp());
		bioResponse.setBioValue(cryptoResult.containsKey("ENC_DATA") ? 
				cryptoResult.get("ENC_DATA") : null);		
		return bioResponse;
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
			//String dataBlock = JwtUtility.getJwt(oB.writeValueAsBytes(data), JwtUtility.getProviderPrivateKey(),
			//		JwtUtility.getProviderCertificate());

			//byte[] x509MOSIPCertByte = mdCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.DEVICE_CERT);
			X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();
			String certStr =  java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());

			String enCodedHeader =  java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mdCommonDeviceAPI.getHeaderfromCert(certStr));//.replace("=", "");
			String enCodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(oB.writeValueAsBytes(data));//.replace("=", "");
			byte [] inSignString = (enCodedHeader + "." + enCodedPayload).getBytes();

			byte[] signature = "".getBytes();
			if (isValidDeviceforCertificate()){
				signature = DeviceMain.deviceKeystore.getSignature(inSignString);
			}
			String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);//.replace("=", "");

			String dataBlock = enCodedHeader + "." + enCodedPayload + "." + enCodedSignature;

			biometricData.put(DATA, dataBlock);

			/*String presentHash = mdCommonDeviceAPI.digestAsPlainText(new String(mdCommonDeviceAPI.Sha256(dataBlock.getBytes())));
			String concatenatedHash = previousHash + presentHash;
			String finalHash = mdCommonDeviceAPI.digestAsPlainText(new String(mdCommonDeviceAPI.Sha256(concatenatedHash.getBytes())));*/

			byte[] previousBioDataHash = null;
			byte[] currentBioDataHash = null;

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
			Map<String, String> map = new HashMap<String, String>();
			map.put(errorCode, "UNKNOWN");
			map.put(errorInfo, ex.getMessage());
			biometricData.put(error, map);
		}
		return biometricData;
	}
	/*private static Map<String, Object> getAuthMinimalResponse(String specVersion, NewBioAuthDto data, String previousHash,
			Map<String, String> cryptoResult, int captureResult) {
		Map<String, Object> biometricData = new LinkedHashMap<>();
		Map<String, Object> errorMap = new HashMap<>();
		CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();
		try {

			if (oB == null){
				oB = new ObjectMapper();
			}

			if (FaceCaptureResult.CAPTURE_SUCCESS == captureResult){
				errorMap.put("errorCode", "0");
				errorMap.put("errorInfo", "Success");			
			}
			else if (FaceCaptureResult.CAPTURE_TIMEOUT == captureResult){
				errorMap.put(errorCode, DeviceErrorCodes.MDS_CAPTURE_TIMEOUT);
				errorMap.put(errorInfo, "Capture Timeout");				
			}
			else{
				errorMap.put(errorCode, DeviceErrorCodes.MDS_CAPTURE_FAILED);
				errorMap.put(errorInfo, "Capture Failed");				
			} 


			biometricData.put(SPEC_VERSION, specVersion);
			//String dataBlock = JwtUtility.getJwt(oB.writeValueAsBytes(data), JwtUtility.getProviderPrivateKey(),
			//		JwtUtility.getProviderCertificate());

			//byte[] x509MOSIPCertByte = mdCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.DEVICE_CERT);
			X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();
			String certStr = java.util.Base64.getEncoder().encodeToString(certificate.getEncoded()); //Base64.encodeToString(certificate.getEncoded(), Base64.NO_WRAP);

			String enCodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mdCommonDeviceAPI.getHeaderfromCert(certStr));//.replace("=", ""); //Base64.encodeToString(mdCommonDeviceAPI.getHeaderfromCert(certStr), Base64.NO_PADDING);
			String enCodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(oB.writeValueAsBytes(data));//.replace("=", ""); //Base64.encodeToString(oB.writeValueAsBytes(data), Base64.NO_PADDING); //
			byte [] inSignString = (enCodedHeader + "." + enCodedPayload).getBytes();

			byte[] signature = "".getBytes();
			if (isValidDeviceforCertificate()){
				signature = DeviceMain.deviceKeystore.getSignature(inSignString);
			}
			String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);//.replace("=", ""); //Base64.encodeToString(signature, Base64.NO_PADDING);

			String dataBlock = enCodedHeader + "." + enCodedPayload + "." + enCodedSignature;			
			biometricData.put(DATA, dataBlock);


			String dataBlockforHash = oB.writeValueAsString(data);

			String presentHash = mdCommonDeviceAPI.digestAsPlainText(mdCommonDeviceAPI.Sha256(dataBlockforHash.getBytes(StandardCharsets.UTF_8)));
			//String presentHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(dataBlockforHash.getBytes(StandardCharsets.UTF_8)));

			String concatenatedHash = previousHash + presentHash;
			String finalHash = mdCommonDeviceAPI.digestAsPlainText(mdCommonDeviceAPI.Sha256(concatenatedHash.getBytes()));
			//String finalHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(concatenatedHash.getBytes()));
			biometricData.put(HASH, finalHash);
			biometricData.put(SESSION_KEY, cryptoResult.get("ENC_SESSION_KEY"));
			*//*if (DeviceMain.deviceMain.mgmtServerConnectivity){
				biometricData.put(THUMB_PRINT, DeviceMain.deviceMain.gmsbEncryptionCertResponse.encryptionCertThumbprint);
			} else{*//*
				biometricData.put(THUMB_PRINT, mdCommonDeviceAPI.getThumbprint(JwtUtility.getCertificate()));
			//}
			biometricData.put(error, errorMap);

		} catch (Exception ex) {
			ex.printStackTrace();
			Map<String, String> map = new HashMap<String, String>();
			map.put(errorCode, "UNKNOWN");
			map.put(errorInfo, ex.getMessage());
			biometricData.put(error, map);
		}
		return biometricData;
	}*/


	/*	
	//For Face
	public static String getRCaptureBiometricsMOSIP(FaceCaptureResult fcResult, CaptureRequestDto captureRequestDto) {

		Map<String, Object> responseMap = new HashMap<>();
		String rCaptureResponse = "";
		String deviceCode = "";
		String serialNumber = "";
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
				String previousHash = mdCommonDeviceAPI.digestAsPlainText(new String(mdCommonDeviceAPI.Sha256("".getBytes())));

				for (CaptureRequestDeviceDetailDto bio : captureRequestDto.mosipBioRequest) {
					List<BioMetricsDataDto> list = new ArrayList<>();

					BioMetricsDataDto dataDto = new BioMetricsDataDto();
					deviceCode = DeviceMain.deviceMain.mainDeviceCode;
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
							//Map<String, String> result = CryptoUtility.encrypt(JwtUtility.getPublicKey(),
							//		dataDto.getBioExtract());

							//NewBioAuthDto data = buildAuthNewBioDto(dto, bio.type, bio.requestedScore,
							//		captureRequestDto.transactionId);

							NewBioAuthDto bioResponse = new NewBioAuthDto();
							bioResponse.setBioSubType(dto.getBioSubType());
							bioResponse.setBioType(dto.getBioType());
							bioResponse.setDeviceCode(dto.getDeviceCode());
							//TODO Device service version should be read from file
							bioResponse.setDeviceServiceVersion(DeviceConstants.MDSVERSION);
							bioResponse.setEnv(dto.getEnv());		
							bioResponse.setPurpose(dto.getPurpose());
							bioResponse.setRequestedScore(String.valueOf(bio.requestedScore));
							bioResponse.setQualityScore(dto.getQualityScore());		
							bioResponse.setTransactionId(captureRequestDto.transactionId);

							//byte[] x509MOSIPCertByte = mdCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.DEVICE_CERT);
							X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();

							if (null == certificate){
								throw new KeysNotFoundException("Device Key not found");
							}

							String certStr =  java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());

							//X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509MOSIPCertByte));
							String enCodedHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
									mdCommonDeviceAPI.getHeaderfromCert(certStr));//.replace("=", ""); 

							String payLoad = getDigitalID(serialNumber, dto.getTimestamp());
							String enCodedPayLoad = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payLoad.getBytes());//.replace("=", "");
							byte [] inSignString = (enCodedHeader + "." + enCodedPayLoad).getBytes();

							byte[] signature = "".getBytes();
							if (isValidDeviceforCertificate()){
								signature = DeviceMain.deviceKeystore.getSignature(inSignString);
							}
							String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);//.replace("=", "");

							String digitalID = enCodedHeader + "." + enCodedPayLoad + "." + enCodedSignature;
							bioResponse.setDigitalId(digitalID); 

							bioResponse.setTimestamp(dto.getTimestamp());
							//bioResponse.setCount("1");
							bioResponse.setBioValue(dataDto.getBioExtract());

							Map<String, Object> biometricData = getAuthMinimalResponse(captureRequestDto.specVersion,
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
			DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Device Key not found, Please restart MDS.");
			Logger.WriteLog(LOGTYPE.ERROR, "Device Key not found, Please restart MDS.");

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
			DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Face SBI", "Invalid Device Certificate, Please restart MDS.");
			Logger.WriteLog(LOGTYPE.ERROR, "Invalid Device Certificate, Please restart MDS.");

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

	private static Map<String, Object> getAuthMinimalResponse(String specVersion, NewBioAuthDto data, String previousHash, FaceCaptureResult fcResult) {
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
			//String dataBlock = JwtUtility.getJwt(oB.writeValueAsBytes(data), JwtUtility.getProviderPrivateKey(),
			//		JwtUtility.getProviderCertificate());

			//byte[] x509MOSIPCertByte = mdCommonDeviceAPI.readFilefromHost((byte)DeviceIDDetails.DEVICE_CERT);
			X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();
			String certStr =  java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());

			String enCodedHeader =  java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mdCommonDeviceAPI.getHeaderfromCert(certStr));//.replace("=", ""); 
			String enCodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(oB.writeValueAsBytes(data));//.replace("=", "");
			byte [] inSignString = (enCodedHeader + "." + enCodedPayload).getBytes();

			byte[] signature = "".getBytes();
			if (isValidDeviceforCertificate()){
				signature = DeviceMain.deviceKeystore.getSignature(inSignString);
			}
			String enCodedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);//.replace("=", "");

			String dataBlock = enCodedHeader + "." + enCodedPayload + "." + enCodedSignature;

			biometricData.put(DATA, dataBlock);
			//String presentHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(dataBlock));
			String presentHash = mdCommonDeviceAPI.digestAsPlainText(new String(mdCommonDeviceAPI.Sha256(dataBlock.getBytes())));
			String concatenatedHash = previousHash + presentHash;
			//String finalHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(concatenatedHash));
			String finalHash = mdCommonDeviceAPI.digestAsPlainText(new String(mdCommonDeviceAPI.Sha256(concatenatedHash.getBytes())));
			biometricData.put(HASH, finalHash);
			biometricData.put(error, errorMap);

		} catch (Exception ex) {
			ex.printStackTrace();
			Map<String, String> map = new HashMap<String, String>();
			map.put(errorCode, "UNKNOWN");
			map.put(errorInfo, ex.getMessage());
			biometricData.put(error, map);
		}
		return biometricData;
	} 
	 */
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

		if (!DeviceMain.deviceMain.mgmtServerConnectivity){
			return true;
		}
		boolean status = false;
		if(null == DeviceMain.deviceMain.mainSerialNumber){
			DeviceMain.deviceMain.mainSerialNumber = new CommonDeviceAPI().getSerialNumber();
		}
		String serialNo = DeviceMain.deviceMain.mainSerialNumber;
		String commonName = "";		
		try{
			X509Certificate certificate = DeviceMain.deviceKeystore.getX509Certificate();

			commonName = getCommonName(certificate);

			if (commonName.startsWith(serialNo)){
				status = true;	
			}
			else{
				status = false;
			}
		}
		catch(Exception ex){
			status = false;
			Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: While validating Certificate and connected device");
		}

		return status;
	}

	private static String getCommonName(X509Certificate c) {
		for (String each : c.getSubjectDN().getName().split(",\\s*")) {
			if (each.startsWith("CN=")) {
				String result = each.substring(3);
				return result;
			}
		}
		throw new IllegalStateException("Missed CN in Subject DN: "
				+ c.getSubjectDN());
	}
}