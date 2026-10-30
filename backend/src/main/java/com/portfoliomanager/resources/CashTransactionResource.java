package com.portfoliomanager.resources;

import com.portfoliomanager.ledger.services.LedgerExportService;
import com.google.inject.Inject;
import com.portfoliomanager.ledger.services.LedgerQueryService;
import com.portfoliomanager.ledger.models.LedgerFilter;
import com.portfoliomanager.transaction.services.CashTransactionService;
import com.portfoliomanager.resources.models.DeletedResponse;
import com.portfoliomanager.resources.models.transaction.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;

@Path("/api/spaces/{spaceId}/transactions")
@Produces(MediaType.APPLICATION_JSON)
@ApiResponses({@ApiResponse(responseCode="400",description="Invalid input"),@ApiResponse(responseCode="404",description="Missing space or cross-space reference"),@ApiResponse(responseCode="409",description="Ledger conflict")})
@Tag(name = "Transactions")
public class CashTransactionResource {
    private final CashTransactionService service;
    private final LedgerQueryService queries;
    private final LedgerExportService exports;
    @Inject public CashTransactionResource(CashTransactionService service,LedgerQueryService queries,LedgerExportService exports) { this.service=service;this.queries=queries;this.exports=exports; }
    @GET @Operation(summary = "List transactions")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TransactionPageResponse.class)))
    public TransactionPageResponse list(@PathParam("spaceId") String spaceId,@Parameter(schema=@Schema(type="integer",minimum="1",defaultValue="1")) @QueryParam("page") String page,@Parameter(schema=@Schema(type="integer",minimum="1",maximum="100",defaultValue="20")) @QueryParam("size") String size,@Parameter(description="Case-insensitive literal substring of description") @QueryParam("q") String q,@Parameter(schema=@Schema(allowableValues={"INCOME","EXPENSE","TRANSFER"})) @QueryParam("type") String type,@Parameter(schema=@Schema(type="string",format="uuid")) @QueryParam("accountId") String accountId,@Parameter(schema=@Schema(type="string",format="uuid")) @QueryParam("categoryId") String categoryId,@Parameter(description="Inclusive ISO date",schema=@Schema(type="string",format="date")) @QueryParam("from") String from,@Parameter(description="Inclusive ISO date",schema=@Schema(type="string",format="date")) @QueryParam("to") String to,@Parameter(schema=@Schema(allowableValues={"date_desc","date_asc","amount_desc","amount_asc"},defaultValue="date_desc")) @QueryParam("sort") String sort) {var result=queries.transactions(ApiParsing.id(spaceId),LedgerFilter.parse(page,size,q,type,accountId,categoryId,from,to,sort));return new TransactionPageResponse(result.items().stream().map(TransactionListResponse::from).toList(),result.page(),result.size(),result.total());}
    @GET @Path("/export") @Produces("text/csv") @Operation(summary="Export filtered transactions as CSV",description="Same filters and stable order as transaction listing, without pagination. UTF-8 BOM, CRLF records, quoted fields and spreadsheet formula escaping.")
    @ApiResponse(responseCode="200",description="CSV download",content=@Content(mediaType="text/csv",schema=@Schema(type="string",format="binary")))
    public CsvExportResponse export(@PathParam("spaceId") String spaceId,@Parameter(description="Case-insensitive literal substring of description") @QueryParam("q") String q,@Parameter(schema=@Schema(allowableValues={"INCOME","EXPENSE","TRANSFER"})) @QueryParam("type") String type,@Parameter(schema=@Schema(type="string",format="uuid")) @QueryParam("accountId") String accountId,@Parameter(schema=@Schema(type="string",format="uuid")) @QueryParam("categoryId") String categoryId,@Parameter(description="Inclusive ISO date",schema=@Schema(type="string",format="date")) @QueryParam("from") String from,@Parameter(description="Inclusive ISO date",schema=@Schema(type="string",format="date")) @QueryParam("to") String to,@Parameter(schema=@Schema(allowableValues={"date_desc","date_asc","amount_desc","amount_asc"},defaultValue="date_desc")) @QueryParam("sort") String sort){return new CsvExportResponse(exports.prepare(ApiParsing.id(spaceId),LedgerFilter.parse(null,null,q,type,accountId,categoryId,from,to,sort)));}
    @POST @Created @Consumes(MediaType.APPLICATION_JSON) @Operation(summary = "Create a transaction")
    @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    public TransactionResponse create(@PathParam("spaceId") String spaceId, TransactionRequest request) {
        if (request == null) throw new IllegalArgumentException("Request body is required");
        return TransactionResponse.from(service.create(ApiParsing.id(spaceId),request.transactionDate(),request.type(),ApiParsing.money(request.amount()),request.sourceAccountId(),request.destinationAccountId(),request.categoryId(),request.description()));
    }
    @GET @Path("/{id}") @Operation(summary = "Get a transaction")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    public TransactionResponse get(@PathParam("spaceId") String spaceId, @PathParam("id") String id) {
        return TransactionResponse.from(service.get(ApiParsing.id(spaceId),ApiParsing.id(id)).orElseThrow(() -> new NoSuchElementException("Transaction not found")));
    }
    @PATCH @Path("/{id}") @Consumes(MediaType.APPLICATION_JSON) @Operation(summary = "Replace transaction details", description = "Supply the complete transaction, including applicable account and category fields.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    public TransactionResponse update(@PathParam("spaceId") String spaceId, @PathParam("id") String id, TransactionRequest request) {
        if (request == null) throw new IllegalArgumentException("Request body is required");
        return TransactionResponse.from(service.update(ApiParsing.id(spaceId),ApiParsing.id(id),request.transactionDate(),request.type(),ApiParsing.money(request.amount()),request.sourceAccountId(),request.destinationAccountId(),request.categoryId(),request.description()));
    }
    @DELETE @Path("/{id}") @Operation(summary = "Delete a transaction")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = DeletedResponse.class)))
    public DeletedResponse delete(@PathParam("spaceId") String spaceId, @PathParam("id") String id) {
        UUID transactionId=ApiParsing.id(id); service.delete(ApiParsing.id(spaceId),transactionId); return new DeletedResponse(transactionId);
    }
}
