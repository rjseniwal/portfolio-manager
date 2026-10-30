package com.portfoliomanager.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises production resource and MessageBodyWriter registration through a real server. */
class LedgerHttpTest {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private String base;

    @Test @Timeout(45)
    void actualHttpPagesValidationReportsAndCsv() throws Exception {
        int appPort=freePort(), adminPort=freePort();
        while(adminPort==appPort)adminPort=freePort();
        Path config=Files.createTempFile("ledger-http-", ".yml"), log=Files.createTempFile("ledger-http-", ".log");
        Files.writeString(config,"""
            server:
              applicationConnectors:
                - type: http
                  port: %d
              adminConnectors:
                - type: http
                  port: %d
            logging:
              level: WARN
            database:
              driverClass: org.h2.Driver
              user: sa
              password: ""
              url: jdbc:h2:mem:http_%s;DB_CLOSE_DELAY=-1
              properties:
                hibernate.hbm2ddl.auto: create
                hibernate.jdbc.time_zone: UTC
              validationQuery: SELECT 1
              initialSize: 1
              minSize: 1
              maxSize: 4
            """.formatted(appPort,adminPort,UUID.randomUUID()));
        base="http://127.0.0.1:"+appPort;
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",
            System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")),
            "com.portfoliomanager.PortfolioManagerApplication","server",config.toString())
            .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            long deadline=System.nanoTime()+Duration.ofSeconds(25).toNanos();
            boolean ready=false;
            while(System.nanoTime()<deadline && process.isAlive()) {
                try {if(call("GET","/status",null).statusCode()==200){ready=true;break;}}
                catch(java.io.IOException ignored) {}
                Thread.sleep(100);
            }
            assertTrue(ready,()->"Server failed to start: "+read(log));
            String space=post("/api/spaces","{\"name\":\"HTTP\"}").get("id").asText();
            String other=post("/api/spaces","{\"name\":\"Other\"}").get("id").asText();
            String prefix="/api/spaces/"+space;
            var bankResponse=post(prefix+"/accounts","{\"name\":\"Bank\",\"type\":\"BANK\",\"startingBalance\":\"100.00\",\"trackingStartDate\":\"2026-01-01\"}");
            String bank=bankResponse.get("id").asText();
            assertEquals("2026-01-01",bankResponse.get("trackingStartDate").asText());
            assertTrue(bankResponse.get("trackingStartDate").isTextual());
            String alien=post("/api/spaces/"+other+"/accounts","{\"name\":\"Foreign\",\"type\":\"BANK\",\"startingBalance\":\"0.00\",\"trackingStartDate\":\"2026-01-01\"}").get("id").asText();
            String category=post(prefix+"/categories","{\"name\":\"Salary\",\"type\":\"INCOME\"}").get("id").asText();
            var transactionResponse=post(prefix+"/transactions","{\"transactionDate\":\"2026-01-31\",\"type\":\"INCOME\",\"amount\":\"20.00\",\"destinationAccountId\":\""+bank+"\",\"categoryId\":\""+category+"\",\"description\":\"=income, \\\"quoted\\\"\"}");
            assertEquals("2026-01-31",transactionResponse.get("transactionDate").asText());
            assertTrue(transactionResponse.get("transactionDate").isTextual());
            assertEquals("2026-01-01",get(prefix+"/accounts/"+bank).get("trackingStartDate").asText());
            assertEquals("2026-01-31",get(prefix+"/transactions/"+transactionResponse.get("id").asText()).get("transactionDate").asText());
            var accounts=get(prefix+"/accounts");
            assertEquals("2026-01-01",accounts.get("items").get(0).get("trackingStartDate").asText());
            assertEquals("2026-01-31",get(prefix+"/transactions").get("items").get(0).get("transactionDate").asText());
            assertEquals(1,accounts.get("page").asInt());assertEquals(20,accounts.get("size").asInt());
            assertEquals("120.00",accounts.get("items").get(0).get("balance").asText());
            assertTrue(get(prefix+"/categories").get("items").isArray());
            assertEquals("Bank",get(prefix+"/transactions").get("items").get(0).get("destinationAccountName").asText());
            for(String bad:new String[]{"/accounts?page=0","/accounts?size=101","/categories?sort=invalid","/transactions?type=INVALID","/transactions?from=2026-02-30","/transactions?from=2026-02-01&to=2026-01-01","/reports/summary?from=2026-01-01"})
                assertEquals(400,call("GET",prefix+bad,null).statusCode(),bad);
            assertEquals(404,call("GET",prefix+"/transactions?accountId="+alien,null).statusCode());
            assertEquals(404,call("GET",prefix+"/transactions/export?accountId="+alien,null).statusCode());
            assertEquals(400,call("GET",prefix+"/transactions/export?sort=invalid",null).statusCode());
            var summary=get(prefix+"/reports/summary?from=2026-01-01&to=2026-01-31");
            assertEquals("2026-01-01",summary.get("from").asText());assertEquals("2026-01-31",summary.get("to").asText());
            var monthlyResponse=get(prefix+"/reports/monthly?from=2026-01-31&to=2026-03-01");
            assertEquals("2026-01-31",monthlyResponse.get("from").asText());assertEquals("2026-03-01",monthlyResponse.get("to").asText());
            assertEquals("20.00",summary.get("income").asText());assertTrue(summary.get("income").isTextual());
            assertEquals("20.00",summary.get("categoryTotals").get(0).get("amount").asText());
            var months=get(prefix+"/reports/monthly?from=2026-01-31&to=2026-03-01").get("items");
            assertEquals(3,months.size());assertEquals("0.00",months.get(1).get("income").asText());
            var csv=call("GET",prefix+"/transactions/export?type=INCOME&sort=date_asc",null);
            assertEquals(200,csv.statusCode());
            assertTrue(csv.headers().firstValue("Content-Type").orElseThrow().startsWith("text/csv"));
            assertTrue(csv.headers().firstValue("Content-Disposition").orElseThrow().contains("attachment; filename="));
            assertTrue(csv.body().startsWith("\uFEFFid,spaceId,transactionDate"));
            assertTrue(csv.body().contains("\"'=income, \"\"quoted\"\"\""));assertTrue(csv.body().endsWith("\r\n"));
            var api=get("/openapi.json");
            assertTrue(api.get("paths").has("/api/spaces/{spaceId}/reports/summary"));
            assertTrue(api.get("paths").has("/api/spaces/{spaceId}/transactions/export"));
        } finally {
            process.destroy();if(!process.waitFor(5,java.util.concurrent.TimeUnit.SECONDS))process.destroyForcibly();
            Files.deleteIfExists(config);Files.deleteIfExists(log);
        }
    }
    private HttpResponse<String> call(String method,String path,String body)throws Exception{
        var request=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(5));
        if(body==null)request.method(method,HttpRequest.BodyPublishers.noBody());
        else request.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private JsonNode get(String path)throws Exception{var response=call("GET",path,null);assertEquals(200,response.statusCode(),response.body());return json.readTree(response.body());}
    private JsonNode post(String path,String body)throws Exception{var response=call("POST",path,body);assertEquals(201,response.statusCode(),response.body());return json.readTree(response.body());}
    private static int freePort()throws Exception{try(var socket=new java.net.ServerSocket(0)){return socket.getLocalPort();}}
    private static String read(Path p){try{return Files.readString(p);}catch(Exception e){return e.toString();}}
}
