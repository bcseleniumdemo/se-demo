package com.backcountry.selenium_framework;

import java.util.concurrent.TimeUnit;

/**
 * Fancy wrapper around Thread.sleep()
 * 
 * @author dbogardus
 *
 */
public class Waiter { 	
	
	private final long millis;

	
	public static void wait(long timeout, TimeUnit timeUnit) {
			
		if (timeUnit == TimeUnit.MILLISECONDS) {
			new Waiter(timeout).engage();
		}
		else if (timeUnit == TimeUnit.SECONDS) {
			new Waiter(timeout * 1000).engage();
		}
		else if (timeUnit == TimeUnit.MINUTES) {
			new Waiter(timeout * 60 * 1000).engage();
		}
		else if (timeUnit == TimeUnit.HOURS) {
			new Waiter(timeout * 60 * 60 * 1000).engage();
		}
		else if (timeUnit == TimeUnit.DAYS) {
			new Waiter(timeout * 24 * 60 * 60 * 1000).engage();
		}
		else
			throw new UnsupportedOperationException(timeUnit
					+ "Is not a supported TimeUnit. Choose from the following: "
					+ TimeUnit.MILLISECONDS + ", " + TimeUnit.SECONDS + ", " + TimeUnit.MINUTES
					+ ", " + TimeUnit.HOURS + ", " + TimeUnit.DAYS + ".");
	}

	
	public static void waitSecs(long timeoutInSeconds){
		wait(timeoutInSeconds, TimeUnit.SECONDS);
	}
	
	
	private Waiter(long millis) {
		this.millis = millis;
	}

	private void engage() {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}