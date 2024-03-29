package session_management;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Timer;
//import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rpc.RPCClient;
import rpc.RPCServer;

/**
 * Servlet implementation class SSMServlet
 */
@WebServlet(value = "/SSMServlet", loadOnStartup = 1)
public class SSMServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public static long DELTA = 1 * 1000;
	public static long TIMEOUT = 10 * 1000; //Timeout in milliseconds
	public static String COOKIE_NAME = "CS5300PROJ1SESSION";
	public static String globalSessionId = "0";
	public static ConcurrentHashMap<String, SessionData> sessionMap = new ConcurrentHashMap<String, SessionData>();
	public static long cleanerDaemonInterval = 60 * 1000;
	public static ServerViewTable serverViewTable = new ServerViewTable();
	public static String network_address = null;
	public static String DELIMITER = SessionData.DELIMITER;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		
	}
	
	/**
     * @see HttpServlet#HttpServlet()
     */
    public SSMServlet() {
        super();
        // TODO Auto-generated constructor stub
        SessionData newTableEntry = new SessionData(1, "deadbeef!", System.currentTimeMillis() + 84000000);
        sessionMap.put("100", newTableEntry);
        
        do{
        	getNetworkAddress();
        } while(network_address == null);
        ServerViewTableEntry new_view_entry = new ServerViewTableEntry(true, System.currentTimeMillis());
        serverViewTable.serverViewTable.put(network_address, new_view_entry);
        System.out.println("SERVLET Local network address = " + network_address + "...");        
        
        System.out.println("SERVLET Setting up cleaner task...");
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new MapCleanerDaemon(), 5*1000, cleanerDaemonInterval);
        
        System.out.println("SERVLET Starting RPC Server...");
        RPCServer rpc_server = new RPCServer();
        rpc_server.start();
        
        new_view_entry = new ServerViewTableEntry(true, System.currentTimeMillis());
        serverViewTable.serverViewTable.put("54.152.48.225", new_view_entry);
        new_view_entry = new ServerViewTableEntry(true, System.currentTimeMillis());
        serverViewTable.serverViewTable.put("54.172.159.86", new_view_entry);
        
        /*
         * Test for sessionWrite
         * */
        /*
        SessionData write_data = new SessionData(99, "Foo", System.currentTimeMillis());
        String write_response = writeRemoteSessionData("105", write_data);
        if(write_response != null && write_response.equals("OK")){
        	System.out.println("TEST Object at 105 is " + sessionMap.get("105").toString());
        }
        else{
        	System.out.println("TEST sessionWrite FAILED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }//*/
        
        /*
         * Test for sessionRead
         */
        /*
        SessionData new_entry = new SessionData(100, "Fubar", System.currentTimeMillis());
        sessionMap.put("100", new_entry);
        SessionData read_response = readRemoteSessionData("100", "127.0.0.1", "null");
        if(read_response == null){
        	System.out.println("TEST sessionRead FAILED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        } else{
        	System.out.println("TEST Received " + read_response.toString());
        }
        
        read_response = readRemoteSessionData("101", "127.0.0.1", "null");
        if(read_response != null){
        	System.out.println("TEST sessionRead FAILED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }//*/
        
        /*
         * Test for mergeView
         */
        ///*
        /*ServerViewTableEntry new_view_entry = new ServerViewTableEntry(false, 10);
        serverViewTable.serverViewTable.put("10.0.0.1", new_view_entry);
        new_view_entry = new ServerViewTableEntry(true, 5);
        serverViewTable.serverViewTable.put("10.0.0.2", new_view_entry);
        new_view_entry = new ServerViewTableEntry(true, System.currentTimeMillis());
        serverViewTable.serverViewTable.put("10.0.0.4", new_view_entry);
        
        System.out.println(serverViewTable.toString());
        //mergeViewTable();
        //System.out.println(serverViewTable.toString());//*/
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Session Data
		String sessionID = null;
		Integer version = 1;
		long expiresOn = 0;
		String primary = null;
		String backup = null;
		String cookieContent = null;
		Cookie cookie = null;
		
		PrintWriter o = response.getWriter();
		String action = request.getParameter("btn-submit");
		Boolean createNewCookie = true;
		
		synchronized(sessionMap){
			//Check to see if a cookie exists
			Cookie[] cookieList = request.getCookies();
			
			if(cookieList != null){
				for(int i=0; i<cookieList.length; i++){
					if(cookieList[i].getName().equals(COOKIE_NAME)){
						cookie = cookieList[i];
					}
				}
			}
			
			if(cookie != null){
				//A cookie exists
				createNewCookie = false;
				//System.out.println("A cookie exists");
				cookieContent = cookie.getValue();
				String[] stringList = cookieContent.split("_");
				sessionID = stringList[0];
				version = Integer.parseInt(stringList[1]);
				version++;
				primary = stringList[2].trim();
				backup = stringList[3].trim();
				
				if(!primary.equals(network_address) && !backup.equals(network_address)){
					sessionMap.remove(sessionID);
					SessionData remote_entry = readRemoteSessionData(sessionID, primary, backup);
					if(remote_entry == null){
						createNewCookie = true;
					}
					else{
						sessionMap.put(sessionID, remote_entry);
					}
				}
				
				if(sessionMap.containsKey(sessionID)){
					//We know about this cookie
					//Check timeout
					long cookieTime = sessionMap.get(sessionID).getExpiresOn();
					long ts = System.currentTimeMillis();
					if(cookieTime > ts){
						//Cookie has not timed out
						sessionMap.get(sessionID).setExpiresOn(System.currentTimeMillis() + TIMEOUT);
						if(action != null){
							if(action.equals("Replace")){
								//Replace was clicked
								String newMessage = request.getParameter("newMessage");
								if(newMessage != null){
									if(newMessage.length() > 256){
										newMessage = newMessage.substring(0, 255);
									}
								}
								else{
									newMessage = " ";
								}
								sessionMap.get(sessionID).setMessage(newMessage);
							}
							else if(action.equals("Refresh")){
								//Refresh was clicked
							}
							else if(action.equals("Logout")){
								//Logout was clicked
								sessionMap.remove(sessionID);
								cookieContent = sessionID + DELIMITER + version + DELIMITER + "NULL" + DELIMITER + "NULL";
								Cookie session = new Cookie(COOKIE_NAME, cookieContent);
								session.setMaxAge(0);
								response.addCookie(session);
								o.println("<h1>Logged out</h1>");
								return;
							}
						}
					}
					else{
						//Cookie has expired, create a new one
						createNewCookie = true;
					}
				}
				else{
					//Unknown cookie, create a new one
					createNewCookie = true;
				}
			}
			
			if(createNewCookie){
				version = 1;
				sessionID = getNewSessionId();
				//Increment timestamp by TIMEOUT milliseconds
				expiresOn = System.currentTimeMillis() + TIMEOUT;
				SessionData newTableEntry = new SessionData(1, "Hello, User!", expiresOn);
				sessionMap.put(sessionID, newTableEntry);
			}
			
			backup = writeRemoteSessionData(sessionID, sessionMap.get(sessionID));
			
			cookieContent = sessionID + DELIMITER + version + DELIMITER + network_address + DELIMITER + backup;
			Cookie session = new Cookie(COOKIE_NAME, cookieContent);
			session.setMaxAge((int)(TIMEOUT/1000));
			response.addCookie(session);
		}
		
		//request.setAttribute("sessionID", sessionID);
		//request.setAttribute("version", version);
		request.setAttribute("expiresOn", new Timestamp(sessionMap.get(sessionID).getExpiresOn()));
		request.setAttribute("message", sessionMap.get(sessionID).getMessage());
		request.getRequestDispatcher("index.jsp").forward(request, response);
		return;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	private String getNewSessionId(){
		globalSessionId = String.valueOf(Integer.valueOf(globalSessionId) + 1);
		return network_address + "." + globalSessionId;
	}
	
	private SessionData readRemoteSessionData(String sessionId, String primary, String backup){
		String new_session_string = null;
		
		if(!primary.equals("NULL")){
			new_session_string = RPCClient.SessionReadClient(sessionId, primary).trim();
			if(new_session_string.equals("NULL") || new_session_string.equals("ERROR")){
				if(!backup.equals("NULL")){
					new_session_string = RPCClient.SessionReadClient(sessionId, backup).trim();
					if(new_session_string.equals("NULL") || new_session_string.equals("ERROR")){
						return null;
					}
				}
			}
		}
		
		//We have session data in a string, convert into an object of sessionData and return
		return new SessionData(new_session_string);
	}
	
	private String writeRemoteSessionData(String sessionId, SessionData sessionData){
		/*
		 * Pick a random server from the view, for now lets make it 127.0.0.1
		 * String server = random_entry_from_view
		 */
		String server = serverViewTable.getRandomKey();
		
		if(server == "NULL"){
			System.out.println("SERVER Could not find a backup");
			return "NULL";
		}
		
		sessionData.expiresOn = System.currentTimeMillis() + TIMEOUT + DELTA;
		String result = RPCClient.SessionWriteClient(sessionId, sessionData.toString(), server).trim();
		
		if(result.equals("OK")){
			return server;
		}
		else{
			return "NULL";
		}
	}
	
	private void mergeViewTable(){
		/*
		 * Pick up a random IP address, and call the RPC mergeViewsClient
		 */
		String server = "127.0.0.1";
		String remote_server_view_string = RPCClient.ExchangeViewsClient(serverViewTable.toString(), server).trim();
		if(!remote_server_view_string.equals("ERROR") || !remote_server_view_string.equals("NULL")){
			serverViewTable.mergeViews(remote_server_view_string);
		}
	}
	
	public void getNetworkAddress(){
		/*
		 * From http://stackoverflow.com/questions/2939218/getting-the-external-ip-address-in-java
		 */
		URL whatismyip = null;
		try {
			whatismyip = new URL("http://checkip.amazonaws.com");
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            network_address = in.readLine();
        } catch (IOException e) {
			e.printStackTrace();
		} finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
}
