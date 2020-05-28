package ru.codebattle.client.api;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class SnakeTarget {
    private BoardPoint head;
    private BoardPoint target;
    private List<BoardPoint> snakeLeftBody = new ArrayList<>();
    private List<BoardPoint> snakeRightBody = new ArrayList<>();

    public SnakeTarget(BoardPoint target) {
        this.target = target;
        snakeLeftBody.add(target);
        snakeRightBody.add(target);
    }

    public void addPieceToLeft(BoardPoint boardPoint) {
        snakeLeftBody.add(boardPoint);
    }

    public void addPieceToRight(BoardPoint boardPoint) {
        snakeRightBody.add(boardPoint);
    }
}
