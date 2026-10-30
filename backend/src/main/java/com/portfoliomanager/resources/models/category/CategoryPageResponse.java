package com.portfoliomanager.resources.models.category;
import java.util.List;
public record CategoryPageResponse(List<CategoryResponse> items,int page,int size,long total) {}
