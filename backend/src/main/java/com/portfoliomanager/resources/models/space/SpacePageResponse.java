package com.portfoliomanager.resources.models.space;

import java.util.List;

public record SpacePageResponse(List<SpaceResponse> items, int page, int size, long total) {}
