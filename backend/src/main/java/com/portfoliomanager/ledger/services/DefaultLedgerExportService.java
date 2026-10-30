package com.portfoliomanager.ledger.services;
import com.google.inject.Inject;
import com.portfoliomanager.ledger.models.*;
import com.portfoliomanager.ledger.repositories.LedgerQueryRepository;
import com.portfoliomanager.persistence.TransactionRunner;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
public class DefaultLedgerExportService implements LedgerExportService {
    private final LedgerQueryService query;private final LedgerQueryRepository repository;private final TransactionRunner tx;
    @Inject public DefaultLedgerExportService(LedgerQueryService query,LedgerQueryRepository repository,TransactionRunner tx){this.query=query;this.repository=repository;this.tx=tx;}
    public CsvExport prepare(UUID space,LedgerFilter filter){query.validateTransactions(space,filter);return new CsvExport(space,filter);}
    public void write(CsvExport export,OutputStream output)throws IOException{
        // The writer belongs to the HTTP response. Closing it here would close the container's stream.
        var writer=new BufferedWriter(new OutputStreamWriter(output,StandardCharsets.UTF_8));
        try {tx.inTransaction(()->{try{
            writer.write('\uFEFF');
            writer.write("id,spaceId,transactionDate,type,amount,sourceAccountId,sourceAccountName,destinationAccountId,destinationAccountName,categoryId,categoryName,description,createdAt,updatedAt\r\n");
            repository.streamTransactions(export.spaceId(),export.filter(),detail->{var t=detail.transaction();try{row(writer,t.getId(),t.getSpaceId(),t.getTransactionDate(),t.getType(),t.getAmount().toPlainString(),t.getSourceAccountId(),detail.sourceAccountName(),t.getDestinationAccountId(),detail.destinationAccountName(),t.getCategoryId(),detail.categoryName(),t.getDescription(),t.getCreatedAt(),t.getUpdatedAt());}catch(IOException e){throw new UncheckedIOException(e);}});
            writer.flush();return null;
        }catch(IOException e){throw new UncheckedIOException(e);}});}catch(UncheckedIOException e){throw e.getCause();}
    }
    private static void row(Writer writer,Object... values)throws IOException{for(int i=0;i<values.length;i++){if(i>0)writer.write(',');String value=values[i]==null?"":values[i].toString();
        // Spreadsheet formula recognition ignores initial whitespace, so inspect after stripping it.
        String significant=value.stripLeading();if(!significant.isEmpty() && "=+-@".indexOf(significant.charAt(0))>=0)value="'"+value;
        if(value.indexOf(',')>=0||value.indexOf('"')>=0||value.indexOf('\r')>=0||value.indexOf('\n')>=0){writer.write('"');writer.write(value.replace("\"","\"\""));writer.write('"');}else writer.write(value);
    }writer.write("\r\n");}
}
