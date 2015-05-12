/*
	Copyright (c) 2015 - York College of Pennsylvania, Paul Glotfelter, Patrick Martin
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
	
	public static final byte[] soundSequencePacket(int sound) {
		//integer tag defines sound to be played:
		//		0 for ON sound
		//		1 for OFF sound
		//		2 for RECHARGE sound
		//		3 for BUTTON sound
		//		4 for ERROR sound
		//		5 for CLEANINGSTART sound
		//		6 for CLEANINGEND sound
		byte[] bytes = new byte[7];
		
		bytes[0] = firstHeader; 
		bytes[1] = secondHeader;
		bytes[2] = 0x03;
		bytes[3] = 0x04; 
		bytes[4] = 0x01;
		bytes[5] = (byte) sound;
		bytes[6] = checksum(bytes, 7);		
		return bytes;		
	}
	
	public static final byte[] ledPacket(int flag) {
		//WARNING: if not careful, this command can turn off external power supplies
		//integer tag defines led/color:
		//		0 for turn off both LED's 
		//		1 for set LED1 red
		//		2 for set LED1 green
		//		3 for set LED2 red
		//		4 for set LED2 green
		
		byte[] bytes = new byte[8];
		
		if(flag < 0 || flag > 4){
			System.out.println("Invalid flag sent to led packet builder!");
			return bytes;
		}
		
		byte command = 0x00;
		if(flag == 1)command = 0x01;
		if(flag == 2)command = 0x02;
		if(flag == 3)command = 0x04;
		if(flag == 4)command = 0x08;
		
		bytes[0] = firstHeader; 
		bytes[1] = secondHeader;
		bytes[2] = 0x03;
		bytes[3] = 0x0C; 
		bytes[4] = 0x02;
		bytes[5] = (byte) 0x80;
		bytes[6] = command;
		bytes[7] = checksum(bytes, 8);		
		return bytes;		
	}
	
}
