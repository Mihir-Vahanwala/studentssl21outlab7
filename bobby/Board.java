package bobby;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.concurrent.Semaphore;

public class Board{
	private int[] detectives;
	private int fugitive;
	private int time;

	//SYNC SHARED VARIABLES
	
	//to keep track of threads hitting barriers
	public int count;

	//some useful global info
	public int totalThreads; // T
	public int playingThreads; // P
	public int quitThreads; // Q

	/*
	Invariant:

	Let the number of new threads spawned during the round (i.e. awaiting registration,
	see Moderator and ServerThread) be N
	
	Q denotes the number of threads that quit during the round.

	P threads played the round. When the Moderator takes control, no new threads can join

	T = P - Q + N

	This T is indeed the number of threads that'll play the next round, so

	P' = T

	Out of P' permits issued to play, 
	all the P-Q old ones must play. This is handled issuing
	only N permits to register, so a new thread cannot accidentally usurp an old one.

	*/

	public boolean[] availableIDs;

	//is it playing?
	public boolean dead;

	//has no move been played? helpful in intialisation
	public boolean embryo;

	//_________________________________________________________________________________
	
	//SYNC PRIMITIVES: SEMAPHORES
	
	//for threads to take turns and respect timesteps
	public Semaphore countProtector; 
	public Semaphore barrier1;
	public Semaphore barrier2;

	//for moderator to execute between rounds
	public Semaphore moderatorEnabler;
	
	//to protect total_Threads, playing_Threads, and allot IDs, or quit
	public Semaphore threadInfoProtector;

	//to wake up threads about to play their first round
	public Semaphore registration;

	//to wake up existing threads that are waiting for their next round
	public Semaphore reentry;

	public Board(){
		this.detectives = new int[5];
		this.availableIDs = new boolean[5];
		for (int i=0; i< 5; i++){
			this.detectives[i] = -1;
			this.availableIDs[i] = true;
		}
		this.fugitive = -1;
		this.time = 0;

		this.count = 0;
		this.totalThreads = 0;
		this.playingThreads = 0;
		this.quitThreads = 0;
		this.dead = true;
		this.embryo = true;

		this.countProtector = new Semaphore(1); //mutex for count
		this.barrier1 = new Semaphore(0); //permits for first part of cyclic barrier
		this.barrier2 = new Semaphore(0); //permits for second part of cyclic barrier

		this.moderatorEnabler = new Semaphore(1); //permit for moderator
		
		this.threadInfoProtector = new Semaphore(1); //mutex for all public variables other than count

		this.registration = new Semaphore(0); //permits for threads playing their first round

		this.reentry = new Semaphore(0); //permits for threads to play a round
	}

	/*
	function that does what its name says.
	useful to map new player to Detective ID. 
	assumed that you need threadInfoProtector permit to call this
	returns -1 on failure
	if it finds an ID, returns the integer, and also sets availability to false
	*/
	public int getAvailableID(){
		for (int i=0; i<5; i++){
			if (this.availableIDs[i]){
				this.availableIDs[i] = false;
				return i;
			}
		}
		return -1;
	}

	/*
	one-liner to erase players from the map 
	assumed that you need threadInfoProtector permit to call this
	*/
	public void erasePlayer(int id) {
		if (id == -1) {
			this.fugitive = -1;
			this.time++;
			this.dead = true;
			return;
		}
		this.detectives[id] = -1;
		this.availableIDs[id] = true;
		return;
	}

	/* 
	one-liner to install players on the map. Guaranteed to be called by
	a unique thread
	*/
	public void installPlayer(int id) {
		if (id == -1) {
			this.fugitive = 42;
			this.embryo = false;
			return;
		}
		this.detectives[id] = 0;
	}

	/*
	_____________________________________________________________________________________
	The Quotidiane

	Is necessary to hold lock while calling these
	*/ 

	public void moveDetective(int id, int target){
		//perform sanity check on input. If failure, do nothing, just return
		if (target < 0 || target > 63 || id < 0 || id > 4){
			return;
		}

		//check that the detective with given id is actually playing. 
		if (detectives[id] == -1){
			return;
		}

		/*
		detectives move like chess rooks: in straight lines
		
		check that target can be reached. source and target should either have
		same quotient, or same remainder, when divided by 8.

		If not, then do nothing, just return

		If yes, make the move
		*/
		int targetRow = target/8;
		int targetCol = target%8;
		int sourceRow = this.detectives[id]/8;
		int sourceCol = this.detectives[id]%8;

		if ((targetRow != sourceRow) && (targetCol != sourceCol)){
			return;
		}

		this.detectives[id] = target;
		return;
		
	}

	

	public void moveFugitive(int target){
		if (this.playingThreads > 1){
			this.time++;
		}
		
		/*
		time is defined in terms of fugitve moves with at least one installed detective
		a showDetective operation reveals the fugitives location when
		the time is 3 mod 5

		The game ends in victory for the fugitive,
		if the fugitive can make 25 moves without colliding 
		with a detective on a square

		Otherwise, the detectives win
		*/

		if (target < 0 || target > 63){
			return;
		}

		/*
		 * The fugitive moves a like chess queen: in straight lines, or diagonally
		 * 
		 * check that target can be reached. source and target should either have same
		 * quotient, or same remainder, when divided by 8.
		 * 
		 * If not, then do nothing, just return
		 * 
		 * If yes, make the move
		 */

		int targetRow = target/8;
		int targetCol = target%8;
		int sourceRow = this.fugitive/8;
		int sourceCol = this.fugitive%8;

		boolean horizontal = (targetRow == sourceRow);
		boolean vertical = (targetCol == sourceCol);
		boolean criss = (targetRow - targetCol == sourceRow - sourceCol);
		boolean cross = (targetRow + targetCol == sourceRow + sourceCol);

		if (!horizontal && !vertical && !criss && !cross){
			return;
		}
		
		this.fugitive = target;
		return;

	}

	public String showDetective(int id){
		/*
		show fugitive's location if time is 3 mod 5
		or if fugitive is caught (victory for detectives)
		or if time is up (25 moves, defeat for detectives)

		in any case, show Move number, Detective id, Game state, Detective Locations
		*/
		boolean caught = false;
		for (int i=0; i<5; i++){
			if (this.fugitive == this.detectives[i] || this.fugitive == -1){
				caught = true;
				break;
			}
		}
		
		if (caught){
			return String.format("Move %d; Detective %d; Victory; Detectives on %d, %d, %d, %d, %d; Fugitive on %d",
					this.time, id, this.detectives[0], this.detectives[1], this.detectives[2], this.detectives[3],
					this.detectives[4], this.fugitive);
		}
		if (this.time%5 == 3){
			return String.format("Move %d; Detective %d; Play; Detectives on %d, %d, %d, %d, %d; Fugitive on %d",
					this.time, id, this.detectives[0], this.detectives[1], this.detectives[2], this.detectives[3],
					this.detectives[4], this.fugitive);
		}
		if (this.time == 25){
			return String.format("Move %d; Detective %d; Defeat; Detectives on %d, %d, %d, %d, %d; Fugitive on %d",
					this.time, id, this.detectives[0], this.detectives[1], this.detectives[2], this.detectives[3],
					this.detectives[4], this.fugitive);
		}
		
		return String.format("Move %d; Detective %d; Play; Detectives on %d, %d, %d, %d, %d",
				this.time, id, this.detectives[0], this.detectives[1], this.detectives[2], this.detectives[3],
				this.detectives[4]);
	}

	public String showFugitive(){
		/*
		Show fugitive all the info, and make sure you tell fugitive state of the game
		*/ 
		boolean caught = false;
		for (int i = 0; i < 5; i++) {
			if (this.fugitive == this.detectives[i] || this.fugitive == -1) {
				caught = true;
				break;
			}
		}
		if (caught) {
			return String.format("Move %d; Fugitive; Defeat; Detectives on %d, %d, %d, %d, %d; Fugitive on %d",
					this.time, this.detectives[0], this.detectives[1], this.detectives[2], this.detectives[3],
					this.detectives[4], this.fugitive);
		}

		if (this.time == 25){
			return String.format("Move %d; Fugitive; Victory; Detectives on %d, %d, %d, %d, %d; Fugitive on %d",
					this.time, this.detectives[0], this.detectives[1], this.detectives[2], this.detectives[3],
					this.detectives[4], this.fugitive);
		}

		return String.format("Move %d; Fugitive; Play; Detectives on %d, %d, %d, %d, %d; Fugitive on %d", this.time,
				this.detectives[0], this.detectives[1], this.detectives[2], this.detectives[3], this.detectives[4],
				this.fugitive);
	}




}