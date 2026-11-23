package com.rvmc.ordertaker.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderStatusTest {

    @Test
    void shouldEnforceTheFullSequentialTransitionMatrix() {
        for (OrderStatus current : OrderStatus.values()) {
            for (OrderStatus next : OrderStatus.values()) {
                boolean expected = switch (current) {
                    case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
                    case CONFIRMED -> next == OrderStatus.PREPARING;
                    case PREPARING -> next == OrderStatus.FOR_PICKUP;
                    case FOR_PICKUP -> next == OrderStatus.RECEIVED;
                    case RECEIVED, CANCELLED -> false;
                };

                assertEquals(expected, current.canTransitionTo(next),
                        current + " to " + next);
            }
        }
    }

    @Test
    void shouldAllowCustomerCancellationOnlyForPendingOrConfirmedOrders() {
        for (OrderStatus current : OrderStatus.values()) {
            for (OrderStatus next : OrderStatus.values()) {
                boolean expected = next == OrderStatus.CANCELLED
                        && (current == OrderStatus.PENDING || current == OrderStatus.CONFIRMED);

                assertEquals(expected, current.canCustomerCancelTo(next),
                        "Customer: " + current + " to " + next);
            }
        }
    }

    @Test
    void shouldEnforceRestaurantStatusPermissions() {
        for (OrderStatus current : OrderStatus.values()) {
            for (OrderStatus next : OrderStatus.values()) {
                boolean expected = switch (current) {
                    case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
                    case CONFIRMED -> next == OrderStatus.PREPARING;
                    case PREPARING -> next == OrderStatus.FOR_PICKUP;
                    case FOR_PICKUP, RECEIVED, CANCELLED -> false;
                };

                assertEquals(expected, current.canRestaurantTransitionTo(next),
                        "Restaurant: " + current + " to " + next);
            }
        }
    }

    @Test
    void shouldKeepTerminalStatesTerminal() {
        for (OrderStatus next : OrderStatus.values()) {
            assertEquals(false, OrderStatus.RECEIVED.canTransitionTo(next));
            assertEquals(false, OrderStatus.CANCELLED.canTransitionTo(next));
            assertEquals(false, OrderStatus.RECEIVED.canRestaurantTransitionTo(next));
            assertEquals(false, OrderStatus.CANCELLED.canRestaurantTransitionTo(next));
            assertEquals(false, OrderStatus.RECEIVED.canCustomerCancelTo(next));
            assertEquals(false, OrderStatus.CANCELLED.canCustomerCancelTo(next));
        }
    }
}
