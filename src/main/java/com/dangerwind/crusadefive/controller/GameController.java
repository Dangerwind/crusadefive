package com.dangerwind.crusadefive.controller;


import com.dangerwind.crusadefive.dto.Cell;
import com.dangerwind.crusadefive.dto.CellType;
import com.dangerwind.crusadefive.dto.MoveRequest;
import com.dangerwind.crusadefive.dto.MoveResponse;
import com.dangerwind.crusadefive.dto.StartRequest;
import com.dangerwind.crusadefive.dto.StartResponse;
import com.dangerwind.crusadefive.dto.SurrenderRequest;
import com.dangerwind.crusadefive.dto.SurrenderResponse;
import com.dangerwind.crusadefive.service.GameService;
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

    GameService gameService;


    // старт игры, принимает имя игрока и за кого он играет, красные или синие
    @PostMapping("/start")
    public ResponseEntity<StartResponse> startGame(@RequestBody StartRequest request) {

        StartResponse ret =  gameService.startNewGame(request);

        return ResponseEntity.ok(ret);
    }

    //
    @PostMapping("/move")
    public ResponseEntity<MoveResponse> move(@RequestBody MoveRequest request) {

        MoveResponse ret = gameService.makeMove(request);


        return ResponseEntity.ok(ret);
    }

    @PostMapping("/surrender")
    public ResponseEntity<SurrenderResponse> surrender(@RequestBody SurrenderRequest request) {
        return ResponseEntity.ok(new SurrenderResponse(request.getGameId())); // ← SurrenderResponse
    }
}
