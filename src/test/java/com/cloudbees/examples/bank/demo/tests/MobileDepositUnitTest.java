package com.cloudbees.examples.bank.demo.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.experimental.categories.Category;

import com.cloudbees.examples.bank.demo.categories.UnitTest;

/**
 * Unit test for simple App.
 */
@Category(UnitTest.class)
public class MobileDepositUnitTest extends TestCase {
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public MobileDepositUnitTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(MobileDepositUnitTest.class);
	}

	/**
	 * Rigorous Test :-)
	 */
	public void testApp() {
		assertTrue(true);
	}
}
