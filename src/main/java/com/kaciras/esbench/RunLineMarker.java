package com.kaciras.esbench;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.lang.ecmascript6.psi.JSExportAssignment;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.icons.AllIcons.RunConfigurations.TestState.Run;
import static com.kaciras.esbench.ESBenchUtils.*;

public final class RunLineMarker extends RunLineMarkerContributor {

	@Override
	public @Nullable Info getInfo(@NotNull PsiElement element) {
		if (!(element instanceof LeafPsiElement leaf)) {
			return null; // Only mark leaf elements for better performance.
		}
		var description = detectEntryPoint(leaf);
		if (description == null) {
			return null; // Check the element is defineSuite() or bench[Async]().
		}
		if (!hasImportDefineSuite(element.getContainingFile())) {
			return null; // The file must have import defineSuite from ESBench.
		}
		var action = new ESBenchAction("Run " + description);
		return new Info(Run, new AnAction[]{action}, x -> "Run Benchmark");
	}

	private @Nullable String detectEntryPoint(LeafPsiElement leaf) {
		// First check the element is a method call.
		if (!(leaf.getParent() instanceof JSReferenceExpression ref)) {
			return null;
		}
		if (!(ref.getParent() instanceof JSCallExpression top)) {
			return null;
		}
		var function = leaf.getChars();
		var description = "Suite";

		// If is bench() or baseline(), find the topmost call.
		if (function.equals(BENCH_1) || function.equals(BENCH_2)) {
			description = ESBenchUtils.getBenchName(top);
			if (description == null) {
				return null; // Arguments is invalid.
			}
			top = PsiTreeUtil.getTopmostParentOfType(top, JSCallExpression.class);
			if (top == null) {
				return null; // Not inside defineSuite().
			}
			description = '"' + description + '"';
		}

		// The topmost statement should be `export default defineSuite()`
		if (!ESBenchUtils.getMethodName(top).equals(DEFINE_SUITE)) {
			return null;
		}
		return top.getParent() instanceof JSExportAssignment ? description : null;
	}
}
