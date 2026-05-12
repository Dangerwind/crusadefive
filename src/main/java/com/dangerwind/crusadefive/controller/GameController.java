package com.dangerwind.crusadefive.controller;


import com.dangerwind.crusadefive.dto.Cell;
import com.dangerwind.crusadefive.dto.CellType;
import com.dangerwind.crusadefive.dto.MoveRequest;
import com.dangerwind.crusadefive.dto.MoveResponse;
import com.dangerwind.crusadefive.dto.StartRequest;
import com.dangerwind.crusadefive.dto.StartResponse;
import com.dangerwind.crusadefive.dto.SurrenderRequest;
import com.dangerwind.crusadefive.dto.SurrenderResponse;
import com.dangerwind.crusadefive.model.Player;
import com.dangerwind.crusadefive.service.GameService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class GameController {

    GameService gameService;


    @GetMapping("/levels")
    public ResponseEntity<List<String>> getLevels() {
        var ret =  gameService.getLevels();

        return ResponseEntity.ok(ret);
    }

    // старт игры, принимает имя игрока и за кого он играет, красные или синие
    @PostMapping("/start")
    public ResponseEntity<StartResponse> startGame(
            @RequestBody StartRequest request,
            @CookieValue(value = "playerId", required = false) String cookiePlayerId) {

        StartResponse ret = gameService.startNewGame(request);

        if (cookiePlayerId != null) {
            gameService.updatePlayerGame(cookiePlayerId, ret.getGameId(), request.getGameLevel());
        }

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



    // если player есть в cookies - то вернуть его или null если player новый
    @GetMapping("/player/check")
    public ResponseEntity<Player> checkPlayer(@CookieValue(value = "playerId", required = false)
                                              String cookiePlayerId) {

        if (cookiePlayerId != null) {
            Player player = gameService.getPlayerById(cookiePlayerId);
            if (player != null) {
                return ResponseEntity.ok(player);
            }
        }

        return ResponseEntity.notFound().build();
    }




    @PostMapping("/player/identify")
    public ResponseEntity<Player> identifyPlayer(@RequestBody String name,
                                                 @CookieValue(value = "playerId", required = false)
                                                 String cookiePlayerId,
                                                 HttpServletResponse response) {

        // выв и выше, если есть такой - вернем его
        if (cookiePlayerId != null) {
            // Если cookie уже есть, возвращаем его
            Player player = gameService.getPlayerById(cookiePlayerId);
            if (player != null) {
                return ResponseEntity.ok(player);
            }
        }

        // Если cookie нет, создаем нового игрока и устанавливаем cookie
        Player newPlayer = gameService.createPlayer(name);


        // cookie  прописываем Id
        Cookie cookie = new Cookie("playerId", String.valueOf(newPlayer.getId()));
        cookie.setMaxAge(60 * 60 * 24 * 30);  // это 30 дней
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok(newPlayer);
    }




    @GetMapping("/statistics")
    public ResponseEntity<List<Player>> getStatistics() {

        List<Player> statistics = gameService.getPlayersStatistic();

        return ResponseEntity.ok(statistics);
    }

    @PutMapping("/statistics")
    public ResponseEntity<Player> addOrUpdatePlayersStatistic(@RequestBody Player player) {

        Player request = gameService.addOrUpdatePlayersStatistic(player);
        return ResponseEntity.ok(request);
    }

    @DeleteMapping("/statistics/{id}")
    public ResponseEntity<Void> deleteStatistics(@PathVariable Long id) {

        gameService.deletePlayersStatistic(id);
        return ResponseEntity.noContent().build();
    }
}
