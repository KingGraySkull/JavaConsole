package policies;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CacheScheduler {
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> future;
	private LocalDateTime expiryDateTime;

	public CacheScheduler(LocalDateTime expiryDateTime) {
		this.expiryDateTime = expiryDateTime;
	}

	public void start(int timeoutInSeconds) {
		future = scheduler.scheduleAtFixedRate(new Caching(), 0, timeoutInSeconds, TimeUnit.SECONDS);
	}

	private class Caching implements Runnable {
		@Override
		public void run() {
			LocalDateTime now = LocalDateTime.now();
			System.out.println("Current Datetime : " + now);
			System.out.println("Expiry Datetime : " + expiryDateTime);
			/*
			 * implement code to call service which writes or clear cache policy service
			 */

			if (now.isAfter(expiryDateTime)) {
				System.out.println("Cancelling task as current datetime : " + now + " is greater than expirydatetime : "
						+ expiryDateTime);
				cancel();
			}
		}

		private void cancel() {
			if (future != null) {
				if (!future.isCancelled()) {
					future.cancel(true);
				}
			}
			shutdown();
		}

		private void shutdown() {
			if (!scheduler.isTerminated()) {
				scheduler.shutdownNow();
			}
		}
	}
}
