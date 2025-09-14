package org.example;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;

import com.google.gson.Gson;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Main extends WebSocketServer {

    private static final Gson gson = new Gson();
    private Set<WebSocket> connections = new CopyOnWriteArraySet<>();

    private Monster playerMonster;
    private Monster opponentMonster;

    // You need a constructor to set up the server's address and port
    public Main(int port) {
        super(new InetSocketAddress(port));
        this.playerMonster = new Monster("Goblin", 10, 10);
        this.opponentMonster = new Monster("Orc", 12, 12);
    }

    public void broadcast(String message) {
        for (WebSocket conn : connections) {
            conn.send(message);
        }
        System.out.println("Broadcasted message: " + message);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn); // Add the new connection
        System.out.println("New connection: " + conn.getRemoteSocketAddress() + " | Total: " + connections.size());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn); // Remove the disconnected connection
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress() + " | Total: " + connections.size());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            GameMessage gameMessage = gson.fromJson(message, GameMessage.class);

            if ("PLAYER_ACTION".equals(gameMessage.getType())) {
                ActionPayload action = gameMessage.getPayload();

                // 1. Apply the damage
                int newHp = Math.max(0, opponentMonster.hp - action.getDamage());
                opponentMonster.hp = newHp;

                // 2. Create an instance of our simplified GameState
                GameState newGameState = new GameState(this.playerMonster, this.opponentMonster);

                // 3. Convert it directly to JSON and broadcast
                String jsonGameState = gson.toJson(newGameState);
                broadcast(jsonGameState);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started successfully");
    }

    // We still need our main method to start the server
    public static void main(String[] args) {
        final int port = 8080;
        Main server = new Main(port);
        server.start();
        System.out.println("WebSocket server started on port: " + port);
    }
}