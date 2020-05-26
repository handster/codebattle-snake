package ru.codebattle.client;

import lombok.extern.slf4j.Slf4j;
import ru.codebattle.client.api.BoardElement;
import ru.codebattle.client.api.BoardPoint;
import ru.codebattle.client.api.GameBoard;

import java.util.List;

import static ru.codebattle.client.api.BoardElement.*;

@Slf4j
public class Test {
    public static void main(String[] args) {
        GameBoard gameBoard = new GameBoard(
                "☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼\n" +
                        "☼☼        ☼○☼              ☼$☼\n" +
                        "*ø       ●●      ☼☼☼☼☼       ☼\n" +
                        "☼☼  ○   *ø          ●●       ☼\n" +
                        "☼☼  ●                   ○    ☼\n" +
                        "☼☼ ○         ●              ☼☼\n" +
                        "☼#          ●     ☼#         ☼\n" +
                        "☼☼      ●●☼☼   ☼   ☼  ☼●    ☼☼\n" +
                        "☼☼ ● ●  ☼     ☼○☼  ●  ●○ ●   ☼\n" +
                        "☼☼ ● ●  ☼○         ☼  ☼●     ☼\n" +
                        "☼☼      ●●☼               ●  ☼\n" +
                        "☼☼          ○   ☼#    ®   ☼ ☼☼\n" +
                        "☼☼○            ☼            ○☼\n" +
                        "☼☼    ●      ●☼○☼●   ●●●☼ ☼ ☼☼\n" +
                        "☼#          ●●○$○●●          ☼\n" +
                        "☼☼   ●       ●☼○☼●   ●●●  ○  ☼\n" +
                        "☼☼   ○         ☼             ☼\n" +
                        "☼☼   ●   ●☼ ☼                ☼\n" +
                        "☼☼☼         ●     ● ®   ○    ☼\n" +
                        "☼☼○  ○   ☼☼●●               ☼☼\n" +
                        "☼☼☼  ○      ☼               ○☼\n" +
                        "☼☼○☼●     ●*ø               ☼☼\n" +
                        "☼☼  ●        ○               ☼\n" +
                        "☼☼                  ☼●☼    ● ☼\n" +
                        "☼☼   ●                 ● ╘►●○☼\n" +
                        "☼☼   ●  ○        ☼●*ø      ● ☼\n" +
                        "☼#         ●               ○ ☼\n" +
                        "☼☼  ®            ○           ☼\n" +
                        "☼☼○☼       ●●      ☼○☼     ☼$☼\n" +
                        "☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼☼");
        System.out.println(gameBoard);
        BoardPoint myHead = gameBoard.getMyHead();
        log.info("My head is " + myHead);
        List<BoardPoint> apples = gameBoard.findAllElements(APPLE, GOLD, FURY_PILL);
        log.info("Apples " + apples);
        List<BoardPoint> nearestPathToApple = Main.getNearestPathToApple(apples, myHead, gameBoard);
        log.info("Nearest path is " + nearestPathToApple);


    }
}
