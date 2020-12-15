package model;

import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameState.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Model {
    private final Map<Integer, GamePlayer> players = new ConcurrentHashMap<>();
    private final Map<Integer, Snake> snakes = new ConcurrentHashMap<>();
    private final Map<Integer, Direction> changes = new ConcurrentHashMap<>();
    private final Field field;
    private final List<Coord> foods;
    private GameConfig config;
    private int stateOrder;
    private final Random random = new Random();



    public Model(GameState state) {
        config = state.getConfig();
        stateOrder = state.getStateOrder();
        field = new Field(config.getHeight(), config.getWidth());
        foods = new ArrayList<>(state.getFoodsList());
        for (int i = 0; i < state.getPlayers().getPlayersCount(); ++i) {
            GamePlayer player = state.getPlayers().getPlayers(i);
            players.put(player.getId(), player);
        }
        for (int i = 0; i < state.getSnakesCount(); ++i) {
            Snake snake = state.getSnakesList().get(i);
            snakes.put(snake.getPlayerId(), snake);
        }
        putSnakes();
        putFoods();
    }

    public void update(GameState state) {
        field.clear();
        foods.clear();
        players.clear();
        snakes.clear();
        config = state.getConfig();
        stateOrder = state.getStateOrder();
        foods.addAll(state.getFoodsList());
        for (int i = 0; i < state.getPlayers().getPlayersCount(); ++i) {
            GamePlayer player = state.getPlayers().getPlayers(i);
            players.put(player.getId(), player);
        }
        for (int i = 0; i < state.getSnakesCount(); ++i) {
            Snake snake = state.getSnakesList().get(i);
            snakes.put(snake.getPlayerId(), snake);
        }
        putSnakes();
        putFoods();
    }

    public void step() {
        moveSnakes();
        createFoods();
        putFoods();
    }

    public void addPlayer(GamePlayer player) {
        players.put(player.getId(), player);
    }

    public void addSnake(Snake snake) {
        snakes.put(snake.getPlayerId(), snake);
    }

    public void addDirectionChange(int id, Direction direction) {
        changes.put(id, direction);
    }

    private void changeDirections() {
        changes.forEach((id, direction) -> {
            Snake snake = snakes.get(id);
            Direction oldDirection = snake.getHeadDirection();
            if ((direction != oldDirection)  && (direction != getReverseDirection(oldDirection))) {
                snake = snake.toBuilder()
                        .setHeadDirection(direction)
                        .build();
                snakes.put(id, snake);
            }
        });
    }

    private Direction getReverseDirection(Direction direction) {
        Direction reverseDirection = direction;
        switch (direction) {
            case DOWN:
                reverseDirection = Direction.UP;
                break;
            case UP:
                reverseDirection = Direction.DOWN;
                break;
            case RIGHT:
                reverseDirection = Direction.LEFT;
                break;
            case LEFT:
                reverseDirection = Direction.RIGHT;
                break;
        }
        return reverseDirection;
    }

    private void createFoods() {
        int foodAmount = config.getFoodStatic() + (int) (players.size() * config.getFoodPerPlayer()) - foods.size();
        if (foodAmount > 0) {
            List<Cell> emptyCells = field.getEmptyCells();
            foodAmount = Integer.min(foodAmount, emptyCells.size());

            for (int i = 0; i < foodAmount; ++i) {
                int index = random.nextInt(emptyCells.size());
                foods.add(emptyCells.get(index).getCoord());
                emptyCells.remove(index);
            }
        }
    }

    private void putFoods() {
        foods.forEach(food -> field.getCell(food.getX(), food.getY()).setState(Cell.State.FOOD));
    }

    private void putSnakes() {
        snakes.forEach((id, snake) -> {
            List<Coord> points = snake.getPointsList();
            int x = points.get(0).getX();
            int y = points.get(0).getY();
            if (field.getCell(x, y).getState() == Cell.State.NOTHING) {
                field.getCell(x, y).setState(Cell.State.HEAD);
                field.getCell(x, y).setPlayerId(id);
            }
            for (int i = 1; i < points.size(); ++i) {
                int xOffset = points.get(i).getX();
                int yOffset = points.get(i).getY();
                if (xOffset == 0) {
                    for (int j = 1; j <= Math.abs(yOffset); ++j) {
                        if (yOffset < 0) {
                            y = (y - 1 + config.getHeight()) % config.getHeight();
                        } else {
                            y = (y + 1) % config.getHeight();;
                        }
                        field.getCell(x, y).setState(Cell.State.BODY);
                        field.getCell(x, y).setPlayerId(id);
                    }
                } else if (yOffset == 0) {
                    for (int j = 1; j <= Math.abs(xOffset); ++j) {
                        if (xOffset < 0) {
                            x = (x - 1 + config.getWidth()) % config.getWidth();
                        } else {
                            x = (x + 1) % config.getWidth();
                        }
                        field.getCell(x, y).setState(Cell.State.BODY);
                        field.getCell(x, y).setPlayerId(id);
                    }
                }
            }
        });
    }

    private void moveSnakes() {
        changeDirections();
        snakes.forEach((id, snake) -> {
            List<Coord> newPoints = new ArrayList<>();
            Coord oldHead = snake.getPoints(0);
            Coord newHead = null;
            Coord newPoint = null;
            switch (snake.getHeadDirection()) {
                case UP: {
                    newHead = createCoord(oldHead.getX(), (oldHead.getY() - 1 + config.getHeight()) % config.getHeight());
                    newPoint = createCoord(0, 1);
                    break;
                }
                case DOWN: {
                    newHead = createCoord(oldHead.getX(), (oldHead.getY() + 1) % config.getHeight());
                    newPoint = createCoord(0, -1);
                    break;
                }
                case RIGHT: {
                    newHead = createCoord((oldHead.getX() + 1) % config.getWidth(), oldHead.getY());
                    newPoint = createCoord(-1, 0);
                    break;
                }
                case LEFT: {
                    newHead = createCoord((oldHead.getX() - 1 + config.getWidth()) % config.getWidth(), oldHead.getY());
                    newPoint = createCoord(1, 0);
                    break;
                }
            }

            newPoints.add(newHead);
            if (changes.containsKey(id)) {
                newPoints.add(newPoint);
                newPoints.add(snake.getPoints(1));
            } else {
                if ((snake.getPointsCount() == 2) && (field.getCell(newHead.getX(), newHead.getY()).getState() != Cell.State.FOOD)) {
                    newPoints.add(snake.getPoints(1));
                } else {
                    newPoints.add(createCoord(newPoint.getX() + snake.getPoints(1).getX(),
                            newPoint.getY() + snake.getPoints(1).getY()));
                }
            }
            for (int i = 2; i < snake.getPointsCount(); ++i) {
                newPoints.add(snake.getPoints(i));
            }

            if (field.getCell(newHead.getX(), newHead.getY()).getState() != Cell.State.FOOD) {
                if (newPoints.size() != 2) {
                    newPoint = newPoints.remove(newPoints.size() - 1);
                    int xOffset = newPoint.getX();
                    int yOffset = newPoint.getY();
                    if ((Math.abs(newPoint.getX()) != 1) && (Math.abs(newPoint.getY()) != 1)) {
                        if (xOffset == 0) {
                            if (yOffset < 0) {
                                newPoint = createCoord(0, (yOffset + 1) % config.getHeight());
                            } else {
                                newPoint = createCoord(0, (yOffset - 1 + config.getHeight()) % config.getHeight());
                            }
                        } else if (yOffset == 0) {
                            if (xOffset < 0) {
                                newPoint = createCoord((xOffset + 1) % config.getWidth(), 0);
                            } else {
                                newPoint = createCoord((xOffset - 1 + config.getWidth()) % config.getWidth(), 0);
                            }
                        }
                        newPoints.add(newPoint);
                    }
                }
            } else {
                increaseScore(id);
                foods.remove(newHead);
            }

            Snake newSnake = Snake.newBuilder()
                    .setHeadDirection(snake.getHeadDirection())
                    .setPlayerId(id)
                    .addAllPoints(newPoints)
                    .setState(Snake.SnakeState.ALIVE)
                    .build();
            snakes.put(id, newSnake);
        });

        checkCollisions();
        putSnakes();
    }

    private void checkCollisions() {
        field.clear();
        putSnakes();

        Set<Integer> deadPlayers = new HashSet<>();
        Map<Coord, Integer> heads = new HashMap<>();
        snakes.forEach((id, snake) -> {
            Coord head = snake.getPoints(0);
            if (heads.containsKey(head)) {
                deadPlayers.add(id);
                deadPlayers.add(heads.get(head));
            } else {
                heads.put(head, id);
            }

            if (field.getCell(head.getX(), head.getY()).getState() == Cell.State.BODY) {
                increaseScore(field.getCell(head.getX(), head.getY()).getPlayerId());
                deadPlayers.add(id);
            }
        });

        deadPlayers.forEach(n -> {
            GamePlayer player = players.get(n);
            player = player.toBuilder()
                    .setRole(NodeRole.VIEWER)
                    .setId(player.getId())
                    .setIpAddress(player.getIpAddress())
                    .setName(player.getName())
                    .setPort(player.getPort())
                    .build();
            players.put(player.getId(), player);
            addDeadFood(player.getId());
            snakes.remove(player.getId());
        });

        field.clear();
    }

    public GamePlayer getPlayer(int id) {
        return players.get(id);
    }

    public void removePlayer(int id) {
        players.values().removeIf(n -> n.getId() == id);
    }

    private void addDeadFood(int id) {
        List<Coord> points = snakes.get(id).getPointsList();
        int x = points.get(0).getX();
        int y = points.get(0).getY();
        for (int i = 1; i < points.size(); ++i) {
            int xOffset = points.get(i).getX();
            int yOffset = points.get(i).getY();
            if (xOffset == 0) {
                for (int j = 1; j < Math.abs(yOffset); ++j) {
                    if (yOffset < 0) {
                        y = (y - 1) % config.getHeight();
                    } else {
                        y = (y + 1) % config.getHeight();;
                    }
                    if (Math.random() < config.getDeadFoodProb()) {
                        foods.add(createCoord(x, y));
                    }
                }
            } else if (yOffset == 0) {
                for (int j = 1; j < Math.abs(xOffset); ++j) {
                    if (xOffset < 0) {
                        x = (x - 1) % config.getWidth();
                    } else {
                        x = (x + 1) % config.getWidth();
                    }
                    if (Math.random() < config.getDeadFoodProb()) {
                        foods.add(createCoord(x, y));
                    }
                }
            }
        }
    }

    private void increaseScore(int id) {
        GamePlayer player = players.get(id);
        if (player != null) {
            player = player.toBuilder()
                    .setScore(player.getScore() + 1)
                    .build();
            players.put(id, player);
        }
    }

    private Coord createCoord(int x, int y) {
        return Coord.newBuilder()
                .setX(x)
                .setY(y)
                .build();
    }

    public GameState getState() {
        return GameState.newBuilder()
                .addAllFoods(foods)
                .addAllSnakes(snakes.values())
                .setPlayers(GamePlayers.newBuilder().addAllPlayers(players.values()))
                .setConfig(config)
                .setStateOrder(++stateOrder)
                .build();
    }

    public GameConfig getConfig() {
        return config;
    }

    public Field getField() {
        return field;
    }
}