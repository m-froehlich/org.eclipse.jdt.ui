/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.StringTokenizer;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.util.StringMatcher;

/**
 *
 */
public class TypeFilter implements IPropertyChangeListener {
	
	private static TypeFilter fgDefault;
	
	public static TypeFilter getDefault() {
		if (fgDefault == null) {
			fgDefault= new TypeFilter();
			PreferenceConstants.getPreferenceStore().addPropertyChangeListener(fgDefault);
		}
		return fgDefault;
	}
	
	public static boolean isFiltered(String fullTypeName) {
		return getDefault().filter(fullTypeName);
	}
	
	public static boolean isFiltered(char[] fullTypeName) {
		return getDefault().filter(new String(fullTypeName));
	}
		
	public static boolean isFiltered(char[] packageName, char[] typeName) {
		return getDefault().filter(JavaModelUtil.concatenateName(packageName, typeName));
	}
	
	public static boolean isFiltered(IType type) {
		TypeFilter typeFilter = getDefault();
		if (typeFilter.hasFilters()) {
			return typeFilter.filter(JavaModelUtil.getFullyQualifiedName(type));
		}
		return false;
	}
	

	private StringMatcher[] fStringMatchers;

	/**
	 * 
	 */
	private TypeFilter() {
		fStringMatchers= null;
	}
	
	private StringMatcher[] getStringMatchers() {
		if (fStringMatchers == null) {
			String str= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.TYPEFILTER_ENABLED);
			StringTokenizer tok= new StringTokenizer(str, ";"); //$NON-NLS-1$
			int nTokens= tok.countTokens();
			
			fStringMatchers= new StringMatcher[nTokens];
			for (int i= 0; i < nTokens; i++) {
				String curr= tok.nextToken();
				if (curr.length() > 0) { 
					if (curr.endsWith(".*")) { //$NON-NLS-1$
						curr= curr.substring(0, curr.length() - 2) + '*';
					}
					fStringMatchers[i]= new StringMatcher(curr, false, false);
				}
			}
		}
		return fStringMatchers;
	}
	
	public boolean hasFilters() {
		return getStringMatchers().length > 0;
	}
	
	
	public boolean filter(String fullTypeName) {
		StringMatcher[] matchers= getStringMatchers();
		for (int i= 0; i < matchers.length; i++) {
			StringMatcher curr= matchers[i];
			if (curr.match(fullTypeName)) {
				return true;
			}
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(PreferenceConstants.TYPEFILTER_ENABLED)) {
			fStringMatchers= null;
			AllTypesCache.forceCacheFlush();
		}
	}


}
