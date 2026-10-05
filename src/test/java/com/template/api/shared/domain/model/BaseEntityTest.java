package com.template.api.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test suite for {@link BaseEntity}.
 * <p>
 * Verifies identity encapsulation and identity-based equality contracts.
 */
@DisplayName("BaseEntity Unit Tests")
class BaseEntityTest {

    static class TestEntity extends BaseEntity<UUID> {
        TestEntity() {
            super();
        }

        TestEntity(UUID id) {
            super(id);
        }
    }

    static class AnotherEntity extends BaseEntity<UUID> {
        AnotherEntity(UUID id) {
            super(id);
        }
    }

    @Test
    @DisplayName("Should return id via id() and getId()")
    void accessors_shouldReturnId() {
        UUID id = UUID.randomUUID();
        TestEntity entity = new TestEntity(id);

        assertThat(entity.id()).isEqualTo(id);
        assertThat(entity.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should be equal when comparing the same instance")
    void sameInstance_shouldBeEqual() {
        TestEntity entity = new TestEntity(UUID.randomUUID());

        assertThat(entity.equals(entity)).isTrue();
    }

    @Test
    @DisplayName("Should be equal when IDs match and classes match")
    void sameId_shouldBeEqual() {
        UUID id = UUID.randomUUID();
        TestEntity e1 = new TestEntity(id);
        TestEntity e2 = new TestEntity(id);

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when IDs differ")
    void differentId_shouldNotBeEqual() {
        TestEntity e1 = new TestEntity(UUID.randomUUID());
        TestEntity e2 = new TestEntity(UUID.randomUUID());

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    @DisplayName("Should not be equal when comparing with null or different class")
    void differentClassOrNull_shouldNotBeEqual() {
        UUID id = UUID.randomUUID();
        TestEntity e1 = new TestEntity(id);
        AnotherEntity e2 = new AnotherEntity(id);

        assertThat(e1.equals(null)).isFalse();
        assertThat(e1.equals("string")).isFalse();
        assertThat(e1.equals(e2)).isFalse();
    }

    @Test
    @DisplayName("Should handle entities with null IDs safely")
    void nullId_shouldHandleEqualitySafely() {
        TestEntity unpersisted1 = new TestEntity();
        TestEntity unpersisted2 = new TestEntity();

        assertThat(unpersisted1.equals(unpersisted2)).isFalse();
        assertThat(unpersisted1.hashCode()).isEqualTo(TestEntity.class.hashCode());
    }

    @Test
    @DisplayName("Should throw NullPointerException when parameterized constructor receives null id")
    void constructor_nullId_shouldThrow() {
        assertThatThrownBy(() -> new TestEntity(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("id cannot be null");
    }
}
