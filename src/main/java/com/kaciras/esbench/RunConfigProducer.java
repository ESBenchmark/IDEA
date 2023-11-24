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
			@NotNull ESBenchRunConfig configuration,
			@NotNull ConfigurationContext context,
			@NotNull Ref<PsiElement> sourceElement
	) {
		var jsCallExpression = sourceElement.get();
		var psiFile = jsCallExpression.getContainingFile();
		var vFile = psiFile.getVirtualFile();

		if (vFile == null) {
			return false;
		}

		var packageJson = PackageJsonUtil.findUpPackageJson(vFile);
		var dir = packageJson.getParent().getPath();
		var filename = VfsUtil.getRelativePath(vFile, packageJson.getParent());

		configuration.setName("ESBench " + filename);
		configuration.workingDir = dir;
		configuration.suite = filename;
		configuration.pattern = getNamePattern(jsCallExpression);
//		configuration.setMainScriptFilePath(dir + "/node_modules/@esbench/core/bin/cli.js");

		return true;
	}

	@Override
	public boolean isConfigurationFromContext(
			@NotNull ESBenchRunConfig configuration,
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
		var dir = configuration.workingDir;

		var relative = Path.of(dir).relativize(Path.of(file.getPath())).toString();
		relative = FileUtil.toSystemIndependentName(relative);

		var pattern = getNamePattern(location.getPsiElement());

		return configuration.suite.equals(relative) &&
				configuration.pattern.equals(pattern);
	}

	private @NonNull String getNamePattern(PsiElement element) {
		var args = ((JSCallExpression) element.getParent().getParent()).getArguments();
		if (args.length == 0 || !(args[0] instanceof JSLiteralExpression literal)) {
			return "";
		}
		var name = literal.getStringValue();
		if (name == null) {
			return "";
		}
		return "^" + JSTestRunnerUtil.escapeJavaScriptRegexp(name) + "$";
	}
}
