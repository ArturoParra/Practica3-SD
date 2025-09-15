package org.example;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;

import com.google.gson.Gson;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends WebSocketServer {

    private static final Gson gson = new Gson();

    private Map<WebSocket, GameRoom> playerRooms = new ConcurrentHashMap<>();

    private WebSocket waitingPlayer = null;

    private final DndApiService dndApiService = new DndApiService();
    private DndApiService.ApiResource[] monsterList;

    // You need a constructor to set up the server's address and port
    public Main(int port) {
        super(new InetSocketAddress(port));
        try{
            System.out.println("Fetching monster list from D&D API...");
            this.monsterList = dndApiService.fetchMonsterList();
            System.out.println("Successfully loaded " + this.monsterList.length + " monsters.");
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if(waitingPlayer == null) {
            this.waitingPlayer = conn;
            conn.send("{\"type\":\"WAITING_FOR_OPPONENT\"}");
            System.out.println("Player connected and waiting: " + conn.getRemoteSocketAddress());
        }else{
            System.out.println("Player connected: " + conn.getRemoteSocketAddress());
            //TODO: Recieve monsters form API and store them in a list for random selection of 6 monsters to send to each GameRoom instance when created


            // Create a new game room for the two players
            GameRoom room = new GameRoom(this.waitingPlayer, conn, dndApiService, monsterList);
            playerRooms.put(this.waitingPlayer, room);
            playerRooms.put(conn, room);

            this.waitingPlayer = null;

        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if(conn == this.waitingPlayer) {
            this.waitingPlayer = null;
            System.out.println("Waiting player disconnected: " + conn.getRemoteSocketAddress());
        }else{
            GameRoom room = playerRooms.get(conn);
            if(room != null) {
               playerRooms.remove(room.getPlayer1());
               playerRooms.remove(room.getPlayer2());
            }
            System.out.println("Player disconnected: " + conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        GameRoom room = playerRooms.get(conn);
        if(room == null){
            return;
        }
        try {
            GameMessage gameMessage = gson.fromJson(message, GameMessage.class);
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);

            // Handle different message types
            String messageType = jsonMessage.get("type").getAsString();

            switch (messageType){
                case "PLAYER_ACTION": {
                    room.handlePlayerAction(conn, gameMessage.getPayload());
                    break;
                }
                case"SELECT_MONSTER" :{
                    // The payload in this case is just the monster's name (a string)
                    String selectedMonsterName = gson.fromJson(jsonMessage.get("payload"), String.class);
                    room.handleMonsterSelection(conn, selectedMonsterName);
                    break;
                }
                default:
                    System.out.println("Unknown message type: " + messageType);
                    break;
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