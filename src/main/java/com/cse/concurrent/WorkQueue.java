package com.cse.concurrent;

import java.util.LinkedList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple work queue implementation based on the IBM developerWorks article by
 * Brian Goetz. It is up to the user of this class to keep track of whether
 * there is any pending work remaining.
 *
 * @see <a href=
 *      "https://web.archive.org/web/20210126172022/https://www.ibm.com/developerworks/library/j-jtp0730/index.html">
 * 		Java Theory and Practice: Thread Pools and Work Queues</a>
 *
 */
public class WorkQueue {
	/** Workers that wait until work (or tasks) are available. */
	private final Worker[] workers;

	/** Queue of pending work (or tasks). */
	private final LinkedList<Runnable> tasks;

	/** Used to signal the workers should terminate. */
	private volatile boolean shutdown;

	/** Logger used for this class. */
	private static final Logger log = LogManager.getLogger();

	/** The number of pending tasks. */
	private int pending;

	/** The lock object used for synchronized access of pending tasks. */
	private final Object pendingLock;

	/**
	 * Starts a work queue with the default number of threads.
	 *
	 * @see #WorkQueue(int)
	 */
	public WorkQueue() {
		this(Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Starts a work queue with the specified number of threads.
	 *
	 * @param threads number of worker threads; should be greater than 1
	 */
	public WorkQueue(int threads) {
		this.tasks = new LinkedList<Runnable>();
		this.workers = new Worker[threads];
		this.shutdown = false;
		this.pending = 0;
		this.pendingLock = new Object();

		// start the threads so they are waiting in the background
		for (int i = 0; i < threads; i++) {
			workers[i] = new Worker();
			workers[i].start();
		}

		log.debug("Work queue started with {} thread(s).", workers.length);
	}

	/**
	 * Adds a work (or task) request to the queue. A worker thread will process this
	 * request when available.
	 *
	 * @param task work request (in the form of a {@link Runnable} object)
	 * @throws IllegalStateException if the work queue is already shutdown
	 */
	public void execute(Runnable task) throws IllegalStateException {
		// safe to do unsynchronized due to volatile keyword
		if (shutdown) {
			throw new IllegalStateException("Work queue is shutdown.");
		}

		synchronized (pendingLock) {
			pending++;
		}

		synchronized (tasks) {
			tasks.addLast(task);
			tasks.notifyAll();
		}
	}

	/**
	 * Waits for all pending work (or tasks) to be finished. Does not terminate the
	 * worker threads so that the work queue can continue to be used.
	 */
	public void finish() {
		synchronized (pendingLock) {
			try {
				while (pending > 0) {
					pendingLock.wait();
				}
			} catch (InterruptedException e) {
				System.err.println("Work queue interrupted.");
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Similar to {@link Thread#join()}, waits for all the work to be finished and
	 * the worker threads to terminate. The work queue cannot be reused after this
	 * call completes.
	 */
	public void join() {
		try {
			finish();
			shutdown();

			for (Worker worker : workers) {
				worker.join();
			}

			log.debug("All {} worker thread(s) terminated.", workers.length);
		} catch (InterruptedException e) {
			System.err.println("Warning: Work queue interrupted while joining.");
			log.catching(Level.WARN, e);
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Asks the queue to shutdown. Any unprocessed work (or tasks) will not be
	 * finished, but threads in-progress will not be interrupted.
	 */
	public void shutdown() {
		
		// safe to do unsynchronized due to volatile keyword
		shutdown = true;

		synchronized (tasks) {
			tasks.notifyAll();
		}

		log.debug("Work queue shutdown triggered.");
	}

	/**
	 * Returns the number of worker threads being used by the work queue.
	 *
	 * @return number of worker threads
	 */
	public int size() {
		return workers.length;
	}

	/**
	 * Waits until work (or a task) is available in the work queue. When work is
	 * found, will remove the work from the queue and run it.
	 *
	 * <p>
	 * If a shutdown is detected, will exit instead of grabbing new work from the
	 * queue. These threads will continue running in the background until a shutdown
	 * is requested.
	 */
	private class Worker extends Thread {
		/**
		 * Initializes a worker thread with a custom name.
		 */
		public Worker() {
			setName("Worker" + getName());
		}

		@Override
		public void run() {
			Runnable task = null;

			try {
				while (true) {
					synchronized (tasks) {
						while (tasks.isEmpty() && !shutdown) {
							tasks.wait();
						}

						// exit while for one of two reasons:
						// (a) queue has work, or (b) shutdown has been called

						if (shutdown) {
							break;
						}

						task = tasks.removeFirst();
					}

					try {
						task.run();
					} catch (RuntimeException e) {
						// catch runtime exceptions to avoid leaking threads
						System.err.printf("Error: %s encountered an exception while running.%n", this.getName());
						log.catching(Level.ERROR, e);
					} finally {
						synchronized (pendingLock) {
							pending--;
							if (pending == 0) {
								pendingLock.notifyAll();
							}
						}
					}
				}
			} catch (InterruptedException e) {
				// causes early termination of worker threads
				System.err.printf("Warning: %s interrupted while waiting.%n", this.getName());
				log.catching(Level.WARN, e);
				Thread.currentThread().interrupt();
			}
		}
	}
}
