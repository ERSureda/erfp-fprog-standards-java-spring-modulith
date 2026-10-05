package com.template.api.ordering.application.port.in;

import com.template.api.ordering.application.command.ProcessOrderPaymentCommand;

/**
 * Primary inbound port defining the contract for asynchronous order payment handling.
 * <p>
 * Implemented by application services to transition order states upon payment notification.
 * Conforms to APP-01 and TRX-05.
 */
public interface ProcessOrderPaymentUseCase {

    void execute(ProcessOrderPaymentCommand command);
}
