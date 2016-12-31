package bittorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class SharedData {

	private static final SharedData inst = new SharedData();
	
	private int downloaded = 0;
	private int uploaded = 0;
	private byte[] requested;
	
	
	private SharedData() {
		super();
	}
	
	public synchronized int getDownloaded(){
		int copy = downloaded;
		downloaded = 0;
		return copy;
	}
	
	public synchronized int getUploaded(){
		int copy = uploaded;
		uploaded = 0;
		return copy;
	}
	
	
	public synchronized byte[] getAvailablePiece(String saveFile){
		downloaded = 0;
		
		byte[] available = null;
		try {
			//File file = new File(saveFile);
			RandomAccessFile access = new RandomAccessFile(saveFile, "r");
			
			available = new byte[(int)access.length()];
			access.readFully(available);
			
			for (int i = 0; i < available.length; i++){
				if(available[i] == 1){
					downloaded++;
				}
			}
			access.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return available;
	}
	//for upload
	public synchronized byte[] getRequestedPiece(String downloadFile, int index, int begin, int length, int size){
		int c;
		byte[] piece = new byte[length];
		
		try {
			//File file = new File(saveFile);
			RandomAccessFile access = new RandomAccessFile(downloadFile, "r");
			
			
			access.seek((index * size) + begin);
			for (int i = 0; i < length; i++){
				c = access.read();
				piece[i] = (byte)c;
			}
			uploaded++;
			access.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return piece;
	}
	
	public synchronized boolean havePiece(String saveFile, int index){
		byte[] available = null;
		boolean havePiece = false;
		try {
			//File file = new File(saveFile);
			RandomAccessFile access = new RandomAccessFile(saveFile, "r");
			
			available = new byte[(int)access.length()];
			access.readFully(available);
			
			if (available[index] == 1){
				havePiece = true;
			}
			access.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return havePiece;
	}
	

	public synchronized int getNextPiece(String saveFile, byte[] peerPieces){
		
		int index = 0;
		int c;
		boolean foundPiece = false;
		
		if (requested == null){
			requested = new byte[peerPieces.length];
		}
		
		try {
			//File file = new File(saveFile);
			RandomAccessFile access = new RandomAccessFile(saveFile, "rw");
			
			while ((c = access.read()) != -1){
				if (c == 0 && peerPieces[index] == 1 && requested[index] != 1){
					foundPiece = true;
					break;
				}
				index++;
			}
			if (!foundPiece){
				
				index = -1;
			}
			else{
				requested[index] = 1;
				//access.seek(index);
				//access.write(1);
			}
			
			access.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		/*
		if (foundPiece){
			access.seek(index);
			access.write(1);
		}
		*/
	
		
		
		return index;
	}
	
	public synchronized void saveToFile(String saveFile, String downloadFile, int index, byte[] bytes, int size){
		

		
		try {
			//File file = new File(downloadFile);
			RandomAccessFile access = new RandomAccessFile(downloadFile, "rw");
			
			access.seek(index * size);
			access.write(bytes);
			
			access.close();
			
			access = new RandomAccessFile(saveFile,"rw");
			access.seek(index);
			access.write(1);
			access.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public synchronized boolean isPieceRequested(int index){
		boolean isRequested = false;
		
		if (requested != null){
			
			if (requested[index] == 1){
				isRequested = true;
			}
			
		} 
		return isRequested;
	}
	
	public synchronized boolean allPiecesRequested(){
		//System.out.println("requested length: " + requested.length);
		for (int i = 0; i < requested.length; i++){
			
			if (requested[i] == 0){
				//System.out.println("requested[" + i + "]: " + requested[i]);
				return false;
			}
		}
		return true;
	}
	
	
	
	public static SharedData getInstance(){
		return inst;
	}
	
}