package model;

import me.ippolitov.fit.snakes.SnakesProto.GameState.*;

import java.util.ArrayList;
import java.util.List;

public class Field {
    //private GameConfig config;
    private final int height;
    private final int width;
    private final Cell[][] field;

    public Field(int height, int width) {
        this.height = height;
        this.width = width;
        field = new Cell[width][height];
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                field[i][j] = new Cell(i, j);
            }
        }
    }

    public List<Cell> getEmptyCells() {
        List<Cell> emptyCells = new ArrayList<>();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (field[i][j].getState() == Cell.State.NOTHING) {
                    emptyCells.add(field[i][j]);
                }
            }
        }
        return emptyCells;
    }

    public Coord getNewHeadCoord() {
        for (int i = 2; i < (width - 2); ++i) {
            for (int j = 2; j < (height - 2); ++j) {
                if (isSuitableSquare(i, j)) {
                    return field[i][j].getCoord();
                }
            }
        }
        return null;
    }

    private boolean isSuitableSquare(int x, int y) {
        for (int i = x; i < 5; ++i) {
            for (int j = y; j < 5; ++j) {
                if (field[x][y].getState() != Cell.State.NOTHING) {
                    return false;
                }
            }
        }
        return true;
    }

    public Cell getCell(int x, int y) {
        return field[x][y];
    }

    public void clear() {
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                field[i][j].setState(Cell.State.NOTHING);
                field[i][j].setPlayerId(0);
            }
        }
    }

}
