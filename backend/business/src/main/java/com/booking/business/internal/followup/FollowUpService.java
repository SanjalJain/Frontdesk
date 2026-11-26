package com.booking.business.internal.followup;

import com.booking.business.api.dto.FollowUpCallDto;
import com.booking.business.api.dto.UpdateFollowUpRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FollowUpService {

    private final FollowUpRepository followUpRepository;

    public FollowUpService(FollowUpRepository followUpRepository) {
        this.followUpRepository = followUpRepository;
    }

    public List<FollowUpCallDto> listFollowUps(Long tenantId) {
        return followUpRepository.listFollowUps(tenantId);
    }

    @Transactional
    public boolean updateFollowUp(Long tenantId, Long appointmentId, UpdateFollowUpRequest req) {
        return followUpRepository.updateFollowUp(tenantId, appointmentId, req);
    }
}
