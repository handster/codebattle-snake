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
                        "☼☼            ☼           ☼     ® ☼\n" +
                        "☼☼   ☼   ☼      ☼     ☼ ☼    ☼    ☼\n" +
                        "☼☼   ☼      ☼      ☼              ☼\n" +
                        "☼#    ☼  ☼    ☼  ☼   ☼    ☼   ☼   ☼\n" +
                        "☼☼        ☼    ☼    ☼    ☼   ☼    ☼\n" +
                        "☼☼   ☼ ☼        ☼  ☼    ☼         ☼\n" +
                        "☼☼      ☼   ☼      ®®  ☼   ☼      ☼\n" +
                        "☼☼   ☼   ☼        ☼       ☼   ☼   ☼\n" +
                        "☼#   ☼☼       ☼®     ☼       ☼    ☼\n" +
                        "☼☼   ☼®    ☼   ☼    ☼   ☼   ☼     ☼\n" +
                        "☼☼      ☼   ☼   ☼      ☼   ☼®     ☼\n" +
                        "☼☼   ☼   ×♣      ☼ ○  ☼           ☼\n" +
                        "☼☼        ☼   ☼        ○ ☼    ☼   ☼\n" +
                        "☼#   ☼☼    ☼   ☼ ○ ☼    ☼    ☼    ☼\n" +
                        "☼☼     ☼    ☼       ☼ ○     ☼     ☼\n" +
                        "☼☼           ☼ ○ ☼   ☼   ○☼       ☼\n" +
                        "☼☼       ☼        ☼   ☼    ☼○     ☼\n" +
                        "☼☼     ☼    ○  ☼      ○     ☼     ☼\n" +
                        "☼#    ☼    ☼    ☼   ☼ ® ☼       ® ☼\n" +
                        "☼☼   ☼    ☼   ○  ☼   ☼ ○          ☼\n" +
                        "☼☼           ☼    ☼       ☼       ☼\n" +
                        "☼☼      ☼   ☼          ☼   ☼      ☼\n" +
                        "☼☼     ☼  ○☼   ☼  ○®☼   ☼         ☼\n" +
                        "☼#    ☼    ☺  ☼          ☼      ® ☼\n" +
                        "☼☼   ☼   ☼ ♥     ☼    ☼   ☼       ☼\n" +
                        "☼☼      ☼   ☼     ☼    ☼   ☼      ☼\n" +
                        "☼☼         ☼    ☼○ ☼         ☼    ☼\n" +
                        "☼☼    ☼®  ☼    ☼               ☼  ☼\n" +
                        "☼#   ☼   ☼    ☼      ® ®☼   ☼     ☼\n" +
                        "☼☼        ●     ☼   ☼     ☼    $  ☼\n" +
                        "☼☼  ☼   ☼  ☼    ☼     ☼     $   ○ ☼\n" +
                        "☼☼    ☼   ☼☼  ☼           ☼  ☼  ☼ ☼\n" +
                        "☼☼                                ☼\n" +
                        "☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼");
//        final GameBoard testGameBoard = gameBoard3;

        // Первоначальные цели
        List<BoardPoint> allApples = gameBoard.findAllElements(APPLE, GOLD, FURY_PILL);

        // Первоначальные элементы через которые можно проложить путь
        List<BoardPoint> pathPoints = gameBoard.findAllElements(NONE, APPLE, GOLD, FURY_PILL);
        List<BoardPoint> pathToAim = Main.getPathToAim(gameBoard, gameBoard.getMyHead(), allApples, pathPoints,
                new ArrayList<>(), new ArrayList<>(), null);
        System.out.println(pathToAim);
    }
}
