package session_management;

import java.sql.Timestamp;

import session_management.SSMServlet;

public class SessionTable {
	int version;
	String message;
	Timestamp expiresOn;
	
	public SessionTable(int version, String message, Timestamp expiresOn){
		this.version = version;
		this.message = message;
		this.expiresOn = new Timestamp(System.currentTimeMillis() + SSMServlet.TIMEOUT);
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
	
	public Timestamp getExpiresOn(){
		return expiresOn;
	}
	
	public void setExpiresOn(Timestamp expiresOn){
		this.expiresOn = expiresOn;
	}
}
