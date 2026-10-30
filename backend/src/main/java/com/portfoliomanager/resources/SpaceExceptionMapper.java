package com.portfoliomanager.resources;

import com.portfoliomanager.resources.models.space.ErrorResponse;

import com.portfoliomanager.space.services.SpaceException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SpaceExceptionMapper implements ExceptionMapper<SpaceException> {
    @Override
    public Response toResponse(SpaceException exception) {
        int status = exception.reason() == SpaceException.Reason.NOT_FOUND ? 404 : 400;
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(exception.reason().name(), exception.getMessage())).build();
    }

}
