package com.backcountry.selenium_framework;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;

/**
 * Utilities used across project 
 * 
 * @author dbogardus
 *
 */
public class CommonUtilities {

	private static final Class thisClass = CommonUtilities.class;

	public static String getBaseURI(){
		return "http://www.backcountry.com/";
	}	
	
	public static void closeEmailLightBoxIfPresent(SimulatedCustomer simulatedCustomer){
		WebDriver driver = simulatedCustomer.getDriverWrapper();
		
		final String emailLightBoxName = "bcx_125262_iframe_overlay";
		
		By emailIframeNameBy = By.name(emailLightBoxName);
		
		By emailCloseXBy = By.className("no_thanks");	
		
		if(doesWebElementExist(driver, emailIframeNameBy)){
			outputln(thisClass, "Closing Email Box");
			
			String parentWindowHandle = driver.getWindowHandle();
			
			driver.switchTo().frame(emailLightBoxName);
			try{
				driver.findElement(emailCloseXBy).click();
			}
			catch(Exception e){
				//Don't care if we have problems clicking the close
			}
			
			
			driver.switchTo().window(parentWindowHandle);
		}
	}
	
	public static void closeChatInviteIfPresent(SimulatedCustomer simulatedCustomer){
		WebDriver driver = simulatedCustomer.getDriverWrapper();
	
		
		By chatInvitePopupDiv = By.id("lpChatInvitation");
		
		if(doesWebElementExist(driver, chatInvitePopupDiv) &&
				driver.findElement(chatInvitePopupDiv).isDisplayed()){
			
			outputln(thisClass, "Closing Chat Box");
			
			try{
				//Only anchor tag is "x" to close
				driver.findElement(chatInvitePopupDiv).findElement(By.tagName("a")).click();
			}
			catch(Exception e){
				//Don't care if we have problems clicking the close
			}
			
		}
	}
	
	public static int getWaitTimeoutInSeconds(){
		return 10;
	}
	
	
	public static boolean doesWebElementExist(WebDriver driver, By by){
		
		return doesWebElementExistWithinParent(driver, driver, by);
	}

	public static boolean doesWebElementExistWithinParent(WebDriver driver,
			SearchContext parent,
			By by){

		try{
			//Set the timeout to zero so we don't wait for an element that might not be there
			WebDriverFactory.setWaitTimeout(driver, 0);
			if(parent.findElements(by).size() > 0){
				return true;
			}else{
				return false;
			}
		}
		catch(NoSuchElementException e){
			return false;
		}
		finally{
			//Set the timeout back to the default value
			WebDriverFactory.setWaitTimeoutToDefault(driver);
		}
	}

	
	public static void outputln(Class callingClass, String text){
		
		System.out.format("%-18s  %s\r\n", "<" + callingClass.getSimpleName() + ">", text);
		
	}


	public static void outputlnAndThrowRuntimeException(Class callingClass, String msg){
		outputln(callingClass, msg);

		throw new RuntimeException(msg);
	}


	public static void outputlnAndThrowRuntimeException(Class callingClass, String msg,
			Throwable causedByException){

		outputln(callingClass, msg);

		throw new RuntimeException(msg, causedByException);
	}

	public static void outputln(Class callingClass, String text, Throwable t){

		text = text + ". EXCEPTION: " + t.getMessage();

		System.out.println(formatClassName(callingClass) + text);
		
	}
	
	
	private static String formatClassName(Class callingClass){
		return callingClass.getSimpleName() + ": \t";
	}
	
	
	
	public static boolean isRunningThroughJenkins(){

		if(!StringUtils.isBlank(System.getProperty("jenkins.buildTag"))){
			return true;
		}

		return false;
	}

	
	public static class Timer{
		public final long startTime;
		public Timer(){
			startTime = System.currentTimeMillis();
		}

		public double getDurationInSeconds(){
			long currentTime = System.currentTimeMillis();

			double timeDifference = currentTime - startTime;

			return timeDifference/1000;
		}

		public long getDurationInMilliSeconds(){
			long currentTime = System.currentTimeMillis();

			return currentTime - startTime;
		}
	}


	/*
	 * Return the name of the calling method 
	 *  
	 */
	public static String getMethodName() {
	    return Thread.currentThread().getStackTrace()[2].getMethodName();
	}
	
	
	public static String getDirectoryInWhichthisTestExists(Class testClass){
		String baseTestDirString = null;
		String separator = System.getProperty("file.separator");

		List<String> possibleBaseTestDirStrings = new ArrayList<String>();

		possibleBaseTestDirStrings.add("test");
		possibleBaseTestDirStrings.add("src/test/java");

		for(String possibleBaseTestDirString : possibleBaseTestDirStrings){

			File possibleBaseTestDir = new File(possibleBaseTestDirString);

			if(		possibleBaseTestDir.exists() &&
					possibleBaseTestDir.isDirectory() &&
					possibleBaseTestDir.list().length>0){

				baseTestDirString = possibleBaseTestDirString;

			}
		}

		if(baseTestDirString == null){
			throw new RuntimeException("Can't find the base test directory, "
				+ "it's not any of " + Arrays.toString(possibleBaseTestDirStrings.toArray()));
		}

		 return 	baseTestDirString +
				 	separator +
				 	testClass.getPackage().toString()
				 		.replace(".", separator).replace("package ", "") +
				 	separator;
	}
}


