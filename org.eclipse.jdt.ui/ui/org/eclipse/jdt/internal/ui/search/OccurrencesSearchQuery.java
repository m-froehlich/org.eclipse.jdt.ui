/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.search;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.IDocument;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;


public class OccurrencesSearchQuery implements ISearchQuery {

	private final OccurrencesSearchResult fResult;
	private IOccurrencesFinder fFinder;
	private IDocument fDocument;
	private final IJavaElement fElement;
	private final String fJobLabel;
	private final String fSingularLabel;
	private final String fPluralLabel;
	
	public OccurrencesSearchQuery(IOccurrencesFinder finder, IDocument document, IJavaElement element) {
		fFinder= finder;
		fDocument= document;
		fElement= element;
		fJobLabel= fFinder.getJobLabel();
		fResult= new OccurrencesSearchResult(this);
		fSingularLabel= fFinder.getSingularLabel(element.getElementName());
		fPluralLabel= fFinder.getPluralLabel(element.getElementName());
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) {
		if (fFinder == null) {
			new StatusInfo(IStatus.ERROR, "Query has already been running"); //$NON-NLS-1$
		}
		try {
			fFinder.perform();
			ArrayList resultingMatches= new ArrayList();
			fFinder.collectOccurrenceMatches(fElement, fDocument, resultingMatches);
			if (!resultingMatches.isEmpty()) {
				fResult.addMatches((Match[]) resultingMatches.toArray(new Match[resultingMatches.size()]));
			}
			//Don't leak AST:
			fFinder= null;
			fDocument= null;
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
	
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel() {
		return fJobLabel;
	}
	
	public String getResultLabel(int nMatches) {
		if (nMatches == 0) {
			return fSingularLabel;
		} else {
			return MessageFormat.format(fPluralLabel, new Object[] { String.valueOf(nMatches) });
		}
	}
		
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#canRerun()
	 */
	public boolean canRerun() {
		return false; // must release finder to not keep AST reference
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#canRunInBackground()
	 */
	public boolean canRunInBackground() {
		return true;
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#getSearchResult()
	 */
	public ISearchResult getSearchResult() {
		return fResult;
	}
}
