package io.mosip.mock.sbi.utility;

/**
 * @author NPrime Technologies
 */

public final class DeviceErrorCodes
{
	public static int SUCCESS = 0;
	public static int DEVICE_NOT_CONNECTED = 100;
	public static int DEVICE_NOT_RECOGNIZED = 101;
	
	public static final String MDS_CAPTURE_TIMEOUT = "501";
	public static final String MDS_CAPTURE_FAILED = "502";
	public static final String MDS_DEVICEKEYS_NOT_FOUND = "503";
	public static final String MDS_INVALID_CERTIFICATE = "504";
	
	public static final String INVALID_JSON = "-301";
	public static final String INVALID_JSONDATA = "-302";
	public static final String INVALID_SIGNATURE_VERIFICATION_FAILED = "-303";
	public static final String INVALID_REQUEST = "-304";
	public static final String INVALID_CERT_TRUST_FAILED = "-305";
	public static final String INVALID_ENV = "-306";

}