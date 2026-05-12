package com.dangerwind.crusadefive.controller;


import com.dangerwind.crusadefive.model.Player;
import com.dangerwind.crusadefive.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;


import java.util.List;

import static org.assertj.core.api.Assertions.tuple;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@AutoConfigureMockMvc
public class StatisticControllerTest {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

 //   @Autowired
 //   ObjectMapper objectMapper; // Spring Boot сам его создаёт


    @Test
    public void testGetPlayerStatistic() throws Exception {


        Player  player = new Player();
        player.setName("John Bonn");
        playerRepository.save(player);


        mockMvc.perform(get("/api/v1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'John Bonn')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'John Bonn')].id", everyItem(notNullValue())))
                .andExpect(jsonPath("$[?(@.name == 'John Bonn')].createdAt", everyItem(notNullValue())))
                .andExpect(jsonPath("$[?(@.name == 'John Bonn')].updatedAt", everyItem(notNullValue())));


        /*
        var response = mockMvc.perform(get("/api/v1/statistics"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        var body = response.getContentAsString();
        System.out.println("-----------");
        System.out.println(body);
        System.out.println("-----------");
*/

    }

    @Test
    public void testInsertNewPlayer() throws Exception {
        String playerName = "John Bonn";
        Player  player = new Player();
        player.setName(playerName);
        player.setPlayerScore(123);

        var request = put("/api/v1/statistics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(player));

        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());

        List<Player> datas = playerRepository.findAll();

        assertThat(datas).extracting(Player::getName)
                        .contains(playerName);

    }

    @Test
    public void testUpdatePlayer() throws Exception {

        String playerName = "John Bonn";
        int playerScore = 345;

        Player  player = new Player();
        player.setName(playerName);
        player.setPlayerScore(123);
        playerRepository.save(player);

        player.setPlayerScore(playerScore);

        var request = put("/api/v1/statistics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(player));

        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());

        List<Player> datas = playerRepository.findAll();

        assertThat(datas).extracting(Player::getName, Player::getPlayerScore)
                .contains(tuple(playerName, playerScore));

    }

}
