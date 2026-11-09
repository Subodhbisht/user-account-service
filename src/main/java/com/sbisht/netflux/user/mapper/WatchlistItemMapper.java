package com.sbisht.netflux.user.mapper;

import com.sbisht.netflux.user.entity.User;
import com.sbisht.netflux.user.entity.WatchlistItem;

public class WatchlistItemMapper {

    private WatchlistItemMapper() {
    }

    public static WatchlistItem toEntity(User user, Long movieId) {
        var item = new WatchlistItem();
        item.setUser(user);
        item.setMovieId(movieId);
        return item;
    }
}
