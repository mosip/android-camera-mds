package nprime.reg.sbi.utility;

import java.util.HashMap;
import java.util.Map;

public class ErrorList {
	   
	private Map<String, String> errorMap = new HashMap<String, String>();
	
	public ErrorList() {
		// TODO Auto-generated constructor stub
		Initialize();
	}
	
	private void Initialize(){
		errorMap.put("MDSRVMSG_0000" , "");
		errorMap.put("MDSRVMSG_0001" , "Device is already Registered.");
		errorMap.put("MDSRVMSG_0002" , "MDS Version is uptodate!!!");
		errorMap.put("MDSRVMSG_0003" , "Device Registration Failed");
		errorMap.put("MDSRVMSG_0004" , "Device is provisioned and ready.");
		errorMap.put("MDSRVMSG_0005" , "Failed to register device, MD license expired.");
		errorMap.put("MDSRVMSG_0006" , "New MDS Version Available!!!");
		errorMap.put("MDSRVMSG_0007" , "MOSIP Cert update is Successful");
		errorMap.put("MDSRVMSG_0008" , "MOSIP Cert Update Failed");
		errorMap.put("MDSRVMSG_0009" , "Invalid Device, Host Authentication failed");
		errorMap.put("MDSRVMSG_0010" , "Host Authentication failed during software update check!!");
		errorMap.put("MDSRVMSG_0012" , "Please download RDSVersion");
		errorMap.put("MDSRVMSG_0013" , "Invalid Device, cannot proceed");
		errorMap.put("MDSRVMSG_0014" , "Device Key Rotated!!!");
		errorMap.put("MDSRVMSG_0015" , "MOSIP Certificates are available for Update!!!");
		errorMap.put("MDSRVMSG_0016" , "Unable to fetch Server Timestamp, cannot start Init process");
		errorMap.put("MDSRVMSG_0017" , "MD Service Started Successfully!!!");
		errorMap.put("MDSRVMSG_0018" , "Init done with different device, Please restart MD Service...");
		errorMap.put("MDSRVMSG_0019" , "Device is not Compatible on current host");
		errorMap.put("MDSRVMSG_0020" , "MD License expired or invalid device.");
		errorMap.put("MDSRVMSG_0021" , "MD License about to expire.");
	}
	
	
	public String getMessage(String msgID){
		String messageString = "Unknown Message";
		try{
			messageString = errorMap.get(msgID);			
		}
		catch(Exception ex){
			
		}
		return messageString;
	}
}
