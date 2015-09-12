package network;

import model.Game;
import model.players.Rocketman;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * Created by Nathan on 9/12/2015.
 */
public class Server {
    ServerSocket socket;
    private HashMap<String, ObjectOutputStream> outputs;
    Game game;
    long lastFpsTime;
    int fps;

    public Server(int port) {
        outputs = new HashMap();

        try {
            // start a new server on port 9001
            socket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            // spawn a client accepter thread
            new Thread(new ClientAccepter()).start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        game = new Game();
        updateLoop();
    }

    /**
     * This thread listens for and sets up connections to new clients
     */
    private class ClientAccepter implements Runnable {
        public void run() {
            try {
                while (true) {
                    // accept a new client, get output & input streams
                    Socket s = socket.accept();
                    ObjectOutputStream output = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream input = new ObjectInputStream(s.getInputStream());

                    // Handle duplicate client names
                    String clientName;
                    while (true) {
                        clientName = (String) input.readObject();
                        if (outputs.containsKey(clientName)) {
                            System.out.println("Refused connection from client with duplicate username " + clientName);
                            output.writeObject(false);
                        } else {
                            //							System.out.println(clientName + " has connected");
                            output.writeObject(true);
                            break;
                        }
                    }

                    // map client name to output stream
                    outputs.put(clientName, output);

                    // spawn a thread to handle communication with this client
                    new Thread(new ClientHandler(input)).start();

                    // spawn player
                    game.players.add(new Rocketman(game));

                    // add a notification message to the chat log
//                    addMessage(clientName + " connected");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This thread reads and executes commands sent by a client
     */
    private class ClientHandler implements Runnable {
        private ObjectInputStream input; // the input stream from the client

        public ClientHandler(ObjectInputStream input) {
            this.input = input;
        }

        @SuppressWarnings("unchecked")
        public void run() {
            try {
                while (true) {
                    System.out.println(input.readObject());
                   /* // read a command from the client, execute on the server
                    Command<Server> command = (Command<Server>)input.readObject();
                    command.execute(Server.this);

                    // terminate if client is disconnecting
                    if (command instanceof DisconnectCommand){
                        input.close();
                        return;
                    }*/
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
    }

    public void updateLoop() {
        long lastLoopTime = System.nanoTime();
        final int TARGET_FPS = 60;
        final long OPTIMAL_TIME = 1000000000 / TARGET_FPS;

        while (true) {
            // work out how long its been since the last update, this
            // will be used to calculate how far the entities should
            // move this loop
            long now = System.nanoTime();
            long updateLength = now - lastLoopTime;
            lastLoopTime = now;
            float delta = updateLength / ((float) OPTIMAL_TIME);

            // update the frame counter
            lastFpsTime += updateLength;
            fps++;

            // update our FPS counter if a second has passed since
            // we last recorded
            if (lastFpsTime >= 1000000000) {
                lastFpsTime = 0;
                fps = 0;
            }

            // update the game logic
            game.update(OPTIMAL_TIME / 1000000000f);

            // send state to all clients
            for (ObjectOutputStream output : outputs.values()) {
                try {
                    output.writeObject(null); // fix this here
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // we want each frame to take 10 milliseconds, to do this
            // we've recorded when we started the frame. We add 10 milliseconds
            // to this and then factor in the current time to give
            // us our final value to wait for
            // remember this is in ms, whereas our lastLoopTime etc. vars are in ns.
            try {
                Thread.sleep((lastLoopTime - System.nanoTime() + OPTIMAL_TIME) / 1000000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Server(9001);
    }
}
