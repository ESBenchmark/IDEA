package com.kaciras.esbench;

import com.intellij.execution.PsiLocation;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.lang.ecmascript6.psi.impl.ES6ImportPsiUtil;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
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
		if (!check(element)) {
			return null;
		}
		if (!hasImportFromESBench(element.getContainingFile())) {
			return null;
		}
		var location = new PsiLocation(element);
		var name = location.getVirtualFile().getPath();
		var action = new ESBenchAction(name);
		return new Info(Run, new AnAction[]{action}, null);
	}

	private boolean check(PsiElement element) {
		if (!(element.getParent() instanceof JSReferenceExpression ref)) {
			return false;
		}
		if (!(ref.getParent() instanceof JSCallExpression)) {
			return false;
		}
		if (ref.textMatches(DEFINE_SUITE)) {
			return true;
		}
		if (element.textMatches(BENCH_1) || element.textMatches(BENCH_2)) {
			return PsiTreeUtil.findFirstParent(ref, RunLineMarker::isDefineSuite) != null;
		}
		return false;
	}

	static boolean isDefineSuite(@Nullable PsiElement element) {
		return element instanceof JSCallExpression call && call.getMethodExpression().textMatches(DEFINE_SUITE);
	}

	/**
	 * Check the file has <code>import { defineSuite } from "@esbench/core/client"</code>
	 */
	private boolean hasImportFromESBench(PsiFile file) {
		return ES6ImportPsiUtil.getImportDeclarations(file)
				.stream()
				.filter(i -> MODULE.equals(i.getFromClause().getReferenceText()))
				.flatMap(i -> Arrays.stream(i.getImportSpecifiers()))
				.anyMatch(s -> s.textMatches(DEFINE_SUITE));
	}
}
