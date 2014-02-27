/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *   Andrej Zachar <andrej@chocolatejar.eu> - Support for showing comparison based on an assertion exception message.
 *******************************************************************************/
package org.eclipse.jdt.internal.junit4.runner;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import org.eclipse.jdt.internal.junit.runner.FailedComparison;
import org.eclipse.jdt.internal.junit.runner.IListensToTestExecutions;
import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.TestReferenceFailure;

public class JUnit4TestListener extends RunListener {

	/**
	 * Delimiter for optional comparison message of failed test. This message is used to provide more useful information about the failure
	 * The same apply for {@link #END_DELIMITER}  {@link #ACTUAL_DELIMITER}
	 */
	private static final String EXPECTED_DELIMITER = "\n__EX__"; //$NON-NLS-1$

	private static final String ACTUAL_DELIMITER = "__AC__"; //$NON-NLS-1$

	private static final String END_DELIMITER = "__EN__"; //$NON-NLS-1$

	private static class IgnoredTestIdentifier extends JUnit4Identifier {
		public IgnoredTestIdentifier(Description description) {
			super(description);
		}

		@Override
		public String getName() {
			String name= super.getName();
			if (name != null)
				return MessageIds.IGNORED_TEST_PREFIX + name;
			return null;
		}
	}

	private static class AssumptionFailedTestIdentifier extends JUnit4Identifier {
		public AssumptionFailedTestIdentifier(Description description) {
			super(description);
		}

		@Override
		public String getName() {
			String name= super.getName();
			if (name != null)
				return MessageIds.ASSUMPTION_FAILED_TEST_PREFIX + name;
			return null;
		}
	}


	private final IListensToTestExecutions fNotified;

	public JUnit4TestListener(IListensToTestExecutions notified) {
		fNotified= notified;
	}

	@Override
	public void testStarted(Description plan) throws Exception {
		fNotified.notifyTestStarted(getIdentifier(plan, false, false));
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		testFailure(failure, false);
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		testFailure(failure, true);
	}

	private void testFailure(Failure failure, boolean assumptionFailed) {
		ITestIdentifier identifier= getIdentifier(failure.getDescription(), false, assumptionFailed);
		TestReferenceFailure testReferenceFailure;
		try {
			String trace= failure.getTrace();
			Throwable exception= failure.getException();
			String status= (assumptionFailed || exception instanceof AssertionError) ? MessageIds.TEST_FAILED : MessageIds.TEST_ERROR;
			FailedComparison comparison= null;
			if (exception instanceof junit.framework.ComparisonFailure) {
				junit.framework.ComparisonFailure comparisonFailure= (junit.framework.ComparisonFailure)exception;
				comparison= new FailedComparison(comparisonFailure.getExpected(), comparisonFailure.getActual());
			} else if (exception instanceof org.junit.ComparisonFailure) {
				org.junit.ComparisonFailure comparisonFailure= (org.junit.ComparisonFailure)exception;
				comparison= new FailedComparison(comparisonFailure.getExpected(), comparisonFailure.getActual());
			} else {
				comparison= getFailedComparisonFromExceptionMessage(exception.getMessage());
				if (comparison != null) {
					trace= removeOptionalMessageFromStackTrace(trace);
				}
			}

			testReferenceFailure= new TestReferenceFailure(identifier, status, trace, comparison);
		} catch (RuntimeException e) {
			StringWriter stringWriter= new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
			testReferenceFailure= new TestReferenceFailure(identifier, MessageIds.TEST_FAILED, stringWriter.getBuffer().toString(), null);
		}
		fNotified.notifyTestFailed(testReferenceFailure);
	}
	
	/**
	 * Remove the optional comparison message from the stack trace.
	 * @param stacktrace any stacktrace
	 * @return not null
	 */
	private String removeOptionalMessageFromStackTrace(String stacktrace) {
		String before= stacktrace.substring(0, stacktrace.indexOf(EXPECTED_DELIMITER));
		String after= stacktrace.substring(stacktrace.indexOf(END_DELIMITER) + END_DELIMITER.length());
		return  before + after;
	}

	/**
	 * Extract the optional comparison message from the exception's message.
	 * 
	 * @param exceptionMsg a message from exception
	 * @return null or no-null Object if success parsing optional comparison message
	 */
	private FailedComparison getFailedComparisonFromExceptionMessage(String exceptionMsg) {
		boolean hasOptionalAttachamentWithComparism= exceptionMsg != null && exceptionMsg.contains(EXPECTED_DELIMITER) && exceptionMsg.contains(ACTUAL_DELIMITER);

		if (hasOptionalAttachamentWithComparism) {

			int beginIndex= exceptionMsg.lastIndexOf(EXPECTED_DELIMITER) + EXPECTED_DELIMITER.length();
			int endIndex= exceptionMsg.indexOf(END_DELIMITER);

			String attachement= exceptionMsg.substring(beginIndex, endIndex);

			String[] expectedAndActual= attachement.split(ACTUAL_DELIMITER);
			return new FailedComparison(expectedAndActual[0], expectedAndActual[1]);
		}
		return null;
	}

	@Override
	public void testIgnored(Description plan) throws Exception {
		// Send message to listeners which would be stale otherwise
		ITestIdentifier identifier= getIdentifier(plan, true, false);
		fNotified.notifyTestStarted(identifier);
		fNotified.notifyTestEnded(identifier);
	}

	@Override
	public void testFinished(Description plan) throws Exception {
		fNotified.notifyTestEnded(getIdentifier(plan, false, false));
	}

	private ITestIdentifier getIdentifier(Description plan, boolean ignored, boolean assumptionFailed) {
		if (ignored)
			return new IgnoredTestIdentifier(plan);
		if (assumptionFailed)
			return new AssumptionFailedTestIdentifier(plan);
		return new JUnit4Identifier(plan);
	}
}
