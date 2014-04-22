package services;

import java.io.IOException;
import java.io.Serializable;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import datatypes.Datagram;

public class TtpService implements Serializable{
	
	private static int seqno;
	private static int ackno;
	private static short flagA = 1;
	private static short flagS = 2;
	private static short flagF = 4;
	private static short srcport;
	private static short dstport;
	private static short flag;
	private static String srcaddr;
	private static String dstaddr;
	boolean handshake = false, isChecksum = false, seqFlag = true, received = false; 
	boolean client;
	private static DatagramService ds;
	
	public TtpService(short srcport, short dstport, String srcaddr, String dstaddr, boolean client){
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
		this.client = client;
		this.flag = 0;
		this.seqno = (int)(Math.random() * 255);
		this.ackno = 0;
		handshake = false;
		isChecksum = false;
		seqFlag = true;
	}
	
	
	
	
	public boolean openConnection() throws IOException, ClassNotFoundException{
		//seqno = 1;
		//ackno = 0;
		if(client == true)
			handshakeSend(flagS, seqno, ackno);
		
		
		boolean ackflag = true;
		Datagram synAck = new Datagram();
		while(ackflag){
			System.out.println("inside");
			synAck = ds.receiveDatagram();
			System.out.println("a fucking packet received");
			System.out.println(synAck.getData());
			boolean flag = false;
			//Object obj = synAck.getData();
			//ByteArrayOutputStream out = new ByteArrayOutputStream();
		    //ObjectOutputStream os = new ObjectOutputStream(out);
		    //os.writeObject(synAck.getData());
		    byte[] synAckHead = (byte[]) synAck.getData();
		   // System.out.println(synA[0]);
		    //byte[] synAckHead = out.toByteArray();
		    System.out.println(synAckHead[0]);
		    System.out.println(synAckHead[1]);
		    System.out.println(synAckHead[2]);
		    if(client == true){
		    	System.out.println("client check");
		    	if(synAckHead[2] == 3 && synAckHead[1] == seqno + 1) flag = true;
		    	//if(synAckHead[1] != seqno + 1) flag = false;
		    }
		    else{
		    	System.out.println("server check");
		    	System.out.println(synAckHead[2]);
		    	if(synAckHead[2] == flagS) flag = true;
		    }
		    if(flag == true){
		    	ackflag = false;
		    	
		    	ackno = synAckHead[0]+1;
		    	seqno +=1;
		    	if(client == true) handshakeSend(flagA, seqno, ackno);
		    	else handshakeSend((short) (flagA + flagS), seqno, ackno);
		    	
		    }
		    
		    
		}
		
		
		
		
		
		return true;
    }
	
	
	public void handshakeSend(short flag, int seqno, int ackno) throws IOException{
		
		  
		  byte[] dataArray = new byte[3];
		  dataArray[0] = (byte)seqno;
		  dataArray[1] = (byte)ackno;
		  //dataArray[2] = (byte)window;
		  dataArray[2] = (byte)flag;
		  //dataArray[4] = (byte)offset;
		
		  Checksum checksum = new CRC32();
		  checksum.update(dataArray, 0, dataArray.length);
		  short synChecksum = (short)checksum.getValue();
		
		  Datagram syn = new Datagram();
		  syn.setChecksum(synChecksum);
		  syn.setSrcaddr(srcaddr);
		  syn.setDstaddr(dstaddr);
		  syn.setSrcport(srcport);
		  syn.setDstport(dstport);
		  syn.setSize((short) 1500);
		  syn.setData(dataArray);
		  System.out.println("Sequence Number: " + dataArray[0]);
		  System.out.println("Acknowledgement Number:" + dataArray[1]);
		  System.out.println("Flag: " + dataArray[2]);
		  
		  ds.sendDatagram(syn);
		  System.out.println("handshake packet sent");
	}
	
	
	public void sendData(String data){
		Datagram datagram = new Datagram();
		datagram.setDstaddr(dstaddr);
		datagram.setSrcaddr(srcaddr);
		datagram.setSrcport(srcport);
		datagram.setDstport(dstport);
		byte header[] = new byte[7];
		byte payload[];
		
		header[0] = (byte)seqno;
		header[1] = (byte)ackno;
		header[2] = (byte)0;
		
		
		
		
		
		
	}
	
	
	public void receiveData() throws IOException, ClassNotFoundException{
		Datagram datagram = new Datagram();
		boolean ackflag = true;
		while(ackflag){
			System.out.println("Receiving");
			datagram = ds.receiveDatagram();
			System.out.println("received something");
			
		}
	}
	/*public void openConnection(String srcaddr, String dstaddr, short srcport, short dstport) throws IOException, ClassNotFoundException{
		ds = new DatagramService(srcport, 10);
		Datagram datagram = new Datagram();
		datagram.setDstport(dstport);
		datagram.setSrcport(srcport);
		datagram.setDstaddr(dstaddr);
		datagram.setSrcaddr(srcaddr);
		int seq = 0 + (int)(Math.random()*255);
		byte header[] = new byte[7];
		header[0] = 0x1;
		header[1] = (byte) seq;
		header[3] = 0;
		Checksum checksum = new CRC32();
		checksum.update(header, 0, header.length);
		short checksumval = (short)checksum.getValue();
		datagram.setChecksum(checksumval);
		ds.sendDatagram(datagram);
		
		while(true){
			Datagram ack = new Datagram();
			ack = ds.receiveDatagram();
			System.out.println("Received datagram from " + datagram.getSrcaddr() + ":" + datagram.getSrcport() + " Data: " + datagram.getData());
			Object obj = ack.getData();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
		    ObjectOutputStream os = new ObjectOutputStream(out);
		    os.writeObject(obj);
		    r out.toByteArray();
			
			
		}
		
		
		
	}*/

	
	/*public void sendData(String srcaddr, String dstaddr, short srcport, short dstport){
		Datagram packet = new Datagram();
		packet.setDstport(dstport);
		packet.setSrcport(srcport);
		packet.setDstaddr(dstaddr);
		packet.setSrcaddr(srcaddr);
		
		int seq = 0 + (int)(Math.random()*255);
		byte header[] = new byte[7];
		byte payload[];
		
		// header[0] set for all the flags
		header[0] = 0x1; // 0x1 for SYN flag
		header[1] = (byte) seq; //Sequence number
		header[3] = 0; //Acknowledgment number
		
		String s = "Hello World";
		
		packet.setData(s);
		payload = s.getBytes();
		
		Checksum checksum = new CRC32();
		checksum.update(payload, 0, payload.length);
		
		short checksumval = (short)checksum.getValue();
		packet.setChecksum(checksumval);
		
		byte data[] = new byte[header.length + payload.length];
		System.arraycopy(header, 0, data, 0, header.length);
		System.arraycopy(payload, 0, data, header.length, payload.length);
		packet.setData(data);
		
		
		
		
		
	}*/
}
