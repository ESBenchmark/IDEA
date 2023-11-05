package com.kaciras.esbench;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.nodejs.run.NodeJsRunConfiguration;
import com.jetbrains.nodejs.run.NodeJsRunConfigurationType;
import org.jetbrains.annotations.NotNull;

public class RunConfigProducer extends LazyRunConfigurationProducer<NodeJsRunConfiguration> {
	@NotNull
	@Override
	public ConfigurationFactory getConfigurationFactory() {
		return NodeJsRunConfigurationType.getInstance().getConfigurationFactories()[0];
	}

	@Override
	protected boolean setupConfigurationFromContext(
			@NotNull NodeJsRunConfiguration configuration,
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

		configuration.setName("ESBench plugin test");
		configuration.setWorkingDirectory(dir);
		configuration.setMainScriptFilePath(dir + "/node_modules/@esbench/core/bin/cli.js");
		configuration.setApplicationParameters("--file " + filename);

		return true;
	}

	@Override
	public boolean isConfigurationFromContext(
			@NotNull NodeJsRunConfiguration configuration,
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
		return false;
	}
}
