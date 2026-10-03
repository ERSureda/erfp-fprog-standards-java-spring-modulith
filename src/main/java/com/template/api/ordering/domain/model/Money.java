package com.template.api.ordering.domain.model;

import com.template.api.shared.domain.exception.ValidationException;

import com.template.api.shared.domain.valueobject.ValueObject;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable monetary Value Object encapsulating an amount and currency.
 * Conforms to DOM-02, DOM-03, and DOM-04.
 */
public final class Money implements ValueObject {

    private final BigDecimal amount;
    private final Currency currency;

    protected Money(BigDecimal amount, Currency currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero");
        }
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency, "currency cannot be null");
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency.getCurrencyCode();
    }
}
