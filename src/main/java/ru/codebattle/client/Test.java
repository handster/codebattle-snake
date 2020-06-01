package ru.codebattle.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import ru.codebattle.client.api.BoardPoint;
import ru.codebattle.client.api.GameBoard;
import ru.codebattle.client.api.Node;
import ru.codebattle.client.api.SnakeTarget;

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
                        "☼☼    ╘═╗                       ○ ☼\n" +
                        "☼☼      ║                         ☼\n" +
                        "☼#   ☼☼☼║  ☼☼☼    ☼#   ☼☼☼   ☼☼☼  ☼\n" +
                        "☼☼   ☼╔═╝    ☼         ☼       ☼  ☼\n" +
                        "☼☼   ☼║      ☼         ☼       ☼  ☼\n" +
                        "☼☼  ╔═╝                        ®  ☼\n" +
                        "☼☼  ╚═►     ┌──┐                  ☼\n" +
                        "☼☼  ┌───────┘  └────ö      ○      ☼\n" +
                        "☼#  │☼       ☼    ☼#   ☼       ☼  ☼\n" +
                        "☼☼  │☼       ☼        ○☼       ☼  ☼\n" +
                        "☼☼  │☼☼☼   ☼☼☼         ☼☼☼   ☼☼☼  ☼\n" +
                        "☼☼  │                      ®○     ☼\n" +
                        "☼☼  │☼☼☼   ☼☼☼         ☼☼☼   ☼☼☼  ☼\n" +
                        "☼☼  │☼       ☼        ®☼®   ®  ☼  ☼\n" +
                        "☼#  │☼ ●     ☼    ☼#   ☼ ● ● ● ☼  ☼\n" +
                        "☼☼  │                      ○○   ® ☼\n" +
                        "☼☼  │      ●             ●   ● ®  ☼\n" +
                        "☼☼  │             ®               ☼\n" +
                        "☼☼  │☼   ● ● ☼         ☼ ●   ● ☼  ☼\n" +
                        "☼☼┌─┘☼       ☼         ☼       ☼  ☼\n" +
                        "☼#♣  ☼☼☼   ☼☼☼    ☼#   ☼☼☼  ®☼☼☼  ☼\n" +
                        "☼☼                          $     ☼\n" +
                        "☼☼   ☼☼☼   ☼☼☼         ☼☼☼   ☼☼☼  ☼\n" +
                        "☼☼   ☼       ☼         ☼       ☼  ☼\n" +
                        "☼☼   ☼ ● ●   ☼  ○ ®    ☼   ● ● ☼  ☼\n" +
                        "☼☼                          ○     ☼\n" +
                        "☼#®               ☼#        ○●    ☼\n" +
                        "☼☼      ●              ○    ○     ☼\n" +
                        "☼☼   ☼       ☼         ☼ ● ● ● ☼  ☼\n" +
                        "☼☼   ☼    ®  ☼         ☼      ○☼ ®☼\n" +
                        "☼☼   ☼☼☼   ☼☼☼         ☼☼☼   ☼☼☼  ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼☼     ®         ®        ○       ☼\n" +
                        "☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼\n");
        GameBoard gameBoard2 = new GameBoard("☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼\n" +
                "☼☼   ®®○            ®             ☼\n" +
                "☼☼® ®                             ☼\n" +
                "☼#   ☼☼☼ ® ☼☼☼    ☼#   ☼☼☼   ☼☼☼   ☼\n" +
                "☼☼   ☼  ○    ☼         ☼○      ☼   ☼\n" +
                "☼☼   ☼ ● ● ● ☼        ○☼ ●     ☼  ☼\n" +
                "☼☼®     ○                  ○      ☼\n" +
                "☼☼     ●  ○●   ╘╗        ●   ●    ☼\n" +
                "☼☼    ┌───────┐ ♥ ○            ○  ☼\n" +
                "☼#   ☼│● ● ● ☼│   ☼#   ☼   ● ● ☼  ☼\n" +
                "☼☼   ☼└─┐    ☼└────ö   ☼       ☼  ☼\n" +
                "☼☼   ☼☼☼└┐ ☼☼☼       ○ ☼☼☼   ☼☼☼  ☼\n" +
                "☼☼  ┌────┘                ○       ☼\n" +
                "☼☼ ┌┘☼☼☼   ☼☼☼         ☼☼☼   ☼☼☼  ☼\n" +
                "☼☼ │ ☼ ♣     ☼  ○      ☼       ☼  ☼\n" +
                "☼# │ ☼ │ ● ● ☼ ○  ☼#   ☼       ☼  ☼\n" +
                "☼☼ └───┘®                         ☼\n" +
                "☼☼     ●   ●                      ☼\n" +
                "☼☼             ®   ®              ☼\n" +
                "☼☼   ☼ ● ● ● ☼         ☼       ☼  ☼\n" +
                "☼☼   ☼       ☼         ☼       ☼  ☼\n" +
                "☼#   ☼☼☼   ☼☼☼    ☼#   ☼☼☼   ☼☼☼  ☼\n" +
                "☼☼            ○                   ☼\n" +
                "☼☼   ☼☼☼   ☼☼☼         ☼☼☼   ☼☼☼  ☼\n" +
                "☼☼  ®☼   ○   ☼         ☼  ®    ☼  ☼\n" +
                "☼☼   ☼       ☼         ☼   ● ● ☼  ☼\n" +
                "☼☼                                ☼\n" +
                "☼#                ☼#      ®  ●    ☼\n" +
                "☼☼                                ☼\n" +
                "☼☼   ☼   ●   ☼         ☼       ☼  ☼\n" +
                "☼☼   ☼       ☼         ☼       ☼ ○☼\n" +
                "☼☼   ☼☼☼   ☼☼☼        ○☼☼☼   ☼☼☼  ☼\n" +
                "☼☼                      ®         ☼\n" +
                "☼☼                                ☼\n" +
                "☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼");

        GameBoard testGameBoard = gameBoard2;

        // Первоначальные цели
        List<BoardPoint> allApples = testGameBoard.findAllElements(APPLE, GOLD, FURY_PILL);

        // Первоначальные элементы через которые можно проложить путь
        List<BoardPoint> pathPoints = testGameBoard.findAllElements(NONE, APPLE, GOLD, FURY_PILL);
        BoardPoint myHead = testGameBoard.getMyHead();
        System.out.println("My head is " + myHead);

        Main.modifyApplesAndPathPoints(allApples, pathPoints, myHead, testGameBoard, true);
        List<BoardPoint> myTail = testGameBoard.getMyTail();
        Main.modifyApplesAndPathPoints(allApples, pathPoints, myHead, testGameBoard, true);
        List<BoardPoint> pathToAttack = Main.getPathToAttack(pathPoints, myHead, testGameBoard);
        System.out.println(pathToAttack);
    }
}
