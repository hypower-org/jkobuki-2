/*
	Copyright (c) 2015 - York College of Pennsylvania, Paul Glotfelter, Patrick Martin
	The MIT License
	See license.txt for details.
*/

package edu.ycp.robotics;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class SerialPortHandler implements SerialPortEventListener {
	
	private SerialPort serialPort;
	private final LinkedBlockingQueue<ByteBuffer> incomingBytes;
	private final int capacity;
	
	private final byte[] poisonPill;
	
	public SerialPortHandler(String path, byte[] pp) {
		incomingBytes = new LinkedBlockingQueue<ByteBuffer>();
		capacity = 2048;
		
		poisonPill = pp;
		
		this.connect(path);
	}
	
	/**
	 * 
	 * @param path The device path to which the serial port will be connected.
	 */
	private final void connect(String path) {
		
		serialPort = new SerialPort(path);
		
		try {
			serialPort.openPort();
			serialPort.setParams(115200, 8, 1, 0);
			serialPort.setEventsMask(SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR);
			serialPort.addEventListener(this);
			System.out.println("Jkobuki connected to " + path);
		} catch (SerialPortException e) {
			System.err.println("Could not connect to supplied port");
			e.printStackTrace();
		}
	}

	/**
	 * Method that performs the proper shutdown to the serial port and shared bytebuffer queue.
	 */
	public final void shutdown(){
		
		try {
			serialPort.removeEventListener();
			serialPort.closePort();
		} catch (SerialPortException e1) {
			e1.printStackTrace();
		}
		// Insert poison pill into the blocking ByteBuffer stream.
		ByteBuffer ppBuf = ByteBuffer.allocate(poisonPill.length);
		ppBuf.put(poisonPill);
		try {
			incomingBytes.put(ppBuf);
		} catch (InterruptedException e) {
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
				b.put(serialPort.readBytes(inputSize));
				
				try {
					//If we're maxed out, don't put anything else in the buffer
					if(incomingBytes.size() > capacity) {
						System.err.println("Incoming queue capacity exceeded!  No more data will be received");
					} else {
						incomingBytes.put(b);
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
	 * Receives a ByteBuffer from an internal LinkedBlockingQueue. WILL BLOCK IF DATA IS UNAVAILABLE.
	 * 
	 * @return The serial port data in a ByteBuffer.
	 */
	public final ByteBuffer receiveBytes() {
		
		//If we're actually connected to a serial port, take a value
		if(serialPort != null) {			
			try {
				return incomingBytes.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Port must be connected before data can be received");
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
		
		if(serialPort != null) {
			try {
				serialPort.writeBytes(q.array());
			} catch (SerialPortException e) {
				System.err.println("There was an error writing to the serial port");
				e.printStackTrace();
			} 
		} else {
			throw new IllegalArgumentException("Port must be connected before data can be sent");
		}	
	}

}



