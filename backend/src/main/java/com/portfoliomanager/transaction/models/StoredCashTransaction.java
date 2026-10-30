package com.portfoliomanager.transaction.models;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "cash_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StoredCashTransaction {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, length = 36)
    private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "space_id", nullable = false, length = 36)
    private UUID spaceId;
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private TransactionType type;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "source_account_id", length = 36)
    private UUID sourceAccountId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "destination_account_id", length = 36)
    private UUID destinationAccountId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "category_id", length = 36)
    private UUID categoryId;
    @Column(name = "description", length = 1000)
    private String description;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
