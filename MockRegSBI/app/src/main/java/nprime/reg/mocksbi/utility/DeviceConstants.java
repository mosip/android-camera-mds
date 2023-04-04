package nprime.reg.mocksbi.utility;

import java.util.Arrays;
import java.util.List;

/**
 * @author NPrime Technologies
 */

public class DeviceConstants {
    public static final String LOG_TAG = "NPrimeSBI";

    public static final String CERTIFICATIONLEVEL = "L0"; //L0, L1, L2
    public static final int MDS_TIMESYNC_TOLERANCE = 900000; //15 minutes
    public static final int MDS_SNAPSHOTVALIDITY_WARNING = 28800000; //8 hrs ///43200000; //12 hrs

    //public static final String REGISTRATION_MODE = "L" + TMFMain.deviceMode;

    public static final String MOCK_MDS_KEYSTORE = "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";

    public static final long DELAY_MULTIPLE_CAPTURE = 2000;
    public static final int USB_ENCRYPTION_YES = 1;
    public static final int USB_ENCRYPTION_NO = 0;

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
    public static final String PURPOSE = DeviceUsage.Registration.toString();
    public static String OSTYPE = "ANDROID";
    public static String DOMAIN_URI = "ANDROID";

    public static enum ServiceStatus {

        READY("Ready"), BUSY("Busy"), NOTREADY("Not Ready"), NOTREGISTERED("Not Registered");

        private final String type;

        public String getType() {
            return this.type;
        }

        // enum constructor - cannot be public or protected
        private ServiceStatus(String type) {
            this.type = type;
        }
    }

    public static DeviceUsage usageStage = DeviceUsage.Registration;

    public static enum BioType {

        BioDevice("Biometric Device"), Finger("Finger"), Face("Face"), Iris("Iris");

        private String type;

        public String getType() {
            return this.type;
        }

        // enum constructor - cannot be public or protected
        private BioType(String type) {
            this.type = type;
        }
    }


    public static enum DeviceUsage {

        Authentication("Auth"), Registration("Registration");

        private String deviceUsage;

        public String getDeviceUsage() {
            return this.deviceUsage;
        }

        // enum constructor - cannot be public or protected
        private DeviceUsage(String deviceUsage) {
            this.deviceUsage = deviceUsage;
        }
    }

	/*public static enum Environment{

		None("None"), Staging("Staging"), Developer("Developer"), PProd("Pre-Production"), Prod("Production");

		private String type;

	    public String getEnvironment(){
	        return this.type;
	    }

	    // enum constructor - cannot be public or protected
	    private Environment(String type){
	        this.type = type;
	    }
	}*/

    public static List<String> environmentList = Arrays.asList("Staging", "Developer", "Pre-Production", "Production");

    public static String[] daysOfWeek = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    //public static String KEYPFXCONTENT = "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";
    public static String KEYPFXCONTENT = "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";

    //For MMC
    public static String HOSTPRIVATEKEYCONTENT = "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";

    public static String TRUSTCERT = "-----BEGIN CERTIFICATE-----\r\n" +
            "MIIDyjCCArKgAwIBAgIEYPj/1zANBgkqhkiG9w0BAQsFADCBhjELMAkGA1UEBhMC\r\n" +
            "SU4xEjAQBgNVBAgMCVRlbGFuZ2FuYTESMBAGA1UEBwwJSHlkZXJhYmFkMSwwKgYD\r\n" +
            "VQQKDCNOUHJpbWUgVGVjaG5vbG9naWVzIFByaXZhdGUgTGltaXRlZDEQMA4GA1UE\r\n" +
            "CwwHTlBSVGVjaDEPMA0GA1UEAwwGTlBSTU1DMCAXDTIxMDcyMjA1MTkxOVoYDzIx\r\n" +
            "MjEwNzIyMDUxOTE5WjCBhjELMAkGA1UEBhMCSU4xEjAQBgNVBAgMCVRlbGFuZ2Fu\r\n" +
            "YTESMBAGA1UEBwwJSHlkZXJhYmFkMSwwKgYDVQQKDCNOUHJpbWUgVGVjaG5vbG9n\r\n" +
            "aWVzIFByaXZhdGUgTGltaXRlZDEQMA4GA1UECwwHTlBSVGVjaDEPMA0GA1UEAwwG\r\n" +
            "TlBSTU1DMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApFNVJsUTrymY\r\n" +
            "LToR8wQsQO5+byQbpIydfOeHXyND8AilApSz3LwyG6d5YVXXkqdtEXBcfgcvIDU0\r\n" +
            "zWaOcGpsR+6hGTCBAjKIv7ie2GRWVG0kzt8siuU2LYXmb87TQ+Ubqpo8D6cz4Id4\r\n" +
            "BZd+3Yp1J4FbWzHJSE4q4c49LPivXNKY7+Aj8TAewp0k7f5S5utwJ1tvfUNLBvE0\r\n" +
            "XQrwWT4BxXPbIYtBmOuUuvdWdjjEJh9sdRUQo1i8bBaGN+iIpZzh6iVJnjSdQeRZ\r\n" +
            "QtfgTXZI1Kt4cwoPGyDMT5v/rF35j5W1x9G8USS26K73fHJz4+XKVXnRO0fxggZs\r\n" +
            "nsUycbDJ7wIDAQABozwwOjAMBgNVHRMEBTADAQH/MB0GA1UdDgQWBBRTZDW3U2pU\r\n" +
            "AhOs8DSbht2ZQOJzVDALBgNVHQ8EBAMCAsQwDQYJKoZIhvcNAQELBQADggEBAIqT\r\n" +
            "mK68VVh7YGcmhZsj2ZQ37N0QR7APRO3Y4A3CXLzvvS+ne+cCDXMmJPoFW51D8Oih\r\n" +
            "v+wwPKgssBk0OFxR2iKnfkdIPIVmjQaZg0OBSYkoHTCa6TpPfEomum42gb89aGp6\r\n" +
            "s8PwKL9dCiMz3jkCBnANsSisj4UbhNuq2KgRUGg5VD0afPxdBQTwprywvarhCmYg\r\n" +
            "MyavqDSUjGJZGAFLeAmGslxiogS1VSC7z+zVdlT0hexYbvrS6AmQdIIDmW3JJ4JE\r\n" +
            "VLz4sqYwswp+5DmjM1yLB3xtcdBVPmhb1qRwYd8gxfy81doZXQzxM/ANcrNQuUXj\r\n" +
            "78p2kth8KLKdrmHqwwQ=\r\n" +
            "-----END CERTIFICATE-----";

    public static String MOSIP_CERT_PROD = "-----BEGIN CERTIFICATE-----\r\n" +
            "MIIFujCCBKKgAwIBAgIEARH+GDANBgkqhkiG9w0BAQsFADCBkDELMAkGA1UEBhMC\r\n" +
            "SU4xKjAoBgNVBAoTIWVNdWRocmEgQ29uc3VtZXIgU2VydmljZXMgTGltaXRlZDEd\r\n" +
            "MBsGA1UECxMUQ2VydGlmeWluZyBBdXRob3JpdHkxNjA0BgNVBAMTLWUtTXVkaHJh\r\n" +
            "IFN1YiBDQSBmb3IgQ2xhc3MgMyBPcmdhbmlzYXRpb24gMjAxNDAeFw0xOTEwMjIx\r\n" +
            "MzUwMTJaFw0yMjEwMjExMzUwMTJaMIG+MQswCQYDVQQGEwJJTjEOMAwGA1UEChMF\r\n" +
            "VUlEQUkxGjAYBgNVBAsTEVRlY2hub2xvZ3kgQ2VudHJlMQ8wDQYDVQQREwY1NjAw\r\n" +
            "OTIxEjAQBgNVBAgTCUtBUk5BVEFLQTFJMEcGA1UEBRNAODdiNzU2ZjQ5ZWZlY2Jh\r\n" +
            "MTczNzllOTY5N2JhMmNmMzFkYTdlOGY4NGE2ZThmNGRhOGQwNWIxODNmYjVkMWFk\r\n" +
            "OTETMBEGA1UEAxMKQU5VUCBLVU1BUjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC\r\n" +
            "AQoCggEBAKsLzL3ZBNu4Czfqa7XarC63uphHTc/mWnDAKHZ0Dh8mBGVb+xeM8zVS\r\n" +
            "9y6VLsciBZ/O6/qXQwfefWyjL2MLt/0LpEE4cG7VLV1C9OYK6y2MbrT9nwzN+pAM\r\n" +
            "zcWTNiiee6W6LaKjXcOqs3Dzgzd19KW1Zk5U6blISRH5NCQ83srTAPMll1xMkzbf\r\n" +
            "PCluvYlR7RtODyNQP5EwvuTncpAVi+7r1weV431LvjeuDajDfV494knt/5XkJha5\r\n" +
            "7MtfzeuQbVXfHzHTgkubcpCjSjMHhxOeBisjtcFtrTi4K2RQ5njWEkvAyXSmDWb1\r\n" +
            "XJx6w+p69ezN9KqwJubbShzd9PVixZcCAwEAAaOCAeowggHmMCIGA1UdEQQbMBmB\r\n" +
            "F2FudXAua3VtYXJAdWlkYWkubmV0LmluMBMGA1UdIwQMMAqACEzRvSoRSATTMB0G\r\n" +
            "A1UdDgQWBBRgl7mQb3UttrwwwusTkidEbBmwyzAMBgNVHRMBAf8EAjAAMA4GA1Ud\r\n" +
            "DwEB/wQEAwIFIDAZBgNVHSUBAf8EDzANBgsrBgEEAYI3CgMEATCBjAYDVR0gBIGE\r\n" +
            "MIGBMC0GBmCCZGQCAzAjMCEGCCsGAQUFBwICMBUaE0NsYXNzIDMgQ2VydGlmaWNh\r\n" +
            "dGUwUAYHYIJkZAEIAjBFMEMGCCsGAQUFBwIBFjdodHRwOi8vd3d3LmUtbXVkaHJh\r\n" +
            "LmNvbS9yZXBvc2l0b3J5L2Nwcy9lLU11ZGhyYV9DUFMucGRmMHsGCCsGAQUFBwEB\r\n" +
            "BG8wbTAkBggrBgEFBQcwAYYYaHR0cDovL29jc3AuZS1tdWRocmEuY29tMEUGCCsG\r\n" +
            "AQUFBzAChjlodHRwOi8vd3d3LmUtbXVkaHJhLmNvbS9yZXBvc2l0b3J5L2NhY2Vy\r\n" +
            "dHMvQzNPU0NBMjAxNC5jcnQwRwYDVR0fBEAwPjA8oDqgOIY2aHR0cDovL3d3dy5l\r\n" +
            "LW11ZGhyYS5jb20vcmVwb3NpdG9yeS9jcmxzL0MzT1NDQTIwMTQuY3JsMA0GCSqG\r\n" +
            "SIb3DQEBCwUAA4IBAQBMONK4yQPA9QPSwaSPsSygegmLAbAnWxZVeKF7iARnRj2e\r\n" +
            "KvC4kPP9fB+ZjeHnsPAbjRO3lLSKdqctMGCsQlbVwrnHKYhEdMHADwd1Q84bUwZf\r\n" +
            "CKfPVkz7A1pky/SNyCWsuFqo1YplmBJxHLO7PKiTpt15K+MqOrZI1FLyzDfh5DSd\r\n" +
            "g77YqUkqbgcqBjuiH7Zc8EtYj76olOXn+B4PHtZi5BG4IvGV+f/WtnHGLi700461\r\n" +
            "CgAWQ59/TzCsESmOpfAUpAlSBhKUamR/YhikeBG0kncxzPLOm+A8Thi2mZhSo6Jo\r\n" +
            "V+YRES1dPFxXa2c4/iD7RG1uHghzIz+vF4dQHNrF\r\n" +
            "-----END CERTIFICATE-----\r\n";

    public static String MOSIP_CERT_PREPROD = "-----BEGIN CERTIFICATE-----\r\n" +
            "MIIFkTCCBHmgAwIBAgIEAOATLTANBgkqhkiG9w0BAQsFADCBkDELMAkGA1UEBhMC\r\n" +
            "SU4xKjAoBgNVBAoTIWVNdWRocmEgQ29uc3VtZXIgU2VydmljZXMgTGltaXRlZDEd\r\n" +
            "MBsGA1UECxMUQ2VydGlmeWluZyBBdXRob3JpdHkxNjA0BgNVBAMTLWUtTXVkaHJh\r\n" +
            "IFN1YiBDQSBmb3IgQ2xhc3MgMyBPcmdhbmlzYXRpb24gMjAxNDAeFw0xNzEwMzEx\r\n" +
            "MzQwNTNaFw0yMDEwMzAxMzQwNTNaMIG+MQswCQYDVQQGEwJJTjEOMAwGA1UEChMF\r\n" +
            "VUlEQUkxGjAYBgNVBAsTEVRlY2hub2xvZ3kgQ2VudHJlMQ8wDQYDVQQREwY1NjAw\r\n" +
            "OTIxEjAQBgNVBAgTCUthcm5hdGFrYTFJMEcGA1UEBRNAODdiNzU2ZjQ5ZWZlY2Jh\r\n" +
            "MTczNzllOTY5N2JhMmNmMzFkYTdlOGY4NGE2ZThmNGRhOGQwNWIxODNmYjVkMWFk\r\n" +
            "OTETMBEGA1UEAxMKQW51cCBLdW1hcjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC\r\n" +
            "AQoCggEBAOlums2zjLx4cBx7MJGSN2hunAo3wuRu9Tk5bSkstTmIo4/Som8st2H5\r\n" +
            "Zy9YBpWNuqkT29LCqtzvJKu1WLClnRyrIWuEVxCerdJCb9+PXjjZwdUIIrGMyo0r\r\n" +
            "UzysN62ybPhYeC1Ab+eQge9JEmQsdrJJ9Mya3A/9/FdVPRG3pIPu9tvodYedtt/I\r\n" +
            "lhhDzzC7wfKKHXDY8ydU06anf+StC3GPiroqNHVKOWTI3ZiYKk9Qxnw9lOc3w4Ty\r\n" +
            "fYSO/NR3/2OeFfjFva+uJrnm2rBZmlb+ScmIkuY13os7OFA9vjLPiGkjCOpAaQZD\r\n" +
            "g8JzHiDSrKc01icLc/To/Dl/dVaLM9cCAwEAAaOCAcEwggG9MCIGA1UdEQQbMBmB\r\n" +
            "F2FudXAua3VtYXJAdWlkYWkubmV0LmluMBMGA1UdIwQMMAqACEzRvSoRSATTMB0G\r\n" +
            "A1UdDgQWBBQtqQG/bJDE3ZRmfzxsgLEIcF7BfzAOBgNVHQ8BAf8EBAMCBSAwgYwG\r\n" +
            "A1UdIASBhDCBgTAtBgZggmRkAgMwIzAhBggrBgEFBQcCAjAVGhNDbGFzcyAzIENl\r\n" +
            "cnRpZmljYXRlMFAGB2CCZGQBCAIwRTBDBggrBgEFBQcCARY3aHR0cDovL3d3dy5l\r\n" +
            "LW11ZGhyYS5jb20vcmVwb3NpdG9yeS9jcHMvZS1NdWRocmFfQ1BTLnBkZjB7Bggr\r\n" +
            "BgEFBQcBAQRvMG0wJAYIKwYBBQUHMAGGGGh0dHA6Ly9vY3NwLmUtbXVkaHJhLmNv\r\n" +
            "bTBFBggrBgEFBQcwAoY5aHR0cDovL3d3dy5lLW11ZGhyYS5jb20vcmVwb3NpdG9y\r\n" +
            "eS9jYWNlcnRzL0MzT1NDQTIwMTQuY3J0MEcGA1UdHwRAMD4wPKA6oDiGNmh0dHA6\r\n" +
            "Ly93d3cuZS1tdWRocmEuY29tL3JlcG9zaXRvcnkvY3Jscy9DM09TQ0EyMDE0LmNy\r\n" +
            "bDANBgkqhkiG9w0BAQsFAAOCAQEAOVx0mfOydPPGd3YW5qd28pWRPR3uq9hANq4e\r\n" +
            "c/Fqe1xg3dzlKWBWcUOLQqKuD5DEb6MYYzBCMe7iinDDzTNQFQq1y5rO+GD4WxN/\r\n" +
            "7mDkMnpdUju+vKi7AEF2wuqZBuNoSvdRqsq7ZgzLZXrdYwYLXaxpxcQ0QlzhECdv\r\n" +
            "/K2AGf/wv8nh/BIckHZuJSs5MrCZtiKS84tpXHHHL/Cjd0y3UO+35VvxFOZ50BQr\r\n" +
            "i4XEIDYQd0liyHwTWkv7CoxTYHO9DPPttd1s9nsY1mHSGGWLWUoy3v1yc4iFaw28\r\n" +
            "GXUxpP5A9BfwWFbeqaHx8Tcn0x8YB1r/dAQlE0G/VTsEiugxig==\r\n" +
            "-----END CERTIFICATE-----\r\n";
}
