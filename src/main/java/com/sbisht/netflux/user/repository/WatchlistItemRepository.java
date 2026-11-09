package com.sbisht.netflux.user.repository;

import com.sbisht.netflux.user.entity.WatchlistItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByUser_IdOrderByIdAsc(Long userId);

    boolean existsByUser_IdAndMovieId(Long userId, Long movieId);

    void deleteByUser_IdAndMovieId(Long userId, Long movieId);
}
