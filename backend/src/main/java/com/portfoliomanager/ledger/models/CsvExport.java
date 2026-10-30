package com.portfoliomanager.ledger.models;
import java.util.UUID;
public record CsvExport(UUID spaceId,LedgerFilter filter) {}
