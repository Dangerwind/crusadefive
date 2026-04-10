package com.dangerwind.crusadefive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartRequest {

    String playerName;

// если есть ID игры, то можно продолжить, если null - то назначим новый и игра сначала
    String gameId;

// если gameID есть, то размеры не имеют значения, так как они уже были заданы при создании игры,
// а если gameID нет, то эти поля должны быть заполнены, так как они нужны для создания новой игры
    Integer fieldHeight;
    Integer fieldWidth;
}
