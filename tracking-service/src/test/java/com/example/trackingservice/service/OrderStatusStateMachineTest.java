package com.example.trackingservice.service;

import com.example.trackingservice.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for the {@link OrderStatusStateMachine}.
 *
 * The state machine is intentionally a pure object (no Spring, no I/O) so
 * every transition can be tested without mocks.
 */
class OrderStatusStateMachineTest {

    @Test
    void validForwardTransitions_areAllowed() {
        // Happy path: walk the entire lifecycle end-to-end without throwing.
        org.assertj.core.api.Assertions.assertThatCode(() ->
                OrderStatusStateMachine.validateTransition(OrderStatus.PLACED, OrderStatus.CONFIRMED))
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() ->
                OrderStatusStateMachine.validateTransition(OrderStatus.CONFIRMED, OrderStatus.PACKED))
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() ->
                OrderStatusStateMachine.validateTransition(OrderStatus.PACKED, OrderStatus.SHIPPED))
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() ->
                OrderStatusStateMachine.validateTransition(OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY))
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() ->
                OrderStatusStateMachine.validateTransition(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED))
                .doesNotThrowAnyException();
    }

    @Test
    void skipStep_isRejected() {
        // PLACED -> SHIPPED skips CONFIRMED + PACKED -> must be rejected
        assertThatThrownBy(() ->
                OrderStatusStateMachine.validateTransition(OrderStatus.PLACED, OrderStatus.SHIPPED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void backwardTransition_isRejected() {
        assertThatThrownBy(() ->
                OrderStatusStateMachine.validateTransition(OrderStatus.SHIPPED, OrderStatus.PLACED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void sameState_isRejected() {
        assertThatThrownBy(() ->
                OrderStatusStateMachine.validateTransition(OrderStatus.SHIPPED, OrderStatus.SHIPPED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void deliveredIsTerminal_allNextStatesAreRejected() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThatThrownBy(() ->
                    OrderStatusStateMachine.validateTransition(OrderStatus.DELIVERED, target))
                    .as("DELIVERED -> " + target + " should be rejected")
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Test
    void isAllowed_matchesValidateTransition() {
        assertThat(OrderStatusStateMachine.isAllowed(OrderStatus.PLACED, OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatusStateMachine.isAllowed(OrderStatus.PLACED, OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatusStateMachine.isAllowed(OrderStatus.SHIPPED, OrderStatus.SHIPPED)).isFalse();
    }
}
