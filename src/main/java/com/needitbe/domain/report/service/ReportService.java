package com.needitbe.domain.report.service;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.domain.report.dto.ReportCreateRequest;
import com.needitbe.domain.report.dto.ReportResponse;
import com.needitbe.domain.report.entity.Report;
import com.needitbe.domain.report.entity.ReportStatus;
import com.needitbe.domain.report.entity.ReportType;
import com.needitbe.domain.report.repository.ReportRepository;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.needitbe.domain.request.entity.ItemRequest;
import com.needitbe.domain.request.repository.ItemRequestRepository;
import java.util.List;
import com.needitbe.domain.report.dto.ReportStatusUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ItemRequestRepository itemRequestRepository;
    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ReportResponse createReport(
            Long memberId,
            ReportCreateRequest request
    ) {

        // 1. 신고자 조회
        Member reporter = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원이 존재하지 않습니다.")
                );

        // 2. 신고 대상 검증
        if (request.reportType() == ReportType.MEMBER) {

            // 회원 신고
            if (!memberRepository.existsById(request.targetId())) {
                throw new IllegalArgumentException(
                        "신고 대상 회원이 존재하지 않습니다."
                );
            }

            // 자기 자신 신고 방지
            if (memberId.equals(request.targetId())) {
                throw new IllegalArgumentException(
                        "자기 자신은 신고할 수 없습니다."
                );
            }

        } else if (request.reportType() == ReportType.REQUEST) {

            // 1. 신고 대상 게시글 조회
            ItemRequest itemRequest = itemRequestRepository
                    .findById(request.targetId())
                    .orElseThrow(() ->
                            new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                    );

            // 2. 본인이 작성한 게시글인지 확인
            if (itemRequest.getMember().getId().equals(memberId)) {
                throw new IllegalArgumentException(
                        "본인이 작성한 게시글은 신고할 수 없습니다."
                );
            }
        }

        // 3. 중복 신고 확인
        // 3. 중복 신고 확인
        boolean duplicate = reportRepository
                .existsByReporterIdAndReportTypeAndTargetIdAndStatusIn(
                        memberId,
                        request.reportType(),
                        request.targetId(),
                        List.of(
                                ReportStatus.PENDING,
                                ReportStatus.IN_REVIEW
                        )
                );

        if (duplicate) {
            throw new IllegalArgumentException(
                    "이미 접수한 신고입니다."
            );
        }

        // 4. 신고 저장
        Report report = new Report(
                reporter,
                request.reportType(),
                request.targetId(),
                request.reason(),
                request.description()
        );

        reportRepository.save(report);

        return ReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports(Long memberId) {

        return reportRepository
                .findByReporterIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(ReportResponse::from)
                .toList();
    }

    // 관리자 - 신고 목록 조회
    @Transactional(readOnly = true)
    public Page<ReportResponse> getAllReports(
            ReportStatus status,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<Report> reports;

        if (status == null) {

            // 전체 신고 조회
            reports = reportRepository.findAll(pageable);

        } else {

            // 상태별 신고 조회
            reports = reportRepository.findByStatus(
                    status,
                    pageable
            );
        }

        return reports.map(ReportResponse::from);
    }

    // 관리자 - 신고 상태 변경
    @Transactional
    public ReportResponse updateReportStatus(
            Long reportId,
            ReportStatusUpdateRequest request
    ) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "신고 내역이 존재하지 않습니다."
                        )
                );

        ReportStatus currentStatus = report.getStatus();
        ReportStatus newStatus = request.status();

        // 이미 처리 완료 또는 반려된 신고는 변경 불가능
        if (currentStatus == ReportStatus.RESOLVED
                || currentStatus == ReportStatus.REJECTED) {

            throw new IllegalArgumentException(
                    "이미 처리가 종료된 신고입니다."
            );
        }

        // 접수 상태에서 검토 중으로만 변경 가능
        if (currentStatus == ReportStatus.PENDING
                && newStatus != ReportStatus.IN_REVIEW) {

            throw new IllegalArgumentException(
                    "접수된 신고는 먼저 검토 중 상태로 변경해야 합니다."
            );
        }

        // 검토 중에서는 처리 완료 또는 반려만 가능
        if (currentStatus == ReportStatus.IN_REVIEW
                && newStatus != ReportStatus.RESOLVED
                && newStatus != ReportStatus.REJECTED) {

            throw new IllegalArgumentException(
                    "검토 중인 신고는 처리 완료 또는 반려만 가능합니다."
            );
        }

        // 신고 상태 변경
        report.updateStatus(newStatus);

        return ReportResponse.from(report);
    }

    // 관리자 - 신고 상세 조회
    @Transactional(readOnly = true)
    public ReportResponse getReport(Long reportId) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "신고 내역이 존재하지 않습니다."
                        )
                );

        return ReportResponse.from(report);
    }
}