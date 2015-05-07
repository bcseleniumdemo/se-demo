package com.backcountry.selenium_framework.pages;

import static org.assertj.core.api.Assertions.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.backcountry.selenium_framework.CommonUtilities;
import com.backcountry.selenium_framework.DriverTypeProvider;
import com.backcountry.selenium_framework.ScreenshotComparer;
import com.backcountry.selenium_framework.SimulatedCustomer;
import com.backcountry.selenium_framework.WebDriverFactory.DRIVER_TYPE;

@RunWith(JUnitParamsRunner.class)
public class ProductDetailPage_IT {
		
	@Test
	@Parameters(source = DriverTypeProvider.FunctionalityTests.class) //Runs with these driver types 
	public void verifyProductName(DRIVER_TYPE driverType){
		
		final String productId = "marmot-limelight-tent-2-person-3-season";
		
		SimulatedCustomer simulatedCustomer = SimulatedCustomer.build(driverType);
		
		try{
			ProductDetailPage.driveToThisPage(simulatedCustomer, productId);
			
			String expectedProductName = 
					"Marmot Limelight 2-Person 3-Season Tent with Footprint and Gear Loft";
		
			assertThat(ProductDetailPage.getProductName(simulatedCustomer))
				.as("Does the product detail page return the expected name "
						+ "for productId [" + productId + "]")
				.isEqualTo(expectedProductName);
		}
		finally{
			simulatedCustomer.quit();
		}
	}

	
	/**
	 * Screenshot individual elements when page changes too much
	 * Another option is to hide frequently changing elements then take screenshot 
	 * 
	 * @param driverType
	 */
	@Test
	@Parameters(source = DriverTypeProvider.ScreenshotTests.class) //Runs with these driver types 
	public void sampleProductDetailElementScreenshot(DRIVER_TYPE driverType){
		
		final String productId = "the-north-face-ultra-fastpack-mid-gtx-hiking-boot-mens";
		
		SimulatedCustomer simulatedCustomer = SimulatedCustomer.build(driverType);
		
		try{
			ProductDetailPage.driveToThisPage(simulatedCustomer, productId);
			
			WebElement techSpecsBox = 
					simulatedCustomer.getDriverWrapper().findElement(By.id("tech-specs"));
			
			ScreenshotComparer.takeElementScreenshotAndCompareAgainstGold(
					this.getClass(), 
					simulatedCustomer,
					techSpecsBox,
					CommonUtilities.getMethodName());
		}
		finally{
			simulatedCustomer.quit();
		}
	}
	
}
