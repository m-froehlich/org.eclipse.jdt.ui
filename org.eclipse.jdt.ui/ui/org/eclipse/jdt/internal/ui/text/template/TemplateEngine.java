/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.util.ArrayList;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.ITableLabelProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.textmanipulation.TextUtil;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;

public class TemplateEngine {

	private ContextType fContextType;
	private ITableLabelProvider fLabelProvider= new TemplateLabelProvider();
	
	private ArrayList fProposals= new ArrayList();

	/**
	 * Creates the template engine for a particular context type.
	 * See <code>TemplateContext</code> for supported context types.
	 */
	public TemplateEngine(ContextType contextType) {
		Assert.isNotNull(contextType);
		fContextType= contextType;
	}

	/**
	 * Empties the collector.
	 * 
	 * @param viewer the text viewer  
	 * @param unit   the compilation unit (may be <code>null</code>)
	 */
	public void reset() {
		fProposals.clear();
	}

	/**
	 * Returns the array of matching templates.
	 */
	public ICompletionProposal[] getResults() {
		return (ICompletionProposal[]) fProposals.toArray(new ICompletionProposal[fProposals.size()]);
	}

	/**
	 * Inspects the context of the compilation unit around <code>completionPosition</code>
	 * and feeds the collector with proposals.
	 * @param viewer             the text viewer
	 * @param completionPosition the context position in the document of the text viewer
	 * @param unit               the compilation unit (may be <code>null</code>)
	 */
	public void complete(ITextViewer viewer, int completionPosition, ICompilationUnit sourceUnit)
		throws JavaModelException
	{
	    IDocument document= viewer.getDocument();
	    
		// prohibit recursion
		if (LinkedPositionManager.hasActiveManager(document))
			return;

		JavaContext context= new JavaContext(fContextType, document.get(), completionPosition, sourceUnit);
		Template[] templates= Templates.getInstance().getTemplates();

		int start= context.getStart();
		int end= context.getEnd();
		IRegion region= new Region(start, end - start);

		for (int i= 0; i != templates.length; i++)
			if (context.canEvaluate(templates[i]))
				fProposals.add(new TemplateProposal(templates[i], context, region, viewer, fLabelProvider.getColumnImage(templates[i], 0)));
	}
}

