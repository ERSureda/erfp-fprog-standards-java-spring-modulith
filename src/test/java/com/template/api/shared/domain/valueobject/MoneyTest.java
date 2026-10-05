package com.template.api.shared.domain.valueobject;

import com.template.api.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test suite for {@link Money} Value Object.
 * <p>
 * Verifies arithmetic operations, currency compatibility, scale-independent equality, and validation invariants.
 */
@DisplayName("Money Value Object Unit Tests")
class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    @Nested
    @DisplayName("Creation & Validation")
    class CreationAndValidation {

        @Test
        @DisplayName("Should create Money successfully with valid amount and currency")
        void shouldCreateMoneySuccessfully() {
            Money money = Money.of(new BigDecimal("100.50"), EUR);

            assertThat(money.amount()).isEqualByComparingTo("100.50");
            assertThat(money.currency()).isEqualTo(EUR);
        }

        @Test
        @DisplayName("Should create Money via string factories")
        void shouldCreateMoneyViaStringFactories() {
            Money m1 = Money.of(new BigDecimal("50.00"), "EUR");
            Money m2 = Money.of("50.00", "EUR");
            Money zero = Money.zero("EUR");

            assertThat(m1).isEqualTo(m2);
            assertThat(zero.amount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(zero.currency()).isEqualTo(EUR);
        }

        @Test
        @DisplayName("Should reject null amount")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() -> new Money(null, EUR))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("amount cannot be null");
        }

        @Test
        @DisplayName("Should reject null currency")
        void shouldRejectNullCurrency() {
            assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("currency cannot be null");
        }

        @Test
        @DisplayName("Should reject negative amount in constructor")
        void shouldRejectNegativeAmount() {
            assertThatThrownBy(() -> new Money(new BigDecimal("-1.00"), EUR))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Amount cannot be negative");
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperations {

        @Test
        @DisplayName("Should add money with same currency")
        void shouldAddMoneyWithSameCurrency() {
            Money m1 = Money.of(new BigDecimal("10.50"), EUR);
            Money m2 = Money.of(new BigDecimal("20.25"), EUR);

            Money result = m1.plus(m2);

            assertThat(result.amount()).isEqualByComparingTo("30.75");
            assertThat(result.currency()).isEqualTo(EUR);
        }

        @Test
        @DisplayName("Should fail adding money with different currency")
        void shouldFailAddingDifferentCurrency() {
            Money m1 = Money.of(new BigDecimal("10.00"), EUR);
            Money m2 = Money.of(new BigDecimal("10.00"), USD);

            assertThatThrownBy(() -> m1.plus(m2))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        @DisplayName("Should subtract money with same currency")
        void shouldSubtractMoneyWithSameCurrency() {
            Money m1 = Money.of(new BigDecimal("30.00"), EUR);
            Money m2 = Money.of(new BigDecimal("12.50"), EUR);

            Money result = m1.minus(m2);

            assertThat(result.amount()).isEqualByComparingTo("17.50");
        }

        @Test
        @DisplayName("Should fail subtracting when result is negative")
        void shouldFailSubtractingResultingInNegative() {
            Money m1 = Money.of(new BigDecimal("10.00"), EUR);
            Money m2 = Money.of(new BigDecimal("20.00"), EUR);

            assertThatThrownBy(() -> m1.minus(m2))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Amount cannot be negative");
        }

        @Test
        @DisplayName("Should multiply money by decimal and long factor")
        void shouldMultiplyMoneyByFactor() {
            Money m = Money.of(new BigDecimal("15.00"), EUR);

            Money res1 = m.multiply(new BigDecimal("2.5"));
            Money res2 = m.multiply(3L);

            assertThat(res1.amount()).isEqualByComparingTo("37.50");
            assertThat(res2.amount()).isEqualByComparingTo("45.00");
        }
    }

    @Nested
    @DisplayName("Predicates and Comparison")
    class PredicatesAndComparison {

        @Test
        @DisplayName("Should test isZero and isPositive correctly")
        void shouldTestPredicates() {
            Money zero = Money.zero(EUR);
            Money pos = Money.of(new BigDecimal("0.01"), EUR);

            assertThat(zero.isZero()).isTrue();
            assertThat(zero.isPositive()).isFalse();

            assertThat(pos.isZero()).isFalse();
            assertThat(pos.isPositive()).isTrue();
        }

        @Test
        @DisplayName("Should compare money amounts with same currency")
        void shouldCompareMoneyAmounts() {
            Money m10 = Money.of(new BigDecimal("10.00"), EUR);
            Money m20 = Money.of(new BigDecimal("20.00"), EUR);

            assertThat(m20.isGreaterThan(m10)).isTrue();
            assertThat(m10.isLessThan(m20)).isTrue();
            assertThat(m10.compareTo(m20)).isNegative();
            assertThat(m20.compareTo(m10)).isPositive();
            assertThat(m10.compareTo(Money.of(new BigDecimal("10.000"), EUR))).isZero();
        }

        @Test
        @DisplayName("Should fail comparison with different currencies")
        void shouldFailComparisonDifferentCurrencies() {
            Money eur = Money.of(BigDecimal.TEN, EUR);
            Money usd = Money.of(BigDecimal.TEN, USD);

            assertThatThrownBy(() -> eur.compareTo(usd))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Scale-Independent Equality")
    class ScaleIndependentEquality {

        @Test
        @DisplayName("Should consider amounts equal irrespective of decimal scale")
        void shouldBeEqualRegardlessOfScale() {
            Money m1 = Money.of(new BigDecimal("10.0"), EUR);
            Money m2 = Money.of(new BigDecimal("10.000"), EUR);

            assertThat(m1).isEqualTo(m2);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal if currencies differ")
        void shouldNotBeEqualIfCurrencyDiffers() {
            Money m1 = Money.of(new BigDecimal("10.00"), EUR);
            Money m2 = Money.of(new BigDecimal("10.00"), USD);

            assertThat(m1).isNotEqualTo(m2);
        }
    }

    @Nested
    @DisplayName("ToString Representation")
    class ToStringRepresentation {

        @Test
        @DisplayName("Should return readable string representation")
        void shouldReturnReadableString() {
            Money m = Money.of(new BigDecimal("99.99"), EUR);

            assertThat(m.toString()).isEqualTo("99.99 EUR");
        }
    }
}
