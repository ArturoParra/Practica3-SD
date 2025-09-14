package org.example;

// A simple container for the data we send to the client.
public class GameState {
    public String type = "GAME_STATE_UPDATE";
    public Monster playerMonster;
    public Monster opponentMonster;

    public GameState(Monster playerMonster, Monster opponentMonster) {
        this.playerMonster = playerMonster;
        this.opponentMonster = opponentMonster;
    }
}