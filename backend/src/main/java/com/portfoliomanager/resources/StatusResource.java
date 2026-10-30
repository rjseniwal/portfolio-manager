package com.portfoliomanager.resources;

import com.portfoliomanager.resources.models.status.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {
    @GET
    @Operation(summary = "Get server status")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    public StatusResponse status() {
        return new StatusResponse("ok");
    }
}
