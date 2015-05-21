/*
	Copyright (c) 2015 - York College of Pennsylvania, Paul Glotfelter, Patrick Martin
	The MIT License
	See license.txt for details.
*/

package edu.ycp.robotics;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.Random;

import edu.ycp.robotics.PacketParser.State;

public class KobukiRobot {
	
	private volatile boolean isStopRequested = false;
	private volatile boolean isDataReceiverTerminated = false;
	private final PacketParser parser = new PacketParser();
	private final SerialPortHandler serialPortHandler;
	private final LinkedBlockingQueue<ByteBuffer> outgoing = new LinkedBlockingQueue<ByteBuffer>();
	private final byte[] poisonPillMsg;
	
	private final ThreadPoolExecutor poolService;
	private final int MIN_UPDATE_PERIOD = 21; //in ms
		
	private final int WHEELBASE = 230; //in mm
	private int leftEncoder;
	private int rightEncoder;
	private int bumper;
	private int button;
	private int cliff;
	private int battery;
	
	public KobukiRobot(String path) {

		// generate the poison pill message:
		final byte ppByte = (byte) new Random().nextInt(127);
		poisonPillMsg = new byte[] {-1, ppByte, (byte) 0x21};
		
		serialPortHandler = new SerialPortHandler(path, poisonPillMsg);
		
		Runnable dataSender = new Runnable(){

			@Override
			public void run() {
				boolean isInterrupted = false;
				while(!isInterrupted) {
					try {
						serialPortHandler.send(outgoing.take());
					} catch (InterruptedException e) {
						isInterrupted = true;
					}
				}
			}
			
		};
		
		Runnable dataReceiver = new Runnable(){

			@Override
			public void run() {
				while(!isDataReceiverTerminated) {
					try{
						ByteBuffer b = serialPortHandler.receiveBytes();
						if(b != null) {
							// check for poison pill
							if(b.get(0) == -1 && b.get(1) == ppByte && b.get(2) == (byte) 0x21){
								isDataReceiverTerminated = true;
							}
							// otherwise process new buffer of data
							else {
								for(int i = 0; i < b.position(); i++) {
									if(parser.advance(b.get(i)) == State.VALID) {
										updateSensors(parser.getPacket());
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		Runnable kobukiThreadManager = new Runnable() {
			
			@Override
			public void run() {
				
				while(true){
					if(isStopRequested) {	
						System.out.println("Shutting down the KobukiRobot...");
						try {
							baseControl((short) 0, (short) 0);
							
							// Sleep a bit before killing the running tasks.
							Thread.sleep(3*MIN_UPDATE_PERIOD);
							
							serialPortHandler.shutdown();
							
							// Wait until the dataReceiver runnable is terminated.
							while(!isDataReceiverTerminated);
							
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}

						poolService.shutdownNow();
						
						System.out.println("KobukiRobot shutdown.");
						return;
					} else {
						try {
							Thread.sleep(MIN_UPDATE_PERIOD);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt(); 
						}
					}
				}
			}									
		};
		
		poolService = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
		
		poolService.execute(dataSender);
		poolService.execute(dataReceiver);
		poolService.execute(kobukiThreadManager);
	}
	
	public void requestSystemStop() {
		isStopRequested = true;
	}
	
	private void updateSensors(byte[] b) {
		
		bumper = b[7]; 
		cliff = b[9];
		leftEncoder = ((b[11] & 0xFF) << 8) | (b[10] & 0xFF);
		rightEncoder = ((b[13] & 0xFF) << 8) | (b[12] & 0xFF);
		button = b[16];
		battery = b[18];
		
	}
	
	public void setLed(int flag) { 
		//PacketBuilder defines integer tags for leds
		this.outgoing.add(ByteBuffer.wrap(PacketBuilder.ledPacket(flag))); 
	}
	
	public void soundSequence(int sound) { 
		//PacketBuilder defines integer tags for sounds
		this.outgoing.add(ByteBuffer.wrap(PacketBuilder.soundSequencePacket(sound))); 
	}
	
	public void baseControl(short velocity, short radius) throws IOException, InterruptedException { 	
		this.outgoing.add(ByteBuffer.wrap(PacketBuilder.baseControlPacket(velocity, radius))); 
	}
	
	public void control(double v, double w) {
		double epsilon = 0.0001; 
		double radius = 0.0;
		
		if(Math.abs(w) < epsilon) {
			this.outgoing.add(ByteBuffer.wrap(PacketBuilder.baseControlPacket((short) v, (short) radius)));
			return;
		}
		
		radius = v / w;
		
		if(Math.abs(v) < epsilon || Math.abs(radius) <= 1.0) {
			this.outgoing.add(ByteBuffer.wrap(PacketBuilder.baseControlPacket((short) (WHEELBASE * w  / 2.0), (short) 1.0)));
			return;
		}
		
		if(radius > 0.0) {
			this.outgoing.add(ByteBuffer.wrap(PacketBuilder.baseControlPacket((short) ((radius + (WHEELBASE / 2.0)) * w), (short) radius)));
		} else {
			this.outgoing.add(ByteBuffer.wrap(PacketBuilder.baseControlPacket((short) ((radius - (WHEELBASE / 2.0)) * w), (short) radius)));
		}
	}

	public int getRightEncoder() {
		return rightEncoder;
	}

	public int getLeftEncoder() {
		return leftEncoder;
	}
	
	public int getBumper(){
		return bumper;
	}
	
	public int getButton() {
		return button;
	}
	
	public int getCliff() {
		return cliff;
	}
	
	public int getBattery() {
		return battery;
	}
	
	public static void main (String[] args) throws InterruptedException {
		
		//Small test to make sure everything important is working!		
		KobukiRobot k = new KobukiRobot("/dev/ttyUSB0");
		int idx = 0;
		
		Thread.sleep(1000);
		
		k.soundSequence(0);
		
		k.setLed(1);
		Thread.sleep(1000);
		k.setLed(2);
		Thread.sleep(1000);
		k.setLed(3);
		Thread.sleep(1000);
		k.setLed(4);
		k.setLed(0);
		
		while(idx < 5) {
			
			System.out.println("button: " + k.getButton());
			System.out.println("cliff: " + k.getCliff());
			System.out.println("battery: " + k.getBattery());
			try {
				k.baseControl((short) 100, (short) 0);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			idx++;
		}

		k.requestSystemStop();

	}
}




