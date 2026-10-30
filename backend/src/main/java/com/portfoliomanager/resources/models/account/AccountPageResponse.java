package com.portfoliomanager.resources.models.account;
import java.util.List;
public record AccountPageResponse(List<AccountListResponse> items,int page,int size,long total) {}
