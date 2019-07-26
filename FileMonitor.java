package main;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class FileMonitor {
	private WatchService watchService;
	private Thread thread;
	
	public void watchDirectory(String directoryPath) {
		WatchThread watchThread = new WatchThread(directoryPath);
		thread = new Thread(watchThread);
		thread.start();
	}

	public void shutdown() throws IOException, ClosedWatchServiceException {
		System.out.println("inside shutdown");
		if (watchService != null) {
			watchService.close();
			thread.interrupt();
			System.out.println("shutting down watchservice.....");
		}
	}

	public class WatchThread implements Runnable {
		private String path;

		public WatchThread(String path) {
			this.path = path;
		}

		public void run() {
			watch();
		}

		public void watch() {
			try {
				watchService = FileSystems.getDefault().newWatchService();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Path path = Paths.get(this.path);
			WatchKey key;
			try {
				key = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
						StandardWatchEventKinds.OVERFLOW);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClosedWatchServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(" Monitor Running ");

			try {
				while ((key = watchService.take()) != null) {
					if (Thread.interrupted()) {
						System.out.println("Closing service");
						break;
					} else {
						for (WatchEvent<?> event : key.pollEvents()) {
							System.out.println("Event kind:" + event.kind() + ". File affected: " + event.context());
						}
						key.reset();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

}
