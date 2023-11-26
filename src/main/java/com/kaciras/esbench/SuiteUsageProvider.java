package com.kaciras.esbench;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.lang.ecmascript6.psi.JSExportAssignment;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import static com.kaciras.esbench.ESBenchUtils.DEFINE_CONFIG;
import static com.kaciras.esbench.ESBenchUtils.DEFINE_SUITE;

/**
 * Avoid unused warning of "export default defineSuite()" in suite files
 * and "export default defineConfig()" in config files.
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
		var name = ESBenchUtils.getMethodName(call);
		var file = element.getContainingFile();

		if (name.equals(DEFINE_SUITE)) {
			return ESBenchUtils.hasImportDefineSuite(file);
		}
		return name.equals(DEFINE_CONFIG)
				&& ESBenchUtils.hasImportDefineConfig(file);
	}
}