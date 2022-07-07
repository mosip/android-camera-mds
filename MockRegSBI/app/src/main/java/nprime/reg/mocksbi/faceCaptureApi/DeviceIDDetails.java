package nprime.reg.mocksbi.faceCaptureApi;

/**
 * @author NPrime Technologies
 */


public interface DeviceIDDetails {
	
	//int DEVICE_CERT = 0;
	int HOST_CERT = 1;
	int RESERVED_CERT_1 = 2;
	int RESERVED_CERT_2 = 3;
	int MOSIP_CERT_PROD = 11;
	int MOSIP_CERT_PRE_PROD = 12;
	int MOSIP_CERT_STAGING = 13;
	int MOSIP_CERT_DEVELOPER = 14;
	//int RESERVED_MOSIP_CERT_1 = 15;
	//int RESERVED_MOSIP_CERT_2 = 16;	
	int SNAPSHOT_RESPONSE_INFO = 20;
	int KEYROTATION_RESPONSE_INFO = 21;
	int HOST_PRIVATEKEY = 22;
	int TRUST_CERT = 23;
	int ENCRYPTIONCERT_RESPONSE_INFO = 24;

}

