package com.bobble.api.resources;

public final class BobbleRequest {
    String character;
    int quantity;

    public BobbleRequest() {}

    public BobbleRequest(String character, int quantity) {
        this.character = character;
        this.quantity = quantity;
    }

    public String getCharacter() { return character; }
    public void setCharacter(String value) { character = value; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { quantity = value; }
}