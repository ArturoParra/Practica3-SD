package org.example;

import com.google.gson.Gson;

// Representa el estado del juego DESDE LA PERSPECTIVA DE UN JUGADOR
public class GameState {
    public String type = "GAME_STATE_UPDATE";
    public Monster yourMonster;
    public Monster opponentMonster;
    public boolean isYourTurn;
    public boolean isGameOver;
    public String winner;

    public GameState(Monster yourMonster, Monster opponentMonster, boolean isYourTurn, boolean isGameOver, String winner) {
        this.yourMonster = yourMonster;
        this.opponentMonster = opponentMonster;
        this.isYourTurn = isYourTurn;
        this.isGameOver = isGameOver;
        this.winner = winner;
    }

    public String toJson(Gson gson) {
        return gson.toJson(this);
    }
}