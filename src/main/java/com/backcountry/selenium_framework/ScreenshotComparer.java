package com.backcountry.selenium_framework;

import static com.backcountry.selenium_framework.CommonUtilities.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.backcountry.selenium_framework.SimulatedCustomer.DriverWrapper;
import com.backcountry.selenium_framework.WebDriverFactory.BROWSER_CAPABILITIES;
import com.backcountry.selenium_framework.WebDriverFactory.DRIVER_TYPE;

public class ScreenshotComparer {
	
	private final static Class thisClass =  ScreenshotComparer.class;
	private final static String separator = System.getProperty("file.separator");		
	
	//Gold files will be stored in a subdirectory where the calling class is located
	private static String getGoldFilePath(Class testClass){
		
		return getDirectoryInWhichthisTestExists(testClass) + "SCREENSHOTS" + separator;
	}
	
	
	private static File findGoldImageFile(Class testClass, 
			String screenshotName,
			DRIVER_TYPE browserUsedForScreenshot){
		
		String uniqueFileName = StringUtils.replace(screenshotName," ","_") + "-" + browserUsedForScreenshot.name();
		
		return new File(getGoldFilePath(testClass) + uniqueFileName + ".png");
	}
		
	
	public interface WebDriverSteps{
		void driveToTargetPage(WebDriver driver);
	}
	
	public interface WebDriverStepsToElement{
		WebElement findTargetElement(WebDriver driver);
	}
	
	
	public static void compareWebElementScreenshotAgainstGold(
			Class callingClass, 
			WebDriverStepsToElement driverStepsToFindElement,
			String screenshotName, 
			DRIVER_TYPE browserToTest){
					
		WebDriver driver = WebDriverFactory.getDriver(browserToTest, 
					BROWSER_CAPABILITIES.SCREENSHOT);
		
		try{
		    WebElement elementToCompareAgainstGold = 
		    		driverStepsToFindElement.findTargetElement(driver);
			    
		    BufferedImage newScreenShot = takeElementScreenShot(driver, 
		    		elementToCompareAgainstGold);
			    
		    findGoldImageAndCompare(callingClass, newScreenShot, 
		    		screenshotName, browserToTest);
		}
		finally{
			driver.quit();
		}
	}
	
	
	public static void compareScreenshotAgainstGold(
			Class callingClass, 
			WebDriverSteps driverStepsToReachScreenshotPage,
			String screenshotName, 
			DRIVER_TYPE browserToTest){
				
		WebDriver driver = WebDriverFactory.getDriver(browserToTest, 
				BROWSER_CAPABILITIES.SCREENSHOT);
					   	
		try{
		    driverStepsToReachScreenshotPage.driveToTargetPage(driver);
		    
		    BufferedImage newScreenShot = takeFullScreenshot(driver);
			    
		    findGoldImageAndCompare(callingClass, newScreenShot,  
		    		screenshotName, browserToTest);
		}
		finally{
			driver.quit();
		}
	}
	
	public static void takeElementScreenshotAndCompareAgainstGold(
			Class callingClass, 
			SimulatedCustomer simulatedCustomer,
			WebElement webElement,
			String screenshotName){	
		
		BufferedImage newScreenShot = takeElementScreenShot(simulatedCustomer.getDriverWrapper(), 
				webElement);
			    
		findGoldImageAndCompare(callingClass, newScreenShot, screenshotName, 
				simulatedCustomer.getDriverType());
	}
	
	
	public static void takeScreenshotAndCompareAgainstGold(
			Class callingClass, 
			SimulatedCustomer simulatedCustomer,
			String screenshotName){
				
		BufferedImage newScreenShot = takeFullScreenshot(simulatedCustomer.getDriverWrapper());
			    
		findGoldImageAndCompare(callingClass, newScreenShot,  
		    		screenshotName, simulatedCustomer.getDriverType());
	}
	
	
	private static void findGoldImageAndCompare(
			Class callingClass, 
			BufferedImage newImage,
			String screenshotName, 
			DRIVER_TYPE browserBeingTested){

		//Gold image file is looked up by naming convention. 

		//If it's not located, this screenshot is assumed to be a gold candidate and written
		//to disk as the new gold file. 

		//The idea being that someone would manually inspect the gold file and check it 
		//into SVN to confirm it should be used. 

		File goldImageFile = 
				findGoldImageFile(callingClass, screenshotName, browserBeingTested);

		if(goldImageFile.exists()){
			outputln(callingClass, 
					"Gold Screenshot exists [" + goldImageFile.getAbsolutePath() + 
					"], running comparison");

			compareImageAgainstGold(goldImageFile, newImage, callingClass,
					browserBeingTested);

		}else{
			saveImage(goldImageFile, newImage);
			
			String message = "Gold image did not exist, " +
					"assuming this is a gold candidate shot. " +  
					"Written to [" + goldImageFile.getAbsolutePath() + "]";
			
			outputln(callingClass, message);

			throw new GoldImageDoesntExistException();
		}
	}
	
	
	public static class GoldImageDoesntExistException extends RuntimeException{
		private static final long serialVersionUID = 1L;
	};
	
	
	private static void compareImageAgainstGold(File goldImageFile, BufferedImage newImage, 
			Class callingClass, DRIVER_TYPE browserBeingTested){
		
		if(goldImageFile == null || !goldImageFile.exists()){
			throw new RuntimeException("Gold Screenshot file does not exist");
		}
		
		outputln(thisClass, "Comparing gold shot [" + goldImageFile.getName() + 
				"] against new image");
		
		BufferedImage goldImage = getImage(goldImageFile);
    	BufferedImage deltaImage = getDeltaImage(goldImage, newImage);
    	
    	if(deltaImage != null){
    		//There was a difference in the images
    		outputln(callingClass, 
	    			"Image difference detected when comparing against [" + 
	    					goldImageFile.getAbsolutePath() + "]");			    					    			
    		
    		throw new AssertionError( 
    				"Difference found during screenshot comparison in browser " +
    				"[" + browserBeingTested.name() + "]." +
    				"\nDelta image here : " + 
    				saveImageAndReturnLocation(goldImageFile, deltaImage, SCREENSHOT_TYPE.DELTA) +
    				"\nGold image here: " + 
    				saveImageAndReturnLocation(goldImageFile, goldImage, SCREENSHOT_TYPE.GOLD) +
		    		"\nNew/Changed image here : " + 
		    		saveImageAndReturnLocation(goldImageFile, newImage, SCREENSHOT_TYPE.NEW));
            
    	}else{
    		outputln(callingClass, "No image differences when comparing against gold shot " + 
    							"[" + goldImageFile.getName() + "]");
    	}
	}
	
	enum SCREENSHOT_TYPE{
		GOLD("GOLD"),
		DELTA("DIFF_FROM_GOLD_IMAGE"),
		NEW("NEW_SHOT_THAT_WAS_DIFFERENT");
		
		SCREENSHOT_TYPE(String fileSuffix){
			this.fileSuffix = fileSuffix;
		}
		
		public final String fileSuffix;
	}
	
	private static String generateImageNameForScreenShot(File originalGoldScreenShotFile, 
			SCREENSHOT_TYPE screenShotType){
		
		return originalGoldScreenShotFile.getName().replace(
				".png", "." + screenShotType.fileSuffix + "." + 
				getFileTimeStampSuffix() + ".png");
	}
	
	private static String getFileTimeStampSuffix(){
		return new SimpleDateFormat("MMdd_HHmm_ss").format(new Date());
	}
	
	private static String saveScreenshotToLocalDisk(File originalGoldScreenShotFile, 
			BufferedImage newImage, SCREENSHOT_TYPE screenShotType){
		
		String fileName = originalGoldScreenShotFile.getAbsolutePath().replace(
				originalGoldScreenShotFile.getName(), 
					separator + "TestFailures" + separator + 
					generateImageNameForScreenShot(
							originalGoldScreenShotFile, screenShotType));
		
		File imageFile = new File(fileName);
		
		saveImage(imageFile, newImage);
		
		return imageFile.getAbsolutePath();
		
	}
	
	
	private static BufferedImage getDeltaImage(BufferedImage goldImage, BufferedImage newImage){
		return ImageCompare.getImageDifferences(goldImage, newImage); 
	}
	
	private static BufferedImage getImage(File imageFile){
		
		try {
			return ImageIO.read(imageFile);
		} 
		 catch (IOException e) {	
				throw new IllegalStateException("Got an exception while reading "
						+ "file, filename [" + imageFile.getName() + "]" , e);
		}
	}
	
	
	/**
	 * If this utility is being used within a maven execution, create an http link so
	 * the images are accessible through the jenkins console output. 
	 * 
	 * If not in a maven context, save the images locally. 
	 * 
	 * @param imageName
	 * @param image
	 * @return String with location of the saved files 
	 */
	private static String saveImageAndReturnLocation(File goldScreenShotFile, BufferedImage image, 
			SCREENSHOT_TYPE screenshotType){

		if(CommonUtilities.isRunningThroughJenkins()){

			String imageName = generateImageNameForScreenShot(goldScreenShotFile, 
					screenshotType);

			return uploadForURLAccess(imageName, image);

		}else{
			return saveScreenshotToLocalDisk(goldScreenShotFile, image, screenshotType);
		}

	}
	
	private static void saveImage(File outputFile, BufferedImage image){
	
		File dir = new File(outputFile.getParent());
		
		if(!dir.exists()){
			dir.mkdirs();
			outputln(thisClass, "Created Directory:" + dir.getAbsolutePath());
		}
		
		try {
			ImageIO.write(image, "PNG", outputFile);
		} 
		 catch (IOException e) {	
				throw new IllegalStateException("Got an exception while writing "
						+ "file to disk, filename [" + outputFile.getName() + "]" , e);
		}
	}
		
	private static BufferedImage takeFullScreenshot(WebDriver driver){
		
		if(driver instanceof DriverWrapper){
			//Need the actual driver here 
			driver = ((DriverWrapper)driver).getDriver();			
		}
		
		try {
			BufferedImage fullScreenshot = 
					ImageIO.read(((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE));
				
			return fullScreenshot;
			
		} catch (IOException e) {
			throw new IllegalStateException("Got an exception while taking screenshot", e);
		}
	}
	
	
	public static BufferedImage takeElementScreenShot(WebDriver driver, 
			WebElement elementToScreenShot){
		
		int elementWidth = elementToScreenShot.getSize().getWidth();
		int elementHeight = elementToScreenShot.getSize().getHeight();
		
		outputln(thisClass, "Taking Element Screenshot, "
				+ "size [" + elementWidth + "x" + elementHeight + "]");
		
		Point elementUpperLeftCorner = elementToScreenShot.getLocation();

		BufferedImage fullScreenshot = takeFullScreenshot(driver);

		BufferedImage elementScreenShot = 
				fullScreenshot.getSubimage(
						elementUpperLeftCorner.x, 
						elementUpperLeftCorner.y, 
						elementWidth, 
						elementHeight);	

		return elementScreenShot;

	}
		
	//Returns the URL from which the file can be accessed
	private static String uploadForURLAccess(String imageName, BufferedImage image) {
	
		throw new NotImplementedException();
		
	}		
	
	
	//Will be used to publish to external service
	@SuppressWarnings("unused")
	private static InputStream getInputStreamfromImage(BufferedImage image) throws IOException{
	
		final ByteArrayOutputStream output = new ByteArrayOutputStream() {
	    	@Override
	    	public synchronized byte[] toByteArray() {
	    		return this.buf;
	    	}
		};
		
		ImageIO.write(image, "png", output);
		
		return new ByteArrayInputStream(output.toByteArray(), 0, output.size());
	}
	
}
