/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation 
 * 		Sebastian Davids <sdavids@gmx.de> - Fix for bug 19346 - Dialog font
 *   	should be activated and used by other components.
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetEditWizard;
import org.eclipse.ui.dialogs.IWorkingSetNewWizard;
import org.eclipse.ui.dialogs.SelectionDialog;

public class WorkingSetConfigurationDialog extends SelectionDialog {

	private static class WorkingSetLabelProvider extends LabelProvider {
		private Map fIcons;
		public WorkingSetLabelProvider() {
			fIcons= new Hashtable();
		}
		public void dispose() {
			Iterator iterator= fIcons.values().iterator();
			while (iterator.hasNext()) {
				Image icon= (Image)iterator.next();
				icon.dispose();
			}
			super.dispose();
		}
		public Image getImage(Object object) {
			Assert.isTrue(object instanceof IWorkingSet);
			IWorkingSet workingSet= (IWorkingSet)object;
			ImageDescriptor imageDescriptor= workingSet.getImage();
			if (imageDescriptor == null)
				return null;
			Image icon= (Image)fIcons.get(imageDescriptor);
			if (icon == null) {
				icon= imageDescriptor.createImage();
				fIcons.put(imageDescriptor, icon);
			}
			return icon;
		}
		public String getText(Object object) {
			Assert.isTrue(object instanceof IWorkingSet);
			IWorkingSet workingSet= (IWorkingSet)object;
			return workingSet.getName();
		}
	}
	
	private static class Filter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			IWorkingSet ws= (IWorkingSet)element;
			String id= ws.getId();
			return HistoryWorkingSetUpdater.ID.equals(id) ||
				OthersWorkingSetUpdater.ID.equals(id) ||
				JavaWorkingSetUpdater.ID.equals(id);
		}
	}

	private List fElements;
	private CheckboxTableViewer fTableViewer;

	private Button fNewButton;
	private Button fEditButton;
	private Button fRemoveButton;
	private Button fUpButton;
	private Button fDownButton;
	private Button fSelectAll;
	private Button fDeselectAll;

	private IWorkingSet[] fResult;
	private List fAddedWorkingSets;
	private List fRemovedWorkingSets;
	private Map fEditedWorkingSets;
	private List fRemovedMRUWorkingSets;

	private int nextButtonId= IDialogConstants.CLIENT_ID + 1;

	public WorkingSetConfigurationDialog(Shell parentShell, IWorkingSet[] workingSets) {
		super(parentShell);
		setTitle(WorkingSetMessages.getString("WorkingSetConfigurationDialog.title")); //$NON-NLS-1$
		setMessage(WorkingSetMessages.getString("WorkingSetConfigurationDialog.message")); //$NON-NLS-1$
		fElements= new ArrayList(workingSets.length);
		Filter filter= new Filter();
		for (int i= 0; i < workingSets.length; i++) {
			if (filter.select(null, null, workingSets[i]))
				fElements.add(workingSets[i]);
		}
	}

	/**
	 * Returns the selected working sets
	 * 
	 * @return the selected working sets
	 */
	public IWorkingSet[] getSelection() {
		return fResult;
	}

	/**
	 * Sets the initial selection
	 * 
	 * @param workingSets the initial selection
	 */
	public void setSelection(IWorkingSet[] workingSets) {
		fResult= workingSets;
		setInitialSelections(workingSets);
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected Control createContents(Composite parent) {
		Control control= super.createContents(parent);
		setInitialSelection();
		updateButtonAvailability();
		return control;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		composite.setFont(parent.getFont());

		createMessageArea(composite);
		Composite inner= new Composite(composite, SWT.NONE);
		inner.setFont(composite.getFont());
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		inner.setLayout(layout);
		createTableViewer(inner);
		createOrderButtons(inner);
		createModifyButtons(composite);
		fTableViewer.setInput(fElements);

		return composite;
	}

	private void createTableViewer(Composite parent) {
		fTableViewer= CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.MULTI);
		fTableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateButtonAvailability();
			}
		});
		GridData data= new GridData(GridData.FILL_BOTH);
		data.heightHint= convertHeightInCharsToPixels(20);
		data.widthHint= convertWidthInCharsToPixels(50);
		fTableViewer.getTable().setLayoutData(data);
		fTableViewer.getTable().setFont(parent.getFont());

		fTableViewer.addFilter(new Filter());
		fTableViewer.setLabelProvider(new WorkingSetLabelProvider());
		fTableViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object element) {
				return ((List)element).toArray();
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged();
			}
		});
	}

	private void createModifyButtons(Composite composite) {
		Composite buttonComposite= new Composite(composite, SWT.RIGHT);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		buttonComposite.setLayout(layout);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.grabExcessHorizontalSpace= true;
		composite.setData(data);

		fNewButton= createButton(buttonComposite, nextButtonId++, 
			WorkingSetMessages.getString("WorkingSetConfigurationDialog.new.label"), false); //$NON-NLS-1$
		fNewButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				createWorkingSet();
			}
		});

		fEditButton= createButton(buttonComposite, nextButtonId++, 
			WorkingSetMessages.getString("WorkingSetConfigurationDialog.edit.label"), false); //$NON-NLS-1$
		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				editSelectedWorkingSet();
			}
		});

		fRemoveButton= createButton(buttonComposite, nextButtonId++, 
			WorkingSetMessages.getString("WorkingSetConfigurationDialog.remove.label"), false); //$NON-NLS-1$
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeSelectedWorkingSets();
			}
		});
	}

	private void createOrderButtons(Composite parent) {
		Composite buttons= new Composite(parent, SWT.NONE);
		buttons.setFont(parent.getFont());
		buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		buttons.setLayout(layout);

		fUpButton= new Button(buttons, SWT.PUSH);
		fUpButton.setText(WorkingSetMessages.getString("WorkingSetConfigurationDialog.up.label")); //$NON-NLS-1$
		setButtonLayoutData(fUpButton);
		fUpButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				moveUp(((IStructuredSelection)fTableViewer.getSelection()).toList());
			}
		});

		fDownButton= new Button(buttons, SWT.PUSH);
		fDownButton.setText(WorkingSetMessages.getString("WorkingSetConfigurationDialog.down.label")); //$NON-NLS-1$
		setButtonLayoutData(fDownButton);
		fDownButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				moveDown(((IStructuredSelection)fTableViewer.getSelection()).toList());
			}
		});
		
		fSelectAll= new Button(buttons, SWT.PUSH);
		fSelectAll.setText(WorkingSetMessages.getString("WorkingSetConfigurationDialog.selectAll.label")); //$NON-NLS-1$
		setButtonLayoutData(fSelectAll);
		fSelectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll();
			}
		});
		
		fDeselectAll= new Button(buttons, SWT.PUSH);
		fDeselectAll.setText(WorkingSetMessages.getString("WorkingSetConfigurationDialog.deselectAll.label")); //$NON-NLS-1$
		setButtonLayoutData(fDeselectAll);
		fDeselectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				deselectAll();
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	protected void okPressed() {
		List newResult= getResultWorkingSets();
		fResult= (IWorkingSet[])newResult.toArray(new IWorkingSet[newResult.size()]);
		setResult(newResult);
		super.okPressed();
	}

	private List getResultWorkingSets() {
		Object[] checked= fTableViewer.getCheckedElements();
		return new ArrayList(Arrays.asList(checked));
	}

	/**
	 * {@inheritDoc}
	 */
	protected void cancelPressed() {
		restoreAddedWorkingSets();
		restoreChangedWorkingSets();
		restoreRemovedWorkingSets();
		super.cancelPressed();
	}

	private void setInitialSelection() {
		List selections= getInitialElementSelections();
		if (!selections.isEmpty()) {
			fTableViewer.setCheckedElements(selections.toArray());
		}
	}

	private void createWorkingSet() {
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetNewWizard wizard= manager.createWorkingSetNewWizard(new String[] {JavaWorkingSetUpdater.ID});
		// the wizard can't be null since we have at least the Java working set.
		WizardDialog dialog= new WizardDialog(getShell(), wizard);
		dialog.create();
		if (dialog.open() == Window.OK) {
			IWorkingSet workingSet= wizard.getSelection();
			Filter filter= new Filter();
			if (filter.select(null, null, workingSet)) {
				fElements.add(workingSet);
				fTableViewer.add(workingSet);
				fTableViewer.setSelection(new StructuredSelection(workingSet), true);
				fTableViewer.setChecked(workingSet, true);
				manager.addWorkingSet(workingSet);
				fAddedWorkingSets.add(workingSet);
			}
		}
	}

	private void editSelectedWorkingSet() {
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSet editWorkingSet= (IWorkingSet)((IStructuredSelection)fTableViewer.getSelection()).getFirstElement();
		IWorkingSetEditWizard wizard= manager.createWorkingSetEditWizard(editWorkingSet);
		WizardDialog dialog= new WizardDialog(getShell(), wizard);
		IWorkingSet originalWorkingSet= (IWorkingSet)fEditedWorkingSets.get(editWorkingSet);
		boolean firstEdit= originalWorkingSet == null;

		// save the original working set values for restoration when selection
		// dialog is cancelled.
		if (firstEdit) {
			originalWorkingSet= 
				PlatformUI.getWorkbench().getWorkingSetManager().
				createWorkingSet(editWorkingSet.getName(), editWorkingSet.getElements());
		} else {
			fEditedWorkingSets.remove(editWorkingSet);
		}
		dialog.create();
		if (dialog.open() == Window.OK) {
			editWorkingSet= wizard.getSelection();
			fTableViewer.update(editWorkingSet, null);
			// make sure ok button is enabled when the selected working set
			// is edited. Fixes bug 33386.
			updateButtonAvailability();
		}
		fEditedWorkingSets.put(editWorkingSet, originalWorkingSet);
	}

	/**
	 * Called when the selection has changed.
	 */
	void handleSelectionChanged() {
		updateButtonAvailability();
	}

	/**
	 * Overrides method in Dialog
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#open()
	 */
	public int open() {
		fAddedWorkingSets= new ArrayList();
		fRemovedWorkingSets= new ArrayList();
		fEditedWorkingSets= new HashMap();
		fRemovedMRUWorkingSets= new ArrayList();
		return super.open();
	}

	/**
	 * Removes the selected working sets from the workbench.
	 */
	private void removeSelectedWorkingSets() {
		ISelection selection= fTableViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
			Iterator iter= ((IStructuredSelection)selection).iterator();
			while (iter.hasNext()) {
				IWorkingSet workingSet= (IWorkingSet)iter.next();
				if (fAddedWorkingSets.contains(workingSet)) {
					fAddedWorkingSets.remove(workingSet);
				} else {
					IWorkingSet[] recentWorkingSets= manager.getRecentWorkingSets();
					for (int i= 0; i < recentWorkingSets.length; i++) {
						if (workingSet.equals(recentWorkingSets[i])) {
							fRemovedMRUWorkingSets.add(workingSet);
							break;
						}
					}
					fRemovedWorkingSets.add(workingSet);
				}
				fElements.remove(workingSet);
				manager.removeWorkingSet(workingSet);
			}
			fTableViewer.remove(((IStructuredSelection)selection).toArray());
		}
	}

	/**
	 * Removes newly created working sets from the working set manager.
	 */
	private void restoreAddedWorkingSets() {
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		Iterator iterator= fAddedWorkingSets.iterator();

		while (iterator.hasNext()) {
			manager.removeWorkingSet(((IWorkingSet)iterator.next()));
		}
	}

	/**
	 * Rolls back changes to working sets.
	 */
	private void restoreChangedWorkingSets() {
		Iterator iterator= fEditedWorkingSets.keySet().iterator();

		while (iterator.hasNext()) {
			IWorkingSet editedWorkingSet= (IWorkingSet)iterator.next();
			IWorkingSet originalWorkingSet= (IWorkingSet)fEditedWorkingSets.get(editedWorkingSet);

			if (editedWorkingSet.getName().equals(originalWorkingSet.getName()) == false) {
				editedWorkingSet.setName(originalWorkingSet.getName());
			}
			if (editedWorkingSet.getElements().equals(originalWorkingSet.getElements()) == false) {
				editedWorkingSet.setElements(originalWorkingSet.getElements());
			}
		}
	}

	/**
	 * Adds back removed working sets to the working set manager.
	 */
	private void restoreRemovedWorkingSets() {
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		Iterator iterator= fRemovedWorkingSets.iterator();

		while (iterator.hasNext()) {
			manager.addWorkingSet(((IWorkingSet)iterator.next()));
		}
		iterator= fRemovedMRUWorkingSets.iterator();
		while (iterator.hasNext()) {
			manager.addRecentWorkingSet(((IWorkingSet)iterator.next()));
		}
	}

	/**
	 * Updates the modify buttons' enabled state based on the current seleciton.
	 */
	private void updateButtonAvailability() {
		IStructuredSelection selection= (IStructuredSelection)fTableViewer.getSelection();
		boolean hasSelection= selection != null && !selection.isEmpty();
		boolean hasSingleSelection= selection.size() == 1;

		fRemoveButton.setEnabled(hasSelection && areAllGlobalWorkingSets(selection));
		fEditButton.setEnabled(hasSingleSelection && ((IWorkingSet)selection.getFirstElement()).isEditable());
		if (fUpButton != null) {
			fUpButton.setEnabled(canMoveUp());
		}
		if (fDownButton != null) {
			fDownButton.setEnabled(canMoveDown());
		}
	}
	
	private boolean areAllGlobalWorkingSets(IStructuredSelection selection) {
		Set globals= new HashSet(Arrays.asList(PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets()));
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			if (!globals.contains(iter.next()))
				return false;
		}
		return true;
	}
	
	private void moveUp(List toMoveUp) {
		if (toMoveUp.size() > 0) {
			setElements(moveUp(fElements, toMoveUp));
			fTableViewer.reveal(toMoveUp.get(0));
		}
	}

	private void moveDown(List toMoveDown) {
		if (toMoveDown.size() > 0) {
			setElements(reverse(moveUp(reverse(fElements), toMoveDown)));
			fTableViewer.reveal(toMoveDown.get(toMoveDown.size() - 1));
		}
	}

	private void setElements(List elements) {
		fElements= elements;
		fTableViewer.setInput(fElements);
		updateButtonAvailability();
	}

	private List moveUp(List elements, List move) {
		int nElements= elements.size();
		List res= new ArrayList(nElements);
		Object floating= null;
		for (int i= 0; i < nElements; i++) {
			Object curr= elements.get(i);
			if (move.contains(curr)) {
				res.add(curr);
			} else {
				if (floating != null) {
					res.add(floating);
				}
				floating= curr;
			}
		}
		if (floating != null) {
			res.add(floating);
		}
		return res;
	}

	private List reverse(List p) {
		List reverse= new ArrayList(p.size());
		for (int i= p.size() - 1; i >= 0; i--) {
			reverse.add(p.get(i));
		}
		return reverse;
	}

	private boolean canMoveUp() {
		int[] indc= fTableViewer.getTable().getSelectionIndices();
		for (int i= 0; i < indc.length; i++) {
			if (indc[i] != i) {
				return true;
			}
		}
		return false;
	}

	private boolean canMoveDown() {
		int[] indc= fTableViewer.getTable().getSelectionIndices();
		int k= fElements.size() - 1;
		for (int i= indc.length - 1; i >= 0; i--, k--) {
			if (indc[i] != k) {
				return true;
			}
		}
		return false;
	}
	
	//---- select / deselect --------------------------------------------------
	
	private void selectAll() {
		fTableViewer.setAllChecked(true);
	}
	
	private void deselectAll() {
		fTableViewer.setAllChecked(false);
	}
}
