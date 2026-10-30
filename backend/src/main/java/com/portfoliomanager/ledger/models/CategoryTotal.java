package com.portfoliomanager.ledger.models;
import com.portfoliomanager.category.models.CategoryType;
import java.math.BigDecimal;
import java.util.UUID;
public record CategoryTotal(UUID categoryId,String name,CategoryType type,BigDecimal amount,long transactionCount) {}
