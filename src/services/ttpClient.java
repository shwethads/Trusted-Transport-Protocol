package services;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import datatypes.Datagram;

public class ttpClient {

	private static int seqno, ackno, offset = 0;
	private static short flagA = 1, flagS = 2, flagF = 4, flag, 
			srcport, dstport;
	private static String srcaddr, dstaddr,
			filename;
	boolean handshake = false, isChecksum = false, seqFlag = true, received = false, client; 
	private static DatagramService ds;

	public ttpClient(short srcport, short dstport, String srcaddr, String dstaddr, String filename){
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
		this.seqno = (int)(Math.random() * 127);
		this.ackno = 0;
		handshake = false;
		isChecksum = false;
		seqFlag = true;
		this.filename = filename;
	}

	public boolean sendData() throws IOException, ClassNotFoundException{
		flag = 0;
		Datagram datagram = new Datagram();
		datagram.setSrcaddr(srcaddr);
		datagram.setDstaddr(dstaddr);
		datagram.setSrcport(srcport);
		datagram.setDstport(dstport);
		byte header[] = new byte[10];
		seqno += 1;
		
		//setting the sequence number
		//Each element in the array has one byte of the seqno - 4 elements
			//											ackno - 4 elements
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
		
		header[8] = (byte)flag;
		header[9] = (byte)offset;

		byte payload[] = filename.getBytes();
		byte data[] = new byte[header.length + payload.length];
		System.arraycopy(header, 0, data, 0, header.length);
		System.arraycopy(payload, 0, data, header.length, payload.length);
		//System.out.println("Data: " + data[3]);
		datagram.setData(data);
		
		/*Checksum checksum = new CRC32();
		checksum.update(data, 0, data.length);
		short checksumclient = (short)checksum.getValue();*/
		short checksumclient  = (short) checksum(data);
		datagram.setChecksum(checksumclient);
		
		datagram.setSize((short) 1500);
		System.out.println("Sending equence Number: " + seqno);
		System.out.println("Sending Acknowledgement Number:" + ackno);
		System.out.println("Sending Flag: " + header[8]);
		ds.sendDatagram(datagram);
		seqno += payload.length;
		System.out.println("Running Seq no" + seqno);
		System.out.println("packet sent");
		return true;
	}


	public String receiveData() throws IOException, ClassNotFoundException{
		Datagram datagram = new Datagram();
		boolean flag = true;
		String contents = "";
		int expectedseq = 0;
		
		while(flag){
			System.out.println("receiving file");
			datagram = ds.receiveDatagram();
			System.out.println("File Received");
			
			byte combined[] = (byte[])datagram.getData();
			
			int rcvdseq = combined[0] & 0xFF | 
					(combined[1] & 0xFF) << 8 |
					(combined[2] & 0xFF) << 16 |
					(combined[3] & 0xFF) << 24;
			
			int rcvdack = combined[4] & 0xFF | 
					(combined[5] & 0xFF) << 8 |
					(combined[6] & 0xFF) << 16 |
					(combined[7] & 0xFF) << 24;
			
			short recvChecksum = datagram.getChecksum();
			short newChecksum = (short) checksum(combined);
			
			if(recvChecksum != newChecksum) {
				System.out.println("Checksum ERROR");
			}
			
			System.out.println("Received seq no" + rcvdseq);
			System.out.println("Received ack no" + rcvdack);
			System.out.println("Received flag: " + combined[8]);
			
			byte data[] = new byte[combined.length - 10];
			System.arraycopy(combined, 10, data, 0, combined.length - 10);
			String data1 = new String(data);
			System.out.println("Data = " + data1);
			
			if(rcvdack == seqno && rcvdseq == ackno){
			  contents = contents + data1;
			  
			  byte[] header = new byte[10];
			  Datagram datagram1 = new Datagram();
		      datagram1.setSrcaddr(srcaddr);
			  datagram1.setDstaddr(dstaddr);
			  datagram1.setSrcport(srcport);
			  datagram1.setDstport(dstport);
			  ackno = rcvdseq + data.length; 
			    header[0] = (byte) (seqno & 0xFF);
				header[1] = (byte) ((seqno >> 8) & 0xFF);
				header[2] = (byte) ((seqno >> 16) & 0xFF);
				header[3] = (byte)((seqno >> 24) & 0xFF);
				
				header[4] = (byte) (ackno & 0xFF);
				header[5] = (byte) ((ackno >> 8) & 0xFF);
				header[6] = (byte) ((ackno >> 16) & 0xFF);
				header[7] = (byte)((ackno >> 24) & 0xFF);
			  
				header[8] = (byte)flagA;
				header[9] = (byte)offset;
				datagram1.setData(header);
				
				/*Checksum checksum = new CRC32();
				checksum.update(data, 0, data.length);
				short checksumclient = (short)checksum.getValue();*/
				short checksumclient = (short) checksum(data);
				datagram1.setChecksum(checksumclient);
				
				datagram1.setSize((short) 1500);
				System.out.println("Sending equence Number: " + seqno);
				System.out.println("Sending Acknowledgement Number:" + ackno);
				System.out.println("Sending Flag: " + header[8]);
				ds.sendDatagram(datagram1);
				
				seqno++;
			}
			
			else{
				byte[] header = new byte[10];
				  Datagram datagram1 = new Datagram();
			      datagram1.setSrcaddr(srcaddr);
				  datagram1.setDstaddr(dstaddr);
				  datagram1.setSrcport(srcport);
				  datagram1.setDstport(dstport);
				  
				  ackno = rcvdseq + data.length; 
				    header[0] = (byte) (seqno & 0xFF);
					header[1] = (byte) ((seqno >> 8) & 0xFF);
					header[2] = (byte) ((seqno >> 16) & 0xFF);
					header[3] = (byte)((seqno >> 24) & 0xFF);
					
					header[4] = (byte) (ackno & 0xFF);
					header[5] = (byte) ((ackno >> 8) & 0xFF);
					header[6] = (byte) ((ackno >> 16) & 0xFF);
					header[7] = (byte)((ackno >> 24) & 0xFF);
				  
					header[8] = (byte)flagA;
					header[9] = (byte)offset;
					datagram1.setData(header);
					
					/*Checksum checksum = new CRC32();
					checksum.update(data, 0, data.length);
					short checksumclient = (short)checksum.getValue();*/
					short checksumclient = (short) checksum(data);
					datagram1.setChecksum(checksumclient);
					
					datagram1.setSize((short) 1500);
					System.out.println("Sending equence Number: " + seqno);
					System.out.println("Sending Acknowledgement Number:" + ackno);
					System.out.println("Sending Flag: " + header[8]);
					ds.sendDatagram(datagram1);
			}
			
			System.out.println("Contents = " + contents);
			if(combined[8] == 8)
				return contents;
		}
		return null;
	}

	public void sendAck(){
		
	}
	
	public boolean openConnection() throws IOException, ClassNotFoundException{
		System.out.println("Sequence number1" + seqno);
		handshakeSend(flagS, seqno, ackno);
		boolean ackflag = true;
		Datagram synAck = new Datagram();
		while(ackflag){
			System.out.println("Sequence number2" + seqno);
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
			
			System.out.println("Received seq no" + rcvdseq);
			System.out.println("Received ack no" + rcvdack);
			System.out.println("Received flag: " + synAckHead[8]);
			System.out.println("Current seq no" + seqno);
			System.out.println("client check");
			
			short recvChecksum = synAck.getChecksum();
			short newChecksum = (short) checksum(synAckHead);
			
			if(recvChecksum != newChecksum) {
				System.out.println("Checksum ERROR");
			}
			
			if(synAckHead[8] == 3 && rcvdack == seqno + 1) flag = true;
			if(flag == true){
				ackflag = false;

				ackno = rcvdseq+1;
				seqno +=1;
				handshakeSend(flagA, seqno, ackno);
				return true;
			}
			return false;
		}
		return false;
	}

	public void handshakeSend(short flag, int seqno, int ackno) throws IOException{
		byte[] dataArray = new byte[10];
		
		dataArray[0] = (byte) (seqno & 0xFF);
		dataArray[1] = (byte) ((seqno >> 8) & 0xFF);
		dataArray[2] = (byte) ((seqno >> 16) & 0xFF);
		dataArray[3] = (byte)((seqno >> 24) & 0xFF);
		
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
		*/
		/*
		Checksum checksum = new CRC32();
		checksum.update(dataArray, 0, dataArray.length);
		short synChecksum = (short)checksum.getValue();*/
		short synChecksum = (short)checksum(dataArray); 

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
		seqno += 1;

		System.out.println("handshake packet sent");
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


}


