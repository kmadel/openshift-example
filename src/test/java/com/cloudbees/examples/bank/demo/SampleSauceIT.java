package com.cloudbees.examples.bank.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.junit.SauceOnDemandTestWatcher;

/**
 * Demonstrates how to write a JUnit test that runs tests against Sauce Labs
 * using multiple browsers in parallel.
 * <p/>
 * The test also includes the {@link SauceOnDemandTestWatcher} which will invoke
 * the Sauce REST API to mark the test as passed or failed.
 * 
 * @author Andy Pemberton
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebIntegrationTest({"server.port:0", "api.proto:http", "api.host:bank-api.beedemo.net", "api.port:8080"})
@SpringApplicationConfiguration(classes = com.cloudbees.examples.bank.demo.App.class)
public class SampleSauceIT implements SauceOnDemandSessionIdProvider {
	
	/**
	 * Constructs a {@link SauceOnDemandAuthentication} instance using the
	 * supplied user name/access key. To use the authentication supplied by
	 * environment variables or from an external file, use the no-arg
	 * {@link SauceOnDemandAuthentication} constructor.
	 */
	public SauceOnDemandAuthentication authentication = new SauceOnDemandAuthentication(
			"cb_cloudbees-sa", "0ce882ef-06c4-4439-8b50-09027e4dd6d7");

	/**
	 * JUnit Rule which will mark the Sauce Job as passed/failed when the test
	 * succeeds or fails.
	 */
	@Rule
	public SauceOnDemandTestWatcher resultReportingTestWatcher = new SauceOnDemandTestWatcher(
			this, authentication);
	
	@Rule public TestName name = new TestName();
	
	@Autowired
    EmbeddedWebApplicationContext server;

    /** 
    * Dynamic port
    */
    @Value("${local.server.port}")
    int port;


	/**
	 * Represents the browser to be used as part of the test run.
	 */
	private String browser;
	/**
	 * Represents the operating system to be used as part of the test run.
	 */
	private String os;
	/**
	 * Represents the version of the browser to be used as part of the test run.
	 */
	private String version;
	/**
	 * Instance variable which contains the Sauce Job Id.
	 */
	private String sessionId;

	/**
	 * The {@link WebDriver} instance which is used to perform browser
	 * interactions with.
	 */
	private WebDriver driver;

	/**
	 * Constructs a new instance of the test. The constructor requires three
	 * string parameters, which represent the operating system, version and
	 * browser to be used when launching a Sauce VM. The order of the parameters
	 * should be the same as that of the elements within the
	 * {@link #browsersStrings()} method.
	 * 
	 * @param os
	 * @param version
	 * @param browser
	 */
	// public SampleSauceTest(String os, String version, String browser) {
	// super();
	// this.os = os;
	// this.version = version;
	// this.browser = browser;
	// }

	public SampleSauceIT() {
	}

	/**
	 * @return a LinkedList containing String arrays representing the browser
	 *         combinations the test should be run against. The values in the
	 *         String array are used as part of the invocation of the test
	 *         constructor
	 */
	// @Parameters
	// public static List<String[]> browsersStrings() {
	// LinkedList<String[]> browsers = new LinkedList<String[]>();
	// browsers.add(new String[] { "Windows 8.1", "11", "internet explorer" });
	// browsers.add(new String[] { "OSX 10.8", "6", "safari" });
	// return browsers;
	// }

	/**
	 * Constructs a new {@link RemoteWebDriver} instance which is configured to
	 * use the capabilities defined by the {@link #browser}, {@link #version}
	 * and {@link #os} instance variables, and which is configured to run
	 * against ondemand.saucelabs.com, using the username and access key
	 * populated by the {@link #authentication} instance.
	 * 
	 * @throws Exception
	 *             if an error occurs during the creation of the
	 *             {@link RemoteWebDriver} instance.
	 */
	@Before
	public void setUp() throws Exception {
		//this.browser = "safari";
		//this.version = "6";
		//this.os = "OSX 10.8";

		DesiredCapabilities capabilities = new DesiredCapabilities();
		// capabilities.setCapability(CapabilityType.BROWSER_NAME, browser);
		//if (version != null) {
		//	capabilities.setCapability(CapabilityType.VERSION, version);
		//}
		//capabilities.setCapability(CapabilityType.PLATFORM, os);
		capabilities.setCapability("name", "Mobile Deposit: " + name.getMethodName());
		this.driver = new RemoteWebDriver(new URL("http://"
				+ authentication.getUsername() + ":"
				+ authentication.getAccessKey()
				+ "@ondemand.saucelabs.com:80/wd/hub"), capabilities);

		this.sessionId = (((RemoteWebDriver) driver).getSessionId()).toString();

	}

	/**
	 * Make sure the page has an account number
	 * 
	 * @throws Exception
	 */
	@Test
	public void hasAnAccountNumber() throws Exception {
		driver.get("http://localhost:" + port + "/deposit"); // TODO parameterize
		assertNotNull(driver.findElement(By.className("account-number")));
	}

	@Test
	public void hasMaskedAccountNumber() throws Exception {
		driver.get("http://localhost:" + port + "deposit"); // TODO parameterize
		WebElement accountNumber = driver.findElement(By
				.className("account-number"));
		assertTrue("Account Number must end and only contain 4 digits!",
				Pattern.matches("([^\\d]*)([\\d]{4})", accountNumber.getText()));
	}

	/**
	 * Closes the {@link WebDriver} session.
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		driver.quit();
	}

	/**
	 * 
	 * @return the value of the Sauce Job id.
	 */
	@Override
	public String getSessionId() {
		return sessionId;
	}
}
