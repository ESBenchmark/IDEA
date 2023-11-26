package com.kaciras.esbench;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.javascript.testing.JSTestRunnerUtil;
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

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
		var leaf = (LeafPsiElement) sourceElement.get();

		var psiFile = leaf.getContainingFile();
		var vFile = psiFile.getVirtualFile();

		if (vFile == null) {
			return false;
		}

		var packageJson = PackageJsonUtil.findUpPackageJson(vFile);
		var dir = packageJson.getParent().getPath();
		var filename = VfsUtil.getRelativePath(vFile, packageJson.getParent());

		config.setName("ESBench " + filename);
		config.workingDir = dir;
		config.suite = filename;
		config.pattern = getNamePattern(leaf);

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
		var file = location.getVirtualFile();
		if (file == null || !file.isInLocalFileSystem()) {
			return false;
		}
		var dir = config.workingDir;

		var relative = Path.of(dir).relativize(Path.of(file.getPath())).toString();
		relative = FileUtil.toSystemIndependentName(relative);

		var pattern = getNamePattern((LeafPsiElement) location.getPsiElement());

		return config.suite.equals(relative) &&
				config.pattern.equals(pattern);
	}

	private @NonNull String getNamePattern(LeafPsiElement element) {
		if (element.getChars().charAt(0) == 'd' /* defineSuite */) {
			return "";
		}
		var args = ((JSCallExpression) element.getParent().getParent()).getArguments();
		if (args.length == 0 || !(args[0] instanceof JSLiteralExpression literal)) {
			return "";
		}
		if (!(literal.getValue() instanceof String name)) {
			return "";
		}
		return "^" + JSTestRunnerUtil.escapeJavaScriptRegexp(name) + "$";
	}
}
