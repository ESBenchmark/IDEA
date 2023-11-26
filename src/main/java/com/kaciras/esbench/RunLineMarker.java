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
		if (!isMarkPoint(leaf)) {
			return null; // Check the element is defineSuite() or bench[Async]().
		}
		if (!hasImportDefineSuite(element.getContainingFile())) {
			return null; // The file must have import defineSuite from ESBench.
		}
		var action = new ESBenchAction();
		return new Info(Run, new AnAction[]{action}, x -> "Run Benchmark");
	}

	private boolean isMarkPoint(LeafPsiElement leaf) {
		// First check the element is a method call.
		if (!(leaf.getParent() instanceof JSReferenceExpression ref)) {
			return false;
		}
		if (!(ref.getParent() instanceof JSCallExpression top)) {
			return false;
		}

		// If is bench() or benchAsync(), find the topmost call.
		var name = leaf.getChars();
		if (name.equals(BENCH_1) || name.equals(BENCH_2)) {
			top = PsiTreeUtil.getTopmostParentOfType(top, JSCallExpression.class);
			if (top == null) return false;
		}

		// Check the topmost statement is export default defineSuite()
		return DEFINE_SUITE.contentEquals(ESBenchUtils.getFuncName(top))
				&& top.getParent() instanceof JSExportAssignment;
	}
}
