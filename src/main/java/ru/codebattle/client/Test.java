package ru.codebattle.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import ru.codebattle.client.api.BoardElement;
import ru.codebattle.client.api.BoardPoint;
import ru.codebattle.client.api.GameBoard;
import ru.codebattle.client.api.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static ru.codebattle.client.api.BoardElement.*;

@Slf4j
public class Test {
    public static final int LIMIT_SIZE = 10;
    public static void main(String[] args) {
        GameBoard gameBoard = new GameBoard(
                "☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼\n" +
                        "☼☼ ○                              ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼☼                               ○☼\n" +
                        "☼☼      ○                         ☼\n" +
                        "☼#     ○                   ○      ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼☼                       ○        ☼\n" +
                        "☼☼                               ®☼\n" +
                        "☼☼               ®                ☼\n" +
                        "☼☼             ○                  ☼\n" +
                        "☼#                                ☼\n" +
                        "☼☼              ○                 ☼\n" +
                        "☼☼         ●                      ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼#                       <───────┐☼\n" +
                        "☼☼                    ○ ▲       ×┘☼\n" +
                        "☼☼                      ║         ☼\n" +
                        "☼☼                      ║         ☼\n" +
                        "☼☼                     ●║         ☼\n" +
                        "☼☼                      ║         ☼\n" +
                        "☼#                     ╔╝         ☼\n" +
                        "☼☼○             ╘══════╝          ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼☼       ○                        ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼#                                ☼\n" +
                        "☼☼  ○                             ☼\n" +
                        "☼☼                    ○           ☼\n" +
                        "☼☼   $                            ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼");
        System.out.println(gameBoard);
        BoardPoint myHead = gameBoard.getMyHead();
        log.info("My head is " + myHead);
        List<BoardPoint> apples = gameBoard.findAllElements(APPLE, GOLD, FURY_PILL);
        List<BoardPoint> nearestPathToApple = Main.getNearestPathToApple(apples, myHead, gameBoard, false);
        log.info("Nearest path is " + nearestPathToApple);
        boolean act = Main.getAct(gameBoard);
        System.out.println(act);
    }

    public static List<BoardPoint> getNearestPathToApple(List<BoardPoint> allApples, BoardPoint myHead, GameBoard gameBoard) {

        List<BoardPoint> pathToOurApples = new ArrayList<>();
        List<Node> nodes = Main.mapToNode(allApples);

        int myBodySize = gameBoard.getMyBodyAndTail().size();
        log.info("My length is " + myBodySize);
        //Если тело меньше либо равно 4 то обходим камни
        List<BoardPoint> allElements = gameBoard.findAllElements(NONE, APPLE, GOLD, FURY_PILL);
        List<BoardPoint> barriers = gameBoard.getBarriers();
        barriers.addAll(gameBoard.getMyBodyAndTail());
        barriers.addAll(gameBoard.getEnemyBodyAndTail());
        List<BoardPoint> stones = gameBoard.findAllElements(STONE);
        if (myBodySize > LIMIT_SIZE) {
            // Если тело больше 4 то едим камни тоже
            allElements = gameBoard.findAllElements(NONE, APPLE, GOLD, FURY_PILL, STONE);
            barriers.removeAll(stones);
        }

        // Если под таблеткой ярости
        if (gameBoard.hasElementAt(myHead, HEAD_EVIL)) {
            List<BoardPoint> enemyBodyAndTail = gameBoard.getEnemyBodyAndTail();
            allElements.addAll(enemyBodyAndTail);
            allElements.addAll(stones);
            barriers = gameBoard.getBarriers();
            barriers.removeAll(stones);
            log.info("В АТАКУ ...");
        }

        //Неразмеченные точки
        List<Node> unsettledNodes = Main.mapToNode(allElements);

        //Размеченные точки
        List<Node> settledNodes = new ArrayList<>();
        settledNodes.add(new Node(myHead, null));

        while (!unsettledNodes.isEmpty()) {
            //Получаем первую ближайшую точку
            Set<Node> currentNodes = Main.getLowestDistanceNode(unsettledNodes, settledNodes, gameBoard, barriers);
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
}
