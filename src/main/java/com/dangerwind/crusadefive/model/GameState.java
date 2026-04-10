package com.dangerwind.crusadefive.model;

import com.dangerwind.crusadefive.dto.CellType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameState {

    String gameId;
    String playerName;
    int width;
    int height;

    CellType[][] board;   // двумерный массив клеток — само поле
    // board[y][x] = EMPTY / PLAYER / AI / WALL / ...

    int playerScore;
    int aiScore;
    boolean isOver;       // игра закончена?
}
