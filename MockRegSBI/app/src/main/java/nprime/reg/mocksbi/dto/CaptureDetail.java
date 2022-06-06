package nprime.reg.mocksbi.dto;

public class CaptureDetail {

    public java.lang.String specVersion;
    public java.lang.String data;
    public java.lang.String hash;
    public Error error;

    @Override
    public String toString() {
        return "CaptureDetail{" +
                "specVersion='" + specVersion + '\'' +
                ", data='" + data + '\'' +
                ", hash='" + hash + '\'' +
                ", error=" + error +
                '}';
    }
}
