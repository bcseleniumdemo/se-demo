package com.backcountry.selenium_framework;

import com.backcountry.selenium_framework.WebDriverFactory.DRIVER_TYPE;

/**
 * Class to manage lists of drivers used for tests   
 * 
 * Facilitates running same test with multiple drivers
 * 
 * @author dbogardus
 *
 */
public class DriverTypeProvider{
	
	public static class FunctionalityTests{
	
		public static Object[] provideDriverTypes(){
			return new Object[]
				{
					new Object[]{DRIVER_TYPE.PHANTOM_JS}, 
					new Object[]{DRIVER_TYPE.HTMLUNIT}
				};
		}
	}
	
	public static class ScreenshotTests{
		
		public static Object[] provideDriverTypes(){
			return new Object[]
				{
					//Some later versions of chrome have problems with 
					//screenshots at sauce labs
					new Object[]{DRIVER_TYPE.REMOTE_CHROME_28},  
					
					new Object[]{DRIVER_TYPE.REMOTE_FIREFOX_31}
				};
		}
	}
	
}	
