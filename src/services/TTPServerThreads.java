package services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import datatypes.Datagram;

public class TTPServerThreads {
	public static TTPSend ttp;
	public static Thread send, recv;
	private static int seqno, ackno, offset = 0, fragSize = 100, totalFragments = 1,
			WINDOW_SIZE = 5, SERVER_TIMEOUT_MS = 1000, base = seqno, nextseqno = base;
	private static short flagA = 1, flagS = 2, flagF = 4, flagM = 8, srcport, dstport, flag;
	private static String srcaddr, dstaddr, fileContents;
	boolean handshake = false, isChecksum = false, seqFlag = true, received = false, client; 
	private static DatagramService ds;
	private static Datagram packet;
	private ArrayList<Datagram> pending;

	/*
	 * TODO
	 * 1. Receive SYN
	 * 2. Handshake
	 * 3. If handshake == false
	 * 		continue listening		
	 */

	public byte[][] fragmentFile(String fileContents, int fragSize) {	
		byte[] buffer = fileContents.getBytes();
		if(buffer.length % fragSize == 0)
			totalFragments = buffer.length/fragSize;
		else
			totalFragments = buffer.length/fragSize + 1;
		System.out.println("Buffer Length = "+ buffer.length);
		System.out.println("Total Fragments = " + totalFragments);
		byte[][] fragment = new byte[totalFragments][fragSize];		

		//totalFragments = buffer.length/fragSize; //possible error

		//Fragment according to the fragment size and store each fragment in a row of a 2D array
		for(int i=0, k=0; i<totalFragments; i++) {
			System.out.println("Inside");
			for(int j=0; j<fragSize; j++){
				fragment[i][j] = buffer[k++];
				if(k == buffer.length)
					break;
				System.out.print("Fragment = " + j + fragment[i][j] + ",");
			}
			System.out.println();
		}				
		return fragment;
	}

	public void sendFile(String content) {	
		fileContents = content;
		ttp = new TTPSend();
		send = new Thread(ttp);
		send.setName("Send");
		recv = new Thread(ttp);
		recv.setName("Recv");

		send.start();
		recv.start();		
	}

	class TTPSend implements Runnable {
		Thread t;

		@Override
		public void run() {
			
			if(t.getName().equalsIgnoreCase("Recv")) {
				boolean flag = true;
				
				try {
					packet = ds.receiveDatagram();
				} catch (Exception e) {
					e.printStackTrace();
				} 
				
				byte combined[] = (byte[])packet.getData();
				
				int rcvdseq = combined[0] & 0xFF | 
						(combined[1] & 0xFF) << 8 |
						(combined[2] & 0xFF) << 16 |
						(combined[3] & 0xFF) << 24;
				
				int rcvdack = combined[4] & 0xFF | 
						(combined[5] & 0xFF) << 8 |
						(combined[6] & 0xFF) << 16 |
						(combined[7] & 0xFF) << 24;
				
				if((rcvdack < base) || (rcvdack > (base + WINDOW_SIZE))) 
					flag = false;
				
				if(flag) {
					base += rcvdack - base;
					//restart timer
					send.interrupt();
					send.start();
				}
				else {
					//restart timer
					//send again
				}
			}

			else if(t.getName().equalsIgnoreCase("Send")) {
				byte [][] fragment = fragmentFile(fileContents, fragSize);
				seqno += 1;
				int tempfragSize = fragSize;
				System.out.println("Sending file" + fileContents.getBytes().length);
				base = seqno;
				nextseqno = base;
				offset = 0;
				int i= 0;
				boolean whileFlag = true;

				while(whileFlag) {
					for(nextseqno = base, i = 0; nextseqno < base + WINDOW_SIZE; nextseqno++, i++){
						System.out.println("Went inside");
						if(i == 0)
							flag = flagS;
						else if(i == totalFragments-1) // n should ideally be 1 less than window size
							flag = flagM;
						else
							flag = 0;

						offset = fragSize * i;//replace 2 by fragSize 
						if(i == totalFragments - 1){
							tempfragSize = fileContents.getBytes().length - (fragSize * (totalFragments - 1));

						}

						byte header[] = new byte[10+tempfragSize];//replace 2 by fragSize

						header[0] = (byte) (seqno & 0xFF);
						header[1] = (byte) ((seqno >> 8) & 0xFF);
						header[2] = (byte) ((seqno >> 16) & 0xFF);
						header[3] = (byte)((seqno >> 24) & 0xFF);

						header[4] = (byte) (ackno & 0xFF);
						header[5] = (byte) ((ackno >> 8) & 0xFF);
						header[6] = (byte) ((ackno >> 16) & 0xFF);
						header[7] = (byte)((ackno >> 24) & 0xFF);


						//header[0] = (byte)seqno;
						//header[1] = (byte)ackno;

						header[8] = (byte) flag;
						header[9] = (byte)offset;
						System.out.println("Header length" + header.length);
						byte payload[] = new byte[2]; // replace 2 by fragSize
						for(int j = 0, x = 0; j < tempfragSize; j++, x++)
							header[10+x] = fragment[i][j];

						Checksum checksum = new CRC32();
						checksum.update(header, 0, header.length);
						short checksumval = (short)checksum.getValue();

						Datagram packet = new Datagram();
						packet.setChecksum(checksumval);
						packet.setSrcaddr(srcaddr);
						packet.setDstaddr(dstaddr);
						packet.setSrcport(srcport);
						packet.setDstport(dstport);
						packet.setSize((short)1500);
						packet.setData(header);

						System.out.println("Sending seq no" + seqno);
						System.out.println("Sending ack no" + ackno);
						System.out.println("Sending flag: " + header[8]);

						try {
							ds.sendDatagram(packet);
						} catch (IOException e) {
							e.printStackTrace();
						}
						seqno += header.length - 10;
						ackno++; // possibly change later after the thread thingy works
						System.out.println("Next seq no" + seqno);
					}

				}
				
			}

		}

		
	}
}
