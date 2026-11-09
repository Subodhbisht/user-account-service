package com.sbisht.netflux.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddWatchlistItemRequest(@NotNull @Positive Long movieId) {
}
