package com.kaciras.esbench;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.icons.AllIcons.RunConfigurations.TestState.Run;
import static com.kaciras.esbench.ESBenchUtils.*;

public final class RunLineMarker extends RunLineMarkerContributor {

	@Override
	@Nullable
	public Info getInfo(@NotNull PsiElement element) {
		if (!(element instanceof LeafPsiElement leaf)) {
			return null; // Only mark leaf elements for better performance.
		}
		var description = detectEntryPoint(leaf);
		if (description == null) {
			return null;
		}
		if (!hasImportDefineSuite(element.getContainingFile())) {
			return null; // The file must have import defineSuite from ESBench.
		}
		var actions = new ESBenchAction[]{
				new ESBenchAction("Run " + description, false),
				new ESBenchAction("Debug " + description, true)
		};
		return new Info(Run, actions, x -> "Run Benchmark");
	}

	/**
	 * Check whatever the leaf element defines a benchmark (suite or case).
	 * You may also want to check the file imports the ESBench package.
	 *
	 * @param leaf The element to check.
	 * @return Description of the benchmark, or null if the element is not that.
	 */
	@Nullable
	public static String detectEntryPoint(LeafPsiElement leaf) {
		var call = ESBenchUtils.getCallFromLeaf(leaf);
		if (call == null) {
			return null; // The element is not a function call.
		}
		var function = leaf.getChars();
		var description = "Suite";

		if (function.equals(BENCH_1) || function.equals(BENCH_2)) {
			description = ESBenchUtils.getBenchName(call);
			if (description == null) {
				return null; // Arguments is invalid.
			}
			call = PsiTreeUtil.getTopmostParentOfType(call, JSCallExpression.class);
			if (call == null) {
				return null; // Not inside defineSuite().
			}
			description = '"' + description + '"';
		}

		// The topmost statement must be `export default defineSuite()`
		return ESBenchUtils.isExportDefineSuite(call) ? description : null;
	}
}
