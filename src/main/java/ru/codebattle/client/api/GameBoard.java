package ru.codebattle.client.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;

import static ru.codebattle.client.api.BoardElement.*;

public class GameBoard {

    public GameBoard(String boardString) {
        this.boardString = boardString.replace("\n", "");
    }

    @Getter
    private String boardString;

    public int size() {
        return (int) Math.sqrt(boardString.length());
    }

    public BoardPoint getMyHead() {
        return findFirstElement(HEAD_DEAD, HEAD_DOWN, HEAD_UP, HEAD_LEFT, HEAD_RIGHT, HEAD_EVIL,
                HEAD_FLY, HEAD_SLEEP);
    }

    public List<BoardPoint> getWalls() {
        return findAllElements(WALL);
    }

    public List<BoardPoint> getStones() {
        return findAllElements(STONE);
    }

    public boolean isBarrierAt(BoardPoint point) {
        return getBarriers().contains(point);
    }

    public List<BoardPoint> getApples() {
        return findAllElements(APPLE);
    }

    public boolean amIEvil() {
        return findAllElements(HEAD_EVIL).contains(getMyHead());
    }

    public boolean amIFlying() {
        return findAllElements(HEAD_FLY).contains(getMyHead());
    }

    public List<BoardPoint> getFlyingPills() {
        return findAllElements(FLYING_PILL);
    }

    public List<BoardPoint> getFuryPills() {
        return findAllElements(FURY_PILL);
    }

    public List<BoardPoint> getGold() {
        return findAllElements(GOLD);
    }

    public List<BoardPoint> getStartPoints() {
        return findAllElements(START_FLOOR);
    }

    public List<BoardPoint> getMyBodyAndTail() {
        return findAllElements(TAIL_END_DOWN, TAIL_END_UP, TAIL_END_LEFT, TAIL_END_RIGHT,
                BODY_HORIZONTAL, BODY_VERTICAL, BODY_LEFT_DOWN, BODY_LEFT_UP, BODY_RIGHT_DOWN, BODY_RIGHT_UP);
    }

    public List<BoardPoint> getEnemyBodyAndTail() {
        return findAllElements(ENEMY_HEAD_DOWN, ENEMY_HEAD_LEFT, ENEMY_HEAD_RIGHT, ENEMY_HEAD_UP,
                ENEMY_HEAD_DEAD, ENEMY_HEAD_EVIL, ENEMY_HEAD_SLEEP,
                // хвосты змеек противников
                ENEMY_TAIL_END_DOWN, ENEMY_TAIL_END_LEFT, ENEMY_TAIL_END_UP, ENEMY_TAIL_END_RIGHT, ENEMY_TAIL_INACTIVE,
                // туловище змеек противников
                ENEMY_BODY_HORIZONTAL, ENEMY_BODY_VERTICAL, ENEMY_BODY_LEFT_DOWN, ENEMY_BODY_LEFT_UP, ENEMY_BODY_RIGHT_DOWN,
                ENEMY_BODY_RIGHT_UP);
    }

    public List<BoardPoint> getBarriers() {
        return findAllElements(WALL, START_FLOOR, ENEMY_HEAD_SLEEP, ENEMY_TAIL_INACTIVE, TAIL_INACTIVE, STONE);
    }

    public boolean hasElementAt(BoardPoint point, BoardElement element) {
        if (point.isOutOfBoard(size())) {
            return false;
        }

        return getElementAt(point) == element;
    }

    public BoardElement getElementAt(BoardPoint point) {
        return BoardElement.valueOf(boardString.charAt(getShiftByPoint(point)));
    }

    public void printBoard() {
        for (int i = 0; i < size(); i++) {
            System.out.println(boardString.substring(i * size(), size() * (i + 1)));
        }
    }

    public BoardPoint findElement(BoardElement elementType) {
        for (int i = 0; i < size() * size(); i++) {
            BoardPoint pt = getPointByShift(i);
            if (hasElementAt(pt, elementType)) {
                return pt;
            }
        }
        return null;
    }

    public BoardPoint findFirstElement(BoardElement... elementType) {
        for (int i = 0; i < size() * size(); i++) {
            BoardPoint pt = getPointByShift(i);

            for (BoardElement elemType : elementType) {
                if (hasElementAt(pt, elemType)) {
                    return pt;
                }
            }
        }
        return null;
    }

    public List<BoardPoint> findAllElements(BoardElement... elementType) {
        List<BoardPoint> result = new ArrayList<>();

        for (int i = 0; i < size() * size(); i++) {
            BoardPoint pt = getPointByShift(i);

            for (BoardElement elemType : elementType) {
                if (hasElementAt(pt, elemType)) {
                    result.add(pt);
                }
            }
        }

        return result;
    }

    public boolean hasElementAt(BoardPoint point, BoardElement... elements) {
        return Arrays.stream(elements).anyMatch(element -> hasElementAt(point, element));
    }

    private int getShiftByPoint(BoardPoint point) {
        return point.getY() * size() + point.getX();
    }

    private BoardPoint getPointByShift(int shift) {
        return new BoardPoint(shift % size(), shift / size());
    }
}
