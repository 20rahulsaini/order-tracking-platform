package com.example.trackingservice.service;

import com.example.trackingservice.model.OrderStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Pure state-machine for order lifecycle:
 *   PLACED -> CONFIRMED -> PACKED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED
 * {@link #validateTransition(OrderStatus, OrderStatus)} before persisting.
 */
public final class OrderStatusStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PLACED, EnumSet.of(OrderStatus.CONFIRMED));
        ALLOWED.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PACKED));
        ALLOWED.put(OrderStatus.PACKED, EnumSet.of(OrderStatus.SHIPPED));
        ALLOWED.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY));
        ALLOWED.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED));
        ALLOWED.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class)); // terminal
    }

    private OrderStatusStateMachine() {
    }

    public static void validateTransition(OrderStatus current, OrderStatus target) {
        if (current == target) {
            throw new InvalidStatusTransitionException(
                    null, current.name(), target.name());
        }
        Set<OrderStatus> allowed = ALLOWED.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new InvalidStatusTransitionException(
                    null, current.name(), target.name());
        }
    }

    public static boolean isAllowed(OrderStatus current, OrderStatus target) {
        if (current == target) return false;
        Set<OrderStatus> allowed = ALLOWED.get(current);
        return allowed != null && allowed.contains(target);
    }
}
