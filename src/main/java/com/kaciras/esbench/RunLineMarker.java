package com.kaciras.esbench;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.lang.ecmascript6.psi.JSExportAssignment;
import com.intellij.lang.ecmascript6.psi.impl.ES6ImportPsiUtil;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.intellij.icons.AllIcons.RunConfigurations.TestState.Run;

public final class RunLineMarker extends RunLineMarkerContributor {

	static final String MODULE = "\"@esbench/core/client\"";
	static final String DEFINE_SUITE = "defineSuite";
	static final String BENCH_1 = "bench";
	static final String BENCH_2 = "benchAsync";

	@Override
	public @Nullable Info getInfo(@NotNull PsiElement element) {
		if (!(element instanceof LeafPsiElement leaf)) {
			return null; // Only mark leaf elements for better performance.
		}
		if (!isMarkPoint(leaf)) {
			return null; // Check the element is defineSuite or bench case.
		}
		if (!hasImportFromESBench(element.getContainingFile())) {
			return null; // Check the file has import defineSuite from ESBench.
		}
		var action = new ESBenchAction();
		return new Info(Run, new AnAction[]{action}, null);
	}

	private boolean isMarkPoint(LeafPsiElement leaf) {
		if (!(leaf.getParent() instanceof JSReferenceExpression ref)) {
			return false;
		}
		if (!(ref.getParent() instanceof JSCallExpression top)) {
			return false;
		}

		// If is bench() or benchAsync(), find the topmost.
		var name = leaf.getChars();
		if (name.equals(BENCH_1) || name.equals(BENCH_2)) {
			top = PsiTreeUtil.getTopmostParentOfType(top, JSCallExpression.class);
			if (top == null) return false;
		}

		// Check the top function call is defineSuite(...)
		var topMethod = top.getMethodExpression();
		if (topMethod == null || !topMethod.textMatches(DEFINE_SUITE)) {
			return false;
		}

		// Make sure the defineSuite(...) is exported.
		return top.getParent() instanceof JSExportAssignment;
	}

	/**
	 * Check the file has <code>import { defineSuite } from "@esbench/core/client"</code>
	 */
	private boolean hasImportFromESBench(PsiFile file) {
		return ES6ImportPsiUtil.getImportDeclarations(file)
				.stream()
				.filter(i -> MODULE.equals(ES6ImportPsiUtil.getFromClauseText(i)))
				.flatMap(i -> Arrays.stream(i.getImportSpecifiers()))
				.anyMatch(s -> s.textMatches(DEFINE_SUITE));
	}
}
