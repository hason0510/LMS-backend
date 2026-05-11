package com.example.backend.service;

import com.example.backend.constant.ClassContentAvailabilityStatus;

public record ClassContentAccessResult(ClassContentAvailabilityStatus availabilityStatus, boolean accessible,
                                       String message, String messageKey) {
}
