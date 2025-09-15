package org.example;

public class GameMessage {
    private String type;
    private ActionPayload payload;

    public GameMessage(String type, ActionPayload payload) {
        this.type = type;
        this.payload = payload;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public ActionPayload getPayload() {
        return payload;
    }
}