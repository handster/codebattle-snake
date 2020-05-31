package ru.codebattle.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Logger;
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
    public static int limitEvilCount = 0;
    public static final int STONE_RADIUS = 2;
    public static int evilCount = 0;
    public static List<BoardPoint> furyPills = new ArrayList<>();

    public static void main(String[] args) throws URISyntaxException, IOException {
        SnakeBattleClient client = new SnakeBattleClient(SERVER_ADDRESS);
        client.run(gameBoard -> {
            // Найти свою голову
            BoardPoint myHead = gameBoard.getMyHead();
            log.info("My head is " + myHead);
            if (myHead == null || gameBoard.getElementAt(myHead) == HEAD_SLEEP) {
                limitEvilCount = 0;
                evilCount = 0;
                return new SnakeAction(false, STOP);
            }
            // Первоначальные цели
            List<BoardPoint> allApples = gameBoard.findAllElements(APPLE, GOLD, FURY_PILL);

            // Первоначальные элементы через которые можно проложить путь
            List<BoardPoint> pathPoints = gameBoard.findAllElements(NONE, APPLE, GOLD, FURY_PILL);

            // Проверям под таблеткой я или нет
            boolean headEvil = isMyHeadEvil(gameBoard, myHead);

            // Изменяем наши цели и точки пути в зависимости от наличия ярости, и длины врагов
            modifyApplesAndPathPoints(allApples, pathPoints, myHead, gameBoard, headEvil);

            //Отфильтровать яблоки в тупиках
            allApples = getApplesWithoutBlockApples(allApples, gameBoard, headEvil);

            List<BoardPoint> pathFromAppleToTail = new ArrayList<>();

            List<BoardPoint> pathFromHeadToApple = new ArrayList<>();
            BoardPoint myAim;
            List<BoardPoint> myTailList = gameBoard.getMyTail();
            BoardPoint myTail;
            if (myTailList.isEmpty()) {
                myTail = null;
            } else {
                myTail = myTailList.get(0);
            }

            pathFromHeadToApple = getPathToAim(gameBoard, myHead, allApples, pathPoints, pathFromAppleToTail, pathFromHeadToApple, myTail);


            log.info("Nearest path is " + pathFromHeadToApple);

            //TODO переработать этот блок
            if (pathFromHeadToApple.isEmpty()) {
                log.warn("Что-то пошло не так");
                log.warn("GameBoard is " + gameBoard.getBoardString());
                Set<BoardPoint> pointsAroundMe = getPointsAroundMe(gameBoard, myHead);
                List<BoardPoint> collect = pointsAroundMe.stream()
                        .filter(boardPoint -> gameBoard.hasElementAt(boardPoint, NONE,
                                BODY_HORIZONTAL, BODY_VERTICAL,
                                BODY_LEFT_DOWN, BODY_LEFT_UP, BODY_RIGHT_DOWN, BODY_RIGHT_UP, TAIL_END_DOWN,
                                TAIL_END_LEFT, TAIL_END_UP, TAIL_END_RIGHT))
                        .collect(Collectors.toList());
                if (collect.isEmpty()) {
                    log.warn("Все равно что-то не так");
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
            Direction direction = goToWin(myHead, pathFromHeadToApple.get(0));

            boolean act = getAct(gameBoard);
            return new SnakeAction(act, direction);
        });

        System.in.read();

        client.initiateExit();
    }

    public static List<BoardPoint> getPathToAim(GameBoard gameBoard, BoardPoint myHead, List<BoardPoint> allApples, List<BoardPoint> pathPoints, List<BoardPoint> pathFromAppleToTail, List<BoardPoint> pathFromHeadToApple, BoardPoint myTail) {
        BoardPoint myAim;
        while (pathFromAppleToTail.isEmpty()) {
            // Получаем путь до яблока
            pathFromHeadToApple = getNearestPathToApple(allApples, myHead, gameBoard, pathPoints);
            log.info("Первый путь до цели " + pathFromHeadToApple);
            if (pathFromHeadToApple.isEmpty() || isMyTailInWall(gameBoard, myTail)) {
                break;
            }
            myAim = pathFromHeadToApple.get(pathFromHeadToApple.size() - 1);
            // Точки пути, которые необходимо убрать из общих
            pathFromHeadToApple.remove(myAim);
            // Убираем, чтобы не идти по этому же пути
            pathPoints.removeAll(pathFromHeadToApple);

            pathFromAppleToTail = getNearestPathFromAppleToTail(myTail, myAim, gameBoard, pathPoints);
            if (pathFromAppleToTail.isEmpty()) {
                pathPoints.remove(myAim);
                allApples.remove(myAim);
                log.info("Gameboard когда попали в тупик " + gameBoard.getBoardString());
            } else {
                pathFromHeadToApple.add(myAim);
            }
            pathPoints.addAll(pathFromHeadToApple);
        }
        return pathFromHeadToApple;
    }

    private static boolean isMyTailInWall(GameBoard gameBoard, BoardPoint myTail) {
        if (myTail == null) {
            return true;
        }
        Set<BoardPoint> pointsAroundMe = getPointsAroundMe(gameBoard, myTail);
        List<BoardPoint> enemyBodyAndTail = gameBoard.getEnemyBodyAndTail();
        return pointsAroundMe.size() == 3 ||
                pointsAroundMe.stream()
                        .filter(boardPoint -> (gameBoard.getElementAt(boardPoint) == WALL ||
                                enemyBodyAndTail.contains(boardPoint)))
                        .count() == 3;
    }

    private static List<BoardPoint> getNearestPathFromAppleToTail(BoardPoint myTail, BoardPoint myAim,
                                                                  GameBoard gameBoard, List<BoardPoint> pathPoints) {
        List<BoardPoint> oneAimList = new ArrayList<>();
        oneAimList.add(myTail);
        return getNearestPathToApple(oneAimList, myAim, gameBoard, pathPoints);
    }

    public static boolean isMyHeadEvil(GameBoard gameBoard, BoardPoint myHead) {
        // Если съели таблетку, то увеличиваем лимит ходов под яростью
        if (furyPills.contains(myHead)) {
            limitEvilCount += 10;
            log.info("Жадность до таблеток " + limitEvilCount);
        }
        furyPills = gameBoard.getFuryPills();

        if (gameBoard.getElementAt(myHead) == HEAD_EVIL) {
            evilCount++;
        } else {
            limitEvilCount = 0;
            evilCount = 0;
        }

        log.info("Evil count " + (evilCount) + " <= " + limitEvilCount);
        return evilCount + 1 <= limitEvilCount;
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
        enemyHeads.removeAll(gameBoard.findAllElements(ENEMY_HEAD_EVIL));
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

    public static List<BoardPoint> getApplesWithoutBlockApples(List<BoardPoint> apples, GameBoard gameBoard, boolean headEvil) {
        return apples.stream()
                .filter(apple -> !getThreeWallsAround(gameBoard, apple, headEvil))
                .collect(Collectors.toList());
    }

    private static boolean getThreeWallsAround(GameBoard gameBoard, BoardPoint apple, boolean headEvil) {
        Set<BoardPoint> pointsAroundMe = getPointsAroundMe(gameBoard, apple);
        List<BoardPoint> myBodyAndTail = gameBoard.getMyBodyAndTail();
        List<BoardPoint> enemyBodyAndTail = gameBoard.getEnemyBodyAndTail();
        // Если под таблеткой, то убираем только цели в трех стенах
        if (headEvil) {
            return pointsAroundMe.stream()
                    .filter(boardPoint -> gameBoard.getElementAt(boardPoint) == WALL
                            || myBodyAndTail.contains(boardPoint))
                    .count() == 3;
        }

        // Добавляем к стенам камни, свое и вражеские тела
        return pointsAroundMe.stream()
                .filter(boardPoint -> gameBoard.hasElementAt(boardPoint, WALL, STONE)
                        || myBodyAndTail.contains(boardPoint)
                        || enemyBodyAndTail.contains(boardPoint))
                .count() == 3;
    }

    public static Set<BoardPoint> getPointsAroundMe(GameBoard gameBoard, BoardPoint boardPoint) {
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
                                                         GameBoard gameBoard, List<BoardPoint> pathPoints) {
        List<BoardPoint> pathToOurApples = new ArrayList<>();

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
        if ((pathsToWin.size() == 1)) {
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

    public static void modifyApplesAndPathPoints(List<BoardPoint> allApples, List<BoardPoint> pathPoints, BoardPoint myHead, GameBoard gameBoard, boolean headEvil) {
        // Враги
        List<BoardPoint> enemyBodyAndTail = gameBoard.getEnemyBodyAndTail();

        // Камни
        List<BoardPoint> stones = gameBoard.findAllElements(STONE);

        //TODO написать обработку общих точек
//        // Получаем точки, на которые могут следующим ходом попасть головы врагов
//        List<BoardPoint> allBoardPointEnemyHeadsAround = getAllBoardPointEnemyHeadsAround(gameBoard);
//
//        // Получаем точки вокруг себя
//        Set<BoardPoint> pointsAroundMe = getPointsAroundMe(gameBoard, myHead);
//        pointsAroundMe.removeAll(gameBoard.getEnemyBodyAndTail());
//
//        // Получаем точку где можем стукнуться головами
//        List<BoardPoint> commonPoints = (List<BoardPoint>) CollectionUtils.retainAll(pointsAroundMe, allBoardPointEnemyHeadsAround);
//
//
//        boolean isMyEnemyEvil = gameBoard.getElementAt(enemyHead) == ENEMY_HEAD_EVIL;
//        boolean isMyEnemyLongerThanI = isMyEnemyLongerThanI(isMyEnemyEvil, enemyHead, gameBoard);
////         Если под таблеткой ярости, до добавить в цели врагов и камни
        if (headEvil) {
            allApples.addAll(enemyBodyAndTail);
            allApples.addAll(stones);
            log.info("В АТАКУ ...");

//            SnakeTarget enemySnake = getEnemySnakeByHead(gameBoard, enemyHead);
//
//            int myBodyLength = gameBoard.getMyBodyAndTail().size();
//
//            // Если враг тоже злой и длинней чем я, то убираем из целей и точек пути общие точки
//            if (isMyEnemyEvil && enemySnake != null && enemySnake.getLength() + 2 > myBodyLength) {
//                pathPoints.removeAll(commonPoints);
//                allApples.removeAll(commonPoints);
//            }
        } else {
//            //Если мой враг злой или длиннее чем я, то тоже убегаем
//            if (isMyEnemyEvil || isMyEnemyLongerThanI) {
//                pathPoints.removeAll(commonPoints);
//                allApples.removeAll(commonPoints);
//            }
        }
    }

    private static SnakeTarget getEnemySnakeByHead(GameBoard gameBoard, BoardPoint enemyHead) {
        List<SnakeTarget> allTargetSnakes = getAllTargetSnakes(gameBoard);
        Optional<SnakeTarget> any = allTargetSnakes.stream()
                .filter(snake -> snake.getEnemyHead().equals(enemyHead))
                .findAny();
        return any.orElse(null);
    }

    private static List<SnakeTarget> getAllTargetSnakes(GameBoard gameBoard) {
        return gameBoard.getEnemyTails().stream()
                .map(tail -> getSnakeTarget(gameBoard, tail))
                .collect(Collectors.toList());
    }

    private static BoardPoint getNearestEnemyHead(List<BoardPoint> commonPoints, Set<BoardPoint> pointsAroundMe, GameBoard gameBoard) {
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
            int enemyLength = getSnakeTarget(gameBoard, enemyHead).getSnakeBody().size();
//            log.info("Enemy length is " + enemyLength);

            // Если враг длиннее или под таблеткой, то убегаем от него
            int myBodySize = gameBoard.getMyBodyAndTail().size();
            if (enemyLength + 2 > myBodySize) {
                return true;
            }
        }
        return false;
    }

    // Получение длины вражеской змеи
    public static SnakeTarget getSnakeTarget(GameBoard gameBoard, BoardPoint enemyTail) {
        List<BoardPoint> snakeBody = new ArrayList<>();
        // Добавляем в тело змеи ее голову
        snakeBody.add(enemyTail);
        // Минимальная длина змейки

        BoardPoint boardPoint = enemyTail;
        Direction direction = STOP;
        switch (gameBoard.getElementAt(enemyTail)) {
            case ENEMY_TAIL_END_UP:
                boardPoint = enemyTail.shiftBottom();
                direction = DOWN;
                break;
            case ENEMY_TAIL_END_DOWN:
                boardPoint = enemyTail.shiftTop();
                direction = UP;
                break;
            case ENEMY_TAIL_END_LEFT:
                boardPoint = enemyTail.shiftRight();
                direction = RIGHT;
                break;
            case ENEMY_TAIL_END_RIGHT:
                boardPoint = enemyTail.shiftLeft();
                direction = LEFT;
                break;
            default:
                break;
        }
        snakeBody.add(boardPoint);

        List<BoardPoint> enemyHeads = gameBoard.getEnemyHeads();
        //Пока не доберемся до головы
        BoardElement elementAt;
        while (!enemyHeads.contains(boardPoint)) {
            elementAt = gameBoard.getElementAt(boardPoint);
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
            snakeBody.add(boardPoint);
        }
        return new SnakeTarget(snakeBody);
    }

    private static List<BoardPoint> getAllBoardPointEnemyHeadsAround(GameBoard gameBoard) {
        List<BoardPoint> enemyHeads = gameBoard.getEnemyHeads();
        List<BoardPoint> collect = enemyHeads.stream()
                .flatMap(head -> getPointsAroundMe(gameBoard, head).stream())
                .collect(Collectors.toList());
        //Удаляем из этих точек тела змей
        collect.removeAll(gameBoard.getEnemyBodyAndTail());
        return collect;
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
