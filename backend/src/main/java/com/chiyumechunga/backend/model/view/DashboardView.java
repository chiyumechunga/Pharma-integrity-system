package com.chiyumechunga.backend.model.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable // Tells Hibernate: "Do not try to update this table"
@Table(name = "vw_poc_dashboard") // Maps directly to your SQL View
@Data
public class DashboardView {

    @Id // Views don't have PKs, but Hibernate needs one. We can pick any unique column or a dummy one.
    // Since your view aggregates to 1 row, we can just treat the first column as ID roughly,
    // or better yet, since it's a single row aggregate, we can just fetch it.
    private Long authenticBatchesTracked;

    private Long pendingConfirmation;
    private Long syncFailures;
    private Long authenticScans;
    private Long flaggedScans;
    private Double authenticityRatePct;
    private Long custodyTransfers;
    private Double avgSyncLatencySeconds;
}