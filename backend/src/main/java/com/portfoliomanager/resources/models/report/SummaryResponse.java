package com.portfoliomanager.resources.models.report;
import com.portfoliomanager.ledger.models.Summary;
import com.portfoliomanager.category.models.CategoryType;
import java.util.*;
import java.time.LocalDate;
public record SummaryResponse(LocalDate from,LocalDate to,String income,String expense,String netCashFlow,long transactionCount,String ledgerBalance,long accountCount,List<CategoryTotalResponse> categoryTotals) {
    public record CategoryTotalResponse(UUID categoryId,String name,CategoryType type,String amount,long transactionCount){}
    public static SummaryResponse from(Summary r){return new SummaryResponse(r.from(),r.to(),r.income().toPlainString(),r.expense().toPlainString(),r.netCashFlow().toPlainString(),r.transactionCount(),r.ledgerBalance().toPlainString(),r.accountCount(),r.categoryTotals().stream().map(c->new CategoryTotalResponse(c.categoryId(),c.name(),c.type(),c.amount().toPlainString(),c.transactionCount())).toList());}
}
