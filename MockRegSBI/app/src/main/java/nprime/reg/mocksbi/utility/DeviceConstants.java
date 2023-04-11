package nprime.reg.mocksbi.utility;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author NPrime Technologies
 */

public class DeviceConstants {
    public static final String LOG_TAG = "NPrimeSBI";
    public static final String CERTIFICATIONLEVEL = "L0"; //L0, L1, L2
    public static final String MDSVERSION = "0.9.5";
    public static final String FIRMWAREVER = "1.0.1";
    public static final String DEVICEMODEL = "AndroidFaceCamera";
    public static final String DEVICEMAKE = "Android";
    public static final String FACE_DEVICESUBTYPE = "Full face";
    public static final String FINGER_DEVICESUBTYPE = "Slap";
    public static final String IRIS_DEVICESUBTYPE = "Double";
    public static final String PROVIDERNAME = "NPrime";
    public static final String PROVIDERID = "Nprime_DP";
    public static final String REGSERVER_VERSION = "0.9.5";
    public static String ENVIRONMENT = "Production";
    public static String DOMAIN_URI = "ANDROID";
    public static final int DEFAULT_TIME_DELAY = 1000;
    public static DeviceUsage usageStage = DeviceUsage.Registration;
    public static List<String> environmentList = Arrays.asList("Staging", "Developer", "Pre-Production", "Production");

    /**
     * Device SubType Id Value
     */
    public final static int DEVICE_IRIS_SINGLE_SUB_TYPE_ID = 0;    // Single IMAGE
    public final static int DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_LEFT = 1;    // LEFT IRIS IMAGE
    public final static int DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_RIGHT = 2;    // RIGHT IRIS IMAGE
    public final static int DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_BOTH = 3;    // BOTH LEFT AND RIGHT IRIS IMAGE
    public final static int DEVICE_FINGER_SINGLE_SUB_TYPE_ID = 0;    // Single IMAGE
    public final static int DEVICE_FINGER_SLAP_SUB_TYPE_ID_LEFT = 1;    // LEFT SLAP IMAGE
    public final static int DEVICE_FINGER_SLAP_SUB_TYPE_ID_RIGHT = 2;    // RIGHT SLAP IMAGE
    public final static int DEVICE_FINGER_SLAP_SUB_TYPE_ID_THUMB = 3;// TWO THUMB IMAGE
    public final static int DEVICE_FACE_SUB_TYPE_ID_FULLFACE = 0;    // TWO THUMB IMAGE

    /**
     * Bio Exceptions/Bio Subtype Names
     */
    public final static String BIO_NAME_UNKNOWN = "UNKNOWN";
    public final static String BIO_NAME_RIGHT_THUMB = "Right Thumb";
    public final static String BIO_NAME_RIGHT_INDEX = "Right IndexFinger";
    public final static String BIO_NAME_RIGHT_MIDDLE = "Right MiddleFinger";
    public final static String BIO_NAME_RIGHT_RING = "Right RingFinger";
    public final static String BIO_NAME_RIGHT_LITTLE = "Right LittleFinger";
    public final static String BIO_NAME_LEFT_THUMB = "Left Thumb";
    public final static String BIO_NAME_LEFT_INDEX = "Left IndexFinger";
    public final static String BIO_NAME_LEFT_MIDDLE = "Left MiddleFinger";
    public final static String BIO_NAME_LEFT_RING = "Left RingFinger";
    public final static String BIO_NAME_LEFT_LITTLE = "Left LittleFinger";
    public final static String BIO_NAME_RIGHT_IRIS = "Right";
    public final static String BIO_NAME_LEFT_IRIS = "Left";

    /**
     * Profile Bio File Names
     */
    public static String PROFILE_BIO_FILE_NAME_RIGHT_THUMB = "Right_Thumb.iso";
    public static String PROFILE_BIO_FILE_NAME_RIGHT_INDEX = "Right_Index.iso";
    public static String PROFILE_BIO_FILE_NAME_RIGHT_MIDDLE = "Right_Middle.iso";
    public static String PROFILE_BIO_FILE_NAME_RIGHT_RING = "Right_Ring.iso";
    public static String PROFILE_BIO_FILE_NAME_RIGHT_LITTLE = "Right_Little.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_THUMB = "Left_Thumb.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_INDEX = "Left_Index.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_MIDDLE = "Left_Middle.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_RING = "Left_Ring.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_LITTLE = "Left_Little.iso";
    public static String PROFILE_BIO_FILE_NAME_RIGHT_THUMB_WSQ = "Right_Thumb_wsq.iso";
    public static String PROFILE_BIO_FILE_NAME_RIGHT_INDEX_WSQ = "Right_Index_wsq.iso";
    public static String PROFILE_BIO_FILE_NAME_RIGHT_MIDDLE_WSQ = "Right_Middle_wsq.iso";
    public static String PROFILE_BIO_FILE_NAME_RIGHT_RING_WSQ = "Right_Ring_wsq.iso";
    public static String PROFILE_BIO_FILE_NAME_RIGHT_LITTLE_WSQ = "Right_Little_wsq.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_THUMB_WSQ = "Left_Thumb_wsq.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_INDEX_WSQ = "Left_Index_wsq.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_MIDDLE_WSQ = "Left_Middle_wsq.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_RING_WSQ = "Left_Ring_wsq.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_LITTLE_WSQ = "Left_Little_wsq.iso";
    public static String PROFILE_BIO_FILE_NAME_RIGHT_IRIS = "Right_Iris.iso";
    public static String PROFILE_BIO_FILE_NAME_LEFT_IRIS = "Left_Iris.iso";
    public static String PROFILE_BIO_FILE_NAME_FACE = "Face.iso";
    public static String PROFILE_BIO_FILE_NAME_FACE_EXCEPTION = "Exception_Photo.iso";

    public enum ServiceStatus {
        READY("Ready"), BUSY("Busy"), NOTREADY("Not Ready"), NOTREGISTERED("Not Registered");
        private final String status;

        public String getStatus() {
            return this.status;
        }

        ServiceStatus(String type) {
            this.status = type;
        }
    }

    public enum BioType {
        BioDevice("Biometric Device"), Finger("Finger"), Face("Face"), Iris("Iris");
        private final String bioType;

        public String getBioType() {
            return this.bioType;
        }

        BioType(String type) {
            this.bioType = type;
        }
    }

    public enum DeviceUsage {
        Authentication("Auth"), Registration("Registration");
        private final String deviceUsage;

        public String getDeviceUsage() {
            return this.deviceUsage;
        }

        DeviceUsage(String deviceUsage) {
            this.deviceUsage = deviceUsage;
        }
    }

    public static DeviceConstants.ServiceStatus getDeviceStatusEnum(String deviceStatus) {
        if (Objects.equals(deviceStatus, DeviceConstants.ServiceStatus.READY.getStatus())) {
            return DeviceConstants.ServiceStatus.READY;
        } else if (Objects.equals(deviceStatus, DeviceConstants.ServiceStatus.BUSY.getStatus())) {
            return DeviceConstants.ServiceStatus.BUSY;
        } else if (Objects.equals(deviceStatus, DeviceConstants.ServiceStatus.NOTREADY.getStatus())) {
            return DeviceConstants.ServiceStatus.NOTREADY;
        } else if (Objects.equals(deviceStatus, DeviceConstants.ServiceStatus.NOTREGISTERED.getStatus())) {
            return DeviceConstants.ServiceStatus.NOTREGISTERED;
        }
        return null;
    }
}
