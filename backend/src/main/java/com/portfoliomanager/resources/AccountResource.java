package com.portfoliomanager.resources;

import com.google.inject.Inject;
import com.portfoliomanager.ledger.services.LedgerQueryService;
import com.portfoliomanager.ledger.models.LedgerFilter;
import com.portfoliomanager.account.services.AccountService;
import com.portfoliomanager.resources.models.DeletedResponse;
import com.portfoliomanager.resources.models.account.*;
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

@Path("/api/spaces/{spaceId}/accounts")
@Produces(MediaType.APPLICATION_JSON)
@ApiResponses({@ApiResponse(responseCode="400",description="Invalid input"),@ApiResponse(responseCode="404",description="Missing space or cross-space reference"),@ApiResponse(responseCode="409",description="Ledger conflict")})
@Tag(name = "Accounts")
public class AccountResource {
    private final AccountService service;
    private final LedgerQueryService queries;
    @Inject public AccountResource(AccountService service,LedgerQueryService queries) { this.service = service;this.queries=queries; }

    @GET @Operation(summary = "List accounts")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AccountPageResponse.class)))
    public AccountPageResponse list(@PathParam("spaceId") String spaceId,@Parameter(schema=@Schema(type="integer",minimum="1",defaultValue="1")) @QueryParam("page") String page,@Parameter(schema=@Schema(type="integer",minimum="1",maximum="100",defaultValue="20")) @QueryParam("size") String size,@Parameter(description="Case-insensitive literal substring of name") @QueryParam("q") String q,@Parameter(schema=@Schema(allowableValues={"BANK","CASH"})) @QueryParam("type") String type,@Parameter(schema=@Schema(allowableValues={"name_asc","name_desc","created_asc","created_desc"},defaultValue="name_asc")) @QueryParam("sort") String sort) {var result=queries.accounts(ApiParsing.id(spaceId),LedgerFilter.parse(page,size,q,type,null,null,null,null,sort));return new AccountPageResponse(result.items().stream().map(AccountListResponse::from).toList(),result.page(),result.size(),result.total());}

    @POST @Created @Consumes(MediaType.APPLICATION_JSON) @Operation(summary = "Create an account")
    @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    public AccountResponse create(@PathParam("spaceId") String spaceId, AccountRequest request) {
        if (request == null) throw new IllegalArgumentException("Request body is required");
        return AccountResponse.from(service.create(ApiParsing.id(spaceId), request.name(), request.type(), ApiParsing.money(request.startingBalance()), request.trackingStartDate()));
    }

    @GET @Path("/{id}") @Operation(summary = "Get an account")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    public AccountResponse get(@PathParam("spaceId") String spaceId, @PathParam("id") String id) {
        return AccountResponse.from(service.get(ApiParsing.id(spaceId), ApiParsing.id(id)).orElseThrow(() -> new NoSuchElementException("Account not found")));
    }

    @GET @Path("/{id}/balance") @Operation(summary = "Get account balance")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BalanceResponse.class)))
    public BalanceResponse balance(@PathParam("spaceId") String spaceId, @PathParam("id") String id) {
        UUID accountId = ApiParsing.id(id);
        return new BalanceResponse(accountId, service.balance(ApiParsing.id(spaceId), accountId).toPlainString());
    }

    @PATCH @Path("/{id}") @Consumes(MediaType.APPLICATION_JSON) @Operation(summary = "Update an account")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    public AccountResponse update(@PathParam("spaceId") String spaceId, @PathParam("id") String id, AccountUpdateRequest request) {
        if (request == null) throw new IllegalArgumentException("Request body is required");
        return AccountResponse.from(service.update(ApiParsing.id(spaceId), ApiParsing.id(id), request.name(), ApiParsing.money(request.startingBalance()), request.trackingStartDate()));
    }

    @DELETE @Path("/{id}") @Operation(summary = "Delete an account")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = DeletedResponse.class)))
    public DeletedResponse delete(@PathParam("spaceId") String spaceId, @PathParam("id") String id) {
        UUID accountId = ApiParsing.id(id); service.delete(ApiParsing.id(spaceId), accountId); return new DeletedResponse(accountId);
    }
}
