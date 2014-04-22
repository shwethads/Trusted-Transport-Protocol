package services;

public class SampleThread extends Object implements Runnable{
	//send packets
	//timer
	//receive packets

	public static Exec sp;
	public static Thread send, recv, timer;
	@Override
	public void run() {
		System.out.println("In run");
	}
	
	public static void main(String args[]) {		
		sp = new Exec();
		send = new Thread(sp);
		send.setName("Send");
		
		recv = new Thread(sp);
		recv.setName("Recv");
		
		timer = new Thread(sp);
		timer.setName("Timer");
		
		timer.start();
		send.start();
		recv.start();
		
	}
}

class Exec extends Object implements Runnable {
	public static Thread t;
	
	public static void main(String[] args) {
		
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		t = Thread.currentThread();
		System.out.println("START: "+ Thread.currentThread().getName());
		//System.out.println("");
		int i;
		String str = t.getName();
		if(str.equalsIgnoreCase("Timer")) {
			for(int k = 0; k<1000; k++)
				System.out.print(".");
			System.out.println("");
		    SampleThread.send.interrupt();
			SampleThread.recv.interrupt();
			t.stop();
		}
			
		else if(str.equalsIgnoreCase("Send")) {
			boolean flag = true;
			while(flag) {
				if(t.isInterrupted()) {
					System.out.println(str+" interrupted");
					System.out.println("");
					flag = false;
				}				
			}
			t.stop();
		}
		
		else if(str.equalsIgnoreCase("Recv")) {
			while(true) {
				if(t.isInterrupted()) {
					System.out.println("RECV: Timeout");
					System.out.println("");
					break;
				}
				
			}
			t.stop();
		}
	}
	
	public void stop() {
		System.out.println("STOP: "+t.getName());
		System.out.println("");
	}
	
}

