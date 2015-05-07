package com.backcountry.selenium_framework;

import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.backcountry.selenium_framework.WebDriverFactory.DRIVER_TYPE;

import static com.backcountry.selenium_framework.CommonUtilities.*;

public class SimulatedCustomer {
	
	private final DRIVER_TYPE driverType;
	private final DriverWrapper driverWrapper;
	
	public static SimulatedCustomer build(DRIVER_TYPE driverType){
		return new SimulatedCustomer(driverType);
	}
	
	private SimulatedCustomer(DRIVER_TYPE driverType){
		this.driverType = driverType;
		this.driverWrapper = DriverWrapper.build(WebDriverFactory.getDriver(driverType));
	}
	
	/**
	 * Mostly used for custom coding for different drivers 
	 * 
	 * @return
	 */
	public DRIVER_TYPE getDriverType(){
		return driverType;
	}
		
	public DriverWrapper getDriverWrapper(){
		return driverWrapper;
	}
	
	public void quit(){
		driverWrapper.quit();
	}
	
	//Ensure driver is closed before GC 
	@Override
	protected void finalize() throws Throwable {
		try{
			driverWrapper.quit();
		}
		catch(Exception e){
			//Don't care if there was an exception
		}
	}
	
	/**
	 * Wrap the driver for futureproofing and keep inconsequential errors from stopping tests  
	 * 
	 * @author dbogardus
	 *
	 */
	public static class DriverWrapper implements WebDriver{

		private WebDriver driver;
		
		/**
		 * Expose actual driver to package only  
		 * 
		 * @return
		 */
		WebDriver getDriver(){
			return driver;
		}
		
		private DriverWrapper(WebDriver driver){
			this.driver = driver;
		}
		
		static DriverWrapper build(WebDriver driver){
			return new DriverWrapper(driver);
		}
		
		//Ensure driver is closed before GC 
		@Override
		protected void finalize() throws Throwable {
			try{
				driver.quit();
			}
			catch(Exception e){
				//Don't care if there was an exception
			}
		}
		
		@Override
		public void close() {
			
			try{
				driver.close();
			}
			catch(Exception e){
				//Don't let a closing problem kill a test
				outputln(this.getClass(), "Exception during driver.close() "
						+ "[" + e.getMessage() + "]");
			}
		}

		@Override
		public void quit() { 
			
			try{
				driver.quit();
			}
			catch(Exception e){
				//Don't let a closing problem kill a test
				outputln(this.getClass(), "Exception during driver.quit() "
						+ "[" + e.getMessage() + "]");
			}
			
		}
		
		//Pass through the rest of the behavior unchanged 
		
		@Override
		public WebElement findElement(By arg0) { return driver.findElement(arg0); }

		@Override
		public List<WebElement> findElements(By arg0) { return driver.findElements(arg0);}

		@Override
		public void get(String arg0) { driver.get(arg0); }

		@Override
		public String getCurrentUrl() { return driver.getCurrentUrl(); }
			
		@Override
		public String getPageSource() { return driver.getPageSource(); }
		
		@Override
		public String getTitle() { return driver.getTitle(); }

		@Override
		public String getWindowHandle() { return driver.getWindowHandle(); }

		@Override
		public Set<String> getWindowHandles() { return driver.getWindowHandles(); }
			
		@Override
		public Options manage() { return driver.manage(); }
			
		@Override
		public Navigation navigate() { return driver.navigate(); }
			

		@Override
		public TargetLocator switchTo() { return driver.switchTo(); }
		
	}
}


