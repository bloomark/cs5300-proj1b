package rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.UUID;

import session_management.SessionData;

public class RPCClient {
	public static int RPC_SERVER_PORT = 5300;
	public static int SOCKET_TIMEOUT = 5 * 1000;
	public static int MAX_PACKET_LENGTH = 512;
	public static String DELIMITER = SessionData.DELIMITER;
	
	/*
	 * I/P: sessionId for which we require data
	 * 
	 * Pick up the primary and backup servers from the sessMap
	 * Generate a unique callId
	 * Set operation code to '0'
	 * Make a call to the primary server with callID + '0' + sessionId
	 * 		If no response is received from primary
	 * 			Make a call to the backup server
	 * 			If no response is received from secondary
	 * 				return null
	 * O/P: session data 
	 * 		null if no response is received 
	 */
	public static String SessionReadClient(
				String sessionId,
				String server
			){
		//TODO SessionReadClient code
		String new_session_data = null;
		String callId = UUID.randomUUID().toString();
		String operation_code = "0";
		
		System.out.println("CLIENT Building request string");
		String request_message = callId + DELIMITER + operation_code + DELIMITER + sessionId;
		byte[] outbuf = new byte[MAX_PACKET_LENGTH];
		outbuf = request_message.getBytes();
		DatagramSocket rpc_socket = null;
		
		/*
		 * Making a request to the server
		 * Need to run through a list of servers and make a request to each one
		 */
		try{
			rpc_socket = new DatagramSocket();
			InetAddress server_address = InetAddress.getByName(server);
			DatagramPacket send_pkt = new DatagramPacket(outbuf, outbuf.length, server_address, RPC_SERVER_PORT);
			
			rpc_socket.send(send_pkt);
			System.out.println("CLIENT Sent request " + request_message + " to server at " + server);
		} catch(SocketException e){
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] response_fields = null;
		
		/*
		 * Waiting for a response from the server
		 * If the callId received is not what was sent, need to wait for the response again
		 * If the socket times out, update server status to down
		 */
		try{
			System.out.println("CLIENT Waiting for response");
			do{
				rpc_socket.setSoTimeout(SOCKET_TIMEOUT);
				byte[] inbuf = new byte[MAX_PACKET_LENGTH];
				DatagramPacket rcv_pkt = new DatagramPacket(inbuf, inbuf.length);
				
				System.out.println("CLIENT Waiting for response/");
				rpc_socket.receive(rcv_pkt);
				
				response_fields = new String(inbuf, "UTF-8").split(DELIMITER);
				System.out.println("CLIENT Received response " + new String(inbuf, "UTF-8"));
			} while(!response_fields[0].equals(callId));
		} catch(SocketTimeoutException e){
			// TODO Auto-generated catch block
			System.out.println("CLIENT serverReadClient response timed out");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e){
			e.printStackTrace();
		}
		
		System.out.println("CLIENT ReadClient returning " + response_fields[1] + DELIMITER + response_fields[2] + DELIMITER + response_fields[3]);
		return response_fields[1] + DELIMITER + response_fields[2] + DELIMITER + response_fields[3];
	}
	
	public static void SessionWriteClient(
				String sessionId,
				int version,
				Timestamp discard_time
			){
		//TODO SessionWriteClient code
		
		return;
	}
	
	/*
	public static void ExchangeViews(
				View v
			){
		
	}*/
}
