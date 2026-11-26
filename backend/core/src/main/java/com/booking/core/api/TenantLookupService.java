package com.booking.core.api;

import com.booking.core.api.dto.TenantDto;

import java.util.Optional;

public interface TenantLookupService {
    Optional<TenantDto> findById(Long id);
    Optional<TenantDto> findBySlug(String slug);
}
