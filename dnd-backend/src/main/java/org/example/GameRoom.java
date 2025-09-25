package org.example;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.Random;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GameRoom implements Runnable {

    // Clase interna para pasar mensajes junto con la conexión del jugador
    public static class PlayerMessage {
        public final WebSocket connection;
        public final String message;

        public PlayerMessage(WebSocket connection, String message) {
            this.connection = connection;
            this.message = message;
        }
    }

    private final BlockingQueue<PlayerMessage> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean isRunning = true;

    private final DndApiService dndApiService;
    private final DndApiService.ApiResource[] gameMonsters;
    private final Map<String, Monster> monsterCache;

    private Main server;

    private WebSocket player1;
    private WebSocket player2;
    private WebSocket currentTurn;

    // The two teams of 3 monsters for selection
    private Monster[] team1 = new Monster[3];
    private Monster[] team2 = new Monster[3];

    // The monsters chosen by the players for the battle
    private Monster player1Monster = null;
    private Monster player2Monster = null;

    // Temporary storage for selected monsters before the battle starts
    private Monster player1SelectedMonster = null;
    private Monster player2SelectedMonster = null;

    private static final Gson gson = new Gson();

    private static final Random random = new Random();

    // Helper class for the new message type (you can put this in its own file or inside GameRoom)
    private static class ChooseMonsterMessage {
        String type = "CHOOSE_MONSTER";
        Monster[] choices;

        ChooseMonsterMessage(Monster[] choices) {
            this.choices = choices;
        }
    }

    public GameRoom(WebSocket player1, WebSocket player2, DndApiService dndApiService, DndApiService.ApiResource[] gameMonsters, Main server, Map<String, Monster> monsterCache) {
        this.server = server;
        this.player1 = player1;
        this.player2 = player2;
        this.currentTurn = player1; // Player 1 always starts first
        this.dndApiService = dndApiService;
        this.gameMonsters = gameMonsters;
        this.monsterCache = monsterCache;
    }

    public WebSocket getPlayer1() {
        return player1;
    }

    public WebSocket getPlayer2() {
        return player2;
    }

    private void broadcastState(boolean isGameOver, String winner, int damageDealt, String attackerName) {
        // Crear y enviar estado para el jugador 1
        GameState stateForP1 = new GameState(player1Monster, player2Monster, damageDealt, attackerName, (currentTurn == player1), isGameOver, winner);
        player1.send(stateForP1.toJson(gson));

        // Crear y enviar estado para el jugador 2
        GameState stateForP2 = new GameState(player2Monster, player1Monster, damageDealt, attackerName, (currentTurn == player2), isGameOver, winner);
        player2.send(stateForP2.toJson(gson));
    }

    public synchronized void handlePlayerAction(WebSocket player, ActionPayload clientAction) {
        if(player != currentTurn) {
            return;
        }

        //Determine the attacker and defender based on whose turn it is
        Monster attacker = (player == player1) ? player1Monster : player2Monster;
        Monster defender = (player == player1) ? player2Monster : player1Monster;

        // 1. Busca la acción completa del monstruo en el servidor
        Action chosenAction = null;
        for (Action action : attacker.actions) {
            if (action.name.equals(clientAction.getName())) {
                chosenAction = action;
                break;
            }
        }

        int damageDealt = 0;
        if (chosenAction != null && chosenAction.damage != null && chosenAction.damage.length > 0) {
            // 2. Calcula el daño usando la fórmula del dado del servidor
            String diceFormula = chosenAction.damage[0].damage_dice;
            System.out.println("DEBUG: About to calculate damage. Formula is: '" + diceFormula + "'");
            damageDealt = calculateDiceDamage(diceFormula);
        }

        // 3. Aplica el daño
        int newHp = Math.max(0, defender.hp - damageDealt);
        defender.hp = newHp;

        //Check winning condition
        boolean isGameOver = (defender.hp <= 0);

        String winner = isGameOver ? ((player == player1) ? "Player 1" : "Player 2") : null;

        if(!isGameOver) {
            this.currentTurn = (player == player1) ? player2 : player1;
        }else{
            scheduleEndGame();
        }

        this.currentTurn = (player == player1) ? player2 : player1;


        this.broadcastState(isGameOver, winner, damageDealt, attacker.name);

    }

    public void handleMonsterSelection(WebSocket conn, String monsterName) {
        Monster chosenMonster = null;

        // First, identify which player is making the selection (conn)
        if (conn == player1) {
            // Find the chosen monster from their specific team
            for (Monster m : team1) {
                if (m.name.equals(monsterName)) {
                    chosenMonster = m;
                    break;
                }
            }
            // Store the choice in the correct variable
            if (chosenMonster != null) {
                this.player1SelectedMonster = chosenMonster;
                System.out.println("Player 1 selected: " + chosenMonster.name);
            }
        } else { // It must be player 2
            for (Monster m : team2) {
                if (m.name.equals(monsterName)) {
                    chosenMonster = m;
                    break;
                }
            }
            if (chosenMonster != null) {
                this.player2SelectedMonster = chosenMonster;
                System.out.println("Player 2 selected: " + chosenMonster.name);
            }
        }

        // --- THIS IS THE CRITICAL SYNCHRONIZATION CHECK ---
        // Start the game only when BOTH players have a selected monster.
        if (player1SelectedMonster != null && player2SelectedMonster != null) {
            System.out.println("Both players have selected. Starting the battle!");

            // Assign the chosen monsters to the active battle state
            this.player1Monster = player1SelectedMonster;
            this.player2Monster = player2SelectedMonster;

            // Send the initial game state update to both players
            broadcastState(false, null, 0, null);
        }
    }

    public void enqueueMessage(PlayerMessage message) {
        messageQueue.add(message);
    }

    @Override
    public void run() {
        // Configuración del juego
        try {
            Monster[] roomMonsters = new Monster[6];
            for (int i = 0; i < 6; i++) {
                roomMonsters[i] = findValidMonster(dndApiService, gameMonsters, monsterCache);
            }

            System.arraycopy(roomMonsters, 0, team1, 0, 3);
            System.arraycopy(roomMonsters, 3, team2, 0, 3);

            player1.send(gson.toJson(new ChooseMonsterMessage(this.team1)));
            player2.send(gson.toJson(new ChooseMonsterMessage(this.team2)));

        } catch (Exception e) {
            System.err.println("Failed to fetch monster details for the game room!");
            e.printStackTrace();
            // Considerar terminar el hilo o notificar a los jugadores del error
            return;
        }

        // Bucle principal del juego
        while (isRunning) {
            try {
                PlayerMessage playerMessage = messageQueue.take(); // Bloquea hasta que haya un mensaje disponible
                WebSocket conn = playerMessage.connection;
                String message = playerMessage.message;

                JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
                String messageType = jsonMessage.get("type").getAsString();

                if (conn == null) continue;

                switch (messageType) {
                    case "PLAYER_ACTION": {
                        ActionPayload action = gson.fromJson(jsonMessage.get("payload"), ActionPayload.class);
                        handlePlayerAction(conn, action);
                        break;
                    }
                    case "SELECT_MONSTER": {
                        String selectedMonsterName = gson.fromJson(jsonMessage.get("payload"), String.class);
                        handleMonsterSelection(conn, selectedMonsterName);
                        break;
                    }
                    default:
                        System.out.println("Unknown message type in game room: " + messageType);
                        break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isRunning = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Este método auxiliar ya no es necesario, ya que la conexión se pasa directamente.
    // private WebSocket getPlayerFromMessage(JsonObject message) { ... }


    private int calculateDiceDamage(String diceFormula) {
        // Patrón para encontrar fórmulas como "2d6+3", "1d8", "1d12-1", etc.
        Pattern pattern = Pattern.compile("(\\d+)d(\\d+)([+-]\\d+)?");
        Matcher matcher = pattern.matcher(diceFormula);

        if (matcher.find()) {
            int numberOfDice = Integer.parseInt(matcher.group(1));
            int diceSides = Integer.parseInt(matcher.group(2));
            int modifier = 0;

            // Revisa si hay un modificador (+3, -1, etc.)
            if (matcher.group(3) != null) {
                modifier = Integer.parseInt(matcher.group(3));
            }

            int totalDamage = 0;
            for (int i = 0; i < numberOfDice; i++) {
                // Lanza un dado (número aleatorio entre 1 y el número de caras)
                totalDamage += random.nextInt(diceSides) + 1;
            }

            System.out.println("Rolled " + diceFormula + " -> Damage: " + (totalDamage + modifier));
            return totalDamage + modifier;
        }

        // Si la fórmula no coincide, devuelve 0 o un valor por defecto
        return 0;
    }

    private Monster findValidMonster(DndApiService apiService, DndApiService.ApiResource[] monsterList, Map<String, Monster> monsterCache) throws Exception {
        // Loop indefinitely until we find a valid monster
        while (true) {
            // 1. Pick a random monster from the master list
            DndApiService.ApiResource randomMonsterInfo = monsterList[random.nextInt(monsterList.length)];

            // 2. Fetch its details. The getMonsterDetails method already filters the actions.
            Monster candidateMonster = apiService.getMonsterDetails(randomMonsterInfo.index, monsterCache);

            // 3. Check if the monster is valid (has at least one action after filtering)
            if (candidateMonster != null && candidateMonster.actions != null && !candidateMonster.actions.isEmpty()) {
                System.out.println("Found valid monster for battle: " + candidateMonster.name);
                return candidateMonster; // The monster is good, return it.
            }

            // If the monster wasn't valid, the loop will simply run again.
            System.out.println("... " + randomMonsterInfo.name + " has no valid damaging actions, trying again.");
        }
    }

    private Thread gameThread;

    public void setGameThread(Thread thread) {
        this.gameThread = thread;
    }

    public void stop() {
        isRunning = false;
        if (gameThread != null) {
            gameThread.interrupt();
        }
    }

    private void scheduleEndGame() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Timer finished. Ending game for room.");
                server.endGameAndRequeue(GameRoom.this);
                stop(); // Detiene el hilo de la sala de juego
            }
        }, 5000); // 5000 milisegundos = 5 segundos
    }

}
