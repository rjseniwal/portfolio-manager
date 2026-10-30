package com.portfoliomanager.resources;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class OpenApiTest {
    @Test
    void documentsSpaceOperationsAndStatusCodes() {
        OpenAPI api = new Reader(new OpenAPI()).read(Set.of(UserSpaceResource.class, StatusResource.class, AccountResource.class, CategoryResource.class, CashTransactionResource.class, ReportResource.class));
        assertNotNull(api.getInfo());
        for (String resource : new String[]{"accounts", "categories", "transactions"}) {
            var collection = api.getPaths().get("/api/spaces/{spaceId}/" + resource);
            var item = api.getPaths().get("/api/spaces/{spaceId}/" + resource + "/{id}");
            assertNotNull(collection.getGet().getResponses().get("200").getContent().get("application/json").getSchema());
            assertNotNull(collection.getPost().getResponses().get("201").getContent().get("application/json").getSchema());
            assertNotNull(item.getGet());
            assertNotNull(item.getPatch());
            assertNotNull(item.getDelete());
        }
        var size=api.getPaths().get("/api/spaces/{spaceId}/accounts").getGet().getParameters().stream().filter(p->p.getName().equals("size")).findFirst().orElseThrow();
        assertEquals(new java.math.BigDecimal("100"),size.getSchema().getMaximum());
        assertNotNull(api.getPaths().get("/api/spaces/{spaceId}/reports/summary").getGet());
        assertNotNull(api.getPaths().get("/api/spaces/{spaceId}/reports/monthly").getGet());
        assertNotNull(api.getPaths().get("/api/spaces/{spaceId}/transactions/export").getGet().getResponses().get("200").getContent().get("text/csv"));
        assertNotNull(api.getPaths().get("/api/spaces/{spaceId}/accounts/{id}/balance"));
        var spaces = api.getPaths().get("/api/spaces");
        var space = api.getPaths().get("/api/spaces/{spaceId}");
        assertNotNull(spaces.getGet().getResponses().get("200").getContent().get("application/json").getSchema());
        assertNotNull(spaces.getPost().getResponses().get("201"));
        assertNotNull(space.getGet().getResponses().get("200").getContent().get("application/json").getSchema());
        assertNotNull(space.getGet().getResponses().get("404"));
        assertNotNull(space.getPatch().getResponses().get("200").getContent().get("application/json").getSchema());
        assertNotNull(space.getPatch().getResponses().get("400"));
        assertNotNull(api.getPaths().get("/status").getGet().getResponses().get("200").getContent().get("application/json").getSchema());
    }
}
