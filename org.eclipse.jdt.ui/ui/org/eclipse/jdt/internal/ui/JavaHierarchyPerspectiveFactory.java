/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui;

import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;
import org.eclipse.ui.console.IConsoleConstants;

import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.ui.JavaUI;

public class JavaHierarchyPerspectiveFactory implements IPerspectiveFactory {
		
	/**
	 * Constructs a new Java hierarchy layout engine.
	 */
	public JavaHierarchyPerspectiveFactory() {
		super();
	}

	public void createInitialLayout(IPageLayout layout) {
 		String editorArea = layout.getEditorArea();
		
		IFolderLayout folder= layout.createFolder("left", IPageLayout.LEFT, (float)0.25, editorArea); //$NON-NLS-1$
		folder.addView(JavaUI.ID_TYPE_HIERARCHY); 
		folder.addPlaceholder(IPageLayout.ID_OUTLINE);
		folder.addPlaceholder(JavaUI.ID_PACKAGES);
		folder.addPlaceholder(IPageLayout.ID_RES_NAV);
		
		IPlaceholderFolderLayout outputfolder= layout.createPlaceholderFolder("bottom", IPageLayout.BOTTOM, (float)0.75, editorArea); //$NON-NLS-1$
		outputfolder.addPlaceholder(IPageLayout.ID_PROBLEM_VIEW);
		outputfolder.addPlaceholder(SearchUI.SEARCH_RESULT_VIEW_ID);
		outputfolder.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		outputfolder.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		outputfolder.addPlaceholder(JavaUI.ID_SOURCE_VIEW);
		outputfolder.addPlaceholder(JavaUI.ID_JAVADOC_VIEW);
		
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);		
		
		// views - java
		layout.addShowViewShortcut(JavaUI.ID_PACKAGES);
		layout.addShowViewShortcut(JavaUI.ID_TYPE_HIERARCHY);

		layout.addShowViewShortcut(SearchUI.SEARCH_RESULT_VIEW_ID);
		
		// views - debugging
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);

		// views - standard workbench
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
	}
}
