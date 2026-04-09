package com.dangerwind.crusadefive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequest {

    private String gameId;

// ход игрока
    @NonNull
    private Cell playerMove;  // не может быть null, так как игрок всегда должен сделать ход

// если на фронте решили добавить еще ход от компьютера
// или в начале когда первым пошел компьютер, или когда происходит какое-то читерство,
// то эти поля могут быть заполнены, а могут быть null
    private Cell aiMove;  // может быть null

}
