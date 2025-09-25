package org.example;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends WebSocketServer {

    private Map<WebSocket, GameRoom> playerRooms = new ConcurrentHashMap<>();

    private WebSocket waitingPlayer = null;

    private final DndApiService dndApiService = new DndApiService();
    private DndApiService.ApiResource[] monsterList;

    private final Map<String, Monster> monsterCache = new ConcurrentHashMap<>();

    // Constructor de la clase Main
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

            GameRoom room = new GameRoom(this.waitingPlayer, conn, dndApiService, monsterList, this, monsterCache);
            playerRooms.put(this.waitingPlayer, room);
            playerRooms.put(conn, room);

            // Start the game room in a new thread
            Thread gameThread = new Thread(room);
            room.setGameThread(gameThread); // Pass the thread to the room
            gameThread.start();

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
        if (room != null) {
            room.enqueueMessage(new GameRoom.PlayerMessage(conn, message));
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

    // Este método es llamado por la sala de juego después del temporizador
    public synchronized void endGameAndRequeue(GameRoom room) {
        System.out.println("Re-queuing players.");

        // Obtener a los jugadores antes de disolver la sala
        WebSocket p1 = room.getPlayer1();
        WebSocket p2 = room.getPlayer2();

        // Disolver la sala
        playerRooms.remove(p1);
        playerRooms.remove(p2);

        // Volver a poner a cada jugador en la cola
        requeuePlayer(p1);
        requeuePlayer(p2);
    }

    // Este método contiene la lógica de matchmaking que ya teníamos en onOpen
    private void requeuePlayer(WebSocket conn) {
        if (waitingPlayer == null) {
            this.waitingPlayer = conn;
            conn.send("{\"type\": \"WAITING_FOR_OPPONENT\"}");
            System.out.println("Player " + conn.getRemoteSocketAddress() + " is now waiting.");
        } else {
            System.out.println("Player " + conn.getRemoteSocketAddress() + " matched with waiting player. Starting new game.");
            GameRoom newRoom = new GameRoom(this.waitingPlayer, conn, dndApiService, monsterList, this, monsterCache);
            playerRooms.put(this.waitingPlayer, newRoom);
            playerRooms.put(conn, newRoom);
            Thread gameThread = new Thread(newRoom);
            newRoom.setGameThread(gameThread);
            gameThread.start();
            this.waitingPlayer = null;
        }
    }

    // We still need our main method to start the server
    public static void main(String[] args) {
        final int port = 8080;
        Main server = new Main(port);
        server.start();
        System.out.println("WebSocket server started on port: " + port);
    }
}