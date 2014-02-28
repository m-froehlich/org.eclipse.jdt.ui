/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *       Andrej Zachar <andrej@chocolatejar.eu> - Added the 'Bug Trace' feature (a failure's html based diff)
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestElementContainer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.util.DiffMatchPatch;
import org.eclipse.jdt.internal.junit.util.DiffMatchPatch.Diff;

/**
 * Shows pretty formatted failed tests.
 */
public class BugTrace {
	final static String NULL= "null"; //$NON-NLS-1$

	final static String EMPTY_STRING= "\"\""; //$NON-NLS-1$

	private Browser fFormattedMessage;

	private final TestRunnerViewPart fTestRunnerPart;

	static final String HTML_HEAD_CSS_BODY= "<!doctype html><html><head><style type=\"text/css\">" + //$NON-NLS-1$
			"body {font-size:0.7em; font-family: Verdana; } " + //$NON-NLS-1$
			"table {padding-left:0.5em;padding-bottom:0.5em;border-collapse:separate;border-spacing:0.3em;} " + //$NON-NLS-1$
			"th {text-align: left; color:black;} " + //$NON-NLS-1$
			"tr {border:1px solid gray;} " + //$NON-NLS-1$
			"td {vertical-align:top;} " + //$NON-NLS-1$
			".e, .a {color:gray;} " + //$NON-NLS-1$
			".a a {color:gray; text-decoration: none;} " + //$NON-NLS-1$
			"del {color:red;} " + //$NON-NLS-1$
			"ins {color:black; font-weight:bold;} " + //$NON-NLS-1$
			".err {color:#CC0033;} " + //$NON-NLS-1$
			".m {color: #0099FF;} " + //$NON-NLS-1$
			"tr:nth-child(2n+3) {background-color:#FFFFDD;} " + //$NON-NLS-1$
			"</style></head><body>"; //$NON-NLS-1$

	public BugTrace(Composite parent, final TestRunnerViewPart testRunnerPart) {
		this.fTestRunnerPart= testRunnerPart;
		try {
			fFormattedMessage= new Browser(parent, SWT.WRAP);
			fFormattedMessage.setFont(parent.getFont());
			fFormattedMessage.addLocationListener(new LocationAdapter() {
				@Override
				public void changing(LocationEvent event) {
					if (fFormattedMessage.isDisposed())
						return;

					if (event.location.contains("openTest#")) { //$NON-NLS-1$
						String testHash= StringUtils.substringBetween(event.location, "openTest#", "?"); //$NON-NLS-1$//$NON-NLS-2$
						openTest(testHash);
						event.doit= false;
					} else
						event.doit= true;
				}

			});
		} catch (SWTError e) {
			//browser is not available
			fFormattedMessage= null;
		}

	}

	private void openTest(String hashCodeOfTheTest) {
		int hash= NumberUtils.toInt(hashCodeOfTheTest, 0);
		TestElement[] allFailures= fTestRunnerPart.getAllFailures();
		for (TestElement testElement : allFailures) {
			if (testElement.hashCode() == hash) {
				openTest(testElement);
			}
		}
	}

	private void openTest(TestElement testElement) {
		OpenTestAction action;
		if (testElement instanceof TestSuiteElement) {
			action= new OpenTestAction(fTestRunnerPart, testElement.getTestName());
		} else if (testElement instanceof TestCaseElement) {
			TestCaseElement testCase= (TestCaseElement)testElement;
			action= new OpenTestAction(fTestRunnerPart, testCase);
		} else {
			throw new IllegalStateException(String.valueOf(testElement));
		}

		if (action.isEnabled())
			action.run();
	}

	/**
	 * Provide information about html browser instance
	 * 
	 * @return <code>true</code> if browser was instanced and we have the pretty html viewer.
	 */
	public boolean isAvailable() {
		return fFormattedMessage != null;
	}

	/**
	 * Shows a TestFailure
	 * 
	 * @param test the failed test
	 */
	public void showFailure(TestElement test) {
		StringBuilder b= new StringBuilder();
		if (test != null && test.getStatus().isErrorOrFailure()) {
			b.append(HTML_HEAD_CSS_BODY);
			b.append("<table>"); //$NON-NLS-1$

			b.append(generateHeader(test));
			b.append(getRows(test, isContainer(test)));

			b.append("</table></body></html>"); //$NON-NLS-1$
		}
		fFormattedMessage.setText(b.toString());

	}

	private String generateHeader(ITestElement test) {
		if (test instanceof ITestElementContainer) {
			return "<tr><th>" + JUnitMessages.CompareResultDialog_expectedLabel + //$NON-NLS-1$
					"</th><th>" + JUnitMessages.CompareResultDialog_actualLabel + //$NON-NLS-1$
					"</th><th>" + JUnitMessages.TestRunnerViewPart_label_test + //$NON-NLS-1$
					"</th></tr>"; //$NON-NLS-1$
		} else if (test instanceof ITestCaseElement) {
			return "<tr><th>" + JUnitMessages.CompareResultDialog_expectedLabel + //$NON-NLS-1$
					"</th><th>" + JUnitMessages.CompareResultDialog_actualLabel + //$NON-NLS-1$
					"</th></tr>"; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	private boolean isContainer(ITestElement test) {
		if (test instanceof ITestElementContainer) {
			return true;
		}
		return false;
	}

	private String getRows(ITestElement test, boolean includeTestMethodName) {
		StringBuilder b= new StringBuilder();
		if (test instanceof ITestElementContainer) {
			ITestElementContainer container= (ITestElementContainer)test;
			for (ITestElement child : container.getChildren()) {
				b.append(getRows(child, includeTestMethodName));
			}
		} else if (test instanceof ITestCaseElement) {
			b.append(getOneRow((ITestCaseElement)test, includeTestMethodName));
		}
		return b.toString();
	}

	/**
	 * Format a failed test to one HTML row
	 * 
	 * @param test instance of ITestCaseElement
	 * @param includeMethodName specify whether to include a last row with the method name
	 * @return HTML text
	 */
	private String getOneRow(ITestCaseElement test, boolean includeMethodName) {
		Result result= test.getTestResult(true);
		if (result != Result.FAILURE && result != Result.ERROR) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder b= new StringBuilder();
		b.append("<tr>"); //$NON-NLS-1$
		b.append(getExpectedAndActualColumns(test));
		if (includeMethodName) {
			b.append("<td><span class='m'>"); //$NON-NLS-1$
			b.append("<a href='openTest#"); //$NON-NLS-1$
			b.append(test.hashCode());
			b.append("?'>"); //$NON-NLS-1$
			b.append(test.getTestMethodName());
			b.append("</a>"); //$NON-NLS-1$
			b.append("</span></td> "); //$NON-NLS-1$
		}
		b.append("</tr>"); //$NON-NLS-1$
		return b.toString();
	}

	/**
	 * Generate a simple message for an unknown error
	 * 
	 * @param test element
	 * @return HTML string
	 */
	private String getUknownError(ITestElement test) {
		StringBuilder b= new StringBuilder();
		b.append("<td></td><td class='err'>"); //$NON-NLS-1$
		if (test.getFailureTrace() != null && test.getFailureTrace().getTrace() != null) {
			b.append(StringUtils.substringBetween(test.getFailureTrace().getTrace(), ":", "at")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			b.append(test);
		}
		b.append("</td>"); //$NON-NLS-1$
		return b.toString();
	}

	/**
	 * Create a part of row in the table representing the given test
	 * 
	 * @param test the primitive test element (without children)
	 * @return HTML TD string
	 */
	private String getExpectedAndActualColumns(ITestElement test) {
		Result result= test.getTestResult(true);
		if (result == Result.ERROR) {
			return getUknownError(test);
		}
		if (test instanceof TestElement) {
			TestElement testElement= (TestElement)test;
			if (!testElement.isComparisonFailure()) {
				return getUknownError(test);
			}
		}
		FailureTrace trace= test.getFailureTrace();
		StringBuilder b= new StringBuilder();

		b.append("<td><span class='e'>"); //$NON-NLS-1$
		b.append(StringUtils.replace(trace.getExpected(), "\n", "<br/>")); //$NON-NLS-1$//$NON-NLS-2$
		b.append("</span></td>"); //$NON-NLS-1$

		b.append("<td><span class='a'>"); //$NON-NLS-1$
		b.append("<a href='openTest#"); //$NON-NLS-1$
		b.append(test.hashCode());
		b.append("?'>"); //$NON-NLS-1$
		b.append(createPrettyHTMLDiff(trace));
		b.append("</a>"); //$NON-NLS-1$
		b.append("</span></td>"); //$NON-NLS-1$
		return b.toString();
	}


	/**
	 * Initial implementation of a pretty diff algorithm
	 * 
	 * @param trace that contains actual and expected value
	 * @return HTML enable text that contains ins and del tags
	 */
	private String createPrettyHTMLDiff(FailureTrace trace) {
		DiffMatchPatch diffMaker= new DiffMatchPatch(); //based on https://code.google.com/p/google-diff-match-patch/wiki/API
		if (NULL.equals(trace.getActual()) || NULL.equals(trace.getExpected()) || EMPTY_STRING.equals(trace.getActual())) {
			return "<ins class='err'>" + StringUtils.replace(trace.getActual(), "\n", "<br/>") + "</ins>"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		LinkedList<Diff> differences= diffMaker.diff_main(trace.getExpected(), trace.getActual(), false);
		return diffMaker.diff_prettyHtml(differences);
	}

	/**
	 * Clears the non-stack trace info
	 */
	public void clear() {
		fFormattedMessage.setText(""); //$NON-NLS-1$
	}

	public Control getComposite() {
		return fFormattedMessage;
	}

	public void dispose() {
		if (fFormattedMessage != null)
			fFormattedMessage.dispose();
	}

}
