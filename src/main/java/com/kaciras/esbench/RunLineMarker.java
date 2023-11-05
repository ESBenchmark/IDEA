package com.kaciras.esbench;

import com.intellij.execution.PsiLocation;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.lang.ecmascript6.psi.impl.ES6ImportPsiUtil;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class RunLineMarker extends RunLineMarkerContributor {

	private static String MOD = "\"@esbench/core/client\"";
	private static String DEFINITION = "defineSuite";

	@Override
	public @Nullable Info getInfo(@NotNull PsiElement element) {
		if (!(element.getParent() instanceof JSReferenceExpression ref)) {
			return null;
		}
		if (!ref.textMatches(DEFINITION) || !(ref.getParent() instanceof JSCallExpression call)) {
			return null;
		}
		if (!hasImportFromESBench(element.getContainingFile())) {
			return null;
		}

		var location = new PsiLocation(call);
		var name = location.getVirtualFile().getPath();
		var action = new ESBenchAction(name);
		return new Info(AllIcons.RunConfigurations.TestState.Run, new AnAction[]{action}, null);
	}

	private boolean hasImportFromESBench(PsiFile file) {
		return ES6ImportPsiUtil.getImportDeclarations(file)
				.stream()
				.filter(i -> MOD.equals(i.getFromClause().getReferenceText()))
				.flatMap(i -> Arrays.stream(i.getImportSpecifiers()))
				.anyMatch(s -> s.textMatches(DEFINITION));
	}
}
