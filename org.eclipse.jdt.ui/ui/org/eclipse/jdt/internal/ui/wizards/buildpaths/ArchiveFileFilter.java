/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Viewer filter for archive selection dialogs.
 * Archives are files with file extension 'jar' and 'zip'.
 * The filter is not case sensitive.
 */
public class ArchiveFileFilter extends ViewerFilter {

	private static final String[] fgArchiveExtensions= { "jar", "zip" };

	private List fExcludes;
	
	/**
	 * @param excludedFiles Excluded files will not pass the filter.
	 * <code>null</code> is allowed if no files should be excluded. 
	 */
	public ArchiveFileFilter(IFile[] excludedFiles) {
		if (excludedFiles != null) {
			fExcludes= Arrays.asList(excludedFiles);
		} else {
			fExcludes= null;
		}
	}
	
	/*
	 * @see ViewerFilter#select
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IFile) {
			if (fExcludes != null && fExcludes.contains(element)) {
				return false;
			}
			String ext= ((IFile)element).getFullPath().getFileExtension();
			if (ext != null && ext.length() != 0) {
				for (int i= 0; i < fgArchiveExtensions.length; i++) {
					if (ext.equalsIgnoreCase(fgArchiveExtensions[i])) {
						return true;
					}
				}
			}
			return false;
		} else if (element instanceof IContainer) { // IProject, IFolder
			try {
				IResource[] resources= ((IContainer)element).members();
				for (int i= 0; i < resources.length; i++) {
					// recursive! Only show containers that contain an archive
					if (select(viewer, parent, resources[i])) {
						return true;
					}
				}
			} catch (CoreException e) {
				JavaPlugin.log(e.getStatus());
			}				
		}
		return false;
	}
			
}