package nprime.reg.mocksbi.dto;

public class Error {

    public String errorCode;
    public String errorInfo;

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public Error() {}

    public Error(String code, String info) {
        this.errorCode = code;
        this.errorInfo = info;
    }

    @Override
    public String toString() {
        return "Error{" +
                "errorCode='" + errorCode + '\'' +
                ", errorInfo='" + errorInfo + '\'' +
                '}';
    }
}
