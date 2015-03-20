package session_management;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Timer;
//import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SSMServlet
 */
@WebServlet("/SSMServlet")
public class SSMServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public static long TIMEOUT = 30 * 1000; //Timeout in milliseconds
	public static String COOKIE_NAME = "CS5300PROJ1SESSION";
	public static String globalSessionId = "0";
	public static ConcurrentHashMap<String, SessionTable> sessionMap = new ConcurrentHashMap<String, SessionTable>();
	   
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SSMServlet() {
        super();
        // TODO Auto-generated constructor stub
        System.out.println("Initializing cleaner");
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new MapCleanerDaemon(), 5*1000, 10*1000);
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Session Data
		String sessionID = null;
		Integer version = 1;
		Timestamp expiresOn = null;
		String cookieContent = null;
		Cookie cookie = null;
		
		PrintWriter o = response.getWriter();
		String action = request.getParameter("btn-submit");
		ConcurrentHashMap<String, SessionTable> sessMap = getSessionMap();
		Boolean createNewCookie = true;
		
		synchronized(sessMap){
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
				System.out.println("A cookie exists");
				cookieContent = cookie.getValue();
				String[] stringList = cookieContent.split("_");
				sessionID = stringList[0];
				version = Integer.parseInt(stringList[1]);
				version++;
				
				if(sessMap.containsKey(sessionID)){
					//We know about this cookie
					//Check timeout
					Timestamp cookieTime = sessMap.get(sessionID).getExpiresOn();
					Timestamp ts = new Timestamp(System.currentTimeMillis());
					if(cookieTime.after(ts)){
						//Cookie has not timed out
						sessMap.get(sessionID).setExpiresOn(new Timestamp(System.currentTimeMillis() + TIMEOUT));
						if(action != null){
							if(action.equals("Replace")){
								//Replace was clicked
								String newMessage = request.getParameter("newMessage");
								if(newMessage != null){
								}
								else{
									newMessage = " ";
								}
								sessMap.get(sessionID).setMessage(newMessage);
							}
							else if(action.equals("Refresh")){
								//Refresh was clicked
							}
							else if(action.equals("Logout")){
								//Logout was clicked
								sessMap.remove(sessionID);
								o.println("<h1>Logged out</h1>");
								return;
							}
						}
					}
					else{
						//Cookie has expired, create a new one
						System.out.println("Timed out");
						System.out.println("Cookie Time = " + cookieTime.toString());
						System.out.println("Curr Time = " + ts.toString());
						createNewCookie = true;
					}
				}
				else{
					//Unknown cookie, create a new one
					createNewCookie = true;
				}
			}
			
			if(createNewCookie){
				System.out.println("Creating new cookie");
				version = 1;
				//sessionID = UUID.randomUUID().toString();
				sessionID = getNewSessionId();
				System.out.println("New Session ID - " + sessionID);
					//Increment timestamp by 60 seconds
				expiresOn = new Timestamp(System.currentTimeMillis() + TIMEOUT);
				System.out.println("Expires On - " + expiresOn.toString());
				SessionTable newTableEntry = new SessionTable(1, "Hello, User!", expiresOn);
				sessMap.put(sessionID, newTableEntry);
			}
			
			cookieContent = sessionID + '_' + version;
			Cookie session = new Cookie(COOKIE_NAME, cookieContent);
			response.addCookie(session);
		}
		
		//request.setAttribute("sessionID", sessionID);
		//request.setAttribute("version", version);
		request.setAttribute("expiresOn", sessMap.get(sessionID).getExpiresOn());
		request.setAttribute("message", sessMap.get(sessionID).getMessage());
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

	private ConcurrentHashMap<String, SessionTable> getSessionMap(){
		return SSMServlet.sessionMap;
	}
	
	private String getNewSessionId(){
		globalSessionId = String.valueOf(Integer.valueOf(globalSessionId) + 1);
		return globalSessionId;
	}
}
