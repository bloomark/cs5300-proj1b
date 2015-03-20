package session_management;

import java.sql.Timestamp;

import session_management.SSMServlet;

public class SessionTable {
	int version;
	String message;
	long expiresOn;
	
	public SessionTable(int version, String message, long expiresOn){
		this.version = version;
		this.message = message;
		this.expiresOn = expiresOn;
	}
	
	public int getVersion(){
		return version;
	}
	
	public void setVersion(int version){
		this.version = version;
	}
	
	public String getMessage(){
		return message;
	}
	
	public void setMessage(String message){
		this.message = message;
	}
	
	public long getExpiresOn(){
		return expiresOn;
	}
	
	public void setExpiresOn(long expiresOn){
		this.expiresOn = expiresOn;
	}
}
