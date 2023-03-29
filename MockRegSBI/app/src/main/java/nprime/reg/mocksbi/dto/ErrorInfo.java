package nprime.reg.mocksbi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class ErrorInfo {
    public String errorCode;
    public String errorInfo;

    public ErrorInfo(String errorCode, String errorInfo) {
        super();
        this.errorCode = errorCode;
        this.errorInfo = errorInfo;
    }
}
