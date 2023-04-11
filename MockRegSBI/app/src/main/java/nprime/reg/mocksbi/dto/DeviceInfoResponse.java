package nprime.reg.mocksbi.dto;

public class DeviceInfoResponse {

    public String deviceInfo;
    public Error error;
    public DeviceInfoResponse() { }
    public DeviceInfoResponse(String deviceInfo, Error error) {
        this.deviceInfo = deviceInfo;
        this.error = error;
    }

    @Override
    public String toString() {
        return "DeviceInfoResponse{" +
                "deviceInfo='" + deviceInfo + '\'' +
                ", error=" + error +
                '}';
    }
}
