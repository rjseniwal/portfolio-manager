package com.portfoliomanager.category.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class Category {
    private UUID id;
    private UUID spaceId;
    private String name;
    private CategoryType type;
    private Instant createdAt;
    private Instant updatedAt;
}
