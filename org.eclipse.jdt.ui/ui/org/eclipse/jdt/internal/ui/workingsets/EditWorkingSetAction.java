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
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetEditWizard;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Displays an IWorkingSetEditWizard for editing a working set.
 * 
 * @since 2.1
 */
public class EditWorkingSetAction extends Action {
	private IWorkbenchPartSite fSite;
	private WorkingSetFilterActionGroup fActionGroup;

	public EditWorkingSetAction(WorkingSetFilterActionGroup actionGroup, IWorkbenchPartSite site) {
		super(WorkingSetMessages.getString("EditWorkingSetAction.text")); //$NON-NLS-1$
		Assert.isNotNull(actionGroup);
		setToolTipText(WorkingSetMessages.getString("EditWorkingSetAction.toolTip")); //$NON-NLS-1$
		setEnabled(actionGroup.getWorkingSet() != null);
		fSite= site;
		fActionGroup= actionGroup;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.EDIT_WORKING_SET_ACTION);
	}
	
	/*
	 * Overrides method from Action
	 */
	public void run() {
		Shell shell= fSite != null 
			? fSite.getShell() 
			: JavaPlugin.getActiveWorkbenchShell();
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSet workingSet= fActionGroup.getWorkingSet();
		if (workingSet == null) {
			setEnabled(false);
			return;
		}
		IWorkingSetEditWizard wizard= manager.createWorkingSetEditWizard(workingSet);
		if (wizard == null) {
			String title= WorkingSetMessages.getString("EditWorkingSetAction.error.nowizard.title"); //$NON-NLS-1$
			String message= WorkingSetMessages.getString("EditWorkingSetAction.error.nowizard.message"); //$NON-NLS-1$
			MessageDialog.openError(shell, title, message);
			return;
		}
		WizardDialog dialog= new WizardDialog(shell, wizard);
	 	dialog.create();		
		if (dialog.open() == Window.OK)
			fActionGroup.setWorkingSet(wizard.getSelection(), true);
	}
}
