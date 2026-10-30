package com.portfoliomanager.space.models;

import java.util.List;

public record UserSpacePage(List<UserSpace> items, int page, int size, long total) {
    public UserSpacePage {
        items = List.copyOf(items);
    }
}
