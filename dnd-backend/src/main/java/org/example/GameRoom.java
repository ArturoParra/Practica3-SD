package org.example;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Random;

public class GameRoom {
    private WebSocket player1;
    private WebSocket player2;
    private WebSocket currentTurn;

    // The two teams of 3 monsters for selection
    private Monster[] team1 = new Monster[3];
    private Monster[] team2 = new Monster[3];

    // The monsters chosen by the players for the battle
    private Monster player1Monster = null;
    private Monster player2Monster = null;

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

    public GameRoom(WebSocket player1, WebSocket player2, DndApiService dndApiService,DndApiService.ApiResource[] gameMonsters) {
        this.player1 = player1;
        this.player2 = player2;
        this.currentTurn = player1; // Player 1 always starts first

        try{
            Monster[] RoomMonsters = new Monster[6];
            for(int i = 0; i < 6; i++){
                DndApiService.ApiResource randomMonster = gameMonsters[random.nextInt(gameMonsters.length)];

                RoomMonsters[i] = dndApiService.fetchMonsterDetails(randomMonster.index);

            }

            // Assign first 3 to player 1 and next 3 to player 2
            System.arraycopy(RoomMonsters, 0, team1, 0, 3);
            System.arraycopy(RoomMonsters, 3, team2, 0, 3);

            // Send the teams to the players
            String team1Json = gson.toJson(new ChooseMonsterMessage(this.team1));
            player1.send(team1Json);

            String team2Json = gson.toJson(new ChooseMonsterMessage(this.team2));
            player2.send(team2Json);

        }catch(Exception e){
            System.err.println("Failed to fetch monster details for the game room!");
            e.printStackTrace();
        }

    }

    public WebSocket getPlayer1() {
        return player1;
    }

    public WebSocket getPlayer2() {
        return player2;
    }

    private void broadcastState(boolean isGameOver, String winner) {
        // Crear y enviar estado para el jugador 1
        GameState stateForP1 = new GameState(player1Monster, player2Monster, (currentTurn == player1), isGameOver, winner);
        player1.send(stateForP1.toJson(gson));

        // Crear y enviar estado para el jugador 2
        GameState stateForP2 = new GameState(player2Monster, player1Monster, (currentTurn == player2), isGameOver, winner);
        player2.send(stateForP2.toJson(gson));
    }

    public synchronized void handlePlayerAction(WebSocket player, ActionPayload action) {
        if(player != currentTurn) {
            player.send("{\"type\":\"ERROR\",\"message\":\"Not your turn!\"}");
            return;
        }

        //Determine the attacker and defender based on whose turn it is
        Monster attacker = (player == player1) ? player1Monster : player2Monster;
        Monster defender = (player == player1) ? player2Monster : player1Monster;

        //Apply damage
        int newHP = Math.max(defender.hp - action.getDamage(), 0);
        defender.hp = newHP;

        //Check winning condition
        boolean isGameOver = (defender.hp <= 0);

        String winner = isGameOver ? ((player == player1) ? "Player 1" : "Player 2") : null;

        if(!isGameOver) {
            this.currentTurn = (player == player1) ? player2 : player1;
        }

        this.currentTurn = (player == player1) ? player2 : player1;


        this.broadcastState(isGameOver, winner);

    }

    public void handleMonsterSelection(WebSocket conn, String monsterName) {
        if (conn == player1) {
            // Find the chosen monster in team1
            for (Monster m : team1) {
                if (m.name.equals(monsterName)) {
                    this.player1Monster = m;
                    System.out.println("Player 1 selected: " + m.name);
                    break;
                }
            }
        } else { // It's player 2
            for (Monster m : team2) {
                if (m.name.equals(monsterName)) {
                    this.player2Monster = m;
                    System.out.println("Player 2 selected: " + m.name);
                    break;
                }
            }
        }

        // Synchronization Check: Start the game only when BOTH players have chosen ✅
        if (player1Monster != null && player2Monster != null) {
            System.out.println("Both players have selected. Starting the battle!");
            this.player1Monster = player1Monster; // Assign the chosen monster to the battle state
            this.player2Monster = player2Monster;

            // Use the broadcast method we created earlier to send the first GameState
            broadcastState(false, null);
        }
    }

}
