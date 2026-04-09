package com.dangerwind.crusadefive.controller;


import com.dangerwind.crusadefive.dto.Cell;
import com.dangerwind.crusadefive.dto.CellType;
import com.dangerwind.crusadefive.dto.MoveRequest;
import com.dangerwind.crusadefive.dto.MoveResponse;
import com.dangerwind.crusadefive.dto.StartRequest;
import com.dangerwind.crusadefive.dto.StartResponse;
import com.dangerwind.crusadefive.dto.SurrenderRequest;
import com.dangerwind.crusadefive.dto.SurrenderResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
public class GameController {


    // старт игры, принимает имя игрока и за кого он играет, красные или синие
    @PostMapping("/start")
    public ResponseEntity<StartResponse> startGame(@RequestBody StartRequest request) {

        String gameId = request.getGameId();
        if (gameId == null) {
            gameId = UUID.randomUUID().toString();
        }

        return ResponseEntity.ok(new StartResponse(gameId));
    }

    //
    @PostMapping("/move")
    public ResponseEntity<MoveResponse> move(@RequestBody MoveRequest request) {

        MoveResponse response = MoveResponse.builder()
        .gameId(request.getGameId())
        .aiMove(new Cell(7, 8, CellType.AI))
        .burnedCells(null)
        .playerScore(0)
        .aiScore(0)
        .winner(null)
        .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/surrender")
    public ResponseEntity<SurrenderResponse> surrender(@RequestBody SurrenderRequest request) {
        return ResponseEntity.ok(new SurrenderResponse(request.getGameId())); // ← SurrenderResponse
    }
}
