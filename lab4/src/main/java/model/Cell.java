package model;

import me.ippolitov.fit.snakes.SnakesProto.GameState.*;

public class Cell {
    private State state;
    private final Coord coord;
    private int playerId;

    public Cell(int x, int y) {
        playerId = 0;
        state = State.NOTHING;
        coord = Coord.newBuilder()
                .setX(x)
                .setY(y)
                .build();
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public Coord getCoord() {
        return coord;
    }

    public enum State {
        NOTHING, FOOD, HEAD, BODY;
    }
}
