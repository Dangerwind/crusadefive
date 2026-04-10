package com.dangerwind.crusadefive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartResponse {

    String playerName;

// ID игры — фронт будет слать его в каждом запросе
    String gameId;

// реальные размеры поля которые надо создать
    Integer fieldHeight;
    Integer fieldWidth;

    int playerScore;
    int aiScore;
    boolean isOver;       // игра закончена?
}
