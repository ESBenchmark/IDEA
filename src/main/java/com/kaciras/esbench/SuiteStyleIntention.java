package com.kaciras.esbench;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.ecmascript6.psi.JSExportAssignment;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory;
import com.intellij.lang.javascript.refactoring.util.JSFunctionsRefactoringUtil;
import com.intellij.lang.javascript.refactoring.util.JSRefactoringUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public final class SuiteStyleIntention extends PsiElementBaseIntentionAction {

	@Override
	@NotNull
	public String getFamilyName() {
		return "Convert suite style";
	}

	@Override
	@NotNull
	public String getText() {
		return "ESBench: Convert suite style";
	}

	@Override
	public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
		var suite = getRawSuite(element);
		if (suite instanceof JSFunctionExpression) {
			return true;
		}
		if (!(suite instanceof JSObjectLiteralExpression object)) {
			return false;
		}
		var props = object.getPropertiesIncludingSpreads();
		if (props.length != 1) {
			return false;
		}
		return props[0] instanceof JSFunctionProperty method && "setup".equals(method.getName());
	}

	private static JSExpression getRawSuite(PsiElement leaf) {
		if (!(leaf.getParent() instanceof JSReferenceExpression ref)) {
			return null;
		}
		if (!(ref.getParent() instanceof JSCallExpression call)) {
			return null;
		}
		if (!(call.getParent() instanceof JSExportAssignment)) {
			return null;
		}
		var name = ESBenchUtils.getMethodName(call);
		if (!ESBenchUtils.DEFINE_SUITE.equals(name)) {
			return null;
		}
		var args = call.getArguments();
		if (args.length != 1) {
			return null; // Not only 1 argument in defineSuite().
		}
		return ESBenchUtils.hasImportDefineSuite(call.getContainingFile()) ? args[0] : null;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
		JSExpression newStyle;
		var suite = getRawSuite(element);

		if (suite instanceof JSFunctionExpression setup) {
			var fp = JSFunctionsRefactoringUtil.createFunctionProperty(setup, "setup");
			var object = JSPsiElementFactory.createJSExpression("{x(){}}", fp, JSObjectLiteralExpression.class);
			newStyle = object;
			object.getFirstProperty().replace(fp);
		} else {
			var object = (JSObjectLiteralExpression) suite;
			var setup = object.getFirstProperty();
			newStyle = JSFunctionsRefactoringUtil.createArrowFunction((JSFunction) setup);
		}

		var expression = JSChangeUtil.replaceExpression(suite, newStyle);
		JSRefactoringUtil.reformatElementWithoutBody(expression, expression);
	}
}
