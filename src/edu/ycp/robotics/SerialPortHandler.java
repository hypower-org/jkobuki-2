/*
	Copyright (c) 2013 - York College of Pennsylvania, Paul Glotfelter
	The MIT License
	See license.txt for details.
*/

package edu.ycp.robotics;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class SerialPortHandler implements SerialPortEventListener {
	
	private SerialPort port;
	private final LinkedBlockingQueue<ByteBuffer> incoming;
	private int capacity;
	
	public SerialPortHandler() {
		incoming = new LinkedBlockingQueue<ByteBuffer>();
		capacity = 2048;
	}
	
	/**
	 * 
	 * @param path The device path to which the serial port will be connected.
	 */
	public final void connect(String path) {
		
		port = new SerialPort(path);
		
		try {
			port.openPort();
			port.setParams(115200, 8, 1, 0);
			port.setEventsMask(SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR);
			port.addEventListener(this);
		} catch (SerialPortException e) {
			System.err.println("Could not connect to supplied port");
			e.printStackTrace();
		}
	}

	@Override
	public final void serialEvent(SerialPortEvent e) {
		
		int inputSize = 0;
		
		//Ensure that we can read what's on the line
		
		if(e.isRXCHAR()) {			
			inputSize = e.getEventValue();	
			ByteBuffer b = ByteBuffer.allocate(inputSize);
			
			try {
				b.put(port.readBytes(inputSize));
				
				try {
					
					//If we're maxed out, don't put anything else in the buffer
					
					if(incoming.size() > capacity) {
						System.err.println("Incoming queue capacity exceeded!  No more data will be received");
					} else {
						incoming.put(b);
					}
				} catch (InterruptedException e1) {
					System.err.println("Could not read from the serial port");
					e1.printStackTrace();
				}
			} catch (SerialPortException e1) {
				System.err.println("Could not read from the serial port");
				e1.printStackTrace();
			}		
		}	
	}
	
	/**
	 * Receives a ByteBuffer from an internal LinkedBlockingQueue.  WILL BLOCK IF DATA IS UNAVAILABLE.
	 * 
	 * @return The serial port data in a ByteBuffer.
	 */
	public final ByteBuffer receive() {
		
		//If we're actually connected to a serial port, take a value
		
		if(port != null) {			
			
			try {
				return incoming.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			throw new IllegalArgumentException("Port must be connected before data can be received");
		}
		
		return null;
	}
	
	/** 
	 * Receives a ByteBuffer from an internal LinkedBlockingQueue.  WILL NOT BLOCK IF DATA IS UNAVAILABLE.
	 * 
	 * @return The serial port data in a ByteBuffer.
	 */
	public final ByteBuffer receiveNB() {
		
		//Check to see if the buffer has anything in it before taking, avoids blocking 
		
		if(port != null) {	//Port isn't opened yet
			
			if (incoming.isEmpty()) {
				return null;			
			} else {
				try {
					return incoming.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else {
			throw new IllegalArgumentException("Port must be connected before data can be received");
		}
		
		return null;		
	}
	
	/** 
	 * Puts data onto the serial port.
	 * 
	 * @param q The queue from which data will be taken
	 */
	public final void send(ByteBuffer q) {
		
		//If the port is valid, write an array of bytes to the serial port
		
		if(port != null) {
			try {
				port.writeBytes(q.array());
			} catch (SerialPortException e) {
				System.err.println("There was an error writing to the serial port");
				e.printStackTrace();
			} 
		} else {
			throw new IllegalArgumentException("Port must be connected before data can be sent");
		}	
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int Capacity) {
		capacity = Capacity;
	}
}



