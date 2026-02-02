package com.chiyumechunga.backend.model.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal; // Import needed for NUMERIC types

@Data
@Entity
@Immutable // Tells Hibernate: "Read-only, do not try to update/create this table"
@Table(name = "vw_poc_dashboard")
public class DashboardView {

    // Since this is a single-row aggregate view, we can treat any unique-ish column as ID
    // or simply mark the first one. Hibernate requires at least one @Id.
    @Id
    @Column(name = "authentic_batches_tracked")
    private Long authenticBatchesTracked;

    @Column(name = "pending_confirmation")
    private Long pendingConfirmation;

    @Column(name = "sync_failures")
    private Long syncFailures;

    @Column(name = "authentic_scans")
    private Long authenticScans;

    @Column(name = "flagged_scans")
    private Long flaggedScans;

    // FIX: Changed from Double to BigDecimal to match Postgres 'numeric' type
    @Column(name = "authenticity_rate_pct")
    private BigDecimal authenticityRatePct;

    @Column(name = "custody_transfers")
    private Long custodyTransfers;

    @Column(name = "avg_sync_latency_seconds")
    private BigDecimal avgSyncLatencySeconds;// AVG() usually returns double precision, so Double is fine here
}