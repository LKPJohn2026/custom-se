package edu.usfca.cs272;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;

import edu.usfca.cs272.utils.WorkQueue;

/**
 * Unit tests for {@link WorkQueue}.
 */
public class WorkQueueTest {

	/** The queue reports the configured number of worker threads. */
	@Test
	public void testSize() {
		WorkQueue queue = new WorkQueue(3);
		try {
			assertEquals(3, queue.size());
		} finally {
			queue.join();
		}
	}

	/** All submitted tasks run before finish returns. */
	@Test
	@Timeout(value = 10, unit = java.util.concurrent.TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
	public void testAllTasksRun() {
		WorkQueue queue = new WorkQueue(4);
		AtomicInteger counter = new AtomicInteger();
		int total = 1000;

		try {
			for (int i = 0; i < total; i++) {
				queue.execute(counter::incrementAndGet);
			}
			queue.finish();

			assertEquals(total, counter.get());
		} finally {
			queue.join();
		}
	}

	/** The queue can be reused after a finish call. */
	@Test
	@Timeout(value = 10, unit = java.util.concurrent.TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
	public void testReuseAfterFinish() {
		WorkQueue queue = new WorkQueue(2);
		AtomicInteger counter = new AtomicInteger();

		try {
			for (int i = 0; i < 50; i++) {
				queue.execute(counter::incrementAndGet);
			}
			queue.finish();
			assertEquals(50, counter.get());

			for (int i = 0; i < 50; i++) {
				queue.execute(counter::incrementAndGet);
			}
			queue.finish();
			assertEquals(100, counter.get());
		} finally {
			queue.join();
		}
	}

	/** Submitting work after shutdown throws an exception. */
	@Test
	public void testExecuteAfterShutdown() {
		WorkQueue queue = new WorkQueue(2);
		queue.shutdown();

		assertThrows(IllegalStateException.class, () -> queue.execute(() -> {}));
		queue.join();
	}

	/** A task that throws does not prevent other tasks from running. */
	@Test
	@Timeout(value = 10, unit = java.util.concurrent.TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
	public void testExceptionDoesNotLeakThread() {
		WorkQueue queue = new WorkQueue(2);
		AtomicInteger counter = new AtomicInteger();

		try {
			queue.execute(() -> {
				throw new RuntimeException("boom");
			});
			for (int i = 0; i < 10; i++) {
				queue.execute(counter::incrementAndGet);
			}
			queue.finish();

			assertEquals(10, counter.get());
		} finally {
			queue.join();
		}
	}
}
