/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.DocumentTemplateContext;
import org.eclipse.jdt.internal.corext.template.ITemplateEditor;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.TemplateMessages;
import org.eclipse.jdt.internal.corext.template.TemplateTranslator;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitCompletion.LocalVariable;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;

/**
 * A context for java source.
 */
public class JavaContext extends DocumentTemplateContext {
	
	/** The associated compilation unit. */
	private final ICompilationUnit fCompilationUnit;
	/** A flag to force evaluation in head-less mode. */
	private boolean fForceEvaluation;
	/** A code completion requestor for guessing local variable names. */
	private CompilationUnitCompletion fCompletion;
	
	/**
	 * Creates a java template context.
	 * 
	 * @param type   the context type.
	 * @param string the document string.
	 * @param completionPosition the completion position within the document.
	 * @param unit the compilation unit (may be <code>null</code>).
	 */
	public JavaContext(ContextType type, String string, int completionPosition, ICompilationUnit unit) {			
		super(type, string, completionPosition);
		fCompilationUnit= unit;
	}
	
	/**
	 * Returns the compilation unit associated with this Java context, or <code>null</code> if
	 * no compilation unit is associated with the context.
	 */
	public ICompilationUnit getUnit() {
		return fCompilationUnit;
	}
	
	/*
	 * @see TemplateContext#evaluate(Template template)
	 */
	public TemplateBuffer evaluate(Template template) throws CoreException {
		if (!canEvaluate(template))
			return null;
		
		TemplateTranslator translator= new TemplateTranslator();
		TemplateBuffer buffer= translator.translate(template.getPattern());

//		if (buffer == null)
//			throw CoreException(...);

		getContextType().edit(buffer, this);
			
		ITemplateEditor formatter= new JavaFormatter();
		formatter.edit(buffer, this);

		return buffer;
	}
	
	/**
	 * Forces evaluation.
	 */
	public void setForceEvaluation(boolean evaluate) {
		fForceEvaluation= evaluate;	
	}
	
	/*
	 * @see TemplateContext#canEvaluate(Template templates)
	 */
	public boolean canEvaluate(Template template) {
		return fForceEvaluation || template.matches(getKey(), getContextType().getName());
	}

	/*
	 * @see DocumentTemplateContext#getCompletionPosition();
	 */
	public int getStart() {
		String string= getString();
		int start= getCompletionPosition();

		while ((start != 0) && Character.isUnicodeIdentifierPart(string.charAt(start - 1)))
			start--;
			
		if ((start != 0) && Character.isUnicodeIdentifierStart(string.charAt(start - 1)))
			start--;

		return start;
	}

	/**
	 * Returns the indentation level at the position of code completion.
	 */
	public int getIndentationLevel() {
		String string= getString();
		int start= getStart();

	    try {
	        TextBuffer textBuffer= TextBuffer.create(string);
	        String lineContent= textBuffer.getLineContentOfOffset(start);

			return TextUtil.getIndent(lineContent, CodeFormatterPreferencePage.getTabSize());

	    } catch (CoreException e) {
	     	return 0;   
	    }
	}

	private CompilationUnitCompletion guessVariableNames() {
		ICompilationUnit unit= fCompilationUnit;
		int start= getStart();
		
		if (unit == null)
			return null;
		
		try {
			CompilationUnitCompletion collector= new CompilationUnitCompletion(unit);
			unit.codeComplete(start, collector);			
			return collector;
		
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			openErrorDialog(null, e);
			return null;
		}
	}	
	
	
	private static void openErrorDialog(Shell shell, Exception e) {
		MessageDialog.openError(shell, TemplateMessages.getString("JavaTemplateContext.error.title"), e.getMessage()); //$NON-NLS-1$
	}	

	private CompilationUnitCompletion getCompletion() {
		if (fCompletion == null) {
			fCompletion= new CompilationUnitCompletion(fCompilationUnit);
			
			if (fCompilationUnit != null) {
				try {
					fCompilationUnit.codeComplete(getStart(), fCompletion);
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
		
		return fCompletion;
	}

	/**
	 * Returns the name of a guessed local array, <code>null</code> if no local
	 * array exists.
	 */
	public String guessArray() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
				
		if (localArrays.length > 0)
			return localArrays[localArrays.length - 1].name;

		return null;	
	}
	
	/**
	 * Returns the name of the type of a local array, <code>null</code> if no local
	 * array exists.
	 */
	public String guessArrayType() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
				
		if (localArrays.length > 0) {
			String typeName= localArrays[localArrays.length - 1].typeName;
			return typeName.substring(0, typeName.indexOf('['));
		}
		
		return null;
	}
	
	/**
	 * Returns a proposal for a variable name of a local array element, <code>null</code>
	 * if no local array exists.
	 */
	public String guessArrayElement() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
		
		if (localArrays.length > 0) {
			String typeName= localArrays[localArrays.length - 1].typeName;
			String baseTypeName= typeName.substring(0, typeName.indexOf('['));
			String variableName= completion.typeToVariable(baseTypeName);
			
			if (!completion.existsLocalName(variableName))
				return variableName;
		}

		return null;
	}

	/**
	 * Returns an array index name. 'i', 'j', 'k' are tried until no name collision with
	 * an existing local variable occurs. If all names collide, <code>null</code> is returned.
	 */	
	public String getIndex() {
		CompilationUnitCompletion completion= getCompletion();
		String[] proposals= {"i", "j", "k"};  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		for (int i= 0; i != proposals.length; i++) {
			String proposal = proposals[i];

			if (!completion.existsLocalName(proposal))
				return proposal;
		}

		return null;
	}
	
	/**
	 * Returns the name of a local collection, <code>null</code> if no local collection
	 * exists.
	 */
	public String guessCollection() {
		CompilationUnitCompletion completion= getCompletion();
		try {
			LocalVariable[] localCollections= completion.findLocalCollections();
		
			if (localCollections.length > 0)
				return localCollections[localCollections.length - 1].name;

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}

		return null;
	}

	/**
	 * Returns an iterator name ('iter'). If 'iter' already exists as local variable,
	 * <code>null</code> is returned.
	 */
	public String getIterator() {
		CompilationUnitCompletion completion= getCompletion();		
		String[] proposals= {"iter"}; //$NON-NLS-1$
		
		for (int i= 0; i != proposals.length; i++) {
			String proposal = proposals[i];

			if (!completion.existsLocalName(proposal))
				return proposal;
		}

		return null;
	}

	/**
	 * Returns the enclosing element of a particular element type, <code>null</code>
	 * if no enclosing element of that type exists.
	 */
	public IJavaElement findEnclosingElement(int elementType) {
		if (fCompilationUnit == null)
			return null;

		try {
			IJavaElement element= fCompilationUnit.getElementAt(getStart());
			while (element != null && element.getElementType() != elementType)
				element= element.getParent();
			
			return element;

		} catch (JavaModelException e) {
			return null;
		}	
	}
}

