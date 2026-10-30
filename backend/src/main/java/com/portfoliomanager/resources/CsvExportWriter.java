package com.portfoliomanager.resources;
import com.google.inject.Inject;
import com.portfoliomanager.ledger.services.LedgerExportService;
import com.portfoliomanager.resources.models.transaction.CsvExportResponse;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
@Provider @Produces("text/csv")
public class CsvExportWriter implements MessageBodyWriter<CsvExportResponse> {
    private final LedgerExportService service;
    @Inject public CsvExportWriter(LedgerExportService service){this.service=service;}
    public boolean isWriteable(Class<?> type,Type genericType,Annotation[] annotations,MediaType mediaType){return type==CsvExportResponse.class;}
    public long getSize(CsvExportResponse response,Class<?> type,Type genericType,Annotation[] annotations,MediaType mediaType){return -1;}
    public void writeTo(CsvExportResponse response,Class<?> type,Type genericType,Annotation[] annotations,MediaType mediaType,MultivaluedMap<String,Object> headers,OutputStream output)throws IOException{
        headers.putSingle("Content-Type","text/csv; charset=UTF-8");
        headers.putSingle("Content-Disposition","attachment; filename=\"transactions-"+response.export().spaceId()+".csv\"");
        service.write(response.export(),output);
    }
}
