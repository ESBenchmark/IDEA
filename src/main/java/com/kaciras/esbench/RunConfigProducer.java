package com.kaciras.esbench;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.execution.ParametersListUtil;
import com.jetbrains.nodejs.run.NodeJsRunConfiguration;
import com.jetbrains.nodejs.run.NodeJsRunConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RunConfigProducer extends LazyRunConfigurationProducer<NodeJsRunConfiguration> {

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

		configuration.setName("ESBench " + filename);
		configuration.setWorkingDirectory(dir);
		configuration.setMainScriptFilePath(dir + "/node_modules/@esbench/core/bin/cli.js");
		configuration.setApplicationParameters(getParameter(filename, getBenchName(jsCallExpression)));

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
		var params = configuration.getApplicationParameters();
		var dir = configuration.getWorkingDirectory();

		if (params == null || dir == null) {
			return false;
		}
		var relative = Path.of(dir).relativize(Path.of(file.getPath())).toString();
		relative = FileUtil.toSystemIndependentName(relative);

		var list = ParametersListUtil.parse(params);
		var name = getBenchName(location.getPsiElement());

		return hasParam(list, "--file", relative)
				&& name == null
				|| hasParam(list, "--name", name);
	}

	private @Nullable String getBenchName(PsiElement element) {
		var args = ((JSCallExpression) element.getParent().getParent()).getArguments();
		if (args.length != 0 && args[0] instanceof JSLiteralExpression literal) {
			return literal.getStringValue();
		}
		return null;
	}

	private String getParameter(String file, @Nullable String name) {
		var list = new ArrayList<String>(4);
		list.add("--file");
		list.add(file);
		if (name != null) {
			list.add("--name");
			list.add(name);
		}
		return ParametersListUtil.join(list);
	}

	private boolean hasParam(List<String> list, String name, String value) {
		var idx = list.indexOf(name) + 1;
		return list.size() > idx && list.get(idx).equals(value);
	}
}
