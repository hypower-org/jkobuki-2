package edu.ycp.robotics;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class PacketParser {
	
	/*
	 * Packet parsing is based on the Kobuki communications protocol that can be found at :
	 * http://files.yujinrobot.com/kobuki/doxygen/html/enAppendixGuide.html
	*/
	
	public enum State {
		EMPTY,
		HEADER,
		SIZE,
		PARTIAL,
		VALID
	}

	private final ByteBuffer packet;	 	//Internally stored packet
	private Byte lastByte; 					//The last byte we read
	private State state; 					//Our current parsing state
	private int length; 					//The payload length of the current packet
	
	
	public PacketParser() {
		packet = ByteBuffer.allocate(100);  //Allocate 200 bytes just to be sure that we never overflow
		lastByte = 0;
		state = State.EMPTY;
	}
	
	
	/**
	 * Flushes the internal ByteBuffer, clears it, and sets all remaining fields to 0.
	 * 
	 */
	private void flush() {
		packet.clear();
		Arrays.fill(packet.array(), (byte) 0);
		lastByte = 0;
		length = 0;
	}
	
	/**
	 * Determines if the internal ByteBuffer holds a valid packet.
	 * 
	 * @param length The length of the packet to be validated.
	 * @return If the packet is valid (i.e., true/false).
	 */
	private boolean validatePacket(int length) {
		
		byte[] b = packet.array();
		
		if(length > 0) {
		
			byte checksum = 0;
			
			//Start on the second index to ignore headers
			
			for(int i = 2; i < length; i++) {
				checksum ^= b[i];
			}
			
			return (checksum == 0);
		} else {
			return false;
		}
	}
	
	/**
	 * Advances the internal state machine of the PacketParser.  
	 * 
	 * @param b The byte to be fed into the state machine.
	 * @return	The state of the state machine given the input.
	 */
	public State advance(byte b) {
		
		switch(state) {
		
			//If we have no bytes, we're looking for the first header byte.
			
			case EMPTY:
				
				if(b == (byte) 0xAA) {
					try {
						packet.put(b);
					} catch (Exception e) {
						flush();
						state = State.EMPTY;
						return state;
					}
					state = State.HEADER;
				} else {
					state = State.EMPTY;
				}
				break;
				
			//If we found the first header byte, look for the second one.
				
			case HEADER: 
				
				if(b == (byte) 0x55) {
					try {
						packet.put(b);
					} catch (Exception e) {
						flush();
						state = State.EMPTY;
						return state;
					}
					state = State.SIZE;
				} else {
					state = State.HEADER;
				}
				break;
				
			//Assume that the next non-zero incoming byte is the payload size.
	
			case SIZE:
				
				if(b > 0 && b < 100) {
					try {
						packet.put(b);
					} catch (Exception e) {
						flush();
						state = State.EMPTY;
						return state;
					}
					length = b + 4; //Account for two headers, payload size, and checksum.
					lastByte = b;
					state = State.PARTIAL;
				} else {
					state = State.SIZE;
				}
				break;			
				
				
			case PARTIAL: 
				
				
				//If our last byte was header one and we just received header two, we need to flush and reset the state machine to the size state.
				
				if(lastByte == (byte) 0xAA && b == (byte) 0x55) {
					byte temp = lastByte;
					flush();
					try {
						packet.put(temp);
						packet.put(b);
					} catch (Exception e) {
						flush();
						state = State.EMPTY;
						return state;
					}
					state = State.SIZE;
				} else {
					
					//If we haven't reached the number of bytes specified by the payload length...
					
					if(packet.position() < length) {
						try {
							packet.put(b);
						} catch (Exception e) {
							flush();
							state = State.EMPTY;
							return state;
						}
						lastByte = b;
						state = State.PARTIAL;
					} else {
						
						//We have a number of bytes equal to that we expected.  Check to see if the packet is valid.  If not, reset the state machine to empty.
						
						try {
							packet.put(b);
						} catch (Exception e) {
							flush();
							state = State.EMPTY;
							return state;
						}
						if(validatePacket(length)) {
							state = State.VALID;
						} else {
							flush();
							state = State.EMPTY;
						}
					}			
				}
				break;
			
			default:
				System.err.println("Something went very wrong in the PacketParser state machine");
				break;		
		}
		
		return state;	
	}
	
	/**
	 * 
	 * Returns the currently held packet and resets the state machine.  Should only be called once the VALID state has been reached 
	 * 
	 * @return The currently held packet
	 */
	public byte[] getPacket() {
		
		int length = packet.position();
		
		byte [] b = new byte[length];
		
		for(int i = length - 1; i >= 0; --i) {
			b[i] = packet.get(i);
		}
		
		flush();
		
		state = State.EMPTY;
				
		return b;
	}
	
	/**
	 * Gets the state
	 * 
	 * @return The current state of the FSM
	 */
	public State getState() {
		return state;
	}
}
