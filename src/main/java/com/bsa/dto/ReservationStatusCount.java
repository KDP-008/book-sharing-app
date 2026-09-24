package com.bsa.dto;

import com.bsa.model.ReservationStatus;

public record ReservationStatusCount(ReservationStatus status, long count) {
}
