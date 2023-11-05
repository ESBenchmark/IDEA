package com.kaciras.esbench;

import com.intellij.execution.PsiLocation;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RunLineMarker extends RunLineMarkerContributor {

	@Override
	public @Nullable Info getInfo(@NotNull PsiElement element) {
		if (!(element instanceof JSCallExpression call)) {
			return null;
		}
		if (!call.getMethodExpression().getText().equals("defineSuite")) {
			return null;
		}
		var children = call.getContainingFile().getChildren();

		var location = new PsiLocation(call);
		var name = location.getVirtualFile().getPath();
		var action = new ESBenchAction(name);
		return new Info(AllIcons.RunConfigurations.TestState.Run, new AnAction[]{action}, null);
	}
}
