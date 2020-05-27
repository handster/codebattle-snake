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
    //    public static final int LIMIT_SIZE = 10;
    public static int limitEvilCount = 9;
    public static final int STONE_RADIUS = 2;
    public static int evilCount = 0;
    public static List<BoardPoint> furyPills = new ArrayList<>();

    public static void main(String[] args) throws URISyntaxException, IOException {
        SnakeBattleClient client = new SnakeBattleClient(SERVER_ADDRESS);
        client.run(gameBoard -> {
            // Найти свою координату
            BoardPoint myHead = gameBoard.getMyHead();
            log.info("My head is " + myHead);
            if (myHead == null) {
                return new SnakeAction(false, STOP);
            }

            // Если под яростью, то искать ближайшую жертву
            // Найти таблетку ярости ближайшую, идти к ней по пути поедая яблоки
            //TODO посчитать, что ты дойдешь до нее быстрее других


            // Пока собирать только яблоки
            List<BoardPoint> apples = gameBoard.findAllElements(APPLE, GOLD, FURY_PILL);
            boolean headEvil = false;
            // Воткнуть проверку была ли съедена таблетка под действием другой таблетки
            if (gameBoard.getElementAt(myHead) == HEAD_EVIL) {
                // Если на месте таблеток ярости теперь моя голова, то увеличиваем счетчик
                if (furyPills.contains(myHead)) {
                    limitEvilCount += 10;
                    log.info("Жадность до таблеток " + limitEvilCount);
                }
                furyPills = gameBoard.getFuryPills();
                evilCount++;
                if (evilCount <= limitEvilCount) {
                    headEvil = true;
//                    List<BoardPoint> enemyBodyAndTail = gameBoard.getEnemyBodyAndTail();
//                    apples.addAll(enemyBodyAndTail);
//                    apples.addAll(gameBoard.findAllElements(STONE));
                }
            } else {
                evilCount = 0;
                limitEvilCount = 8;
            }
            log.info("Evil count is " + evilCount);
            // Удаляем яблоки, которые стоят в тупиках
            apples = getApplesWithoutBlockApples(apples, gameBoard, headEvil);
            log.info("Apples " + apples);
            List<BoardPoint> nearestPathToApple = getNearestPathToApple(apples, myHead, gameBoard, headEvil);
            log.info("Nearest path is " + nearestPathToApple);
            // и можно ли в них идти
            if (nearestPathToApple.isEmpty()) {
                System.out.println("Что-то пошло не так");
                //TODO Проверить соседние поля, если там камень то идти туда
                Set<BoardPoint> pointsAroundMe = getPointsAroundMe(gameBoard, myHead);
                List<BoardPoint> collect = pointsAroundMe.stream()
                        .filter(boardPoint -> gameBoard.hasElementAt(boardPoint, NONE, STONE, ENEMY_HEAD_DOWN,
                                ENEMY_HEAD_LEFT,
                                ENEMY_HEAD_RIGHT,
                                ENEMY_HEAD_UP))
                        .collect(Collectors.toList());
                if (collect.isEmpty()) {
                    System.out.println("Все равно что-то не так");
                    return new SnakeAction(false, STOP);
                }
                Direction direction = goToWin(myHead, collect.get(new Random().nextInt(collect.size())));
                return new SnakeAction(false, direction);
            }
            Direction direction = goToWin(myHead, nearestPathToApple.get(0));

            boolean act = getAct(gameBoard);
            return new SnakeAction(act, direction);
        });

        System.in.read();

        client.initiateExit();
    }

    private static boolean getAct(GameBoard gameBoard) {
        List<BoardPoint> myTails = gameBoard.getMyTail();
        if (myTails.isEmpty()) {
            return false;
        }
        BoardPoint myTail = myTails.get(0);
        // Получить все точки на расстоянии 2 ходов от моего хвоста
        List<BoardPoint> pointsAroundMyTail = getPointsAroundAnotherPoint(gameBoard, myTail, STONE_RADIUS);
        List<BoardPoint> enemyHeads = gameBoard.getEnemyHeads();
        //TODO проверить оставляем мы камни или нет
        if (CollectionUtils.containsAny(pointsAroundMyTail, enemyHeads)) {
            return true;
        }
        return false;
    }

    private static List<BoardPoint> getPointsAroundAnotherPoint(GameBoard gameBoard, BoardPoint boardPoint, int radius) {
        int size = gameBoard.size();
        List<BoardPoint> aroundMePoints = new ArrayList<>();
        // Получить все точки вокруг себя
        for (int i = 1; i <= radius; i++) {
            BoardPoint leftPoint = boardPoint.shiftLeft(i);
            if (!leftPoint.isOutOfBoard(size)) {
                aroundMePoints.add(leftPoint);
            }
            BoardPoint rightPoint = boardPoint.shiftRight(i);
            if (!rightPoint.isOutOfBoard(size)) {
                aroundMePoints.add(rightPoint);
            }
            BoardPoint topPoint = boardPoint.shiftTop(i);
            if (!topPoint.isOutOfBoard(size)) {
                aroundMePoints.add(topPoint);
            }
            BoardPoint bottomPoint = boardPoint.shiftBottom(i);
            if (!bottomPoint.isOutOfBoard(size)) {
                aroundMePoints.add(bottomPoint);
            }
        }
        return aroundMePoints;
    }

    private static List<BoardPoint> getApplesWithoutBlockApples(List<BoardPoint> apples, GameBoard gameBoard, boolean headEvil) {
        return apples.stream()
                .filter(apple -> !getThreeWallsAround(gameBoard, apple, headEvil))
                .collect(Collectors.toList());
    }

    private static boolean getThreeWallsAround(GameBoard gameBoard, BoardPoint apple, boolean headEvil) {
        Set<BoardPoint> pointsAroundMe = getPointsAroundMe(gameBoard, apple);
        int size = gameBoard.getMyBodyAndTail().size();
        if (
            // Неактуально стало есть камни без ярости
//                size > LIMIT_SIZE ||
                headEvil) {
            return pointsAroundMe.stream()
                    .filter(boardPoint -> gameBoard.getElementAt(boardPoint) == WALL)
                    .count() == 3;
        }

        return pointsAroundMe.stream()
                .filter(boardPoint -> gameBoard.hasElementAt(boardPoint, WALL, STONE))
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

    // Поиск пути к ближайшей цели
    public static List<BoardPoint> getNearestPathToApple(List<BoardPoint> allApples, BoardPoint myHead,
                                                         GameBoard gameBoard, boolean headEvil) {

        List<BoardPoint> pathToOurApples = new ArrayList<>();
        List<BoardPoint> stones = gameBoard.findAllElements(STONE);

        // Элементы через которые можно проложить путь
        List<BoardPoint> allElements = gameBoard.findAllElements(NONE);

        // Элементы, в которые лучше не врезаться. Убрал отсюда камни
        List<BoardPoint> barriers = gameBoard.getBarriers();
        barriers.addAll(gameBoard.getMyBodyAndTail());

        // Враги
        List<BoardPoint> enemyBodyAndTail = gameBoard.getEnemyBodyAndTail();

        // Получаем длину тела
        int myBodySize = gameBoard.getMyBodyAndTail().size();
        log.info("My length is " + myBodySize);

        // Получаем точки, на которые могут следующим ходом попасть головы врагов
        List<BoardPoint> allBoardPointEnemyHeadsAround = getAllBoardPointEnemyHeadsAround(gameBoard);
        Set<BoardPoint> pointsAroundMe = getPointsAroundMe(gameBoard, myHead);

        // Если на клетку куда двигается моя голова могут попасть головы врагов,
        // то убираем такие точки из целей и точек передвижения
        Collection<BoardPoint> commonPoints = CollectionUtils.retainAll(pointsAroundMe, allBoardPointEnemyHeadsAround);

        boolean isMyEnemyLongerThanI = false;
        boolean isMyEnemyEvil = false;
        if (!commonPoints.isEmpty()) {
            // Находим голову вражеской змеи, с которой можем столкнуться
            Optional<BoardPoint> any = gameBoard.getEnemyHeads().stream()
                    .filter(head -> CollectionUtils.containsAny(getPointsAroundMe(gameBoard, head), pointsAroundMe))
                    .findAny();
            if (any.isPresent()) {
                BoardPoint enemyHead = any.get();

                // Если враг под таблеткой, то длину не посчитать
                if (gameBoard.hasElementAt(enemyHead, ENEMY_HEAD_EVIL)) {
                    isMyEnemyEvil = true;
                } else {
                    //Теперь нужно посчитать длину врага
                    int enemyLength = getEnemyLength(gameBoard, enemyHead);
                    log.info("Enemy length is " + enemyLength);

                    // Если враг длиннее или под таблеткой, то убегаем от него
                    if (enemyLength + 2 > myBodySize) {
                        isMyEnemyLongerThanI = true;
                    }
                }
            }
        }

        // Если под таблеткой ярости, до добавить в цели врагов и камни
        if (headEvil) {
            allApples.addAll(enemyBodyAndTail);
            allApples.addAll(stones);
            // Добавляем в список клеток для движения врагов и камни
            log.info("В АТАКУ ...");

            // Если враг под яростью и длиннее чем я, то убегаем
            if (isMyEnemyEvil && isMyEnemyLongerThanI) {
                allElements.removeAll(commonPoints);
                allApples.removeAll(commonPoints);
            }
        } else {
        // Неактуально стало есть камни без ярости
                    // Если длина меньше, то не едим камни
//            if (myBodySize < LIMIT_SIZE) {
//                barriers.addAll(stones);
//            }
            //Если мой враг злой или длиннее чем я, то тоже убегаем
            if (isMyEnemyEvil || isMyEnemyLongerThanI) {
                allElements.removeAll(commonPoints);
                allApples.removeAll(commonPoints);
            }
        }

        log.info("Apples are " + allApples);
        allElements.addAll(allApples);

        // Наши цели
        List<Node> nodes = mapToNode(allApples);

        //Неразмеченные точки для нахождения пути
        List<Node> unsettledNodes = mapToNode(allElements);

        //Размеченные точки
        List<Node> settledNodes = new ArrayList<>();
        settledNodes.add(new Node(myHead, null));

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

    // Получение длины вражеской змеи
    private static int getEnemyLength(GameBoard gameBoard, BoardPoint enemyHead) {
        BoardElement nextPoint = gameBoard.getElementAt(enemyHead);
        // Минимальная длина змейки
        int count = 2;
        // Боюсь даже представить какой здесь будет цикл
        BoardPoint boardPoint = enemyHead;
        Direction direction = STOP;
        switch (nextPoint) {
            case ENEMY_HEAD_UP:
                boardPoint = enemyHead.shiftBottom();
                direction = DOWN;
                break;
            case ENEMY_HEAD_DOWN:
                boardPoint = enemyHead.shiftTop();
                direction = UP;
                break;
            case ENEMY_HEAD_LEFT:
                boardPoint = enemyHead.shiftRight();
                direction = RIGHT;
                break;
            case ENEMY_HEAD_RIGHT:
                boardPoint = enemyHead.shiftLeft();
                direction= LEFT;
                break;
            default:
                break;
        }
        //Пока не доберемся до хвоста
        while (!gameBoard.hasElementAt(boardPoint,
                ENEMY_TAIL_END_DOWN, ENEMY_TAIL_END_UP, ENEMY_TAIL_END_RIGHT, ENEMY_TAIL_END_LEFT)) {
            BoardElement elementAt = gameBoard.getElementAt(boardPoint);
            if (direction == DOWN) {
                // Направление не меняется
                if (elementAt == ENEMY_BODY_VERTICAL) {
                    boardPoint = boardPoint.shiftBottom();
                }

                if (elementAt == ENEMY_BODY_LEFT_UP) {
                    boardPoint = boardPoint.shiftLeft();
                    direction = LEFT;
                }

                if (elementAt == ENEMY_BODY_RIGHT_UP) {
                    boardPoint = boardPoint.shiftRight();
                    direction = RIGHT;
                }
            }

            if (direction == UP) {
                // Направление не меняется
                if (elementAt == ENEMY_BODY_VERTICAL) {
                    boardPoint = boardPoint.shiftTop();
                }

                if (elementAt == ENEMY_BODY_LEFT_DOWN) {
                    boardPoint = boardPoint.shiftLeft();
                    direction = LEFT;
                }

                if (elementAt == ENEMY_BODY_RIGHT_DOWN) {
                    boardPoint = boardPoint.shiftRight();
                    direction = RIGHT;
                }
            }

            if (direction == LEFT) {
                // Направление не меняется
                if (elementAt == ENEMY_BODY_HORIZONTAL) {
                    boardPoint = boardPoint.shiftLeft();
                }

                if (elementAt == ENEMY_BODY_RIGHT_UP) {
                    boardPoint = boardPoint.shiftTop();
                    direction = UP;
                }

                if (elementAt == ENEMY_BODY_RIGHT_DOWN) {
                    boardPoint = boardPoint.shiftBottom();
                    direction = DOWN;
                }
            }

            if (direction == RIGHT) {
                // Направление не меняется
                if (elementAt == ENEMY_BODY_HORIZONTAL) {
                    boardPoint = boardPoint.shiftRight();
                }

                if (elementAt == ENEMY_BODY_LEFT_UP) {
                    boardPoint = boardPoint.shiftTop();
                    direction = UP;
                }

                if (elementAt == ENEMY_BODY_LEFT_DOWN) {
                    boardPoint = boardPoint.shiftBottom();
                    direction = DOWN;
                }
            }
            count++;
        }
        return count;
    }

    private static List<BoardPoint> getAllBoardPointEnemyHeadsAround(GameBoard gameBoard) {
        List<BoardPoint> enemyHeads = gameBoard.getEnemyHeads();
        return enemyHeads.stream()
                .flatMap(head -> getPointsAroundMe(gameBoard, head).stream())
                .collect(Collectors.toList());
    }

    public static Set<Node> getLowestDistanceNode(List<Node> unsettledNodes, List<Node> settledNodes,
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

    public static List<Node> mapToNode(Collection<BoardPoint> boardPoints) {
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
