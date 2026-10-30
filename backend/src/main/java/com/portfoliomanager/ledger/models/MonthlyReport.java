package com.portfoliomanager.ledger.models;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record MonthlyReport(LocalDate from,LocalDate to,List<MonthTotal> items) {
    public record MonthTotal(String month,BigDecimal income,BigDecimal expense,BigDecimal netCashFlow) {}
}
