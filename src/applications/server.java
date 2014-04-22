/*
 * A sample server that uses DatagramService
 */
package applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;

import services.DatagramService;
import services.TtpService;
import services.ttpServer;
import datatypes.Datagram;

public class server implements Runnable{

	private static DatagramService ds;
	static ttpServer ttp = new ttpServer((short)5024,(short)5000 , "127.0.0.1", "127.0.0.1");
	static String contents = "";
	static server s = new server();
	
	static Thread sendPackets = new Thread(s);
	
	/*
	 * 1. Handshake
	 * 2. Is filename from client valid? If valid, read contents
         * 3. Thread1 - Call sendfile() - TTP
		Fragmentation
		Sending	
	   4. Thread2 - Call receiveACKS() - TTP
		//TODO - Synchronization
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {

		if(args.length != 1) {
			printUsage();
		}

		System.out.println("Starting Server ...");
		
		
		
		
		sendPackets.setName("sendPackets");
		
		
		
		int port = Integer.parseInt(args[0]);
		//ds = new DatagramService(port, 10);
		char[] buffer = null;
		//Datagram datagram;
		System.out.println("test");
		//while(true) {
		
		if(ttp.openConnection()){
			System.out.println("classpath = " + System.getProperty("java.class.path"));
			//System.out.println("Hello");
		
			String filename = ttp.receiveData();
			System.out.println(filename);

			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			StringBuffer sb = new StringBuffer();

			while((line = br.readLine()) != null){
				sb.append(line);
				sb.append(System.getProperty("line.separator"));				   
			}
			 contents = sb.toString();
			
			sendPackets.start();
		    while(true){
		    	boolean flag = ttp.getAcks();
		    	if(flag == true){
		    		System.out.println("ooooo");
		    		sendPackets.interrupt();
		    	}
		    	
		    }
			

		}
	
	}
	
	public void run(){
			if(Thread.currentThread().getName().equals("sendPackets")){
			System.out.println("Inside send pakcets");
			try {
				ttp.sendFile(contents);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			if(Thread.currentThread().isInterrupted()){
				System.out.println("Was interrupted");
			}
			  
		}		
	}

	

	private static void printUsage() {
		System.out.println("Usage: server <port>");
		System.exit(-1);
	}
}
