package bittorrent;

import java.util.Scanner;

public class Terminator implements Runnable{
	
	private ClientManager manager;
	
	public Terminator(ClientManager manager){
		this.manager = manager;
	}
	
	public void run(){
		Scanner sc = new Scanner(System.in);
		while (true){
			String input = sc.next();
			if (input.equalsIgnoreCase("q")){
				manager.notifyTrackerOfStopped();
				System.exit(0);
			}
		}
	}
}
