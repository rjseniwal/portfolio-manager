package com.portfoliomanager.ledger.models;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
public record LedgerFilter(int page,int size,String q,String type,UUID accountId,UUID categoryId,LocalDate from,LocalDate to,String sort) {
    public LedgerFilter {
        if(page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid pagination; page starts at 1 and size must be 1..100");
        if(from!=null && to!=null && from.isAfter(to)) throw new IllegalArgumentException("from must not follow to");
    }
    public static LedgerFilter parse(String page,String size,String q,String type,String account,String category,String from,String to,String sort) {
        try { return new LedgerFilter(page==null?1:Integer.parseInt(page),size==null?20:Integer.parseInt(size),q,type,id(account),id(category),date(from),date(to),sort); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Pagination must be integers"); }
    }
    private static UUID id(String s) { if(s==null)return null; try { var id=UUID.fromString(s); if(!id.toString().equalsIgnoreCase(s))throw new IllegalArgumentException();return id;}catch(RuntimeException e){throw new IllegalArgumentException("ID must be a UUID");} }
    public static LocalDate date(String s) { if(s==null)return null;try{if(!s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException();return LocalDate.parse(s);}catch(DateTimeParseException e){throw new IllegalArgumentException("Date must be a valid ISO date");} }
    public int offset(){return (page-1)*size;}
}
