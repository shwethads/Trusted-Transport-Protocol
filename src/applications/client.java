/*
 * A sample client that uses DatagramService
 */

package applications;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;

import services.DatagramService;
import services.ttpClient;

public class client {

	private static DatagramService ds;


	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		if(args.length != 3) {
			printUsage();
		}

		System.out.println("Starting client ...");

		int port = Integer.parseInt(args[0]);
		String filename = args[2];
		ttpClient ttp = new ttpClient((short)port, (short)Integer.parseInt(args[1]), "127.0.0.1", "127.0.0.1", filename);
		if(ttp.openConnection()){
			if(ttp.sendData()){
				String s = ttp.receiveData();
				System.out.println(s);
				File file = new File(args[2]);
				FileWriter fstream = new FileWriter("FuckYouBitches.txt"); //replace with file
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(s);
				out.close();
				System.out.println("File Created");
			}
		}
	}

	private static void printUsage() {
		System.out.println("Usage: server <localport> <serverport>\n");
		System.exit(-1);
	}
}
