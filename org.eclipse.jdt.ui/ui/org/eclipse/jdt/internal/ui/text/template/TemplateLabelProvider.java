/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;

import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class TemplateLabelProvider implements ITableLabelProvider {

	/*
	 * @see ITableLabelProvider#getColumnImage(Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex != 0)
			return null;
		
		Template template= (Template) element;
		
		if (template.getContextTypeName().equals("javadoc") && //$NON-NLS-1$
			template.getName().startsWith("<")) //$NON-NLS-1$ 
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_HTMLTAG);
		else
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE);
	}

	/*
	 * @see ITableLabelProvider#getColumnText(Object, int)
	 */
	public String getColumnText(Object element, int columnIndex) {
		Template template = (Template) element;
		
		switch (columnIndex) {
			case 0:
				return template.getName();
			case 1:
				return template.getContextTypeName();
			case 2:
				return template.getDescription();
			default:
				return null;
		}
	}

	/*
	 * @see IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/*
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/*
	 * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}

}

