# Scotland Yard

## The Game
-----------
The city is an **8 x 8** grid, a Fugitive runs amok. In a single turn, the Fugitive can move like a chess Queen: horizontally, vertically, or diagonally. Upto k (say, 5, but that’s easily winning for a smart Fugitive. You can tweak this) Detectives attempt to catch the Fugitive. Detectives move like chess Rooks: horizontally, or vertically. In this game, Players don’t block each others’ paths. 

The Detectives win if one of them lands on the same square as the Fugitive, or if the Fugitive quits the game.

The Detectives lose if none of them can catch the Fugitive within N (say, 25; again, you can tweak this) timesteps.

The Fugitive can see **everyone**, but is mostly invisible to the Detectives: the Fugitive only surfaces when **3 mod 5** timesteps have passed, i.e. at timesteps, 3, 8, … , 23, and finally 25, when the game is over.

## The Client-Server Model
--------------------------
How do we make computer programs play Scotland Yard? We’ve given you `ManualPlayer.java` and `RandomPlayer.java`. A process running the main method of either of these files is what we call a `Client`. The Client establishes a socket connection to the game Server (that’s the process running the main method of ScotlandYard.java) that listens on a port. 

Look at the client code. It basically listens for incoming input from the connection with a `BufferedReader`, and then decides on a move (either gets it from a human typing on the terminal, or generates it randomly), and sends that move across to the server with the `PrintWriter` associated with the socket connection.

The Server, on the other hand, caters to several clients, who are playing a single game. The [Oracle Java Documentation](https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html) is a good place to start learning about socket servers. For multiple clients, the way to go is to spawn threads, one for serving each client, and then continue listening again.

Our server is organised as follows: we have a few ports that we can specify on the command line. For each port, the server loops: spawning a `ScotlandYardGame` and letting it run to completion. It is the `ScotlandYardGame` that listens on a port for client connections.

For each client that connects via a socket, we create a `ServerThread` to talk to it. This `ServerThread` facilitates round by round play. `ServerThread`s playing the same game have shared access to the same Board, and need to access data consistently and run in synchrony.

Finally, we also have the `Moderator`, whose primary job is to allow `ServerThread`s into the next round.

Your job is to get this synchronization right, by completing `ScotlandYard.java`, `Moderator.java`, `ServerThread.java`

## Barriers
-----------
We said that the game proceeds, round by round. Here’s what a round looks like:

1. `ServerThread` gets its move from the client, reading a single line from the socket connection buffer.
2. `ServerThread` is allowed to play, and makes the move on the board
3. `ServerThread` then waits for all other active `ServerThread`s to make their move
4. `ServerThread` gets the feedback for the round, and relays it to the client via the socket buffer.
5. `ServerThread` then waits for all other active `ServerThread`s before proceeding to the next round.

This scenario, where several threads have to wait for each other before proceeding to the next step, is typical: we call the solution a barrier. Java does have an inbuilt barrier, but that only works for a fixed number of threads, once initialised.

In our game, the number of threads each round can vary. A  player might quit, either gracefully by pressing “Q”, or rage quit with `Ctrl+C`. We need to deal with both gracefully. There can also be players waiting to join, and need to be inducted into the game as soon as possible.

It is the `Moderator`’s job to regulate all this between rounds, and set this flexible barrier up for the round. Have a look at the code and comments to see how this can be done. The idea is, the number of threads that we expect to trigger the barrier is `playingThreads`.
