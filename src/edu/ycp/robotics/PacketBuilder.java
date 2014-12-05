/*
	Copyright (c) 2014 - York College of Pennsylvania, Paul Glotfelter
	The MIT License
	See license.txt for details.
*/

package edu.ycp.robotics;

public class PacketBuilder {
	
	/*
	 * Packet structures are based on the Kobuki communications protocol that can be found at :
	 * http://files.yujinrobot.com/kobuki/doxygen/html/enAppendixGuide.html
	*/
	
	private static final byte firstHeader = (byte) 0xAA; 
	private static final byte secondHeader = (byte) 0x55;
	/**
	 * 
	 * 
	 * @param bytes The byte array for which the checksum is computed.
	 * @param length The length of the byte array.
	 * @return The checksum of the given byte array is obtained by xor-ing the packet data, excluding the two header bytes.
	 */
	private static final byte checksum(byte[] bytes, int length) {
		
		int b = 0;
		
		for(int i = 2; i < length; i++) {
			b = (b^bytes[i]);
		}
		
		return (byte) b;
	}
	
	/**
	 * 
	 * @param velocity The velocity at which the robot should move (mm/s).
	 * @param radius The radius of the arc that the robot should follow (mm).
	 * @return The a 10-byte array containing the control packet.
	 */
	
	public static final byte[] baseControlPacket(short velocity, short radius) {
	
		byte[] bytes = new byte[10];
		
		bytes[0] = firstHeader; 
		bytes[1] = secondHeader;
		bytes[2] = 0x06;
		bytes[3] = 0x01; 
		bytes[4] = 0x04;
		bytes[5] = (byte) (velocity & 0xFF);
		bytes[6] = (byte) ((velocity >> 8) & 0xFF);
		bytes[7] = (byte) (radius & 0xFF);
		bytes[8] = (byte) ((radius >> 8) & 0xFF);
		bytes[9] = checksum(bytes, 10);		
		return bytes;		
	}
}
