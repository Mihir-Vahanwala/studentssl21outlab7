package bobby;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.concurrent.Semaphore;

public class ManualPlayer{
	public static void main(String [] args){
		int port = Integer.parseInt(args[0]);
		BufferedReader input;
		PrintWriter output;
		Socket socket;
		Console console = System.console();
		try{
			socket = new Socket("127.0.0.1", port);
			String feedback;
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);

			//first move, feedback is the welcome
			if ((feedback = input.readLine()) != null){
				System.out.println(feedback);
				String move = console.readLine();
				output.println(move);
			}
			else{
				return;
			}

			//subsequent moves, feedback in format
			while ((feedback = input.readLine()) != null){
				System.out.println(feedback);
				String indicator;

				indicator = feedback.split("; ")[2];

				if (!indicator.equals("Play")){
					break;
				}
				
				String move = console.readLine();
				output.println(move);
			}
		}
		catch(IOException i){
			return;
		}
		
	}
}