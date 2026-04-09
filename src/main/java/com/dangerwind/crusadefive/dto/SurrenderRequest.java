package com.dangerwind.crusadefive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurrenderRequest {
// пока что это значит что надо закончить игру и начать заново
    String gameId;
}
