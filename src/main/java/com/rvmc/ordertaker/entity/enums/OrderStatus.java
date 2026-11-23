package com.rvmc.ordertaker.entity.enums;

public enum OrderStatus {
    PENDING, CONFIRMED, PREPARING, FOR_PICKUP, RECEIVED, CANCELLED;

    public boolean canTransitionTo(OrderStatus nextStatus) {
        if (nextStatus == null) return false;
        return switch (this) {
            case PENDING -> nextStatus == CONFIRMED || nextStatus == CANCELLED;
            case CONFIRMED -> nextStatus == PREPARING;
            case PREPARING -> nextStatus == FOR_PICKUP;
            case FOR_PICKUP -> nextStatus == RECEIVED;
            case RECEIVED, CANCELLED -> false;
        };
    }

    public boolean canCustomerCancelTo(OrderStatus nextStatus) {
        if (nextStatus == null) return false;
        return nextStatus == CANCELLED
                && (this == PENDING || this == CONFIRMED);
    }

    public boolean canRestaurantTransitionTo(OrderStatus nextStatus) {
        if (nextStatus == null) return false;
        if (this == PENDING) {
            return nextStatus == CONFIRMED || nextStatus == CANCELLED;
        }
        if (this == CONFIRMED) {
            return nextStatus == PREPARING;
        }
        if (this == PREPARING) {
            return nextStatus == FOR_PICKUP;
        }
        return false;
    }
}
