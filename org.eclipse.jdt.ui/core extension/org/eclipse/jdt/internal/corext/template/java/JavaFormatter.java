/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.template.ITemplateEditor;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplatePosition;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.NopTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.textmanipulation.TextUtil;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.ui.preferences.TemplatePreferencePage;

/**
 * A template editor using the Java formatter to format a template buffer.
 */
public class JavaFormatter implements ITemplateEditor {

	private static final String CURSOR= "cursor"; //$NON-NLS-1$
	private static final String MARKER= "/*${cursor}*/"; //$NON-NLS-1$

	/*
	 * @see ITemplateEditor#edit(TemplateBuffer, TemplateContext)
	 */
	public void edit(TemplateBuffer buffer, TemplateContext context) throws CoreException {
		int indentationLevel= ((JavaContext) context).getIndentationLevel();
		
		if (TemplatePreferencePage.useCodeFormatter())
			format(buffer, indentationLevel);
		else
			indentate(buffer, indentationLevel);
			
		trimBegin(buffer);
	}

	private static int getCaretOffset(TemplatePosition[] variables) {
	    for (int i= 0; i != variables.length; i++) {
	        TemplatePosition variable= variables[i];
	        
	        if (variable.getName().equals(CURSOR))
	        	return variable.getOffsets()[0];
	    }
	    
	    return -1;
	}

	private void format(TemplateBuffer templateBuffer, int indentationLevel) throws CoreException {
		// XXX 4360
		// workaround for code formatter limitations
		// handle a special case where cursor position is surrounded by whitespaces		

		String string= templateBuffer.getString();
		TemplatePosition[] variables= templateBuffer.getVariables();
		int caretOffset= getCaretOffset(variables);

		if ((caretOffset > 0) && Character.isWhitespace(string.charAt(caretOffset - 1)) &&
			(caretOffset < string.length()) && Character.isWhitespace(string.charAt(caretOffset)))
		{
			MultiTextEdit positions= variablesToPositions(variables);

		    TextEdit insert= SimpleTextEdit.createInsert(caretOffset, MARKER);
		    string= edit(string, positions, insert);
			positionsToVariables(positions, variables);
		    templateBuffer.setContent(string, variables);

			plainFormat(templateBuffer, indentationLevel);			

			string= templateBuffer.getString();
			variables= templateBuffer.getVariables();
			caretOffset= getCaretOffset(variables);

			positions= variablesToPositions(variables);
			TextEdit delete= SimpleTextEdit.createDelete(caretOffset, MARKER.length());
		    string= edit(string, positions, delete);
			positionsToVariables(positions, variables);		    
		    templateBuffer.setContent(string, variables);
	
		} else {
			plainFormat(templateBuffer, indentationLevel);			
		}	    
	}
	
	private void plainFormat(TemplateBuffer templateBuffer, int indentationLevel) throws CoreException {

		String string= templateBuffer.getString();
		TemplatePosition[] variables= templateBuffer.getVariables();

		int[] offsets= variablesToOffsets(variables);
		
		CodeFormatter formatter= new CodeFormatter(JavaCore.getOptions());
		string= formatter.format(string, indentationLevel, offsets);
		
		offsetsToVariables(offsets, variables);

		templateBuffer.setContent(string, variables);	    
	}

	private static void indentate(TemplateBuffer templateBuffer, int indentationLevel) throws CoreException {

		String string= templateBuffer.getString();
		TemplatePosition[] variables= templateBuffer.getVariables();
   		String indentation= TextUtil.createIndentString(indentationLevel);   		

		MultiTextEdit positions= variablesToPositions(variables);
		MultiTextEdit multiEdit= new MultiTextEdit();
		
		TextBuffer textBuffer= TextBuffer.create(string);
	    int lineCount= textBuffer.getNumberOfLines();
	    for (int i= 0; i < lineCount; i++) {
	    	TextRegion region= textBuffer.getLineInformation(i);
			multiEdit.add(SimpleTextEdit.createInsert(region.getOffset(), indentation));
	    }

		string= edit(string, positions, multiEdit);
		positionsToVariables(positions, variables);
		
		templateBuffer.setContent(string, variables);
	}

	private static void trimBegin(TemplateBuffer templateBuffer) throws CoreException {
		String string= templateBuffer.getString();
		TemplatePosition[] variables= templateBuffer.getVariables();

		MultiTextEdit positions= variablesToPositions(variables);

		int i= 0;
		while ((i != string.length()) && Character.isWhitespace(string.charAt(i)))
			i++;

		string= edit(string, positions, SimpleTextEdit.createDelete(0, i));
		positionsToVariables(positions, variables);

		templateBuffer.setContent(string, variables);
	}
	
	private static String edit(String string, MultiTextEdit positions, MultiTextEdit multiEdit) throws CoreException {
	    TextBuffer textBuffer= TextBuffer.create(string);
		TextBufferEditor editor= new TextBufferEditor(textBuffer);
		editor.add(positions);
		editor.add(multiEdit);
		editor.performEdits(null);
		
		return textBuffer.getContent();
	}

	private static String edit(String string, MultiTextEdit positions, TextEdit singleEdit) throws CoreException {
	    TextBuffer textBuffer= TextBuffer.create(string);
		TextBufferEditor editor= new TextBufferEditor(textBuffer);
		editor.add(positions);
		editor.add(singleEdit);
		editor.performEdits(null);
		
		return textBuffer.getContent();
	}
		
	private static int[] variablesToOffsets(TemplatePosition[] variables) {
		Vector vector= new Vector();
		for (int i= 0; i != variables.length; i++) {
		    int[] offsets= variables[i].getOffsets();
		    for (int j= 0; j != offsets.length; j++)
				vector.add(new Integer(offsets[j]));
		}
		
		int[] offsets= new int[vector.size()];
		for (int i= 0; i != offsets.length; i++)
			offsets[i]= ((Integer) vector.get(i)).intValue();

		Arrays.sort(offsets);

		return offsets;	    
	}
	
	private static void offsetsToVariables(int[] allOffsets, TemplatePosition[] variables) {
		int[] currentIndices= new int[variables.length];
		for (int i= 0; i != currentIndices.length; i++)
			currentIndices[i]= 0;

		int[][] offsets= new int[variables.length][];		
		for (int i= 0; i != variables.length; i++)
			offsets[i]= variables[i].getOffsets();
		
		for (int i= 0; i != allOffsets.length; i++) {

			int min= Integer.MAX_VALUE;
			int minVariableIndex= -1;
			for (int j= 0; j != variables.length; j++) {
			    int currentIndex= currentIndices[j];
			    
			    // determine minimum
				if (currentIndex == offsets[j].length)
					continue;
					
				int offset= offsets[j][currentIndex];

				if (offset < min) {
				    min= offset;
					minVariableIndex= j;
				}		
			}

			offsets[minVariableIndex][currentIndices[minVariableIndex]]= allOffsets[i];
			currentIndices[minVariableIndex]++;
		}

		for (int i= 0; i != variables.length; i++)
			variables[i].setOffsets(offsets[i]);	
	}

	private static MultiTextEdit variablesToPositions(TemplatePosition[] variables) {
   		MultiTextEdit positions= new MultiTextEdit();
		for (int i= 0; i != variables.length; i++) {
		    int[] offsets= variables[i].getOffsets();
		    for (int j= 0; j != offsets.length; j++)
				positions.add(new NopTextEdit(offsets[j], 0));
		}
		
		return positions;	    
	}
	
	private static void positionsToVariables(MultiTextEdit positions, TemplatePosition[] variables) {
		Iterator iterator= positions.iterator();
		
		for (int i= 0; i != variables.length; i++) {
		    TemplatePosition variable= variables[i];
		    
			int[] offsets= new int[variable.getOffsets().length];
			for (int j= 0; j != offsets.length; j++)
				offsets[j]= ((TextEdit) iterator.next()).getTextRange().getOffset();
			
		 	variable.setOffsets(offsets);   
		}
	}	
}
