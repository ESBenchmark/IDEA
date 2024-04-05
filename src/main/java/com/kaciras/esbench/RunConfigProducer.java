package com.kaciras.esbench;

import com.intellij.execution.RunManager;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.javascript.testing.JSTestRunnerUtil;
import com.intellij.javascript.testing.runConfiguration.JsTestRunConfigurationProducer;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.jetbrains.annotations.NotNull;

public final class RunConfigProducer extends LazyRunConfigurationProducer<ESBenchRunConfig> {

	@NotNull
	@Override
	public ConfigurationFactory getConfigurationFactory() {
		return ConfigurationTypeUtil.findConfigurationType(ESBenchConfigType.class);
	}

	@Override
	protected boolean setupConfigurationFromContext(
			@NotNull ESBenchRunConfig config,
			@NotNull ConfigurationContext context,
			@NotNull Ref<PsiElement> sourceElement
	) {
		var location = context.getLocation();
		if (location == null) {
			return false;
		}
		var vFile = location.getVirtualFile();
		if (vFile == null) {
			return false;
		}
		var suite = vFile.getPath();
		var dir = JsTestRunConfigurationProducer.guessWorkingDirectory(context.getProject(), suite);
		if (dir == null) {
			return false;
		}
		config.workingDir = dir.getPath();
		config.suite = suite;
		config.pattern = getNamePattern(location.getPsiElement());
		config.setGeneratedName();
		return true;
	}

	@Override
	public boolean isConfigurationFromContext(@NotNull ESBenchRunConfig config, @NotNull ConfigurationContext context) {
		var newConfig = createConfigurationFromContext(context);
		if (newConfig == null) {
			return false;
		}
		return ESBenchUtils.isReusable(config, newConfig.getConfiguration());
	}

	// More efficient replacement of the base method, avoid unnecessary creation of ESBenchRunConfig.
	@Override
	public ConfigurationFromContext findOrCreateConfigurationFromContext(@NotNull ConfigurationContext context) {
		var fromContext = createConfigurationFromContext(context);
		if (fromContext == null) {
			return null;
		}
		var newConfig = fromContext.getConfiguration();
		var runManager = RunManager.getInstance(context.getProject());

		ProgressManager.checkCanceled();
		var existing = getConfigurationSettingsList(runManager)
				.stream()
				.filter(c -> ESBenchUtils.isReusable(c.getConfiguration(), newConfig))
				.findFirst().orElse(null);

		if (existing == null) {
			runManager.setUniqueNameIfNeeded(newConfig);
		} else {
			fromContext.setConfigurationSettings(existing);
		}
		return fromContext;
	}

	private @NonNull String getNamePattern(PsiElement element) {
		if (((LeafPsiElement) element).getChars().charAt(0) == 'd') {
			return ""; // Marked element is `defineSuite`.
		}
		var expression = (JSCallExpression) element.getParent().getParent();
		var name = ESBenchUtils.getBenchName(expression);
		if (name == null) {
			return ""; // Arguments of bench[Async]() is invalid.
		}
		return "^" + JSTestRunnerUtil.escapeJavaScriptRegexp(name) + "$";
	}
}
