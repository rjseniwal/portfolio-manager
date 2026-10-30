package com.portfoliomanager.resources;

import com.portfoliomanager.resources.models.CreatedModel;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Created
public class CreatedSpaceResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (response.getEntity() instanceof CreatedModel space) {
            response.setStatus(201);
            response.getHeaders().putSingle("Location", request.getUriInfo().getAbsolutePathBuilder().path(space.id().toString()).build());
        }
    }
}
