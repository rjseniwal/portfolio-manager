package com.portfoliomanager.ledger.models;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record Summary(LocalDate from,LocalDate to,BigDecimal income,BigDecimal expense,BigDecimal netCashFlow,long transactionCount,BigDecimal ledgerBalance,long accountCount,List<CategoryTotal> categoryTotals) {}
