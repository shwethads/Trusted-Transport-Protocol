package services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import datatypes.Datagram;

/*
Actual implementation of the server functions

*/

public class ttpServer {

	private static int seqno, ackno, offset = 0, fragSize = 100, totalFragments = 1,
			WINDOW_SIZE = 5, SERVER_TIMEOUT_MS = 1000, base = seqno, nextseqno = base;
	private static short flagA = 1, flagS = 2, flagF = 4, flagM = 8, srcport, dstport, flag;
	private static String srcaddr, dstaddr;
	boolean handshake = false, isChecksum = false, seqFlag = true, received = false, client; 
	private static DatagramService ds;
	private ArrayList<Datagram> pending;

	public ttpServer(short srcport, short dstport, String srcaddr, String dstaddr){
		super();
		
		try{
			ds = new DatagramService(srcport, 10);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		this.srcport = srcport;
		this.dstport = dstport;
		this.dstaddr = dstaddr;
		this.srcaddr = srcaddr;
		//this.client = client;
		this.flag = 0;
		this.seqno = (int)(Math.random() * 127);
		this.ackno = 0;
		
		handshake = false;
		isChecksum = false;
		seqFlag = true;
		pending = new ArrayList<Datagram>();
	}


	/* Receive datagram, extract filename and pass the filename to server */
	public String receiveData() throws IOException, ClassNotFoundException{

		boolean servflag = true;
		Datagram datagram = new Datagram();
		while(servflag){
			datagram = ds.receiveDatagram();
			System.out.println("filename received");

			byte combined[] = (byte[])datagram.getData();
			
			int rcvdseq = combined[0] & 0xFF | 
					(combined[1] & 0xFF) << 8 |
					(combined[2] & 0xFF) << 16 |
					(combined[3] & 0xFF) << 24;
			
			int rcvdack = combined[4] & 0xFF | 
					(combined[5] & 0xFF) << 8 |
					(combined[6] & 0xFF) << 16 |
					(combined[7] & 0xFF) << 24;
			
			System.out.println("Filename Received seq no" + rcvdseq);
			System.out.println("Filename Received ack no" + rcvdack);
			System.out.println("Filename Received flag: " + combined[8]);
			short recvChecksum = datagram.getChecksum();
			short newChecksum = (short) checksum(combined);
			
			if(recvChecksum != newChecksum) {
				System.out.println("Checksum ERROR");
			}
			ackno = rcvdseq;

			byte data[] = new byte[combined.length - 10];
			System.arraycopy(combined, 10, data, 0, combined.length - 10);
			String data1 = new String(data);
			ackno += data.length;
			System.out.println("File name = " + data1);
			return data1;
		}
		return null;
	}
	

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


	/* Sends all packets in the window */
	public void sendFile(String fileContents) throws IOException{

		byte [][] fragment = fragmentFile(fileContents, fragSize);
		seqno += 1;
		int tempfragSize = fragSize;
		System.out.println("Sending file" + fileContents.getBytes().length);
		base = seqno;
		nextseqno = base;
		offset = 0;
		int i= 0;

		/* i: ith packet that has been sent - to check last fragment
	           nextseqno: seqno of the next packet that will be sent next
		*/
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

				/*Checksum checksum = new CRC32();
				checksum.update(header, 0, header.length);
				short checksumval = (short)checksum.getValue();*/
				short checksumval = (short) checksum(header);
				
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
				
				ds.sendDatagram(packet);
				seqno += header.length - 10;
				ackno++; // possibly change later after the thread thingy works
				System.out.println("Next seq no" + seqno);
			}
	}

	public long checksum(byte[] buf) {
		int i = 0, length = buf.length;
		long sum = 0;
		
		while(length > 0) {
			sum += (buf[i++]&0xff) << 8;
	        if ((--length)==0) break;
	        sum += (buf[i++]&0xff);
	        --length;
		}
		return (~((sum & 0xFFFF)+(sum >> 16)))&0xFFFF;
	}

	public boolean openConnection() throws IOException, ClassNotFoundException{
		boolean ackflag = true;
		Datagram synAck = new Datagram();
		while(ackflag){
			System.out.println("inside");
			synAck = ds.receiveDatagram();
			System.out.println("a fucking packet received");

			boolean flag = false;
			byte[] synAckHead = (byte[]) synAck.getData();
			
			int rcvdseq = synAckHead[0] & 0xFF | 
					(synAckHead[1] & 0xFF) << 8 |
					(synAckHead[2] & 0xFF) << 16 |
					(synAckHead[3] & 0xFF) << 24;
			
			int rcvdack = synAckHead[4] & 0xFF | 
					(synAckHead[5] & 0xFF) << 8 |
					(synAckHead[6] & 0xFF) << 16 |
					(synAckHead[7] & 0xFF) << 24;	
			
			short recvChecksum = synAck.getChecksum();
			short newChecksum = (short) checksum(synAckHead);
			
			if(recvChecksum != newChecksum) {
				System.out.println("Checksum ERROR");
			}
			
			System.out.println("Handshake Received" + rcvdseq);
			System.out.println("Handshake received" + rcvdack);
			System.out.println("Handshake received" + synAckHead[8]);
			System.out.println("server check");
			//System.out.println(synAckHead[2]);
			if(synAckHead[8] == flagS) 
				flag = true;

			if(synAckHead[8] == flagA) {
				System.out.println("ACK received");
				ackflag = false;
				return true;
			}

			if(flag == true){ 
				System.out.println("SYN received");
				ackno = rcvdseq+1;
				//seqno +=1;
				handshakeSend((short) (flagA + flagS), seqno, ackno);
			}
		}
		return false;	 
	}

	public void handshakeSend(short flag, int seqno, int ackno) throws IOException{		
		byte[] dataArray = new byte[10];
		
		//storing sequence number
		dataArray[0] = (byte) (seqno & 0xFF);
		dataArray[1] = (byte) ((seqno >> 8) & 0xFF);
		dataArray[2] = (byte) ((seqno >> 16) & 0xFF);
		dataArray[3] = (byte)((seqno >> 24) & 0xFF);
		
		//storing the ack no
		dataArray[4] = (byte) (ackno & 0xFF);
		dataArray[5] = (byte) ((ackno >> 8) & 0xFF);
		dataArray[6] = (byte) ((ackno >> 16) & 0xFF);
		dataArray[7] = (byte)((ackno >> 24) & 0xFF);
		
		dataArray[8] = (byte)flag;
		
		dataArray[9] = (byte)offset;
/*
		dataArray[0] = (byte)seqno;
		dataArray[1] = (byte)ackno;
		//dataArray[2] = (byte)window;
		dataArray[2] = (byte)flag;
		//dataArray[4] = (byte)offset;
 
		Checksum checksum = new CRC32();
		checksum.update(dataArray, 0, dataArray.length);
		short synChecksum = (short)checksum.getValue();
*/
		short synChecksum = (short) checksum(dataArray);
		
		Datagram syn = new Datagram();
		syn.setChecksum(synChecksum);
		syn.setSrcaddr(srcaddr);
		syn.setDstaddr(dstaddr);
		syn.setSrcport(srcport);
		syn.setDstport(dstport);
		syn.setSize((short) 1500);
		syn.setData(dataArray);
		System.out.println("Handshake sending Sequence Number: " + seqno);
		System.out.println("Handshake sending Acknowledgement Number:" + ackno);
		System.out.println("Handshake Flag: " + dataArray[8]);

		ds.sendDatagram(syn);
		System.out.println("handshake packet sent");
		seqno += 1;
	}
	
	
	public boolean getAcks() throws IOException, ClassNotFoundException{
		Datagram datagram = new Datagram();
		datagram = ds.receiveDatagram();
		System.out.println("ACK received");

		byte combined[] = (byte[])datagram.getData();
		
		int rcvdseq = combined[0] & 0xFF | 
				(combined[1] & 0xFF) << 8 |
				(combined[2] & 0xFF) << 16 |
				(combined[3] & 0xFF) << 24;
		
		int rcvdack = combined[4] & 0xFF | 
				(combined[5] & 0xFF) << 8 |
				(combined[6] & 0xFF) << 16 |
				(combined[7] & 0xFF) << 24;
		
		System.out.println("Filename Received seq no" + rcvdseq);
		System.out.println("Filename Received ack no" + rcvdack);
		System.out.println("Filename Received flag: " + combined[8]);
		
		short recvChecksum = datagram.getChecksum();
		short newChecksum = (short) checksum(combined);
		
		if(recvChecksum != newChecksum) {
			System.out.println("Checksum ERROR");
		}
		
		if(combined[8] == 1){
			ackno = rcvdseq;
			return true;
		}
		return false;		
	}
}
