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

// ID игры — фронт будет слать его в каждом запросе
    private String gameId;

}