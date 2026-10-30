package com.portfoliomanager.resources;

import com.portfoliomanager.resources.models.space.ErrorResponse;

import com.portfoliomanager.space.services.InvalidPaginationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidPaginationMapper implements ExceptionMapper<InvalidPaginationException> {
    @Override
    public Response toResponse(InvalidPaginationException exception) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse("INVALID_PAGINATION", exception.getMessage())).build();
    }

}
