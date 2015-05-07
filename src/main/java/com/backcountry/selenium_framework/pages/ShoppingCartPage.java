package com.backcountry.selenium_framework.pages;

import static com.backcountry.selenium_framework.CommonUtilities.*;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.backcountry.selenium_framework.CommonUtilities.Timer;
import com.backcountry.selenium_framework.SimulatedCustomer;

/**
 * Product Detail Page 
 * 
 * @author dbogardus
 *
 */
public class ShoppingCartPage{
	
	private static Class thisClass = ShoppingCartPage.class;
	
	
	
	/**
	 * Verify a user is on this page, should be called with each public method. 
	 * Why? Need to keep this object stateless, and don't want to reference to a 
	 * possibly stale webdriver.  
	 * 
	 * @param simulatedCustomer
	 * @param operationDescription
	 */
	private static void verifyOnThisPage(SimulatedCustomer simulatedCustomer, 
			String operationDescription) {
		
		if(!StringUtils.startsWith(simulatedCustomer.getDriverWrapper().getTitle(), "Cart")){
			
			outputlnAndThrowRuntimeException(thisClass, 
					"Expected user to be on page [" + thisClass.getSimpleName() + "], " +
							"to perform operation [" + operationDescription + "]. " +
							"Instead found page with title "
							+ "[" + simulatedCustomer.getDriverWrapper().getTitle() + "]. ");
		}
		
		outputln(thisClass, operationDescription);
	}
	
	
	
	
	
	/**
	 * Drive directly to a certain product detail page 
	 * 
	 * @param simulatedCustomer
	 * @param productId
	 */
	public static void driveToThisPage(SimulatedCustomer simulatedCustomer){	
		
		final String URI = getBaseURI() + "Store/cart/cart.jsp";
		
		Timer t = new Timer();
		
		simulatedCustomer.getDriverWrapper().get(URI);
		
		try{
			new WebDriverWait(simulatedCustomer.getDriverWrapper(), getWaitTimeoutInSeconds()).
				until(ExpectedConditions.presenceOfElementLocated(
						By.className("footer-copyright")));
			
			outputln(thisClass, "Loaded [" + thisClass.getSimpleName() + "] "
					+ "after [" + t.getDurationInSeconds() + "]s.");
			
			closeEmailLightBoxIfPresent(simulatedCustomer);
			closeChatInviteIfPresent(simulatedCustomer);
		}
		catch(TimeoutException e){
			outputlnAndThrowRuntimeException(thisClass, 
					"Could not load [" + thisClass.getSimpleName() + "] after [" + 
							t.getDurationInSeconds() + "]s, instead found page with title "
							+ "[" + simulatedCustomer.getDriverWrapper().getTitle() + "]. ", e);
		}
	}

	
	
	public static boolean isCartEmpty(SimulatedCustomer simulatedCustomer){	
		verifyOnThisPage(simulatedCustomer, "Checking if cart is empty");
		
		if(doesWebElementExist(simulatedCustomer.getDriverWrapper(), By.id("empty-cart"))){
			outputln(thisClass, "Cart is empty");
			return true;
		}
		
		outputln(thisClass, "Cart is not empty");
		return false;
	}
}
