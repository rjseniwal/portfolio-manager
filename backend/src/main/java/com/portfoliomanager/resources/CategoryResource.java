package com.portfoliomanager.resources;

import com.google.inject.Inject;
import com.portfoliomanager.ledger.services.LedgerQueryService;
import com.portfoliomanager.ledger.models.LedgerFilter;
import com.portfoliomanager.category.services.CategoryService;
import com.portfoliomanager.resources.models.DeletedResponse;
import com.portfoliomanager.resources.models.category.*;
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

@Path("/api/spaces/{spaceId}/categories")
@Produces(MediaType.APPLICATION_JSON)
@ApiResponses({@ApiResponse(responseCode="400",description="Invalid input"),@ApiResponse(responseCode="404",description="Missing space or cross-space reference"),@ApiResponse(responseCode="409",description="Ledger conflict")})
@Tag(name = "Categories")
public class CategoryResource {
    private final CategoryService service;
    private final LedgerQueryService queries;
    @Inject public CategoryResource(CategoryService service,LedgerQueryService queries) { this.service = service;this.queries=queries; }

    @GET @Operation(summary = "List categories")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CategoryPageResponse.class)))
    public CategoryPageResponse list(@PathParam("spaceId") String spaceId,@Parameter(schema=@Schema(type="integer",minimum="1",defaultValue="1")) @QueryParam("page") String page,@Parameter(schema=@Schema(type="integer",minimum="1",maximum="100",defaultValue="20")) @QueryParam("size") String size,@Parameter(description="Case-insensitive literal substring of name") @QueryParam("q") String q,@Parameter(schema=@Schema(allowableValues={"INCOME","EXPENSE"})) @QueryParam("type") String type,@Parameter(schema=@Schema(allowableValues={"name_asc","name_desc","created_asc","created_desc"},defaultValue="name_asc")) @QueryParam("sort") String sort) {var result=queries.categories(ApiParsing.id(spaceId),LedgerFilter.parse(page,size,q,type,null,null,null,null,sort));return new CategoryPageResponse(result.items().stream().map(CategoryResponse::from).toList(),result.page(),result.size(),result.total());}
    @POST @Created @Consumes(MediaType.APPLICATION_JSON) @Operation(summary = "Create a category")
    @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    public CategoryResponse create(@PathParam("spaceId") String spaceId, CategoryRequest request) {
        if (request == null) throw new IllegalArgumentException("Request body is required");
        return CategoryResponse.from(service.create(ApiParsing.id(spaceId),request.name(),request.type()));
    }
    @GET @Path("/{id}") @Operation(summary = "Get a category")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    public CategoryResponse get(@PathParam("spaceId") String spaceId, @PathParam("id") String id) {
        return CategoryResponse.from(service.get(ApiParsing.id(spaceId),ApiParsing.id(id)).orElseThrow(() -> new NoSuchElementException("Category not found")));
    }
    @PATCH @Path("/{id}") @Consumes(MediaType.APPLICATION_JSON) @Operation(summary = "Rename a category")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    public CategoryResponse rename(@PathParam("spaceId") String spaceId, @PathParam("id") String id, CategoryRenameRequest request) {
        if (request == null) throw new IllegalArgumentException("Request body is required");
        return CategoryResponse.from(service.rename(ApiParsing.id(spaceId),ApiParsing.id(id),request.name()));
    }
    @DELETE @Path("/{id}") @Operation(summary = "Delete a category")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = DeletedResponse.class)))
    public DeletedResponse delete(@PathParam("spaceId") String spaceId, @PathParam("id") String id) {
        UUID categoryId=ApiParsing.id(id); service.delete(ApiParsing.id(spaceId),categoryId); return new DeletedResponse(categoryId);
    }
}
