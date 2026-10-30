package com.portfoliomanager.space.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserSpace {
    private UUID id;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;
}
