package session_management;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Pattern;

public class ServerViewTable {
	//Thread safe session table
	protected static ConcurrentHashMap<String, ServerViewTableEntry> serverViewTable = new ConcurrentHashMap<String, ServerViewTableEntry>();
	
	//create or update and an server view table entry
	public void upsertViewTableEntry(String ipAddress, Boolean status, long timestamp)
	{
		if(serverViewTable.containsKey(ipAddress))//update
		{
			serverViewTable.get(ipAddress).updateViewTableEntry(status, timestamp);
		}
		else//create
		{
			ServerViewTableEntry newEntry = new ServerViewTableEntry(status, timestamp);
			serverViewTable.put(ipAddress, newEntry);
		}
	}
	
	/*
	 * Entire view table as a string for exchanging views
	 * format ipaddress1>status1_timestamp2,ipaddress2>status2_timestamp2,ipaddress3>status3_timestamp3
	 */
	@Override
	public String toString() {
				
		String returnString = new String();
		
		for(Map.Entry<String, ServerViewTableEntry> entry : ServerViewTable.serverViewTable.entrySet())
		{
			returnString += entry.getKey() + ">" + entry.getValue().toString() + ",";
		}
		
		if (returnString.length() > 0) //removing the last ,
		{
			returnString = returnString.substring(0, returnString.length()-1);
		}
		
		return returnString;
	}
	
	/*
	 * Merges a view as a string with the current view.
	 * ViewB needs to be in the toString format of ServerViewTbale class
	 */
	public void mergeViews(String viewB)
	{
		if(viewB.length() == 0)
			return;
		
		String[] ipToEntriesOfB = viewB.split(java.util.regex.Pattern.quote(","));
		
		for(String ipEntry : ipToEntriesOfB)
		{
			//constructing view table entry of B	
			ServerViewTableEntry viewBEntry = constructTableEntryHelper(ipEntry);
			String ip = ipEntry.split(java.util.regex.Pattern.quote(">"))[0];
					
			if(serverViewTable.containsKey(ip))
			{
				ServerViewTableEntry viewAEntry = serverViewTable.get(ip);
				if(viewBEntry.getTimestamp() > viewAEntry.getTimestamp())
					serverViewTable.put(ip, viewBEntry);
			}
			else
			{
				serverViewTable.put(ip, viewBEntry);
			}
		}
		
	}
	
	private ServerViewTableEntry constructTableEntryHelper(String entry)
	{
		String[] entries = entry.split(java.util.regex.Pattern.quote(">"));
		
		String[] status_timestamp = entries[1].split(java.util.regex.Pattern.quote("_"));
		
		Boolean status = Boolean.valueOf(status_timestamp[0]);
		long timestamp = Long.valueOf(status_timestamp[1]);
		
		return new ServerViewTableEntry(status, timestamp);
	}
}
