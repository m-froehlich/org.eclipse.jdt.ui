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
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Toggles the orientationof the layout of the type hierarchy
 */
public class ToggleOrientationAction extends Action {

	private TypeHierarchyViewPart fView;	
	private int fActionOrientation;
	
	public ToggleOrientationAction(TypeHierarchyViewPart v, int orientation) {
		super("", AS_RADIO_BUTTON); //$NON-NLS-1$
		if (orientation == TypeHierarchyViewPart.VIEW_ORIENTATION_HORIZONTAL) {
			setText(TypeHierarchyMessages.getString("ToggleOrientationAction.horizontal.label")); //$NON-NLS-1$
			setDescription(TypeHierarchyMessages.getString("ToggleOrientationAction.horizontal.description")); //$NON-NLS-1$
			setToolTipText(TypeHierarchyMessages.getString("ToggleOrientationAction.horizontal.tooltip")); //$NON-NLS-1$
			JavaPluginImages.setLocalImageDescriptors(this, "th_horizontal.gif"); //$NON-NLS-1$
		} else if (orientation == TypeHierarchyViewPart.VIEW_ORIENTATION_VERTICAL) {
			setText(TypeHierarchyMessages.getString("ToggleOrientationAction.vertical.label")); //$NON-NLS-1$
			setDescription(TypeHierarchyMessages.getString("ToggleOrientationAction.vertical.description")); //$NON-NLS-1$
			setToolTipText(TypeHierarchyMessages.getString("ToggleOrientationAction.vertical.tooltip")); //$NON-NLS-1$	
			JavaPluginImages.setLocalImageDescriptors(this, "th_vertical.gif"); //$NON-NLS-1$
		} else if (orientation == TypeHierarchyViewPart.VIEW_ORIENTATION_AUTOMATIC) {
			setText(TypeHierarchyMessages.getString("ToggleOrientationAction.automatic.label")); //$NON-NLS-1$
			setDescription(TypeHierarchyMessages.getString("ToggleOrientationAction.automatic.description")); //$NON-NLS-1$
			setToolTipText(TypeHierarchyMessages.getString("ToggleOrientationAction.automatic.tooltip")); //$NON-NLS-1$	
			JavaPluginImages.setLocalImageDescriptors(this, "th_automatic.gif"); //$NON-NLS-1$
		} else if (orientation == TypeHierarchyViewPart.VIEW_ORIENTATION_SINGLE) {
			setText(TypeHierarchyMessages.getString("ToggleOrientationAction.single.label")); //$NON-NLS-1$
			setDescription(TypeHierarchyMessages.getString("ToggleOrientationAction.single.description")); //$NON-NLS-1$
			setToolTipText(TypeHierarchyMessages.getString("ToggleOrientationAction.single.tooltip")); //$NON-NLS-1$	
			JavaPluginImages.setLocalImageDescriptors(this, "th_single.gif"); //$NON-NLS-1$
		} else {
			Assert.isTrue(false);
		}
		fView= v;
		fActionOrientation= orientation;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.TOGGLE_ORIENTATION_ACTION);
	}
	
	public int getOrientation() {
		return fActionOrientation;
	}	
	
	/*
	 * @see Action#actionPerformed
	 */		
	public void run() {
		if (isChecked()) {
			fView.fOrientation= fActionOrientation; 
			fView.computeOrientation();
		}
	}
	
}
