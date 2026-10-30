package com.portfoliomanager.resources.models.space;

import io.swagger.v3.oas.annotations.media.Schema;

public record NameRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255) String name) {}
