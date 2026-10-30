package com.portfoliomanager.resources;

import com.portfoliomanager.common.LedgerConflictException;
import com.portfoliomanager.resources.models.space.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.util.NoSuchElementException;

public final class LedgerExceptionMappers {
    private LedgerExceptionMappers() {}
    public static class Invalid implements ExceptionMapper<IllegalArgumentException> {
        public Response toResponse(IllegalArgumentException e) { return error(400, "INVALID_INPUT", e.getMessage()); }
    }
    public static class Missing implements ExceptionMapper<NoSuchElementException> {
        public Response toResponse(NoSuchElementException e) { return error(404, "NOT_FOUND", e.getMessage()); }
    }
    public static class Conflict implements ExceptionMapper<LedgerConflictException> {
        public Response toResponse(LedgerConflictException e) { return error(409, "CONFLICT", e.getMessage()); }
    }
    private static Response error(int status, String code, String message) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorResponse(code, message)).build();
    }
}
