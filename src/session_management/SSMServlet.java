package session_management;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Timer;
//import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;

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
@WebServlet("/SSMServlet")
public class SSMServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public static long DELTA = 1 * 1000;
	public static long TIMEOUT = 10 * 1000; //Timeout in seconds
	public static String COOKIE_NAME = "CS5300PROJ1SESSION";
	public static String globalSessionId = "0";
	public static ConcurrentHashMap<String, SessionData> sessionMap = new ConcurrentHashMap<String, SessionData>();
	public static long cleanerDaemonInterval = 60 * 1000;
	public static ServerViewTable serverViewTable = new ServerViewTable();
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SSMServlet() {
        super();
        // TODO Auto-generated constructor stub
        SessionData newTableEntry = new SessionData(1, "deadbeef!", System.currentTimeMillis() + 84000000);
        sessionMap.put("100", newTableEntry);
        
        System.out.println("SERVLET Setting up cleaner task...");
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new MapCleanerDaemon(), 5*1000, cleanerDaemonInterval);
        
        System.out.println("SERVLET Starting RPC Server...");
        RPCServer rpc_server = new RPCServer();
        rpc_server.start();
        
        /*
         * Test for sessionWrite
         * */
        /*
        SessionData write_data = new SessionData(99, "Foo", System.currentTimeMillis());
        String write_response = writeRemoteSessionData("105", write_data);
        if(write_response.equals("OK")){
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
        ServerViewTableEntry new_view_entry = new ServerViewTableEntry(true, System.currentTimeMillis());
        serverViewTable.serverViewTable.put("10.0.0.1", new_view_entry);
        new_view_entry = new ServerViewTableEntry(true, System.currentTimeMillis());
        serverViewTable.serverViewTable.put("10.0.0.2", new_view_entry);
        System.out.println(serverViewTable.toString());
        mergeViewTable();
        System.out.println(serverViewTable.toString());
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Session Data
		String sessionID = null;
		Integer version = 1;
		long expiresOn = 0;
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
				//System.out.println("Creating new cookie");
				version = 1;
				//sessionID = UUID.randomUUID().toString();
				sessionID = getNewSessionId();
				//System.out.println("New Session ID - " + sessionID);
				//Increment timestamp by TIMEOUT milliseconds
				expiresOn = System.currentTimeMillis() + TIMEOUT;
				//System.out.println("Expires On - " + expiresOn.toString());
				SessionData newTableEntry = new SessionData(1, "Hello, User!", expiresOn);
				sessionMap.put(sessionID, newTableEntry);
			}
			
			cookieContent = sessionID + '_' + version;
			Cookie session = new Cookie(COOKIE_NAME, cookieContent);
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
		return globalSessionId;
	}
	
	private SessionData readRemoteSessionData(String sessionId, String primary, String backup){
		String new_session_string = null;
		
		new_session_string = RPCClient.SessionReadClient(sessionId, primary);
		if(new_session_string.trim().equals("NULL")){
			return null;
			/*new_session_string = RPCClient.SessionReadClient(sessionId, backup);
			if(new_session_string == null){
				return null;
			}*/
		}
		
		//We have session data in a string, convert into an object of sessionData and return
		return new SessionData(new_session_string);
	}
	
	private String writeRemoteSessionData(String sessionId, SessionData sessionData){
		/*
		 * Pick a random server from the view, for now lets make it 127.0.0.1
		 * String server = random_entry_from_view
		 */
		String server = "127.0.0.1";
		sessionData.expiresOn = System.currentTimeMillis() + TIMEOUT + DELTA;
		String result = RPCClient.SessionWriteClient(sessionId, sessionData.toString(), server);
		
		if(result.trim().equals("OK")){
			return "OK";
		}
		else{
			return null;
		}
	}
	
	private void mergeViewTable(){
		/*
		 * Pick up a random IP address, and call the RPC mergeViewsClient
		 */
		String server = "127.0.0.1";
		String remote_server_view_string = RPCClient.ExchangeViewsClient(serverViewTable.toString(), server);
		serverViewTable.mergeViews(remote_server_view_string);
	}
}
