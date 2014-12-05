package edu.ycp.robotics;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * NOT THREADSAFE
 * 
 * @author ug-research
 *
 */

public class PacketParserDep {
	
	public enum State {
		EMPTY,
		PARTIAL,
		VALID
	}
	
	/*
	 * Packet parsing is based on the Kobuki communications protocol that can be found at :
	 * http://files.yujinrobot.com/kobuki/doxygen/html/enAppendixGuide.html
	*/
	
	private final ByteBuffer currentPacket;
	private State lastState = State.EMPTY;
	
	public PacketParserDep() {
		
		currentPacket = ByteBuffer.allocate(200);
	}
	
	/**
	 * Flushes the internal ByteBuffer
	 * 
	 */
	private void flush() {
		currentPacket.clear();
		Arrays.fill(currentPacket.array(), (byte) 0);
	}
	
	/**
	 * Determines if the internal ByteBuffer holds a valid packet.
	 * 
	 * @param length The length of the packet to be validated.
	 * @return If the packet is valid (i.e., true/false).
	 */
	private boolean validatePacket(int length) {
				
		byte[] b = currentPacket.array();
		
		if(length > 0) {
		
			byte checksum = 0;
			
			for(int i = 2; i < length; i++) {
				checksum ^= b[i];
			}
			
			System.out.println("CHECKSUM: " + checksum + " " + b[length- 1]);
			
			return (checksum == 0);
		} else {
			return false;
		}
	}
	
	private int containsHeader(byte[] b) {
		
		for(int i = 0; i < b.length - 1; i++) {
			if((b[i] == -86) && (b[i + 1] == 85)) {
				return i;
			}
		}	
		return -1;		
	}
	
	/**
	 * Stores the incoming bytes in an internal array and return the state of the parser (e.g., EMPTY, PARTIAL, or VALID).
	 * 
	 * @param bytes The bytes to be added to the internal ByteBuffer.
	 * @return The state of the current packet.
	 */
	public State checkPacket(ByteBuffer bytes) {
		
		byte[] b = bytes.array().clone();
		
		int header = containsHeader(b);
		
		if(header > -1) {
			System.out.println("RESET!");
			flush();
			System.arraycopy(b, header, b, 0, b.length - header);
		}
		
		for(int i = 0; i < b.length; i++) {
			System.out.print(b[i] + " ");
		}
		
		System.out.println();
		
		currentPacket.put(b);
		
		b = currentPacket.array();
		
		if((b[0] == -86 && lastState == State.VALID) || (b[0] == -86) && (b[1] == 85)) {
						
			int packetLength = b[2] + 4; //Add 4 to include: 2 header bytes, data size byte, and checksum byte
			
			if(validatePacket(packetLength)) {
				if(currentPacket.position() >= packetLength - 1) {
					System.out.println("VALID!");
					lastState = State.VALID;
				} else {
					lastState = State.PARTIAL;
				}
			} else {
				if(currentPacket.position() > packetLength) {
					System.out.println("FLUSHED!");
					flush();
					lastState = State.EMPTY;
				} else {
					lastState = State.PARTIAL;
				}
			}						
		} else {
			System.out.println("POOP!");
			flush();
			lastState = State.EMPTY;
		}
		
		return lastState;
	}	
	
	public byte[] getPacket() {
		
		byte[] b = currentPacket.array().clone();
		
		int packetLength = b[2] + 4;
		int position = currentPacket.position();
		
		if(position > packetLength) {	
			
			System.out.println("EXTRA!");
			
			flush();
			
			for(int i = packetLength; i <= position; i++) {
				currentPacket.put(b[i]);
			}
		} else {
			
			flush();
		}
		
		return b;
	}
}
