package com.dangerwind.crusadefive.repository;

import com.dangerwind.crusadefive.model.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

   // Optional<Player> findByName(String name);

    Page<Player> findAll(Pageable pageable);

    Optional<List<Player>> findAllByOrderByPlayerScoreDesc();
}



