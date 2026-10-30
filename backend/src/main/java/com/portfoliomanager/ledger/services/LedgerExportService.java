package com.portfoliomanager.ledger.services;
import com.portfoliomanager.ledger.models.*;
import java.io.*;
import java.util.UUID;
public interface LedgerExportService {
    CsvExport prepare(UUID spaceId,LedgerFilter filter);
    void write(CsvExport export,OutputStream output)throws IOException;
}
