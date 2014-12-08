package edu.ycp.robotics;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.ycp.robotics.PacketParser.State;

public class KobukiRobot {
	
	private boolean stop = false;
	private final PacketParser parser = new PacketParser();
	private final SerialPortHandler handler = new SerialPortHandler();
	private final LinkedBlockingQueue<ByteBuffer> outgoing = new LinkedBlockingQueue<ByteBuffer>();
	private final ScheduledExecutorService executor;
	private final Vector<Future<?>> tasks;
	private final int MIN_UPDATE_PERIOD = 21; //in ms
		
	private final int WHEELBASE = 354; //in mm
	private int leftEncoder;
	private int rightEncoder;
	
	public KobukiRobot(String path) {
		
		Callable<Object> dataSender = new Callable<Object>() {

			@Override
			public Object call() throws Exception {
				while(true) {
					try {
						handler.send(outgoing.take());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}		
		};
		
		Callable<Object> dataReceiver = new Callable<Object>() {

			@Override
			public Object call() throws Exception {
				while(true) {
					ByteBuffer b = handler.receive();
					if(b != null) {
						byte[] ba = b.array().clone();
						for(int i = 0; i < ba.length; i++) {
							if(parser.advance(ba[i]) == State.VALID) {
								updateSensors(parser.getPacket());
							}
						}
					} else {
						//Nothing
					}
				}
			}	
		};
		
		Runnable kobukiThreadManager = new Runnable() {
			
			@Override
			public void run() {
				
				if(stop) {	
					System.out.println("Shutting down the KobukiRobot.");
					try {
						baseControl((short) 0, (short) 0);
						
						// Sleep a bit before killing the running tasks.
						Thread.sleep(3*MIN_UPDATE_PERIOD);
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
					for(Future<?> currTask : tasks) {  //Properly ends/shuts down all currently active threads
						currTask.cancel(true); 
					}	
					executor.shutdown();
				} else {
					try {
						Thread.sleep(MIN_UPDATE_PERIOD);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt(); 
					}
				}			
			}									
		};
		
		handler.connect(path);
		
		executor = Executors.newScheduledThreadPool(3);
		tasks = new Vector<Future<?>>();
		
		tasks.add(executor.schedule(dataSender, 0, TimeUnit.MILLISECONDS));
		tasks.add(executor.schedule(dataReceiver, 0, TimeUnit.MILLISECONDS));
		tasks.add(executor.scheduleAtFixedRate(kobukiThreadManager, 0, MIN_UPDATE_PERIOD, TimeUnit.MILLISECONDS));
	}
	
	public void stop() {
		stop = true;
	}
	
	private void updateSensors(byte[] b) {
		
		for(int i = 0; i < b.length; i++) {
			System.out.print(b[i] + " ");
		}
		
		System.out.println();
		
		leftEncoder = ((b[11] & 0xFF) << 8) | (b[10] & 0xFF);
		rightEncoder = ((b[13] & 0xFF) << 8) | (b[12] & 0xFF);
		
		System.out.println("ENCODERS: " + leftEncoder + " " + rightEncoder);
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
	
	public static void main (String[] args) {
		
		//Small test to make sure everything important is working!
		
		KobukiRobot k = new KobukiRobot("/dev/ttyUSB0");
		
		while(true) {
			
			//System.out.println(k.getLeftEncoder() + " and " + k.getRightEncoder());
			k.control(0, 0);
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}




