package bobby;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;


public class RandomPlayer {

	private boolean isFugitive;
	public int square;

	public int nextSquare(){
		int previous = this.square;
		int abort = ThreadLocalRandom.current().nextInt(0, 50);
		if (abort == 0){
			this.square = -1;
			return -1;
		}
		int row = previous / 8;
		int col = previous % 8;
		if (!this.isFugitive){
			int direction = ThreadLocalRandom.current().nextInt(0, 2);
			int shift = ThreadLocalRandom.current().nextInt(0, 8);
			

			int tarrow, tarcol, tarsq;
			if (direction == 1){
				tarrow = (row + shift)%8;
				tarcol = col;
			}
			else{
				tarrow = row;
				tarcol = (col+shift)%8;
			}
			tarsq = 8*tarrow + tarcol;
			this.square = tarsq;
			return tarsq;
		}
		else{
			int direction = ThreadLocalRandom.current().nextInt(0, 4);
			if (direction < 2){
				int shift = ThreadLocalRandom.current().nextInt(0, 8);

				int tarrow, tarcol, tarsq;
				if (direction == 1) {
					tarrow = (row + shift) % 8;
					tarcol = col;
				} else {
					tarrow = row;
					tarcol = (col + shift) % 8;
				}
				tarsq = 8 * tarrow + tarcol;
				this.square = tarsq;
				return tarsq;
			}
			if (direction == 2){
				int sum = row + col;
				int tarrow = ThreadLocalRandom.current().nextInt(0, 8);
				int tarcol = sum - tarrow;
				int tarsq = 8 * tarrow + tarcol;
				this.square = tarsq;
				return tarsq;
			}

			int diff = row - col;
			int tarrow = -1;
			int tarcol = -1;
			if (diff >= 0){
				tarrow = ThreadLocalRandom.current().nextInt(diff, 8);
				tarcol = tarrow - diff;
			}
			else{
				tarcol = ThreadLocalRandom.current().nextInt(-diff, 8);
				tarrow = diff+tarcol;
			}
			int tarsq = 8 * tarrow + tarcol;
			this.square = tarsq;
			return tarsq;
		}
	}

	public RandomPlayer(boolean isFugitive){
		this.isFugitive = isFugitive;
		if (isFugitive){
			this.square = 42;
		}
		else {
			this.square = 0;
		}
	}


	public static void main(String[] args) {
		int port = Integer.parseInt(args[0]);
		BufferedReader input;
		PrintWriter output;
		Socket socket;
		Console console = System.console();
		boolean isFugitive = false;
		RandomPlayer agent;
		int square = -1;
		
		try {
			socket = new Socket("127.0.0.1", port);
			String feedback;
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);

			// first move, feedback is the welcome
			if ((feedback = input.readLine()) != null) {
				System.out.println(feedback);
				if (feedback.split(" ")[3].equals("Fugitive")){
					isFugitive = true;
				}
				else{
					isFugitive = false;
				}
				agent = new RandomPlayer(isFugitive);
				
				if (agent.nextSquare() == -1){
					output.println("Q");
				}
				output.println(agent.square);
			}
			else{
				return;
			}

			// subsequent moves, feedback in format
			while ((feedback = input.readLine()) != null) {
				System.out.println(feedback);
				String indicator;

				indicator = feedback.split("; ")[2];

				if (!indicator.equals("Play")) {
					break;
				}

				if (agent.nextSquare() == -1) {
					output.println("Q");
				}
				output.println(agent.square);
				
			}
		} catch (IOException i) {
			return;
		}

	}
}