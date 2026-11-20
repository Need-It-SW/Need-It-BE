package com.needitbe.domain.report.dto;

public interface ReportedMemberProjection {

    Long getMemberId();

    Long getReportCount();

    Long getPendingCount();

    Long getInReviewCount();

    Long getResolvedCount();

    Long getRejectedCount();
}