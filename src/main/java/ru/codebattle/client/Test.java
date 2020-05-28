package ru.codebattle.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
                        "☼☼            ☼       ○   ☼      ●☼\n" +
                        "☼☼   ☼   ☼      ☼     ☼ ☼    ☼    ☼\n" +
                        "☼☼   ☼    $ ☼      ☼              ☼\n" +
                        "☼#    ☼  ☼    ☼  ☼   ☼    ☼   ☼   ☼\n" +
                        "☼☼        ☼    ☼    ☼    ☼ ® ☼    ☼\n" +
                        "☼☼   ☼ ☼        ☼  ☼    ☼         ☼\n" +
                        "☼☼      ☼   ☼          ☼   ☼      ☼\n" +
                        "☼☼   ☼   ☼        ☼       ☼   ☼   ☼\n" +
                        "☼#   ☼☼       ☼   ○  ☼       ☼    ☼\n" +
                        "☼☼   ☼  ○® ☼   ☼    ☼   ☼   ☼     ☼\n" +
                        "☼☼      ☼   ☼   ☼      ☼   ☼      ☼\n" +
                        "☼☼   ☼           ☼    ☼        ○  ☼\n" +
                        "☼☼        ☼   ☼          ☼    ☼   ☼\n" +
                        "☼#   ☼☼ ○  ☼   ☼   ☼    ☼    ☼    ☼\n" +
                        "☼☼     ☼    ☼       ☼       ☼     ☼\n" +
                        "☼☼           ☼   ☼   ☼    ☼       ☼\n" +
                        "☼☼       ☼      ◄╗☼   ☼    ☼      ☼\n" +
                        "☼☼     ☼       ☼○╚╗╔═══╗    ☼    ○☼\n" +
                        "☼#    ☼    ☼    ☼ ╚╝☼  ║☼  ○      ☼\n" +
                        "☼☼   ☼    ☼      ☼   ☼ ║          ☼\n" +
                        "☼☼   ○       ☼    ☼   ♣╙  ☼       ☼\n" +
                        "☼☼      ☼   ☼       ×─┘☼○  ☼      ☼\n" +
                        "☼☼     ☼   ☼   ☼    ☼   ☼         ☼\n" +
                        "☼#    ☼       ☼    ○     ☼        ☼\n" +
                        "☼☼   ☼   ☼       ☼    ☼   ☼       ☼\n" +
                        "☼☼      ☼   ☼     ☼    ☼   ☼      ☼\n" +
                        "☼☼         ☼    ☼  ☼         ☼    ☼\n" +
                        "☼☼    ☼   ☼    ☼               ☼  ☼\n" +
                        "☼#   ☼   ☼    ☼     ○   ☼   ☼     ☼\n" +
                        "☼☼              ☼   ☼     ☼       ☼\n" +
                        "☼☼  ☼   ☼  ☼    ☼     ☼           ☼\n" +
                        "☼☼    ☼●  ☼☼  ☼           ☼  ☼  ☼ ☼\n" +
                        "☼☼       ○                        ☼\n" +
                        "☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼");
        System.out.println(gameBoard);
        BoardPoint myHead = gameBoard.getMyHead();
        log.info("My head is " + myHead);
        List<BoardPoint> apples = gameBoard.findAllElements(APPLE, GOLD, FURY_PILL);
        List<BoardPoint> pathPoints = gameBoard.findAllElements(NONE);
        List<BoardPoint> nearestPathToApple = Main.getNearestPathToApple(apples, myHead, gameBoard, false, pathPoints);
        log.info("Nearest path is " + nearestPathToApple);
        boolean act = Main.getAct(gameBoard);
        int totalEnemyEvilSnakesLength = Main.getTotalEnemyEvilSnakesLength(gameBoard);
        System.out.println("total " + totalEnemyEvilSnakesLength);
        System.out.println(act);
    }
}
