package com.kaciras.esbench;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment;
import com.intellij.lang.ecmascript6.psi.JSExportAssignment;
import com.intellij.lang.javascript.inspections.JSInspection;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory;
import com.intellij.lang.javascript.refactoring.util.JSFunctionsRefactoringUtil;
import com.intellij.lang.javascript.refactoring.util.JSRefactoringUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public final class SuiteStyleInspection extends JSInspection {

	static final String HINT = "Suite style can be converted";

	@Override
	@NotNull
	protected PsiElementVisitor createVisitor(
			@NotNull ProblemsHolder problemsHolder,
			@NotNull LocalInspectionToolSession localInspectionToolSession
	) {
		return new SuiteStyleVisitor(problemsHolder);
	}

	private static class SuiteStyleVisitor extends JSElementVisitor {

		private final ProblemsHolder holder;

		private SuiteStyleVisitor(ProblemsHolder holder) {
			this.holder = holder;
		}

		@Override
		public void visitES6ExportDefaultAssignment(@NotNull ES6ExportDefaultAssignment node) {
			var call = checkDefineSuite(node);
			if (call == null) {
				return; // Is not a suite file.
			}
			var args = call.getArguments();
			if (args.length != 1) {
				return; // Not only 1 argument in defineSuite().
			}

			if (args[0] instanceof JSObjectLiteralExpression object) {
				var props = object.getPropertiesIncludingSpreads();
				if (props.length != 1) {
					return;
				}
				if (props[0] instanceof JSFunctionProperty method && "setup".equals(method.getName())) {
					holder.registerProblem(node, HINT, new ToFunctionalQuickFix(method, object));
				}
			} else if (args[0] instanceof JSFunctionExpression fn) {
				holder.registerProblem(node, HINT, new ToObjectQuickFix(fn));
			}
		}

		private static JSCallExpression checkDefineSuite(JSExportAssignment node) {
			var expr = node.getExpression();
			if (!(expr instanceof JSCallExpression call)) {
				return null;
			}
			var name = ESBenchUtils.getMethodName(call);
			if (!ESBenchUtils.DEFINE_SUITE.equals(name)) {
				return null;
			}
			return ESBenchUtils.hasImportDefineSuite(node.getContainingFile()) ? call : null;
		}
	}

	private record ToFunctionalQuickFix(JSFunction setup, JSExpression suite) implements LocalQuickFix {

		@Override
		@NotNull
		public String getFamilyName() {
			return "ESBench: Convert to function-style";
		}

		@Override
		public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
			var arrowFn = JSFunctionsRefactoringUtil.createArrowFunction(setup);
			var expression = JSChangeUtil.replaceExpression(suite, arrowFn);
			JSRefactoringUtil.reformatElementWithoutBody(expression, expression);
		}
	}

	private record ToObjectQuickFix(JSFunctionExpression setup) implements LocalQuickFix {

		@Override
		@NotNull
		public String getFamilyName() {
			return "ESBench: Convert to object-style";
		}

		@Override
		public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
			var method = JSFunctionsRefactoringUtil.createFunctionProperty(setup, "setup");
			var result = JSPsiElementFactory.createJSExpression('{' + method.getText() + '}', method, JSObjectLiteralExpression.class);
			var expression = JSChangeUtil.replaceExpression(setup, result);
			JSRefactoringUtil.reformatElementWithoutBody(expression, expression);
		}
	}
}
