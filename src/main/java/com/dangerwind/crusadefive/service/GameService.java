package com.dangerwind.crusadefive.service;

import com.dangerwind.crusadefive.ai.AI;
import com.dangerwind.crusadefive.dto.Cell;
import com.dangerwind.crusadefive.dto.CellType;
import com.dangerwind.crusadefive.dto.MoveRequest;
import com.dangerwind.crusadefive.dto.MoveResponse;
import com.dangerwind.crusadefive.dto.StartRequest;
import com.dangerwind.crusadefive.dto.StartResponse;
import com.dangerwind.crusadefive.model.GameState;
import com.dangerwind.crusadefive.model.Player;
import com.dangerwind.crusadefive.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.dangerwind.crusadefive.ai.AI.burnCells;
import static com.dangerwind.crusadefive.ai.AI.isBoardFull;

@Service
public class GameService {


    private static final int MAX_PLAYER_PER_PAGE= 100;
    private static final String[] LEVELS = {
            "Обучение",  // 1
            "Предгорье",  // 2
            "Привал",    // 3
            "Горный хребет",  // 4

            "Битва на мосту",  //5
            "Осада замка"  //6
         //   "Загадочный лес",  //7
         //   "Пустыня испытаний",  //8
         //   "Подземелье теней", // 9
         //   "Башня магов" // 10
    };

    @Autowired
    PlayerRepository playerRepository;

    AI ai = new AI();

    private final Map<String, GameState> games = new ConcurrentHashMap<>();


// генерить доски с уровнями

    private GameState makeBoard(StartRequest request) {
        GameState gameState = new GameState();

        gameState.setGameId(request.getGameId());
        gameState.setPlayerName(request.getPlayerName());
        gameState.setGameLevel(request.getGameLevel());

        gameState.setPlayerScore(0);
        gameState.setAiScore(0);
        gameState.setOver(false);

        gameState.setGameLevelName(LEVELS[request.getGameLevel()-1]);

// просто 10 на 10 пустое поле
        if (request.getGameLevel() == 1) {


            gameState.setHeight(10);
            gameState.setWidth(10);

            CellType[][] board = new CellType[gameState.getWidth()][gameState.getHeight()];

            for (int i = 0; i < gameState.getWidth(); i++) {
                for (int j = 0; j < gameState.getHeight(); j++) {
                    board[i][j] = CellType.EMPTY;
                }
            }
            gameState.setBoard(board);
        }
// 15 на 15 с камнями
        if (request.getGameLevel() == 2) {
            gameState.setHeight(15);
            gameState.setWidth(15);
            CellType[][] board = new CellType[gameState.getWidth()][gameState.getHeight()];
            for (int i = 0; i < gameState.getWidth(); i++) {
                for (int j = 0; j < gameState.getHeight(); j++) {
                    if (Math.random() <0.1) {
                        board[i][j] = CellType.WALL;
                    } else {
                        board[i][j] = CellType.EMPTY;
                    }
                }
            }
            gameState.setBoard(board);
        }

 // поле 20 на 20 и 4 зоны по 4 сторонам, костер, по краям рыцари и сзади камень
        if (request.getGameLevel() == 3) {
            gameState.setHeight(20);
            gameState.setWidth(20);
            CellType[][] board = new CellType[gameState.getWidth()][gameState.getHeight()];
            for (int i = 0; i < gameState.getWidth(); i++) {
                for (int j = 0; j < gameState.getHeight(); j++) {
                    board[i][j] = CellType.EMPTY;
                }
            }

            //  сидят у костра AI
            board[5][5] = CellType.FIRE_PLACE;
            board[5][4] = CellType.WALL;
            board[4][5] = CellType.AI;
            board[6][5] = CellType.AI;

            //  сидят у костра AI
            board[15][15] = CellType.FIRE_PLACE;
            board[15][14] = CellType.WALL;
            board[14][15] = CellType.AI;
            board[16][15] = CellType.AI;

            //  сидят у костра PLAYER
            board[15][5] = CellType.FIRE_PLACE;
            board[15][4] = CellType.WALL;
            board[14][5] = CellType.PLAYER;
            board[16][5] = CellType.PLAYER;

            //  сидят у костра PLAYER
            board[5][15] = CellType.FIRE_PLACE;
            board[5][14] = CellType.WALL;
            board[4][15] = CellType.PLAYER;
            board[6][15] = CellType.PLAYER;

            // центральные камни
            board[10][10] = CellType.WALL;
            board[10][11] = CellType.WALL;
            board[11][10] = CellType.WALL;
            board[11][11] = CellType.WALL;


            gameState.setBoard(board);
        }
 // 20 x 20 и горынй хребет делит поле пополам
        if (request.getGameLevel() == 4) {
            gameState.setHeight(20);
            gameState.setWidth(20);
            CellType[][] board = new CellType[gameState.getWidth()][gameState.getHeight()];
            for (int i = 0; i < gameState.getWidth(); i++) {
                for (int j = 0; j < gameState.getHeight(); j++) {
                    board[i][j] = CellType.EMPTY;
                }
            }

            int startX = gameState.getWidth() / 2  + (int) (Math.random() *6 ) - 3; // случайное смещение от центра
            double delatX = 0;
            for (int i = 0; i < gameState.getHeight(); i++) {
                board[startX][i] = CellType.WALL;
                delatX = delatX + (Math.random()*1.8 - 0.9);

                if (delatX > 1.9) {
                    delatX = 1.9 ;
                }
                if (delatX < -1.9) {
                    delatX = -1.9;
                }

                startX = startX + (int) delatX;
                if (startX > gameState.getWidth() || startX < 0) {
                    delatX = -delatX;
                    startX = startX + (int) delatX;
                }
            }

            int startY = gameState.getHeight() / 2  + (int) (Math.random() * 6) - 3; // случайное смещение от центра
            double delatY = 0;
            for (int i = 0; i < gameState.getWidth(); i++) {
                board[i][startY] = CellType.WALL;
                delatY = delatY + (Math.random()*1.8 - 0.9);

                if (delatY > 1.9) {
                    delatY = 1.9 ;
                }
                if (delatY < -1.9) {
                    delatY = -1.9;
                }

                startY = startY + (int) delatY;
                if (startY > gameState.getHeight() || startY < 0) {
                    delatY = -delatY;
                    startY = startY + (int) delatY;
                }
            }

            gameState.setBoard(board);
        }


        // Битва на мосту - 20 на 20 и 2 извилистые стены
        if (request.getGameLevel() == 5) {
            gameState.setHeight(20);
            gameState.setWidth(30);
            CellType[][] board = new CellType[gameState.getWidth()][gameState.getHeight()];
            for (int i = 0; i < gameState.getWidth(); i++) {
                for (int j = 0; j < gameState.getHeight(); j++) {
                    board[i][j] = CellType.EMPTY;
                }
            }


            for (int i = 0; i < gameState.getWidth(); i++) {
                int startY = (int) (Math.sin(i /1.3 ) * 2) + 1;
                for  (int j = 0; j <= startY; j++) {
                    board[i][j] = CellType.WALL;
                    board[i][gameState.getHeight() - j - 1] = CellType.WALL;
                }
            }

            gameState.setBoard(board);
        }

        // Осада замка
        if (request.getGameLevel() == 6) {

            gameState.setHeight(21);
            gameState.setWidth(21);


            CellType[][] board = new CellType[gameState.getWidth()][gameState.getHeight()];
            for (int i = 0; i < gameState.getWidth(); i++) {
                for (int j = 0; j < gameState.getHeight(); j++) {
                    board[i][j] = CellType.EMPTY;
                }
            }
            // Замок по центру 5x5 из камней                                                                                                     src/main/resources/static/app. +14 -3
            int castleCenter = gameState.getWidth() / 2;  // = 10
            int castleSize = 5;
            int castleStart = castleCenter - castleSize / 2;  // = 8

            for (int x = castleStart; x < castleStart + castleSize; x++) {
                for (int y = castleStart; y < castleStart + castleSize; y++) {
                    board[x][y] = CellType.WALL;
                }
            }

            // Костёр на вершине замка
            board[castleCenter][castleStart] = CellType.FIRE_PLACE;

            // Стражники по углам замка
            board[castleStart][castleStart] = CellType.AI;           // верх-лево
            board[castleStart + castleSize - 1][castleStart] = CellType.AI;  // верх-право
            board[castleStart][castleStart + castleSize - 1] = CellType.AI;  // низ-лево
            board[castleStart + castleSize - 1][castleStart + castleSize - 1] = CellType.AI;  // низ-право

            // Игрок начинает снизу от замка (4 позиции)
            board[castleCenter - 1][castleStart + castleSize + 2] = CellType.PLAYER;
            // board[castleCenter][castleStart + castleSize + 2] = CellType.PLAYER;
            board[castleCenter + 1][castleStart + castleSize + 2] = CellType.PLAYER;
            //board[castleCenter][castleStart + castleSize + 3] = CellType.PLAYER;

            gameState.setBoard(board);
        }


   // на случай если ширина и высота 0
        if (gameState.getHeight() < 5) gameState.setHeight(15);
        if (gameState.getWidth() < 5) gameState.setWidth(15);


        return gameState;
    }


// если новая игра полностью
    private StartResponse makeNewGame(StartRequest request) {
        StartResponse response = new StartResponse();

        if (request.getGameLevel() == null) {
            request.setGameLevel(1); // по умолчанию 1 уровень
        }

        if(request.getPlayerName() == null || request.getPlayerName().isEmpty()) {
            request.setPlayerName("Без имени");
        } else {
            request.setPlayerName(request.getPlayerName());
        }



        GameState gameState = makeBoard(request);




        response.setPlayerName(gameState.getPlayerName());
        response.setGameId(gameState.getGameId());
        response.setGameLevel(gameState.getGameLevel());
        response.setFieldHeight(gameState.getHeight());
        response.setFieldWidth(gameState.getWidth());
        response.setGameLevelName(gameState.getGameLevelName());

        response.setOver(false);
        response.setAiScore(0);
        response.setPlayerScore(0);


        games.put(gameState.getGameId(), gameState); // сохранили в мапу


        // тут надо еще сохранение в базу данных

        response.setBoard(collectNonEmptyCells(gameState.getBoard()));
        return response;
    }


    public StartResponse startNewGame(StartRequest request) {

        StartResponse response = new StartResponse();

        String gameId = request.getGameId();
        System.out.println("[startNewGame] получен gameId: " + gameId);
        System.out.println("[startNewGame] games.containsKey(gameId): " + (gameId != null && games.containsKey(gameId)));
        System.out.println("[startNewGame] games.size(): " + games.size());
// если новая игра полностью
        if (gameId == null || gameId.isEmpty() || (!games.containsKey(gameId))) {
            System.out.println("[startNewGame] создаём НОВУЮ игру");
            do {
                gameId = UUID.randomUUID().toString();
            } while (games.containsKey(gameId));
            request.setGameId(gameId);

            if (request.getGameLevel() == null) {
                request.setGameLevel(1);
            }
            response = makeNewGame(request);  // Должен быть уровень 1

// если gameId передан
        } else {
            System.out.println("[startNewGame] продолжаем СУЩЕСТВУЮЩУЮ игру");
            GameState gameState = games.get(gameId);  // получили всю игру
            System.out.println("[startNewGame] gameState.gameLevel: " + gameState.getGameLevel());
            System.out.println("[startNewGame] request.gameLevel: " + request.getGameLevel());
//  если уровень совпал, то возвращаем состояние игры, то есть продолжаем игру
            if (gameState.getGameLevel() == request.getGameLevel()) {
                if (gameState.isOver() ==  true) {  // если игра была закончена то заново

                    response = makeNewGame(request);
                    response.setPlayerScore(0);
                    response.setAiScore(0);
                    response.setOver(false);

                } else {

                    response.setGameId(gameId);
                    response.setPlayerName(gameState.getPlayerName());
                    response.setPlayerScore(gameState.getPlayerScore());
                    response.setAiScore(gameState.getAiScore());
                    response.setGameLevel(gameState.getGameLevel());
                    response.setOver(gameState.isOver());
                    response.setGameLevelName(gameState.getGameLevelName());

                    response.setFieldHeight(gameState.getHeight());
                    response.setFieldWidth(gameState.getWidth());
                    response.setBoard(collectNonEmptyCells(gameState.getBoard()));
                }
            } else {

                GameState newGameState = makeBoard(request); // создаем новую игру с нужным уровнем

                newGameState.setAiScore(gameState.getAiScore());
                newGameState.setPlayerScore(gameState.getPlayerScore());

// заменяем старую игру на новую, но сохраняем старый gameId
                games.put(gameId, newGameState);

                response.setGameId(gameId);
                response.setPlayerName(newGameState.getPlayerName());
                response.setPlayerScore(newGameState.getPlayerScore());
                response.setAiScore(newGameState.getAiScore());
                response.setGameLevel(newGameState.getGameLevel());
                response.setFieldHeight(newGameState.getHeight());
                response.setFieldWidth(newGameState.getWidth());
                response.setGameLevelName(newGameState.getGameLevelName());
                response.setBoard(collectNonEmptyCells(newGameState.getBoard()));

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

        CellType whoWinner = null;

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


            response.setBurnedCells(cells);
        }

        if (isBoardFull(gameState.getBoard())) {
            System.out.println(" --- игра закончена -----!");

           if ((gameState.getAiScore()) >  gameState.getPlayerScore()) {
               whoWinner = CellType.AI;
           } else if ((gameState.getAiScore()) <  gameState.getPlayerScore()) {
               whoWinner = CellType.PLAYER;
           } else {
               whoWinner = CellType.EMPTY; // ничья
           }
        }

// логика игры тут
        Cell aiMove = ai.calculateBestMove(gameState.getBoard());


        System.out.printf("Компьютер выбрал клетку: (%d, %d)\n", aiMove.getX(), aiMove.getY());
        gameState.getBoard()[aiMove.getX()][aiMove.getY()] = aiMove.getCellType();

        if(ai.isWinner(gameState.getBoard(), aiMove.getX(), aiMove.getY(), CellType.AI)) {
            System.out.println("!!!!!!!!! Компьютер победил!");


            var cells = ai.burnCells(gameState.getBoard(), aiMove.getX(), aiMove.getY(), CellType.AI_PLACE );

            gameState.setAiScore(gameState.getAiScore() + cells.size());
            response.setBurnedCells(cells);
        }

        if (isBoardFull(gameState.getBoard())) {
            System.out.println(" --- игра закончена -----!");

            if ((gameState.getAiScore()) >  gameState.getPlayerScore()) {
                whoWinner = CellType.AI;
            } else if ((gameState.getAiScore()) <  gameState.getPlayerScore()) {
                whoWinner = CellType.PLAYER;
            } else {
                whoWinner = CellType.EMPTY; // ничья
            }
        }


        response.setAiMove(aiMove);
        response.setWinner(whoWinner);

        // всегда возвращаем актуальный счёт из состояния игры
        response.setPlayerScore(gameState.getPlayerScore());
        response.setAiScore(gameState.getAiScore());

        return response;
    }

    private List<Cell> collectNonEmptyCells(CellType[][] board) {
        List<Cell> cells = new ArrayList<>();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                if (board[x][y] != CellType.EMPTY) {
                    cells.add(new Cell(x, y, board[x][y]));
                }
            }
        }
        return cells;
    }

    public Player getPlayerById(String cookiePlayerId) {
        // получить из базы данных игрока по id если что то не так нет id или он кривой - то null вернуть
        Long playerId;

        try {
            playerId = Long.valueOf(cookiePlayerId);
        } catch (NumberFormatException e) {
            return null; // или выбросить исключение, если id некорректный
        }

        return playerRepository.findById(playerId).orElse(null);
    }



    public List<Player> getPlayersStatistic() {
        return playerRepository.findAll(
                PageRequest.of(0, MAX_PLAYER_PER_PAGE, Sort.by(Sort.Direction.DESC, "playerScore"))
        ).getContent();
    }



    public Player addOrUpdatePlayersStatistic(Player player) {

        System.out.println("[addOrUpdatePlayersStatistic] получен player: id=" + player.getId() + 
                           ", name=" + player.getName() + 
                           ", score=" + player.getPlayerScore() + 
                           ", level=" + player.getLevel() + 
                           ", gameId=" + player.getGameId());

        Player playerFromDb = player.getId() == null ? null : playerRepository.findById(player.getId()).orElse(null);


        // если нет такого игрока - то делаем нового и сохраняем в базу
        if (playerFromDb == null) {
            System.out.println("[addOrUpdatePlayersStatistic] игрок не найден, создаём нового");
            playerFromDb = player;
            playerRepository.save(playerFromDb);

        } else {
            System.out.println("[addOrUpdatePlayersStatistic] игрок найден, обновляем");
            // если игрок есть - то достаем из базы
            playerFromDb.setName(player.getName());

            // если в базе меньше очков чем в запросе - то обновляем очки, если больше - то не трогаем
            if (playerFromDb.getPlayerScore() < player.getPlayerScore()) {
                playerFromDb.setPlayerScore(player.getPlayerScore());
            }
           // playerFromDb.setPlayerScore(player.getPlayerScore());

            playerFromDb.setGameId(player.getGameId());
            playerRepository.save(playerFromDb);
            System.out.println("[addOrUpdatePlayersStatistic] сохранено: gameId=" + playerFromDb.getGameId());
        }

        return playerFromDb;
    }

    public void deletePlayersStatistic(Long id) {

        playerRepository.deleteById(id);
    }

    public Player createPlayer(String name) {
        Player player = new Player();

        player.setName(name);
        player.setPlayerScore(0);
        player.setGameId(null);
        player.setLevel(1);

        playerRepository.save(player);
        return player;
    }

    public void updatePlayerGame(String cookiePlayerId, String gameId, Integer level) {
        Player player = getPlayerById(cookiePlayerId);
        if (player != null) {
            player.setGameId(gameId);
            if (level != null) player.setLevel(level);
            playerRepository.save(player);
        }
    }

    public List<String> getLevels() {
        List<String> list = new ArrayList<>(Arrays.asList(LEVELS));
        return list;
    }
}
