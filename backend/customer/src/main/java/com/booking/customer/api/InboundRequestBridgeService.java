package com.booking.customer.api;

import com.booking.customer.api.dto.HandleInboundRequest;
import com.booking.customer.api.dto.InboundRequestDto;

import java.util.List;
import java.util.Optional;

public interface InboundRequestBridgeService {
    List<InboundRequestDto> listPendingRequests(Long tenantId);
    Optional<InboundRequestDto> getRequest(Long tenantId, Long requestId);
    boolean handleRequest(Long tenantId, Long requestId, HandleInboundRequest request, Long handledBy);
}
