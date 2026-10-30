package com.portfoliomanager.resources.models.transaction;
import java.util.List;
public record TransactionPageResponse(List<TransactionListResponse> items,int page,int size,long total) {}
