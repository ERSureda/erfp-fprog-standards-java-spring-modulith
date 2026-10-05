package com.template.api.shared.domain.valueobject;

import com.template.api.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        @DisplayName("Should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("-0.01"), EUR))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Amount cannot be negative");
        }
    }

    @Nested
    @DisplayName("Scale-Independent Equality & HashCode")
    class EqualityAndHashCode {

        @Test
        @DisplayName("Amounts with different scale should be equal")
        void amountsWithDifferentScaleShouldBeEqual() {
            Money m1 = Money.of(new BigDecimal("10.0"), EUR);
            Money m2 = Money.of(new BigDecimal("10.00"), EUR);
            Money m3 = Money.of(new BigDecimal("10.000"), EUR);

            assertThat(m1).isEqualTo(m2);
            assertThat(m2).isEqualTo(m3);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
            assertThat(m2.hashCode()).isEqualTo(m3.hashCode());
        }

        @Test
        @DisplayName("Different amounts or currencies should not be equal")
        void differentAmountsOrCurrenciesShouldNotBeEqual() {
            Money eur10 = Money.of("10.00", "EUR");
            Money eur20 = Money.of("20.00", "EUR");
            Money usd10 = Money.of("10.00", "USD");

            assertThat(eur10).isNotEqualTo(eur20);
            assertThat(eur10).isNotEqualTo(usd10);
            assertThat(eur10).isNotEqualTo(null);
            assertThat(eur10).isNotEqualTo("10.00 EUR");
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperations {

        @Test
        @DisplayName("Should add money with same currency")
        void shouldAddMoneyWithSameCurrency() {
            Money m1 = Money.of("20.50", "EUR");
            Money m2 = Money.of("10.25", "EUR");

            Money result = m1.plus(m2);

            assertThat(result).isEqualTo(Money.of("30.75", "EUR"));
        }

        @Test
        @DisplayName("Should subtract money with same currency")
        void shouldSubtractMoneyWithSameCurrency() {
            Money m1 = Money.of("20.50", "EUR");
            Money m2 = Money.of("10.25", "EUR");

            Money result = m1.minus(m2);

            assertThat(result).isEqualTo(Money.of("10.25", "EUR"));
        }

        @Test
        @DisplayName("Should fail when subtracting to a negative result")
        void shouldFailWhenSubtractingToNegative() {
            Money m1 = Money.of("10.00", "EUR");
            Money m2 = Money.of("20.00", "EUR");

            assertThatThrownBy(() -> m1.minus(m2))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Amount cannot be negative");
        }

        @Test
        @DisplayName("Should fail arithmetic operations with different currency")
        void shouldFailArithmeticWithDifferentCurrency() {
            Money eur = Money.of("10.00", "EUR");
            Money usd = Money.of("10.00", "USD");

            assertThatThrownBy(() -> eur.plus(usd))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Currency mismatch");

            assertThatThrownBy(() -> eur.minus(usd))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        @DisplayName("Should multiply money by factor")
        void shouldMultiplyMoneyByFactor() {
            Money m = Money.of("15.00", "EUR");

            Money byLong = m.multiply(3);
            Money byBigDecimal = m.multiply(new BigDecimal("1.5"));

            assertThat(byLong).isEqualTo(Money.of("45.00", "EUR"));
            assertThat(byBigDecimal).isEqualTo(Money.of("22.50", "EUR"));
        }
    }

    @Nested
    @DisplayName("Predicates and Comparison")
    class PredicatesAndComparison {

        @Test
        @DisplayName("Should test isZero and isPositive correctly")
        void shouldTestZeroAndPositive() {
            Money zero = Money.zero("EUR");
            Money positive = Money.of("0.01", "EUR");

            assertThat(zero.isZero()).isTrue();
            assertThat(zero.isPositive()).isFalse();

            assertThat(positive.isZero()).isFalse();
            assertThat(positive.isPositive()).isTrue();
        }

        @Test
        @DisplayName("Should compare money amounts with same currency")
        void shouldCompareMoneyAmounts() {
            Money small = Money.of("10.00", "EUR");
            Money large = Money.of("20.00", "EUR");

            assertThat(small.isLessThan(large)).isTrue();
            assertThat(large.isGreaterThan(small)).isTrue();
            assertThat(small.compareTo(large)).isNegative();
            assertThat(large.compareTo(small)).isPositive();
        }

        @Test
        @DisplayName("Should fail comparison with different currencies")
        void shouldFailComparisonWithDifferentCurrencies() {
            Money eur = Money.of("10.00", "EUR");
            Money usd = Money.of("20.00", "USD");

            assertThatThrownBy(() -> eur.compareTo(usd))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Currency mismatch");
        }
    }

    @Nested
    @DisplayName("ToString Representation")
    class ToStringRepresentation {

        @Test
        @DisplayName("Should return readable string representation")
        void shouldReturnReadableStringRepresentation() {
            Money money = Money.of("99.99", "EUR");

            assertThat(money.toString()).isEqualTo("99.99 EUR");
        }
    }
}
