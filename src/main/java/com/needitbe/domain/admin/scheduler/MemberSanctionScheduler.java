package com.needitbe.domain.admin.scheduler;

import com.needitbe.domain.admin.entity.MemberSanction;
import com.needitbe.domain.admin.repository.MemberSanctionRepository;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.entity.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberSanctionScheduler {

    private final MemberSanctionRepository sanctionRepository;

    // 1분마다 만료된 제재 확인
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void releaseExpiredSanctions() {

        List<MemberSanction> expiredSanctions =
                sanctionRepository
                        .findByReleasedAtIsNullAndExpiresAtLessThanEqual(
                                LocalDateTime.now()
                        );

        for (MemberSanction sanction : expiredSanctions) {

            if (!sanction.isExpired()) {
                continue;
            }

            Member member = sanction.getMember();

            if (member.getStatus() == MemberStatus.SUSPENDED) {
                member.activate();
            }

            sanction.release();

            log.info(
                    "회원 제재 자동 만료 처리 - memberId: {}, sanctionId: {}",
                    member.getId(),
                    sanction.getId()
            );
        }
    }
}