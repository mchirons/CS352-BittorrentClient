package bittorrent;

/*
 * Mark Hirons mch165 167008833
 */

import java.security.SecureRandom;

public class Utilities {

	public static int byteArrayToInt(byte[] b) 
	{
	    return   b[3] & 0xFF |
	            (b[2] & 0xFF) << 8 |
	            (b[1] & 0xFF) << 16 |
	            (b[0] & 0xFF) << 24;
	}
	
	public static byte[] copyByteArray(byte[] array, int index, int numOfBytes){
		byte [] copy = new byte[numOfBytes];
		for (int i = 0; i < numOfBytes; i++){
			copy[i] = array[index + i];
		}
		return copy;
	}
	
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	static SecureRandom rnd = new SecureRandom();

	public static String generatePeerID(){
	   StringBuilder sb = new StringBuilder( 20 );
	   for( int i = 0; i < 20; i++ ) 
	      sb.append( AB.charAt( rnd.nextInt(AB.length())));
	   return sb.toString();
	}
}
