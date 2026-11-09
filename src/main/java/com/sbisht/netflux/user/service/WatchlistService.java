package com.sbisht.netflux.user.service;

import com.sbisht.netflux.user.entity.WatchlistItem;
import com.sbisht.netflux.user.exceptions.UserNotFoundException;
import com.sbisht.netflux.user.repository.UserRepository;
import com.sbisht.netflux.user.repository.WatchlistItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchlistService {

    private final UserRepository userRepository;
    private final WatchlistItemRepository watchlistItemRepository;

    public WatchlistService(UserRepository userRepository, WatchlistItemRepository watchlistItemRepository) {
        this.userRepository = userRepository;
        this.watchlistItemRepository = watchlistItemRepository;
    }

    public List<Long> getWatchlist(Long userId) {
        ensureUserExists(userId);
        return watchlistItemRepository.findByUser_IdOrderByIdAsc(userId)
                .stream()
                .map(WatchlistItem::getMovieId)
                .toList();
    }

    @Transactional
    public void addMovie(Long userId, Long movieId) {
        ensureUserExists(userId);
        if (watchlistItemRepository.existsByUser_IdAndMovieId(userId, movieId)) {
            return;
        }
        var item = new WatchlistItem();
        item.setUser(userRepository.getReferenceById(userId));
        item.setMovieId(movieId);
        watchlistItemRepository.save(item);
    }

    @Transactional
    public void removeMovie(Long userId, Long movieId) {
        ensureUserExists(userId);
        watchlistItemRepository.deleteByUser_IdAndMovieId(userId, movieId);
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }
}
