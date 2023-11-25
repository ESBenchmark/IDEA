package com.kaciras.esbench;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.lang.ecmascript6.psi.JSExportAssignment;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Avoid unused warning on "export default defineSuite()".
 */
public final class SuiteUsageProvider implements ImplicitUsageProvider {

	@Override
	public boolean isImplicitWrite(@NotNull PsiElement element) {
		return false;
	}

	@Override
	public boolean isImplicitRead(@NotNull PsiElement element) {
		return false;
	}

	@Override
	public boolean isImplicitUsage(@NotNull PsiElement element) {
		if (!(element instanceof JSExportAssignment export)) {
			return false;
		}
		if (!(export.getExpression() instanceof JSCallExpression call)) {
			return false;
		}
		var method = call.getMethodExpression();
		if (method == null || !method.textMatches(RunLineMarker.DEFINE_SUITE)) {
			return false;
		}
		return RunLineMarker.hasImportFromESBench(element.getContainingFile());
	}
}
