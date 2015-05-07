package com.backcountry.selenium_framework.pages;

import static com.backcountry.selenium_framework.CommonUtilities.*;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.backcountry.selenium_framework.CommonUtilities.Timer;
import com.backcountry.selenium_framework.SimulatedCustomer;
import com.backcountry.selenium_framework.SimulatedCustomer.DriverWrapper;

/**
 * Product Detail Page 
 * 
 * @author dbogardus
 *
 */
public class ProductDetailPage{
	
	private static Class thisClass = ProductDetailPage.class;
	
	private static By productHeaderBy = By.className("product-header-wrap");
	
	/**
	 * Verify a user is on this page, should be called with each public method. 
	 * Why? Need to keep this object stateless, and don't want to reference to a 
	 * possibly stale webdriver.  
	 *
	 * 
	 * @param simulatedCustomer
	 * @param operationDescription
	 */
	private static void verifyOnThisPage(SimulatedCustomer simulatedCustomer, 
			String operationDescription) {
		
		if(!doesWebElementExist(simulatedCustomer.getDriverWrapper(), productHeaderBy))
			
			outputlnAndThrowRuntimeException(thisClass, 
					"Expected user to be on page [" + thisClass.getSimpleName() + "], " +
							"to perform operation [" + operationDescription + "]. " +
							"Instead found page with title "
							+ "[" + simulatedCustomer.getDriverWrapper().getTitle() + "]. ");
		
		outputln(thisClass, operationDescription);
	}
	
	
	
	
	/**
	 * Drive directly to a product detail page for a certain product  
	 * 
	 * @param simulatedCustomer
	 * @param productId
	 */
	public static void driveToThisPage(SimulatedCustomer simulatedCustomer, String productId){	
		
		final String URI = getBaseURI() + productId;
		
		Timer t = new Timer();
		
		simulatedCustomer.getDriverWrapper().get(URI);
		
		try{
			new WebDriverWait(simulatedCustomer.getDriverWrapper(), getWaitTimeoutInSeconds()).
				until(ExpectedConditions.presenceOfElementLocated(productHeaderBy));
			
			outputln(thisClass, "Loaded [" + thisClass.getSimpleName() + "] "
					+ "after [" + t.getDurationInSeconds() + "]s.");
			
			closeEmailLightBoxIfPresent(simulatedCustomer);
			closeChatInviteIfPresent(simulatedCustomer);
		}
		catch(TimeoutException e){
			outputlnAndThrowRuntimeException(thisClass, 
					"Could not load [" + thisClass.getSimpleName() + "] after " + 
							t.getDurationInSeconds() + "]s, instead found page with title "
							+ "[" + simulatedCustomer.getDriverWrapper().getTitle() + "]. ", e);
		}
	}
	
	
	
	
	/**
	 * Get the large text product name, including brand 
	 * 
	 * @param simulatedCustomer
	 * @return
	 */
	public static String getProductName(SimulatedCustomer simulatedCustomer){
		verifyOnThisPage(simulatedCustomer, "Getting Product Name");
		
		String productName = findProductNameDiv(simulatedCustomer.getDriverWrapper()).getText();
		
		outputln(thisClass, "Found Product Name [" + productName + "]");
		
		return productName;
	}
	
	
	private static WebElement findProductNameDiv(DriverWrapper driverWrapper){
		return driverWrapper.findElement(By.className("product-group-title"));
	}

}
