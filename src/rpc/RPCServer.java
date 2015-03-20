package rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class RPCServer extends Thread{
	public static int RPC_SERVER_PORT = 5300;
	public static int SOCKET_TIMEOUT = 2 * 1000;
	public static int MAX_PACKET_LENGTH = 512;
	public static String DELIMITER = "_";
	
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
			try{
				//TODO Server code
				/*
				 * Wait for RPC call from client on RPC_SERVER_PORT
				 * Look at the opcode and perform 
				 */
				byte[] inbuf = new byte[MAX_PACKET_LENGTH];
				DatagramPacket recv_pkt = new DatagramPacket(inbuf, inbuf.length);
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
				
				String[] request_fields = new String(inbuf, "UTF-8").split(DELIMITER);
				switch(Integer.valueOf(request_fields[1])){
				case 0:
					//Session Read
					// [2] - sessionId
					System.out.println("SERVER Received sessionRead request for sessionId #" + request_fields[2]);
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
		}
	}
}