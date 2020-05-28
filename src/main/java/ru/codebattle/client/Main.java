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

            // Пока собирать только яблоки
            List<BoardPoint> allApples = gameBoard.findAllElements(APPLE, GOLD, FURY_PILL);
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
            // Элементы через которые можно проложить путь
            List<BoardPoint> pathPoints = gameBoard.findAllElements(NONE);

            // Проверка ярости и длины противника
            modifyApplesAndPathPoints(allApples, myHead, gameBoard, headEvil, pathPoints);

            List<BoardPoint> nearestPathToApple = getNearestPathToApple(allApples, myHead, gameBoard, headEvil, pathPoints);

            log.info("Nearest path is " + nearestPathToApple);
            // и можно ли в них идти
            if (nearestPathToApple.isEmpty()) {
                System.out.println("Что-то пошло не так");
                //TODO Проверить соседние поля, если там камень то идти туда
                Set<BoardPoint> pointsAroundMe = getPointsAroundMe(gameBoard, myHead);
                List<BoardPoint> collect = pointsAroundMe.stream()
                        .filter(boardPoint -> gameBoard.hasElementAt(boardPoint, NONE,
                                BODY_HORIZONTAL, BODY_VERTICAL,
                                BODY_LEFT_DOWN, BODY_LEFT_UP, BODY_RIGHT_DOWN, BODY_RIGHT_UP, TAIL_END_DOWN,
                                TAIL_END_LEFT, TAIL_END_UP, TAIL_END_RIGHT))
                        .collect(Collectors.toList());
                if (collect.isEmpty()) {
                    System.out.println("Все равно что-то не так");
                    //Если опять некуда идти, то идем в камень либо на врага
                    collect = pointsAroundMe.stream()
                            .filter(boardPoint -> gameBoard.hasElementAt(boardPoint,
                                    ENEMY_TAIL_END_DOWN, ENEMY_TAIL_END_RIGHT, ENEMY_TAIL_END_UP, ENEMY_TAIL_END_LEFT,
                                    STONE, ENEMY_HEAD_DOWN, ENEMY_HEAD_LEFT, ENEMY_HEAD_RIGHT, ENEMY_HEAD_UP))
                            .collect(Collectors.toList());
                    if (collect.isEmpty()) {
                        return new SnakeAction(false, STOP);
                    }
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

    public static boolean getAct(GameBoard gameBoard) {
        List<BoardPoint> myTails = gameBoard.getMyTail();
        if (myTails.isEmpty()) {
            return false;
        }
        BoardPoint myTail = myTails.get(0);
        // Получить все точки на расстоянии 2 ходов от моего хвоста
        Set<BoardPoint> pointsAroundMyTail = getPointsAroundAnotherPoint(gameBoard, myTail, STONE_RADIUS);
        List<BoardPoint> enemyHeads = gameBoard.getEnemyHeads();
        if (CollectionUtils.containsAny(pointsAroundMyTail, enemyHeads)) {
            return true;
        }
        return false;
    }

    private static Set<BoardPoint> getPointsAroundAnotherPoint(GameBoard gameBoard, BoardPoint boardPoint, int radius) {
        int size = gameBoard.size();
        Set<BoardPoint> aroundMePoints = new HashSet<>();
        int x = boardPoint.getX();
        int y = boardPoint.getY();
        // Получить все точки вокруг себя
        BoardPoint point;
        for (int i = x - radius; i <= x + radius; i++) {
            for (int j = y - radius; j < y + radius; j++) {
                point = new BoardPoint(i, j);
                if (!point.isOutOfBoard(size)) {
                    aroundMePoints.add(point);
                }
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
        List<BoardPoint> myBodyAndTail = gameBoard.getMyBodyAndTail();
        if (headEvil) {
            return pointsAroundMe.stream()
                    .filter(boardPoint -> gameBoard.getElementAt(boardPoint) == WALL)
                    .count() == 3;
        }

        return pointsAroundMe.stream()
                .filter(boardPoint -> gameBoard.hasElementAt(boardPoint, WALL, STONE) || myBodyAndTail.contains(boardPoint))
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
                                                         GameBoard gameBoard, boolean headEvil,
                                                         List<BoardPoint> pathPoints) {

        List<BoardPoint> pathToOurApples = new ArrayList<>();

        //Отфильтровать яблоки в тупиках
        allApples = getApplesWithoutBlockApples(allApples, gameBoard, headEvil);

        // Добавляем в точки пути наши цели, иначе мы их не увидим
        pathPoints.addAll(allApples);

        // Наши цели
        List<Node> nodes = mapToNode(allApples);

        //Неразмеченные точки для нахождения пути
        List<Node> unsettledNodes = mapToNode(pathPoints);

        //Размеченные точки
        List<Node> settledNodes = new ArrayList<>();
        settledNodes.add(new Node(myHead, null));

        while (!unsettledNodes.isEmpty()) {
            //Получаем первую ближайшую точку
            Set<Node> currentNodes = getLowestDistanceNode(unsettledNodes, settledNodes, gameBoard);
            if (currentNodes.isEmpty()) {
                System.out.println("Попали в тупик");
                return Collections.emptyList();
            }
            // Из неразмеченных точек убираем вновь размеченные точки
            unsettledNodes.removeAll(currentNodes);
            // В размеченные добавляем
            settledNodes.addAll(currentNodes);
            if (CollectionUtils.containsAny(settledNodes, nodes)) {
                List<Node> pathsToWin = (List<Node>) CollectionUtils.retainAll(settledNodes, nodes);
//                log.info("path size is " + pathsToWin.size());

                // Получаем более приоритетную цель
                Node path = getPriorityNode(gameBoard, pathsToWin);

                while (path.getParent() != null) {
                    pathToOurApples.add(path.getBoardPoint());
                    path = path.getParent();
                }
                Collections.reverse(pathToOurApples);
                return pathToOurApples;
            }
        }

        return pathToOurApples;
    }

    private static Node getPriorityNode(GameBoard gameBoard, List<Node> pathsToWin) {
        // Если до нескольких объектов одинаковое расстояние, то выбирать таблетку
        // Если нет таблетки, то может хотя бы золото, ну а если уж ничего нет
        // будем есть яблоки
        if ((pathsToWin.size() <= 1)) {
            return pathsToWin.get(0);
        }

        List<BoardPoint> enemyBodyAndTail = gameBoard.getEnemyBodyAndTail();
        //Головы и тела врагов
        List<Node> collect = pathsToWin.stream()
                .filter(node -> enemyBodyAndTail.contains(node.getBoardPoint()))
                .collect(Collectors.toList());

        if (collect.size() == 1) {
            return collect.get(0);
        } else {
            // Определить длину змеи по ее куску
            // для каждой цели найти длину змеи
//            List<BoardPoint> enemyHeads = gameBoard.getEnemyHeads();
//            for (Node node : collect) {
//                //Если голова, то идем к ней
            // Возможно лучше не рисковать и на голову не идти
//                if (enemyHeads.contains(node.getBoardPoint())) {
//                    return node;
//                } else {
//                    getTargetSnake(gameBoard, node.getBoardPoint());
//                }
//            }
        }

        //Таблетка
        collect = pathsToWin.stream()
                .filter(node -> gameBoard.getElementAt(node.getBoardPoint()) == FURY_PILL)
                .collect(Collectors.toList());
        if (!collect.isEmpty()) {
            return collect.get(0);
        }

        //Золото
        collect = pathsToWin.stream()
                .filter(node -> gameBoard.getElementAt(node.getBoardPoint()) == GOLD)
                .collect(Collectors.toList());
        if (!collect.isEmpty()) {
            return collect.get(0);
        }

        return pathsToWin.get(0);
    }

    private static SnakeTarget getTargetSnake(GameBoard gameBoard, BoardPoint boardPoint) {
        BoardElement elementAt = gameBoard.getElementAt(boardPoint);
        SnakeTarget snakeTarget = new SnakeTarget(boardPoint);
        List<BoardPoint> enemyHeads = gameBoard.getEnemyHeads();
        List<BoardPoint> enemyTails = gameBoard.getEnemyTails();
        //Пока не найдем голову и хвост
        BoardPoint leftDirection;
        BoardPoint rightDirection;
//        while (true) {
//            if (elementAt == )
//                break;
//        }
        return null;
    }

    private static void modifyApplesAndPathPoints(List<BoardPoint> allApples, BoardPoint myHead, GameBoard gameBoard, boolean headEvil, List<BoardPoint> pathPoints) {
        // Враги
        List<BoardPoint> enemyBodyAndTail = gameBoard.getEnemyBodyAndTail();

        // Камни
        List<BoardPoint> stones = gameBoard.findAllElements(STONE);

        // Получаем длину тела
        int myBodySize = gameBoard.getMyBodyAndTail().size();
        log.info("My length is " + myBodySize);

        // Получаем точки, на которые могут следующим ходом попасть головы врагов
        List<BoardPoint> allBoardPointEnemyHeadsAround = getAllBoardPointEnemyHeadsAround(gameBoard);
        Set<BoardPoint> pointsAroundMe = getPointsAroundMe(gameBoard, myHead);

        // Если на клетку куда двигается моя голова могут попасть головы врагов,
        // то убираем такие точки из целей и точек передвижения
        List<BoardPoint> commonPoints = (List<BoardPoint>) CollectionUtils.retainAll(pointsAroundMe, allBoardPointEnemyHeadsAround);

        BoardPoint enemyHead = getEnemyHead(commonPoints, pointsAroundMe, gameBoard);
        boolean isMyEnemyEvil = isMyEnemyEvil(enemyHead, gameBoard);
        boolean isMyEnemyLongerThanI = isMyEnemyLongerThanI(isMyEnemyEvil, enemyHead, gameBoard);

        // Если под таблеткой ярости, до добавить в цели врагов и камни
        if (headEvil) {
            allApples.addAll(enemyBodyAndTail);
            allApples.addAll(stones);
            log.info("В АТАКУ ...");

            // TODO заменить на метод определения длины змеи по ее куску
            int totalEnemyEvilSnakesLength = getTotalEnemyEvilSnakesLength(gameBoard);
            if (isMyEnemyEvil && totalEnemyEvilSnakesLength + 2 > myBodySize) {
                pathPoints.removeAll(commonPoints);
                allApples.removeAll(commonPoints);
            } else {

            }
        } else {
            // Неактуально стало есть камни без ярости
            // Если длина меньше, то не едим камни
//            if (myBodySize < LIMIT_SIZE) {
//                barriers.addAll(stones);
//            }
            //Если мой враг злой или длиннее чем я, то тоже убегаем
            if (isMyEnemyEvil || isMyEnemyLongerThanI) {
                pathPoints.removeAll(commonPoints);
                allApples.removeAll(commonPoints);
            }
        }
    }

    public static int getTotalEnemyEvilSnakesLength(GameBoard gameBoard) {
        // Берем сумму всех тел змей и вычитаем сумму тех, кто не злой
        List<BoardPoint> enemyHeads = gameBoard.getEnemyHeads();
        int totalLength = gameBoard.getEnemyBodyAndTail().size() + enemyHeads.size();
        Integer reduce = enemyHeads.stream()
                .filter(head -> gameBoard.getElementAt(head) != ENEMY_HEAD_EVIL)
                .map(head -> getEnemyLength(gameBoard, head))
                .reduce(0, Integer::sum);
        return totalLength - reduce;
    }

    private static BoardPoint getEnemyHead(List<BoardPoint> commonPoints, Set<BoardPoint> pointsAroundMe, GameBoard gameBoard) {
        if (!commonPoints.isEmpty()) {
            // Находим голову вражеской змеи, с которой можем столкнуться
            Optional<BoardPoint> any = gameBoard.getEnemyHeads().stream()
                    .filter(head -> CollectionUtils.containsAny(getPointsAroundMe(gameBoard, head), pointsAroundMe))
                    .findAny();
            if (any.isPresent()) {
                return any.get();
            }
        }
        return null;
    }

    private static boolean isMyEnemyLongerThanI(boolean isMyEnemyEvil, BoardPoint enemyHead, GameBoard gameBoard) {
        if (enemyHead == null) {
            return false;
        }

        if (!isMyEnemyEvil) {
            //Теперь нужно посчитать длину врага
            int enemyLength = getEnemyLength(gameBoard, enemyHead);
//            log.info("Enemy length is " + enemyLength);

            // Если враг длиннее или под таблеткой, то убегаем от него
            int myBodySize = gameBoard.getMyBodyAndTail().size();
            if (enemyLength + 2 > myBodySize) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMyEnemyEvil(BoardPoint enemyHead, GameBoard gameBoard) {
        if (enemyHead == null) {
            return false;
        }
        // Если враг под таблеткой, то длину не посчитать
        if (gameBoard.hasElementAt(enemyHead, ENEMY_HEAD_EVIL)) {
            return true;
        }
        return false;
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
                direction = LEFT;
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

    // Получаем новые размеченные точки
    public static Set<Node> getLowestDistanceNode(List<Node> unsettledNodes, List<Node> settledNodes,
                                                  GameBoard gameBoard) {
        // Для всех точек из settledNodes получить точки вокруг
        Set<Node> set = new HashSet<>();
        for (Node node : settledNodes) {
            List<Node> nodes = mapToNode(getPointsAroundMe(gameBoard, node.getBoardPoint()));
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
