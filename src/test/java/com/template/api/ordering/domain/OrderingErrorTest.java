package com.template.api.ordering.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite for {@link OrderingError} catalog.
 * <p>
 * Verifies that all ordering error codes follow the global hierarchical [MODULO]_[ENTIDAD]_[MOTIVO] structure.
 */
@DisplayName("OrderingError Unit Tests")
class OrderingErrorTest {

    @ParameterizedTest
    @EnumSource(OrderingError.class)
    @DisplayName("OrderingError codes should match enum constant names")
    void orderingErrorCodeMatchesName(OrderingError error) {
        assertThat(error.code()).isEqualTo(error.name());
    }

    @ParameterizedTest
    @EnumSource(OrderingError.class)
    @DisplayName("OrderingError codes should adhere to ORDERING_[ENTIDAD]_[MOTIVO] pattern")
    void orderingErrorCodeFollowsStandardPattern(OrderingError error) {
        assertThat(error.code())
                .startsWith("ORDERING_")
                .matches("^[A-Z]+_[A-Z]+_[A-Z_]+$");
    }
}
