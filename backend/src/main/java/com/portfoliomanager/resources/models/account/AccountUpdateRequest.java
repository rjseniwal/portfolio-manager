package com.portfoliomanager.resources.models.account;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.portfoliomanager.resources.DecimalStringDeserializer;
public record AccountUpdateRequest(String name, @JsonDeserialize(using = DecimalStringDeserializer.class) String startingBalance, LocalDate trackingStartDate) {}
