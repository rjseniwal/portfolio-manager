package com.portfoliomanager.resources;
import com.google.inject.Inject;
import com.portfoliomanager.ledger.services.LedgerQueryService;
import com.portfoliomanager.resources.models.report.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
@Path("/api/spaces/{spaceId}/reports") @Produces(MediaType.APPLICATION_JSON) @Tag(name="Reports")
@io.swagger.v3.oas.annotations.responses.ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="Invalid or missing date range"),@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="Space not found")})
public class ReportResource {
    private final LedgerQueryService service;
    @Inject public ReportResource(LedgerQueryService service){this.service=service;}
    @GET @Path("/summary") @Operation(summary="Summarize cash flow and current ledger balance",description="Inclusive date range. Transaction count includes transfers; cash flow excludes transfers and starting balances. Ledger balance includes every date.")
    public SummaryResponse summary(@PathParam("spaceId") String spaceId,@Parameter(required=true,description="Inclusive ISO date") @QueryParam("from") String from,@Parameter(required=true,description="Inclusive ISO date") @QueryParam("to") String to){return SummaryResponse.from(service.summary(ApiParsing.id(spaceId),from,to));}
    @GET @Path("/monthly") @Operation(summary="Get monthly cash flow",description="Inclusive date range with zero-filled months and exact partial-month boundaries; transfers and starting balances are excluded.")
    public MonthlyResponse monthly(@PathParam("spaceId") String spaceId,@Parameter(required=true,description="Inclusive ISO date") @QueryParam("from") String from,@Parameter(required=true,description="Inclusive ISO date") @QueryParam("to") String to){return MonthlyResponse.from(service.monthly(ApiParsing.id(spaceId),from,to));}
}
