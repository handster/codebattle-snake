package ru.codebattle.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import ru.codebattle.client.api.*;

import static ru.codebattle.client.api.BoardElement.*;
import static ru.codebattle.client.api.Direction.*;

@Slf4j
public class Main {

    private static final String SERVER_ADDRESS = "http://codebattle-pro-2020s1.westeurope.cloudapp.azure.com/codenjoy-contest/board/player/0i28858kqgje0hm6uqui?code=9205253768897784839&gameName=snakebattle";

    public static void main(String[] args) throws URISyntaxException, IOException {
        SnakeBattleClient client = new SnakeBattleClient(SERVER_ADDRESS);
        client.run(gameBoard -> {
            // Найти свою координату
            BoardPoint myHead = gameBoard.getMyHead();
            log.info("My head is " + myHead);

            // Найти таблетку ярости ближайшую, идти к ней по пути поедая яблоки
            //TODO посчитать, что ты дойдешь до нее быстрее других

            // Пока собирать только яблоки
            List<BoardPoint> apples = gameBoard.findAllElements(APPLE, GOLD, FURY_PILL);
            //TODO удалить отсюда яблоки, которые в тупиках
            apples = getApplesWithoutBlockApples(apples, gameBoard);
//            log.info("Apples " + apples);
            List<BoardPoint> nearestPathToApple = getNearestPathToApple(apples, myHead, gameBoard);
            log.info("Nearest path is " + nearestPathToApple);
            // и можно ли в них идти
            if (nearestPathToApple.isEmpty()) {
                System.out.println("Что-то пошло не так");
                return new SnakeAction(false, STOP);
            }
            Direction direction = goToWin(myHead, nearestPathToApple.get(0));
            log.info("Direction " + direction);

            return new SnakeAction(false, direction);
        });

        System.in.read();

        client.initiateExit();
    }

    private static List<BoardPoint> getApplesWithoutBlockApples(List<BoardPoint> apples, GameBoard gameBoard) {
        return apples.stream()
                .filter(apple -> !getThreeWallsAround(gameBoard, apple))
                .collect(Collectors.toList());
    }

    private static boolean getThreeWallsAround(GameBoard gameBoard, BoardPoint apple) {
        Set<BoardPoint> pointsAroundMe = getPointsAroundMe(gameBoard, apple);
        return pointsAroundMe.stream()
                .filter(boardPoint -> gameBoard.getElementAt(boardPoint) == WALL)
                .count() == 3;
    }

    private static Set<BoardPoint> getPointsAroundMe(GameBoard gameBoard, BoardPoint boardPoint) {
        int size = gameBoard.size();
        Set<BoardPoint> aroundMePoints = new HashSet<>();
        // Получить все точки вокруг себя
        BoardPoint leftPoint = boardPoint.shiftLeft(1);
        if (!leftPoint.isOutOfBoard(size)) {
            aroundMePoints.add(leftPoint);
        }
        BoardPoint rightPoint = boardPoint.shiftRight(1);
        if (!rightPoint.isOutOfBoard(size)) {
            aroundMePoints.add(rightPoint);
        }
        BoardPoint topPoint = boardPoint.shiftTop(1);
        if (!topPoint.isOutOfBoard(size)) {
            aroundMePoints.add(topPoint);
        }
        BoardPoint bottomPoint = boardPoint.shiftBottom(1);
        if (!bottomPoint.isOutOfBoard(size)) {
            aroundMePoints.add(bottomPoint);
        }
        return aroundMePoints;
    }

    public static List<BoardPoint> getNearestPathToApple(List<BoardPoint> allApples, BoardPoint myPlace, GameBoard gameBoard) {

        List<BoardPoint> pathToOurApples = new ArrayList<>();
        List<Node> nodes = mapToNode(allApples);

        List<BoardPoint> allElements = gameBoard.findAllElements(NONE, APPLE, GOLD, FURY_PILL);

        //Неразмеченные точки
        List<Node> unsettledNodes = mapToNode(allElements);

        //Размеченные точки
        List<Node> settledNodes = new ArrayList<>();
        settledNodes.add(new Node(myPlace, null));
        List<BoardPoint> barriers = gameBoard.getBarriers();
        barriers.addAll(gameBoard.getMyBodyAndTail());
        barriers.addAll(gameBoard.getEnemyBodyAndTail());
        while (!unsettledNodes.isEmpty()) {
            //Получаем первую ближайшую точку
            Set<Node> currentNodes = getLowestDistanceNode(unsettledNodes, settledNodes, gameBoard, barriers);
            if (currentNodes.isEmpty()) {
                System.out.println("Попали в тупик");
                return Collections.emptyList();
            }
            unsettledNodes.removeAll(currentNodes);
            settledNodes.addAll(currentNodes);
            if (CollectionUtils.containsAny(settledNodes, nodes)) {
                List<Node> nodes1 = (List<Node>) CollectionUtils.retainAll(settledNodes, nodes);
                Node path = nodes1.get(0);
                while (path.getParent() != null) {
                    pathToOurApples.add(path.getBoardPoint());
                    path = path.getParent();
                }
                Collections.reverse(pathToOurApples);
                log.warn("Путь самурая " + pathToOurApples);
                return pathToOurApples;
            }
        }

        return pathToOurApples;
    }

    private static Set<Node> getLowestDistanceNode(List<Node> unsettledNodes, List<Node> settledNodes,
                                                   GameBoard gameBoard, List<BoardPoint> barriers) {
        // Для всех точек из settledNodes получить точки вокруг
        Set<Node> set = new HashSet<>();
        for (Node node : settledNodes) {
            List<Node> nodes = mapToNode(Main.getAroundMePointsExcludingBarriers(node.getBoardPoint(), gameBoard, barriers));
            // Проверить, что этих точек нет в unsettledNodes
            List<Node> nodes1 = (List<Node>) CollectionUtils.retainAll(nodes, unsettledNodes);
            if (nodes1.isEmpty()) {
                continue;
            }
            // Устанавливаем нашего родителя
            nodes1.forEach(node1 -> node1.setParent(node));
            set.addAll(nodes1);
        }
        return set;
    }

    private static List<Node> mapToNode(Collection<BoardPoint> boardPoints) {
        return boardPoints.stream()
                .map(boardPoint -> new Node(boardPoint, null))
                .collect(Collectors.toList());
    }

    public static Set<BoardPoint> getAroundMePointsExcludingBarriers(BoardPoint boardPoint, GameBoard gameBoard, List<BoardPoint> barriers) {
        Set<BoardPoint> pointsAroundAnotherPoint = getPointsAroundMe(gameBoard, boardPoint);
        pointsAroundAnotherPoint.removeAll(barriers);
        return pointsAroundAnotherPoint;
    }

    private static Direction goToWin(BoardPoint myPlace, BoardPoint placeToMove) {

        if (placeToMove == null) {
            return STOP;
        }

        int x = placeToMove.getX();
        int y = placeToMove.getY();
        int myPlaceX = myPlace.getX();
        int myPlaceY = myPlace.getY();

        // Двигаемся либо вверх, либо вниз
        if (x == myPlaceX) {
            if (y > myPlaceY) {
                return Direction.DOWN;
            }
            return UP;
        }

        // Двигаемся либо вправо, либо влево
        if (y == myPlaceY) {
            if (x > myPlaceX) {
                return Direction.RIGHT;
            } else return LEFT;
        }

        return STOP;
    }
}
