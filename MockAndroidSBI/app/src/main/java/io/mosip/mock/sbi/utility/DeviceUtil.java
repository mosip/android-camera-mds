package io.mosip.mock.sbi.utility;

/**
 * @author Anshul Vanawat
 */

public class DeviceUtil {

    public String CERTIFICATION_LEVEL;
    public DeviceConstants.DeviceUsage DEVICE_USAGE;
    public String FACE_DEVICE_SUBTYPE;
    public String FINGER_DEVICE_SUBTYPE;
    public String IRIS_DEVICE_SUBTYPE;

    public DeviceUtil(String deviceUsage) {
        if (deviceUsage.equals(DeviceConstants.DeviceUsage.Registration.getDeviceUsage())) {
            CERTIFICATION_LEVEL = DeviceConstants.CERTIFICATION_L0; //L0, L1, L2
            DEVICE_USAGE = DeviceConstants.DeviceUsage.Registration;
            FINGER_DEVICE_SUBTYPE = DeviceConstants.MOSIP_BIOMETRIC_SUBTYPE_FINGER_SLAP;
            IRIS_DEVICE_SUBTYPE = DeviceConstants.MOSIP_BIOMETRIC_SUBTYPE_IRIS_DOUBLE;
        } else {
            CERTIFICATION_LEVEL = DeviceConstants.CERTIFICATION_L1; //L0, L1, L2
            DEVICE_USAGE = DeviceConstants.DeviceUsage.Authentication;
            FINGER_DEVICE_SUBTYPE = DeviceConstants.MOSIP_BIOMETRIC_SUBTYPE_FINGER_SINGLE;
            IRIS_DEVICE_SUBTYPE = DeviceConstants.MOSIP_BIOMETRIC_SUBTYPE_IRIS_SINGLE;
        }
        FACE_DEVICE_SUBTYPE = DeviceConstants.MOSIP_BIOMETRIC_SUBTYPE_FACE;
    }
}
