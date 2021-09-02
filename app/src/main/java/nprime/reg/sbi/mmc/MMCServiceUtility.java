package nprime.reg.sbi.mmc;

import java.util.Properties;

import nprime.reg.sbi.utility.CommonDeviceAPI;
import nprime.reg.sbi.utility.DeviceConstants;
import nprime.reg.sbi.utility.Logger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MMCServiceUtility {
    private String strUserName = "";
    Properties properties;
    CommonDeviceAPI tmfCommonDeviceAPI;

    public MMCServiceUtility() {
        tmfCommonDeviceAPI = new CommonDeviceAPI();
    }

    public String getDataFromMMC(String msbDeviceSnapshotRequest) {
        String serverResponse = null;
        String strServerUrl="";

        try
        {
            String mgmtSrvrURL = DeviceConstants.mgmtSvrIpAddress; //getMgmtSrvIpAddress();
            Response httpResponse = null;

            if (mgmtSrvrURL.toLowerCase().contains("https") || mgmtSrvrURL.toLowerCase().contains("http")){
                strServerUrl = mgmtSrvrURL + DeviceConstants.webFolder;
                OkHttpClient httpclient = new MDServiceUtility().getHttpClient();
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(mediaType, msbDeviceSnapshotRequest);
                Request request = new Request.Builder().url(strServerUrl).post(body).build();
                //HttpPost httppost = new HttpPost(strServerUrl);
                //httppost.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
                httpResponse = httpclient.newCall(request).execute();
                if(200 != httpResponse.code()){
                    //showToast("Check Internet connection");
                }else {
                    serverResponse = httpResponse.body().string();
                }
            }
			/*else
			{
                strServerUrl = mgmtSrvrURL + DeviceConstants.webFolder;

				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(strServerUrl);
				httppost.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
				httpResponse = httpclient.execute(httppost);
			}*/
        }
        catch(Exception ex)
        {
            Logger.e(DeviceConstants.LOG_TAG,  ex.getMessage());
        }
        return serverResponse;
    }
}
