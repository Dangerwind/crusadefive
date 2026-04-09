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
    Integer fieldHeight;
    Integer fieldWidth;
}
