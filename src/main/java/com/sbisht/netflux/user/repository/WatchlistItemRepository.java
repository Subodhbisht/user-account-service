package com.sbisht.netflux.user.repository;

import com.sbisht.netflux.user.entity.WatchlistItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByUser_IdOrderByIdAsc(Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO watchlist (user_id, movie_id)
            VALUES (:userId, :movieId)
            ON CONFLICT (user_id, movie_id) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(Long userId, Long movieId);

    void deleteByUser_IdAndMovieId(Long userId, Long movieId);
}
