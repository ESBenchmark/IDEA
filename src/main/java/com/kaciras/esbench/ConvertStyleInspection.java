package com.kaciras.esbench;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment;
import com.intellij.lang.javascript.inspections.JSInspection;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory;
import com.intellij.lang.javascript.refactoring.util.JSFunctionsRefactoringUtil;
import com.intellij.lang.javascript.refactoring.util.JSRefactoringUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public final class ConvertStyleInspection extends JSInspection {

	@Override
	protected @NotNull PsiElementVisitor createVisitor(
			@NotNull ProblemsHolder problemsHolder,
			@NotNull LocalInspectionToolSession localInspectionToolSession
	) {
		return new Visitor(problemsHolder);
	}

	private static class Visitor extends JSElementVisitor {

		private final ProblemsHolder holder;

		private Visitor(ProblemsHolder holder) {
			this.holder = holder;
		}

		@Override
		public void visitES6ExportDefaultAssignment(@NotNull ES6ExportDefaultAssignment node) {
			var expr = node.getExpression();
			if (!(expr instanceof JSCallExpression call)) {
				return;
			}
			var name = ESBenchUtils.getMethodName(call);
			if (!"defineSuite".equals(name) || !ESBenchUtils.hasImportDefineSuite(node.getContainingFile())) {
				return;
			}
			var args = call.getArguments();
			if (args.length != 1) {
				return;
			}
			if (args[0] instanceof JSObjectLiteralExpression object) {
				var props = object.getPropertiesIncludingSpreads();
				if (props.length != 1) {
					return;
				}
				if (props[0] instanceof JSFunctionProperty method) {
					holder.registerProblem(node, "Test", new ToFunctionalQuickFix(method, object));
				}
			} else if (args[0] instanceof JSFunctionExpression fn) {
				holder.registerProblem(node, "Test", new ToObjectQuickFix(fn, fn));
			}
		}
	}

	private static class ToFunctionalQuickFix implements LocalQuickFix {

		private final JSFunction setup;
		private final JSExpression suite;

		public ToFunctionalQuickFix(JSFunction setup, JSExpression suite) {
			this.setup = setup;
			this.suite = suite;
		}

		@Override
		public @IntentionFamilyName @NotNull String getFamilyName() {
			return "Convert to function-style";
		}

		@Override
		public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
			var arrowFn = JSFunctionsRefactoringUtil.createArrowFunction(setup);
			var expression = JSChangeUtil.replaceExpression(suite, arrowFn);
			JSRefactoringUtil.reformatElementWithoutBody(expression, expression);
		}
	}

	private static class ToObjectQuickFix implements LocalQuickFix {

		private final JSFunctionExpression setup;
		private final JSExpression suite;

		public ToObjectQuickFix(JSFunctionExpression setup, JSExpression suite) {
			this.setup = setup;
			this.suite = suite;
		}

		@Override
		public @IntentionFamilyName @NotNull String getFamilyName() {
			return "Convert to object-style";
		}

		@Override
		public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
			var method = JSFunctionsRefactoringUtil.createFunctionProperty(setup, "setup");
			var result = JSPsiElementFactory.createJSExpression('{' + method.getText() + '}', method, JSObjectLiteralExpression.class);
			var expression = JSChangeUtil.replaceExpression(suite, result);
			JSRefactoringUtil.reformatElementWithoutBody(expression, expression);
		}
	}
}
