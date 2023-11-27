package com.kaciras.esbench;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.javascript.testing.JSTestRunnerUtil;
import com.intellij.javascript.testing.JsTestRunConfigurationProducer;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
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
		var leaf = location.getPsiElement();
		var file = location.getVirtualFile();
		if (file == null || !file.isInLocalFileSystem()) {
			return false;
		}

		var dir = JsTestRunConfigurationProducer.guessWorkingDirectory(context.getProject(), file.getPath());

		config.workingDir = dir.getPath();
		config.suite = file.getPath();
		config.pattern = getNamePattern(leaf);
		config.setGeneratedName();

		return true;
	}

	@Override
	public boolean isConfigurationFromContext(
			@NotNull ESBenchRunConfig config,
			@NotNull ConfigurationContext context
	) {
		var location = context.getLocation();
		if (location == null) {
			return false;
		}
		var leaf = location.getPsiElement();
		var file = location.getVirtualFile();

		var template = (ESBenchRunConfig) this
				.cloneTemplateConfiguration(context)
				.getConfiguration();
		var workingDir = JsTestRunConfigurationProducer.guessWorkingDirectory(context.getProject(), file.getPath());
		if (workingDir == null) {
			return false;
		}

		return config.configFile.equals(template.configFile)
				&& config.workingDir.equals(workingDir.getPath())
				&& config.suite.equals(file.getPath())
				&& config.pattern.equals(getNamePattern(leaf));
	}

	private @NonNull String getNamePattern(PsiElement element) {
		if (((LeafPsiElement) element).getChars().charAt(0) == 'd') {
			return ""; // Marked element is `defineSuite`.
		}
		var args = ((JSCallExpression) element.getParent().getParent()).getArguments();
		if (args.length == 0 || !(args[0] instanceof JSLiteralExpression literal)) {
			return ""; // bench[Async]() but missing the name argument.
		}
		if (!(literal.getValue() instanceof String name)) {
			return ""; // The name argument is invalid.
		}
		return "^" + JSTestRunnerUtil.escapeJavaScriptRegexp(name) + "$";
	}
}
