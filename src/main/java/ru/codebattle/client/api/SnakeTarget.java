package ru.codebattle.client.api;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class SnakeTarget {
    private List<BoardPoint> snakeBody = new ArrayList<>();

    public SnakeTarget(List<BoardPoint> snakeBody) {
        this.snakeBody = snakeBody;
    }

    public BoardPoint getEnemyHead() {
        return snakeBody.get(snakeBody.size() - 1);
    }

    public int getLength() {
        return snakeBody.size();
    }
}
