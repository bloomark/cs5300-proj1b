package rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.omg.PortableServer.REQUEST_PROCESSING_POLICY_ID;

import session_management.SSMServlet;
import session_management.SessionData;

public class RPCServer extends Thread{
	public static int RPC_SERVER_PORT = 5300;
	public static int SOCKET_TIMEOUT = 2 * 1000;
	public static int MAX_PACKET_LENGTH = 512;
	public static String DELIMITER = SessionData.DELIMITER;
	
	DatagramSocket rpc_server_socket = null;
	
	public RPCServer(){
		System.out.println("RPC Server initializing...");
		try{
			rpc_server_socket = new DatagramSocket(RPC_SERVER_PORT);
		} catch(SocketException e){
			e.printStackTrace();
		}
		System.out.println("SERVER Initialized at Port " + rpc_server_socket.getLocalPort() + "...");
	}
	
	public void run(){
		while(true){
			String response_string = null;
			String response_value = null;
			String[] request_fields = null;
			byte[] outbuf = new byte[MAX_PACKET_LENGTH];
			byte[] inbuf = new byte[MAX_PACKET_LENGTH];
			DatagramPacket recv_pkt = new DatagramPacket(inbuf, inbuf.length);
			
			try{
				//TODO Server code
				/*
				 * Wait for RPC call from client on RPC_SERVER_PORT
				 * Look at the opcode and perform 
				 */
				System.out.println("SERVER Waiting for client request");
				rpc_server_socket.receive(recv_pkt);
				
				System.out.println("SERVER Received client request");
				InetAddress returnAddr = recv_pkt.getAddress();
				int return_port = recv_pkt.getPort();
				
				/*
				 * request_fields[] - Split the message received in the datagram on the DELIMITER
				 * Indexes
				 * [0] - callId
				 * [1] - operation_code
				 */
				
				request_fields = new String(inbuf, "UTF-8").split(DELIMITER);
				switch(Integer.valueOf(request_fields[1])){
					case 0:
						//Session Read
						// [2] - sessionId
						System.out.println("SERVER Received sessionRead request from server at " + recv_pkt.getAddress().getHostAddress() + "for sessionId #" + request_fields[2]);
						response_value = sessionRead(request_fields[2]);
						break;
					case 1:
						//Session Write
						break;
					case 2:
						//Exchange Views
						break;
				}
			} catch(Exception e){
				e.printStackTrace();
			}
			
			//Build and send response
			response_string = request_fields[0] + DELIMITER + response_value;
			outbuf = response_string.getBytes();
			InetAddress client_address = recv_pkt.getAddress();
			System.out.println("Server sending response string " + response_string + " to client IP "+ client_address);
			DatagramPacket send_pkt = new DatagramPacket(outbuf, outbuf.length, client_address, recv_pkt.getPort());
		}
	}
	
	private String sessionRead(String sessionId){
		if(!SSMServlet.sessionMap.containsKey(sessionId)){
			System.out.println(sessionId + " Not found");
			return "";
		}
		/*
		 * Map has relevant data, just return toString of the data
		 */
		System.out.println("In sessionRead " + sessionId + " is " + SSMServlet.sessionMap.get(sessionId).toString());
		return SSMServlet.sessionMap.get(sessionId).toString();
	}
}