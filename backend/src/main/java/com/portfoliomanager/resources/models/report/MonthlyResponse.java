package com.portfoliomanager.resources.models.report;
import com.portfoliomanager.ledger.models.MonthlyReport;
import java.util.List;
import java.time.LocalDate;
public record MonthlyResponse(LocalDate from,LocalDate to,List<MonthResponse> items) {
    public record MonthResponse(String month,String income,String expense,String netCashFlow){}
    public static MonthlyResponse from(MonthlyReport r){return new MonthlyResponse(r.from(),r.to(),r.items().stream().map(m->new MonthResponse(m.month(),m.income().toPlainString(),m.expense().toPlainString(),m.netCashFlow().toPlainString())).toList());}
}
