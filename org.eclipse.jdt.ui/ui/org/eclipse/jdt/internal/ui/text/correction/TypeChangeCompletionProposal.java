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

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

public class TypeChangeCompletionProposal extends LinkedCorrectionProposal {

	private IBinding fBinding;
	private CompilationUnit fAstRoot;
	private ITypeBinding fNewType;
	private boolean fOfferSuperTypeProposals;
	
	public TypeChangeCompletionProposal(ICompilationUnit targetCU, IBinding binding, CompilationUnit astRoot, ITypeBinding newType, boolean offerSuperTypeProposals, int relevance) {
		super("", targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)); //$NON-NLS-1$
		
		Assert.isTrue(binding != null && binding.getKind() == IBinding.METHOD || binding.getKind() == IBinding.VARIABLE && Bindings.isDeclarationBinding(binding));
		
		fBinding= binding; // must be generic method or (generic) variable
		fAstRoot= astRoot;
		fNewType= newType;
		fOfferSuperTypeProposals= offerSuperTypeProposals;
		
		if (binding.getKind() == IBinding.VARIABLE) {
			IVariableBinding varBinding= (IVariableBinding) binding;
			
			String[] args= { varBinding.getName(), newType.getName() };
			if (varBinding.isField()) {
				setDisplayName(CorrectionMessages.getFormattedString("TypeChangeCompletionProposal.field.name", args)); //$NON-NLS-1$
			} else if (astRoot.findDeclaringNode(binding) instanceof SingleVariableDeclaration) {
				setDisplayName(CorrectionMessages.getFormattedString("TypeChangeCompletionProposal.param.name", args)); //$NON-NLS-1$
			} else {
				setDisplayName(CorrectionMessages.getFormattedString("TypeChangeCompletionProposal.variable.name", args)); //$NON-NLS-1$
			}
		} else {
			String[] args= { binding.getName(), newType.getName() };
			setDisplayName(CorrectionMessages.getFormattedString("TypeChangeCompletionProposal.method.name", args)); //$NON-NLS-1$
		}
	}
	
	protected ASTRewrite getRewrite() throws CoreException {
		ASTNode boundNode= fAstRoot.findDeclaringNode(fBinding);
		ASTNode declNode= null;
				
		if (boundNode != null) {
			declNode= boundNode; // is same CU
		} else {
			ASTParser astParser= ASTParser.newParser(ASTProvider.AST_LEVEL);
			astParser.setSource(getCompilationUnit());
			astParser.setResolveBindings(true);
			CompilationUnit newRoot= (CompilationUnit) astParser.createAST(null);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		if (declNode != null) {
			AST ast= declNode.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			
			Type type= getImportRewrite().addImport(fNewType, ast);
			
			if (declNode instanceof MethodDeclaration) {
				MethodDeclaration methodDecl= (MethodDeclaration) declNode;
				rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, type, null);
				rewrite.set(methodDecl, MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
			} else if (declNode instanceof VariableDeclarationFragment) {
				ASTNode parent= declNode.getParent();
				if (parent instanceof FieldDeclaration) {
					FieldDeclaration fieldDecl= (FieldDeclaration) parent;
					if (fieldDecl.fragments().size() > 1 && (fieldDecl.getParent() instanceof AbstractTypeDeclaration)) { // split
						VariableDeclarationFragment placeholder= (VariableDeclarationFragment) rewrite.createMoveTarget(declNode);
						FieldDeclaration newField= ast.newFieldDeclaration(placeholder);
						newField.setType(type);
						AbstractTypeDeclaration typeDecl= (AbstractTypeDeclaration) fieldDecl.getParent();
						
						ListRewrite listRewrite= rewrite.getListRewrite(typeDecl, typeDecl.getBodyDeclarationsProperty());
						if (fieldDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
							listRewrite.insertBefore(newField, parent, null);
						} else {
							listRewrite.insertAfter(newField, parent, null);
						}
					} else {
						rewrite.set(fieldDecl, FieldDeclaration.TYPE_PROPERTY, type, null);
						rewrite.set(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
					}
				} else if (parent instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement varDecl= (VariableDeclarationStatement) parent;
					if (varDecl.fragments().size() > 1 && (varDecl.getParent() instanceof Block)) { // split
						VariableDeclarationFragment placeholder= (VariableDeclarationFragment) rewrite.createMoveTarget(declNode);
						VariableDeclarationStatement newStat= ast.newVariableDeclarationStatement(placeholder);
						newStat.setType(type);
						
						ListRewrite listRewrite= rewrite.getListRewrite(varDecl.getParent(), Block.STATEMENTS_PROPERTY);
						if (varDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
							listRewrite.insertBefore(newStat, parent, null);
						} else {
							listRewrite.insertAfter(newStat, parent, null);
						}
					} else {
						rewrite.set(varDecl, VariableDeclarationStatement.TYPE_PROPERTY, type, null);
						rewrite.set(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
					}
				} else if (parent instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression varDecl= (VariableDeclarationExpression) parent;
					
					rewrite.set(varDecl, VariableDeclarationExpression.TYPE_PROPERTY, type, null);
					rewrite.set(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
				}
			} else if (declNode instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration variableDeclaration= (SingleVariableDeclaration) declNode;
				rewrite.set(variableDeclaration, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
				rewrite.set(variableDeclaration, SingleVariableDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
			}
			
			// set up linked mode
			final String KEY_TYPE= "type"; //$NON-NLS-1$
			addLinkedPosition(rewrite.track(type), true, KEY_TYPE);
			if (fOfferSuperTypeProposals) {
				ITypeBinding[] typeProposals= ASTResolving.getRelaxingTypes(ast, fNewType);
				for (int i= 0; i < typeProposals.length; i++) {
					addLinkedPositionProposal(KEY_TYPE, typeProposals[i]);
				}
			}
			return rewrite;
		}
		return null;
	}
	
	
}
