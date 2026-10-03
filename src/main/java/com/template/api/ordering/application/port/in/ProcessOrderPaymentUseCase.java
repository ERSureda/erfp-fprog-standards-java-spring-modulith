package com.template.api.ordering.application.port.in;

import com.template.api.ordering.application.command.ProcessOrderPaymentCommand;

/**
 * Primary port / Use Case for processing order payment confirmations asynchronously.
 */
public interface ProcessOrderPaymentUseCase {
    void execute(ProcessOrderPaymentCommand command);
}
