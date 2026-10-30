package com.portfoliomanager.resources;

import com.google.inject.Inject;
import com.portfoliomanager.resources.models.space.ErrorResponse;
import com.portfoliomanager.resources.models.space.NameRequest;
import com.portfoliomanager.resources.models.space.SpacePageResponse;
import com.portfoliomanager.resources.models.space.SpaceResponse;
import com.portfoliomanager.space.services.SpaceException;
import com.portfoliomanager.space.services.UserSpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@OpenAPIDefinition(info = @Info(title = "Portfolio Manager API", version = "1.0"))
@Path("/api/spaces")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Spaces")
public class UserSpaceResource {
    private final UserSpaceService service;

    @Inject
    public UserSpaceResource(UserSpaceService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "List spaces")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SpacePageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid pagination", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SpacePageResponse list(
            @Parameter(schema = @Schema(type = "integer", minimum = "1", defaultValue = "1")) @QueryParam("page") @DefaultValue("1") String page,
            @Parameter(schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")) @QueryParam("size") @DefaultValue("20") String size) {
        var result = service.list(page, size);
        return new SpacePageResponse(result.items().stream().map(SpaceResponse::from).toList(),
                result.page(), result.size(), result.total());
    }

    @POST
    @Created
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a space")
    @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = SpaceResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid name", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SpaceResponse create(NameRequest request) {
        return SpaceResponse.from(service.create(request == null ? null : request.name()));
    }

    @GET
    @Path("/{spaceId}")
    @Operation(summary = "Get a space")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SpaceResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Space not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SpaceResponse get(@PathParam("spaceId") String spaceId) {
        return SpaceResponse.from(service.get(parseId(spaceId))
                .orElseThrow(() -> new SpaceException(SpaceException.Reason.NOT_FOUND, "Space not found")));
    }

    @PATCH
    @Path("/{spaceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Rename a space")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SpaceResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid ID or name", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Space not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SpaceResponse rename(@PathParam("spaceId") String spaceId, NameRequest request) {
        return SpaceResponse.from(service.rename(parseId(spaceId), request == null ? null : request.name()));
    }

    private static UUID parseId(String value) {
        try {
            UUID id = UUID.fromString(value);
            if (!id.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
            return id;
        } catch (IllegalArgumentException e) {
            throw new SpaceException(SpaceException.Reason.INVALID_ID, "Space ID must be a UUID");
        }
    }
}
