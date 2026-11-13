package com.rvmc.ordertaker.entity.enums;

public enum OrderStatus {
    PENDING, CONFIRMED, PREPARING, FOR_PICKUP, RECEIVED, CANCELLED;

    public boolean canTransitionTo(OrderStatus nextStatus) {
        if (this == CANCELLED || this == RECEIVED) return false; // Terminal states
        if (nextStatus == CANCELLED) return true; // Can usually cancel active orders

        // Define allowed sequential flow
        return switch (this) {
            case PENDING -> nextStatus == CONFIRMED;
            case CONFIRMED -> nextStatus == PREPARING;
            case PREPARING -> nextStatus == FOR_PICKUP;
            case FOR_PICKUP -> nextStatus == RECEIVED;
            default -> false;
        };
    }
}
