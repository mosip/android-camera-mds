package nprime.reg.mocksbi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public class CaptureDetail {

    public String specVersion;
    public String data;
    public String hash;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String sessionKey;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String thumbprint;
    public Error error;

    @Override
    public String toString() {
        return "CaptureDetail{" +
                "specVersion='" + specVersion + '\'' +
                ", data='" + data + '\'' +
                ", hash='" + hash + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                ", thumbprint='" + thumbprint + '\'' +
                ", error=" + error +
                '}';
    }

}
