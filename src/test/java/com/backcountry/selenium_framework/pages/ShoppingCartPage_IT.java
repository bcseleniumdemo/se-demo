package com.backcountry.selenium_framework.pages;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.backcountry.selenium_framework.CommonUtilities;
import com.backcountry.selenium_framework.DriverTypeProvider;
import com.backcountry.selenium_framework.ScreenshotComparer;
import com.backcountry.selenium_framework.SimulatedCustomer;
import com.backcountry.selenium_framework.Waiter;
import com.backcountry.selenium_framework.WebDriverFactory.DRIVER_TYPE;

@RunWith(JUnitParamsRunner.class)
public class ShoppingCartPage_IT {
		
	/**
	 * Compare shopping cart page screenshot
	 * 
	 * @param driverType
	 */
	@Test
	@Parameters(source = DriverTypeProvider.ScreenshotTests.class) //Runs with these driver types 
	public void sampleShoppingCartPageScreenshot(DRIVER_TYPE driverType){
		
		SimulatedCustomer simulatedCustomer = SimulatedCustomer.build(driverType);
		
		try{
			ShoppingCartPage.driveToThisPage(simulatedCustomer);
			
			//Wait for a moment to let the page stabilize, could be better handled by doing 
			//page state comparison and waiting for the page to remain the same for a period 
			//of time 
			Waiter.waitSecs(1);
			
			ScreenshotComparer.takeScreenshotAndCompareAgainstGold(
					this.getClass(), 
					simulatedCustomer,
					CommonUtilities.getMethodName());
		}
		finally{
			simulatedCustomer.quit();
		}
	}
	
}
