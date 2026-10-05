package com.template.api.shared.infrastructure.adapter.out.context;

import com.template.api.shared.application.context.ExecutionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExecutionContextHolder}.
 * <p>
 * Verifies thread-local binding, retrieval, clearing, and multi-threaded isolation of request execution contexts.
 */
@DisplayName("ExecutionContextHolder Unit Tests")
class ExecutionContextHolderTest {

    @BeforeEach
    @AfterEach
    void cleanUp() {
        ExecutionContextHolder.clear();
    }

    @Test
    @DisplayName("Should set and retrieve context on the same thread")
    void setAndGet_shouldReturnContext() {
        ExecutionContext context = ExecutionContext.anonymous();

        ExecutionContextHolder.set(context);

        assertThat(ExecutionContextHolder.get()).isSameAs(context);
    }

    @Test
    @DisplayName("Should clear context when setting null or calling clear")
    void clear_shouldRemoveContext() {
        ExecutionContext context = ExecutionContext.anonymous();

        ExecutionContextHolder.set(context);
        assertThat(ExecutionContextHolder.get()).isNotNull();

        ExecutionContextHolder.set(null);
        assertThat(ExecutionContextHolder.get()).isNull();

        ExecutionContextHolder.set(context);
        ExecutionContextHolder.clear();
        assertThat(ExecutionContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Should maintain separate contexts across different threads")
    void concurrency_threadsShouldHaveIsolatedContexts() throws InterruptedException {
        ExecutionContext mainContext = ExecutionContext.anonymous();
        ExecutionContextHolder.set(mainContext);

        AtomicReference<ExecutionContext> threadContext = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread otherThread = new Thread(() -> {
            threadContext.set(ExecutionContextHolder.get());
            latch.countDown();
        });
        otherThread.start();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(threadContext.get()).isNull();
        assertThat(ExecutionContextHolder.get()).isSameAs(mainContext);
    }
}
