package com.dangerwind.crusadefive.service;

import com.dangerwind.crusadefive.ai.AI;
import com.dangerwind.crusadefive.dto.Cell;
import com.dangerwind.crusadefive.dto.CellType;
import com.dangerwind.crusadefive.dto.MoveRequest;
import com.dangerwind.crusadefive.dto.MoveResponse;
import com.dangerwind.crusadefive.dto.StartRequest;
import com.dangerwind.crusadefive.dto.StartResponse;
import com.dangerwind.crusadefive.model.GameState;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.dangerwind.crusadefive.ai.AI.burnCells;
import static com.dangerwind.crusadefive.ai.AI.isBoardFull;

@Service
public class GameService {

    AI ai = new AI();

    private final Map<String, GameState> games = new ConcurrentHashMap<>();

// если новая игра полностью
    private StartResponse makeNewGame(StartRequest request) {
        StartResponse response = new StartResponse();
        String gameId;
        do {
            gameId = UUID.randomUUID().toString();
        } while (games.containsKey(gameId));
        response.setGameId(gameId);

        if(request.getPlayerName() == null || request.getPlayerName().isEmpty()) {
            response.setPlayerName("Без имени");
        } else {
            response.setPlayerName(request.getPlayerName());
        }

        if (request.getFieldWidth() != null) {
            response.setFieldWidth(request.getFieldWidth());
        } else {
            response.setFieldWidth(15);
        }

        if (request.getFieldHeight() != null) {
            response.setFieldHeight(request.getFieldHeight());
        } else {
            response.setFieldHeight(15);
        }

        response.setOver(false);
        response.setAiScore(0);
        response.setPlayerScore(0);


        GameState gameState = new GameState();

        gameState.setGameId(response.getGameId());
        gameState.setPlayerName(response.getPlayerName());

        gameState.setHeight(response.getFieldHeight());
        gameState.setWidth(response.getFieldWidth());

        gameState.setPlayerScore(response.getPlayerScore());
        gameState.setAiScore(response.getAiScore());
        gameState.setOver(response.isOver());

// заполняем поле пустотой
        CellType[][] board = new CellType[request.getFieldWidth()][request.getFieldHeight()];
        for (int i = 0; i < request.getFieldWidth(); i++) {
            for (int j = 0; j < request.getFieldHeight(); j++) {
                board[i][j] = CellType.EMPTY;
            }
        }
        gameState.setBoard(board);

        games.put(gameId, gameState); // сохранили в мапу
        // тут надо еще сохранение в бац данных

        return response;
    }


    public StartResponse startNewGame(StartRequest request) {

        StartResponse response = new StartResponse();

        String gameId = request.getGameId();
// если новая игра полностью
        if (gameId == null) {
            response = makeNewGame(request);
// если gameId передан
        } else {
// если был передан какой-то Id, но его нет в нашей базе, то создаем новую игру и используем это ID
            if (!games.containsKey(gameId)) {
                response = makeNewGame(request);
            } else {
                GameState gameState = games.get(gameId);  // получили всю игру

                response.setGameId(gameId);
                response.setPlayerName(gameState.getPlayerName());
                response.setPlayerScore(gameState.getPlayerScore());
                response.setAiScore(gameState.getAiScore());

                response.setOver(gameState.isOver());

                response.setFieldHeight(gameState.getHeight());
                response.setFieldWidth(gameState.getWidth());
            }
        }

        return response;
    }

    public String surrenderGame(String gameId) {
        // Логика для обработки капитуляции игрока
        // Обновление состояния игры в базе данных или в памяти
        // saveGameState(gameId);
        return gameId;
    }

    public MoveResponse makeMove(MoveRequest request) {
        MoveResponse response = new MoveResponse();

        String gameId = request.getGameId();
        if (!games.containsKey(gameId)) {
            throw new IllegalArgumentException("Игра с таким ID не найдена");
        }

        GameState gameState = games.get(gameId);

        response.setGameId(gameId);

        gameState.getBoard()[request.getPlayerMove().getX()][request.getPlayerMove().getY()] = CellType.PLAYER;
        System.out.printf("  Игрок выбрал клетку: (%d, %d)\n", request.getPlayerMove().getX(), request.getPlayerMove().getY());

        if(ai.isWinner(gameState.getBoard(),request.getPlayerMove().getX(),
                request.getPlayerMove().getY(), CellType.PLAYER)) {
            System.out.println("!!!!!!!!! Игрок победил!");

            var cells = ai.burnCells(gameState.getBoard(),
                    request.getPlayerMove().getX(), request.getPlayerMove().getY(), CellType.PLAYER_PLACE);

            gameState.setPlayerScore(gameState.getPlayerScore() + cells.size());
            response.setAiScore(gameState.getPlayerScore());
            response.setBurnedCells(cells);
        }

        if (isBoardFull(gameState.getBoard())) {
            System.out.println(" --- игра закончена -----!");
            response.setAiScore(gameState.getAiScore());
            response.setPlayerScore(gameState.getPlayerScore());
        }

// логика игры тут
        Cell aiMove = ai.calculateBestMove(gameState.getBoard());


        System.out.printf("Компьютер выбрал клетку: (%d, %d)\n", aiMove.getX(), aiMove.getY());
        gameState.getBoard()[aiMove.getX()][aiMove.getY()] = aiMove.getCellType();

        if(ai.isWinner(gameState.getBoard(), aiMove.getX(), aiMove.getY(), CellType.AI)) {
            System.out.println("!!!!!!!!! Компьютер победил!");


            var cells = ai.burnCells(gameState.getBoard(), aiMove.getX(), aiMove.getY(), CellType.AI_PLACE );

            gameState.setAiScore(gameState.getAiScore() + cells.size());
            response.setAiScore(gameState.getAiScore());
            response.setBurnedCells(cells);
        }


        if (isBoardFull(gameState.getBoard())) {
            System.out.println(" --- игра закончена -----!");
            response.setAiScore(gameState.getAiScore());
            response.setPlayerScore(gameState.getPlayerScore());
        }


        response.setAiMove(aiMove);


        response.setWinner(null); //    CellType winner;



        return response;
    }

}
