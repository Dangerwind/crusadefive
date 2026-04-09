package com.dangerwind.crusadefive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveResponse {

    private String gameId;

    private Cell aiMove;

    private List<Cell> burnedCells;

    private int playerScore;
    private int aiScore;

// null - когда еще есть клетки
// или передаст кто победил
    private CellType winner;

}
