package org.example;

import java.util.List;

public class Monster {
    public String name;
    public int hp;
    public int maxHp;
    public String imageUrl; // <-- Nuevo
    public List<Action> actions; // <-- Nuevo

    public Monster(String name, int hp, int maxHp, String imageUrl, List<Action> actions) {
        this.name = name;
        this.hp = hp;
        this.maxHp = maxHp;
        this.imageUrl = imageUrl;
        this.actions = actions;
    }
}