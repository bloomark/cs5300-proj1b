package rpc;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.sql.Timestamp;
import java.util.UUID;

import session_management.SessionTable;

public class RPCClient {
	public static int RPC_SERVER_PORT = 5300;
	public static int SOCKET_TIMEOUT = 2 * 1000;
	public static int MAX_PACKET_LENGTH = 512;
	public static String DELIMITER = "_";
	
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
	public static SessionTable SessionReadClient(
				String sessionId
			){
		//TODO SessionReadClient code
		SessionTable existing_session_data = null;
		
		String callId = UUID.randomUUID().toString();
		String operation_code = "0";
		
		String request_message = callId + DELIMITER + operation_code + DELIMITER + sessionId;
		byte[] outbuf = new byte[MAX_PACKET_LENGTH];
		
		try{
			DatagramSocket rpcSocket = new DatagramSocket();
			DatagramPacket send_pkt = new DatagramPacket(outbuf, );
		} catch(SocketException e){
			e.printStackTrace();
		}
		
		return existing_session_data;
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
