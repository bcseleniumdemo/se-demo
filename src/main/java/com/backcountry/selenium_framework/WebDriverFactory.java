package com.backcountry.selenium_framework;

import static com.backcountry.selenium_framework.CommonUtilities.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.backcountry.selenium_framework.CommonUtilities.Timer;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindowImpl;

/**
 * Class which manages all WebDriver instantiation
 *
 * Logic here determines of the requested driver is compatible with the specified
 * capabilities. If there is a mismatch, decide on the appropriate driver to use instead.
 *
 * @author dbogardus
 *
 */
public class WebDriverFactory {

	//This is my personal sauce labs account, feel free to keep using it or switch to your own
	private static final String sauceLabsURLString = 
			"http://dbogardus1:e8fd172b-1c3c-4835-964a-19383282d1c8@" +
			"ondemand.saucelabs.com:80/wd/hub";
	
	private static final Class thisClass = WebDriverFactory.class;


	//////////////////////////////////////////////////////////////////////////////////////////////
	//Each of these capabilities are lacking in one or more of the drivers.
	//Tests indicate if it they require a certain capability to run.
	//Then, if the test suite has been configured to run with a driver not having the capability,
	//we can use logic below to bail out of the test - or more commonly substitute a driver
	//that has the capability

	public enum BROWSER_CAPABILITIES{

		JAVASCRIPT,
		BACK_NAVIGATION,
		SCREENSHOT,
		SCROLLING,
		IFRAMES,
		REAL_BROWSER_NOT_HEADLESS,
		AJAX,

		//PhantomJS has a weird bug, this capability introduced for this scenario only
		//See : http://stackoverflow.com/questions/9941408/phantomjs-and-iframe
		IFRAMES_IN_DIFFERENT_DOMAIN,

	};
	///////////////////////////////////////////////////////////////////////////////////////////////




	///////////////////////////////////////////////////////////////////////////////////////////////
	//Types of WebDrivers Supported by this Factory

	public enum DRIVER_TYPE {
		
		//Headless Browsers, Phantom is newer with a better javascript engine
		HTMLUNIT, 
		PHANTOM_JS, 

		//Get the iPhone version of a site inside phantomJS
		PHANTOM_JS_IPHONE_EMULATION,

		//Local browsers that can run on a desktop PC
		LOCAL_CHROME, LOCAL_FIREFOX, LOCAL_IE, 

		//Browsers which run on external VM/Browser service (As of Feb 2015 still Sauce Labs).
		REMOTE_IE7,
		REMOTE_IE_8,
		REMOTE_IE_9,
		REMOTE_IE_10,
		REMOTE_OPERA,
		REMOTE_FIREFOX_31,
		REMOTE_CHROME_28,
		REMOTE_SAFARI,
		REMOTE_IPHONE,
		REMOTE_IPAD,
		REMOTE_ANDROID_PHONE,
		REMOTE_ANDROID_TABLET;
	}
	///////////////////////////////////////////////////////////////////////////////////////////////











	// END - Initial Enums and other static resources
	///////////////////////////////////////////////////////////////////////////////////////////////
	// BEGIN - Main method for created a driver, with logic to configure or substitute if
	//         capabilities are more than native behaviour of the requested driver












	public static WebDriver getDriver(DRIVER_TYPE driverTypeToCreate,
			BROWSER_CAPABILITIES... browserCapabilities) {

		List<BROWSER_CAPABILITIES> capabilities = Arrays.asList(browserCapabilities);

		WebDriver driver = null;

		Timer timer = new Timer();

		// Start up Message
		String startMessage = "WebDriver of type [" + driverTypeToCreate + "] requested";

		if(capabilities.size() > 0){
			startMessage += ", with capabilities : " + Arrays.toString(capabilities.toArray());
		}

		outputln(thisClass, startMessage);

		switch (driverTypeToCreate) {
		case LOCAL_CHROME:
			driver = new ChromeDriver();
			break;
		case LOCAL_FIREFOX:
			if(capabilities.contains(BROWSER_CAPABILITIES.BACK_NAVIGATION) &&
					!capabilities.contains(BROWSER_CAPABILITIES.AJAX)){

				outputlnAndThrowRuntimeException(thisClass,  "FirefoxDriver doesn't work with "
						+ "back navigation. Use Chrome or HTMLUnit");
			}

			driver = new FirefoxDriver();
			break;
		case HTMLUNIT:
			return getHtmlUnitDriver_OrSubstituteDriverToSupportCapabilities(browserCapabilities);

		case LOCAL_IE:
			driver = new InternetExplorerDriver();
			break;
		case REMOTE_CHROME_28:
			driver = getChromeSauceLabsDriver();
			break;
		case REMOTE_SAFARI:
			driver = getSafariSauceLabsDriver();
			break;
		case REMOTE_OPERA:
			driver = getOperaSauceLabsDriver();
			break;
		case REMOTE_FIREFOX_31:
			driver = getFirefoxSauceLabsDriver();
			break;
		case REMOTE_IE7:
			if(capabilities.contains(BROWSER_CAPABILITIES.IFRAMES)){
				outputlnAndThrowRuntimeException(thisClass, "IE7 doesn't work with iframes, "
						+ "choose another browser");
			}
			driver = getIE7_XP_SauceLabsDriver();
			break;
		case REMOTE_IE_8:
			driver = getIE8_SauceLabsDriver();
			break;
		case REMOTE_IE_9:
			driver = getIE9_SauceLabsDriver();
			break;
		case REMOTE_IE_10:
			driver = getIE10_Win8_SauceLabsDriver();
			break;
		case REMOTE_IPHONE:
			driver = getIPhone_SauceLabsDriver();
			break;
		case REMOTE_IPAD:
			if(capabilities.contains(BROWSER_CAPABILITIES.SCREENSHOT)){
				outputlnAndThrowRuntimeException(thisClass, "IPAD Screenshots don't work "
						+ "well with Sauce Labs, the sides of the image are cut off, "
						+ "support ticket opened with Sauce Labs on 5/6/2013.");
			}
			driver = getIPad_SauceLabsDriver();
			break;
		case REMOTE_ANDROID_PHONE:
			driver = getAndroidPhone_SauceLabsDriver();
			break;
		case REMOTE_ANDROID_TABLET:
			driver = getAndroidTablet_SauceLabsDriver();
			break;
		case PHANTOM_JS:

			if(capabilities.contains(BROWSER_CAPABILITIES.IFRAMES_IN_DIFFERENT_DOMAIN)){
				outputln(thisClass, "PhantomJS can't handle iframes in a different domain, "
						+ "getting a browser that can");

				return getDriver(DRIVER_TYPE.PHANTOM_JS_IPHONE_EMULATION,
						browserCapabilities);

			}else{

				driver = getPhantomJSDriver();
			}
			break;
		case PHANTOM_JS_IPHONE_EMULATION:

			driver = getPhantomJSDriver_IPhoneEmulator();
			break;
		default:
			break;
		}

		if (driver == null) {
			outputlnAndThrowRuntimeException(thisClass, "Couldn't setup webdriver for type [ " +
													driverTypeToCreate + "]");
		}

		outputln(thisClass, "Started WebDriver Type [" + driverTypeToCreate + "] in [" +
				timer.getDurationInSeconds() + "]s #####");

		return driver;
	}









	// END - Main driver factory method
	///////////////////////////////////////////////////////////////////////////////////////////////
	// BEGIN - Helper methods for factory










	private static WebDriver getHtmlUnitDriver_OrSubstituteDriverToSupportCapabilities(
			BROWSER_CAPABILITIES... browserCapabilities){

		List<BROWSER_CAPABILITIES> capabilities = Arrays.asList(browserCapabilities);

		if(capabilities.contains(BROWSER_CAPABILITIES.SCROLLING) ||
				capabilities.contains(BROWSER_CAPABILITIES.IFRAMES) ||
				capabilities.contains(BROWSER_CAPABILITIES.SCREENSHOT) ||
				capabilities.contains(BROWSER_CAPABILITIES.REAL_BROWSER_NOT_HEADLESS)){

			outputln(thisClass, "HTMLUNIT does not support requested features. "
					+ "Substituting a browser that does.");

			//Firefox works better than Chrome with angular.js
			return getDriver(DRIVER_TYPE.REMOTE_FIREFOX_31, browserCapabilities);
		}

		if(capabilities.contains(BROWSER_CAPABILITIES.AJAX)){

			//PhantomJS has a better Javascript engine, can handle AJAX
			outputln(thisClass, "HTMLUNIT doesn't support AJAX. Substituting a browser that does.");

			return getDriver(DRIVER_TYPE.PHANTOM_JS, browserCapabilities);
		}

		///////////////////////////////////////////////////////////////////////////////////
		// We dont' have any capabilities what HTMLUnit can't handle, now we just configure
		// the HTMLUnit driver itself

		WebDriver htmlUnitDriver = null;

		boolean enableJavascript = false;

		if(capabilities.contains(BROWSER_CAPABILITIES.JAVASCRIPT)){
			outputln(thisClass, "Enabling JavaScript in this HTMLUNIT Instance");
			enableJavascript = true;
		}

		//For back navigation we use a custom subclass of HTMLUnitDriver
		if(capabilities.contains(BROWSER_CAPABILITIES.BACK_NAVIGATION)){
			outputln(thisClass, "Enabling Back-Navigation in this HTMLUNIT Instance");
			htmlUnitDriver = new PageCachingHtmlUnitDriver(enableJavascript);
		}else{
			htmlUnitDriver = new CustomHtmlUnitDriver(enableJavascript);
		}


		return htmlUnitDriver;
	}


	private static WebDriver getPhantomJSDriver(){

		return getPhantomJSDriverWithUserAgent(null);
	}


	private static WebDriver getPhantomJSDriver_IPhoneEmulator(){

		return getPhantomJSDriverWithUserAgent(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X; en-us) "
				+ "AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 "
				+ "Mobile/11A465 Safari/9537.53");
	}

	private static WebDriver getPhantomJSDriverWithUserAgent(String userAgent){

		DesiredCapabilities phantomJScaps = new DesiredCapabilities();

		phantomJScaps.setJavascriptEnabled(true);

		if(!StringUtils.isBlank(userAgent)){
			phantomJScaps.setCapability("phantomjs.page.settings.userAgent", userAgent);
		}

		//See cli args at
		//https://github.com/detro/ghostdriver/blob/master/binding/java/src/main/java/org/openqa/selenium/phantomjs/PhantomJSDriverService.java
		//http://phantomjs.org/api/command-line.html
		phantomJScaps.setCapability("phantomjs.cli.args",
				new String[] {"--debug=false" , "--webdriver-loglevel=NONE",
				"--ignore-ssl-errors=true", 
				"--ssl-protocol=tlsv1", //sslv3 patch blocks all PhantomJS connections
				"--load-images=true", 
				"--proxy-type=none" //Attempting to resolve slow performance on windows
				}
		);

		return new PhantomJSDriver(phantomJScaps);
	}


	

	// END - Helper Methods For Driver Factory
	///////////////////////////////////////////////////////////////////////////////////////////////
	// BEGIN - Public Methods for manipulating driver timeouts









	public static void setWaitTimeoutToDefault(WebDriver driver) {
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}

	
	public static void setWaitTimeout(WebDriver driver, int seconds) {
		driver.manage().timeouts().implicitlyWait(seconds, TimeUnit.SECONDS);
	}









	// END - Public Methods for manipulating driver timeouts
	///////////////////////////////////////////////////////////////////////////////////////////////
	// BEGIN - Sauce Labs Driver Creation Methods











	///////////////////////////////////////////////////////////////////////////////////////
	//
	private static final int SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS = 5 * 60;
	//
	//Increased total job length to 6 minutes on 9/20/14 as some of the GoRec WS2.0 tests
	//were running longer than the previous 3 minute length.
	//Decreased to 5 min on 2/13/2015 to limit damage for hung jobs
	///////////////////////////////////////////////////////////////////////////////////////


	//Decreased to 5 min on 2/13/2015 to limit damage for hung jobs
	private static final int SAUCE_LABS_MAX_IDLE_TIME_IN_SECONDS = 90;


	private static WebDriver getChromeSauceLabsDriver() {
		DesiredCapabilities capabilities = DesiredCapabilities.chrome();
		capabilities.setCapability("platform", Platform.WINDOWS);
		capabilities.setCapability("name", "Chrome");
		capabilities.setCapability("version", "28");
		return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}

	private static WebDriver getOperaSauceLabsDriver() {
		DesiredCapabilities capabilities = DesiredCapabilities.opera();
		capabilities.setCapability("version", "11");
		capabilities.setCapability("platform", Platform.LINUX);
		capabilities.setCapability("name", "Opera on linux");
	 	return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}

	private static WebDriver getIE7_XP_SauceLabsDriver() {
		String ieVersion = "7";
		DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
		capabilities.setCapability("version", ieVersion);
		capabilities.setCapability("platform", Platform.XP);
		capabilities.setCapability("name", "IE " + ieVersion + " on XP");
		return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}

	private static WebDriver getIE9_SauceLabsDriver() {
		String ieVersion = "9";
		DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
		capabilities.setCapability("version", ieVersion);
		capabilities.setCapability("platform", Platform.VISTA);
		capabilities.setCapability("name", "IE " + ieVersion);
		return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}

	private static WebDriver getIE10_Win8_SauceLabsDriver() {
		String ieVersion = "10";
		DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
		capabilities.setCapability("version", ieVersion);
		capabilities.setCapability("platform", "Windows 8");
		capabilities.setCapability("name", "IE " + ieVersion + " on Windows 8");
		return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}

	private static WebDriver getIE8_SauceLabsDriver() {
		String ieVersion = "8";
		DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
		capabilities.setCapability("version", ieVersion);
		capabilities.setCapability("platform", "Windows 7");
		capabilities.setCapability("name", "IE " + ieVersion + " on Windows 7");
		return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}

	private static WebDriver getSafariSauceLabsDriver() {
		DesiredCapabilities capabilities = DesiredCapabilities.safari();
		capabilities.setCapability("platform", "OS X 10.6");
		capabilities.setCapability("version", "5");
		capabilities.setCapability("name", "Safari 5");
		return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}

	private static WebDriver getIPhone_SauceLabsDriver() {
		String iosVersion = "6";
		DesiredCapabilities capabilities = DesiredCapabilities.iphone();
		capabilities.setCapability("version", iosVersion);
		capabilities.setCapability("platform", "OS X 10.8");
		capabilities.setCapability("name", "IOS version " + iosVersion + " on IPhone");
		return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}


	private static WebDriver getIPad_SauceLabsDriver() {
		String iosVersion = "6";
		DesiredCapabilities capabilities = DesiredCapabilities.ipad();
		capabilities.setCapability("version", iosVersion);
		capabilities.setCapability("platform", "Mac 10.8");
		capabilities.setCapability("name", "IOS version " + iosVersion + " on IPad");
		return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}

	private static WebDriver getAndroidPhone_SauceLabsDriver() {
		String osVersion = "4";
		DesiredCapabilities capabilities = DesiredCapabilities.android();
		capabilities.setCapability("version", osVersion);
		capabilities.setCapability("platform", "Linux");
		capabilities.setCapability("name", "Andriod version " + osVersion + " on Android Phone");
		return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}

	private static WebDriver getAndroidTablet_SauceLabsDriver() {
		String osVersion = "4";
		DesiredCapabilities caps = DesiredCapabilities.android();
		caps.setCapability("platform", "Linux");
		caps.setCapability("version", "4.4");
		caps.setCapability("deviceName", "Samsung Galaxy S4 Emulator");
		caps.setCapability("device-orientation", "portrait");
		caps.setCapability("name", "Andriod version " + osVersion + " on Android Phone");
		return getSauceLabsDriver(caps, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}


	private static WebDriver getFirefoxSauceLabsDriver() {
		DesiredCapabilities capabilities = DesiredCapabilities.firefox();
		capabilities.setCapability("version", "31");
		capabilities.setCapability("platform", Platform.VISTA);
		capabilities.setCapability("name", "Firefox on Windows 7");

		///////////////////////////////////////////////////////////////////////////////////////
		// Fix for "native elements" error
		// Per -
		// http://support.saucelabs.com/entries/21400576-invalidelementstate-could-not-load-native-events-component
		FirefoxProfile prof = new FirefoxProfile();
		prof.setEnableNativeEvents(false);
		capabilities.setCapability("firefox_profile", prof);
		////////////////////////////////////////////////////////////////////////////////////////

		return getSauceLabsDriver(capabilities, SAUCE_LABS_MAX_JOB_RUNTIME_IN_SECONDS);
	}

	private static WebDriver getSauceLabsDriver(DesiredCapabilities capabilities,
			int sauceLabsTimeOutInSeconds) {
		try {

			URL sauceLabsURL = new URL(sauceLabsURLString);
			
			// Avoid runaway tests with this timeout that could burn sauce labs minutes
			capabilities.setCapability("max-duration", Integer.valueOf(sauceLabsTimeOutInSeconds));
			capabilities.setCapability("idle-timeout",
					Integer.valueOf(SAUCE_LABS_MAX_IDLE_TIME_IN_SECONDS));
			capabilities.setCapability("idleTimeout",
					Integer.valueOf(SAUCE_LABS_MAX_IDLE_TIME_IN_SECONDS));

			WebDriver driver = new RemoteWebDriver(sauceLabsURL, capabilities);

			//Added this to fight chronic sauce labs timeouts
			driver.manage().timeouts().implicitlyWait(sauceLabsTimeOutInSeconds, TimeUnit.SECONDS);

			//Augmenter needed for remote driver to support screenshots
			return new Augmenter().augment(driver);

		} catch (MalformedURLException e) {

			throw new IllegalStateException("Problem with Sauce Labs URL", e);
		}
	}




	// END - Sauce Labs Creation Methods
	///////////////////////////////////////////////////////////////////////////////////////////////




}










// END - WebDriverFactory Class
///////////////////////////////////////////////////////////////////////////////////////////////
// BEGIN - Classes to Support HTMLUnit Customization



















// END - FilteringWebConnection class to support canceling certain Http requests by HtmlUnitDriver
///////////////////////////////////////////////////////////////////////////////////////////////
// BEGIN - Custom HTMLUnitDriver Class












class CustomHtmlUnitDriver extends HtmlUnitDriver{

	@SuppressWarnings("unused")
	private static final Class thisClass = CustomHtmlUnitDriver.class;

	static{
		//This class spews worthless error messages when javascript is enabled,
		//especially on the delivery queue page because of some jquery.
		//Made for distracting output in Jenkins.
		//Also set the commons-logging.properties to have no logger to stop other messages
	    Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.StrictErrorReporter")
	    		.setLevel(Level.OFF);

	    Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.host.ActiveXObject")
	    		.setLevel(Level.OFF);

	    Logger.getLogger("com.gargoylesoftware.htmlunit.IncorrectnessListenerImpl")
	    		.setLevel(Level.OFF);

	    Logger.getLogger("com.gargoylesoftware.htmlunit.html.HtmlScript")
	    		.setLevel(Level.OFF);
	}

	CustomHtmlUnitDriver(boolean enableJavascript){
		super(enableJavascript);

		if(enableJavascript){
			//To help with review site AJAX loading challenges
			//From http://htmlunit.sourceforge.net/faq.html#AJAXDoesNotWork
			super.getWebClient().setAjaxController(new NicelyResynchronizingAjaxController());
		}

		///////////////////////////////////////////////////////////////////////////////////////////
		//Page loading timeout value
		//
		super.getWebClient().getOptions().setTimeout(120 * 1000);
		//
		//Update 9/4/2013.
		//Increased this from 5s to 120s on early sept 2013, was getting lots of timeouts
		//when testing report app pages.
		///////////////////////////////////////////////////////////////////////////////////////////

		//To resolve SSLPeerUnverifiedException exception in Jenkins during first selenium load test
		super.getWebClient().getOptions().setUseInsecureSSL(true);

		//Attempting to resolve CSS error storm showing in console
		super.getWebClient().getOptions().setCssEnabled(true);
		super.getWebClient().setCssErrorHandler(new SilentCssErrorHandler());


		//Not sure if this is needed, but it sounds good to have.
		super.getWebClient().getOptions().setThrowExceptionOnScriptError(false);
	}

	//To allow review site to wait for javascript to finish
	//From http://htmlunit.sourceforge.net/faq.html#AJAXDoesNotWork
	public void waitForAllJavaScriptToFinish(){
		super.getWebClient().waitForBackgroundJavaScriptStartingBefore(10000);
	}

	/**
	 * Overriding quit to do some extra logic before closing the driver
	 *
	 */
	@Override
	public void quit(){

		try{
			//In Jprofile was seeing a lot of Javascript resources hanging around,
			//ensuring that the memory hungry javascript executor gets shut down.
			super.getWebClient().getJavaScriptEngine().shutdown();
		}
		catch(Exception e){}

		try{
			//Per http://htmlunit.sourceforge.net/faq.html#MemoryLeak, close all windows
			super.getWebClient().closeAllWindows();
		}
		catch(Exception e){}

		try{
			super.quit();
		}
		catch(Exception e){}

	}

}












// END - Customized HTMLUnit subclass for internal use
///////////////////////////////////////////////////////////////////////////////////////////////
// BEGIN - Further Customized HTMLUnit subclass to support back navigation in WebSurveys



/**
 * Created to support back navigation in surveys with HtmlUnit
 *
 * @author dbogardus
 *
 */
class PageCachingHtmlUnitDriver extends CustomHtmlUnitDriver{

	private Stack<Page> pageCache = new Stack<Page>();

	PageCachingHtmlUnitDriver(boolean enableJavascript){
		super(enableJavascript);
	}

	public void purgePageCache(){
		pageCache = new Stack<Page>();
	}

	public void cacheCurrentPageForFutureBackNav(){
		WebClient webClient = super.getWebClient();

		WebWindowImpl loadedWindow = (WebWindowImpl)webClient.getCurrentWindow();

		Page pageToCache = loadedWindow.getEnclosedPage();

		if(pageCache.size() > 10){
			pageCache.setSize(5);
		}

		pageCache.push(pageToCache);
	}

	public void cacheBack(){
		if(pageCache.size() > 0){
			outputln(this.getClass(), "### Going back in page history ###");
			super.getWebClient().getCurrentWindow().setEnclosedPage(pageCache.pop());
		}
	}
}










//END - Further Customized HTMLUnit subclass to support back navigation in WebSurveys
///////////////////////////////////////////////////////////////////////////////////////////////











