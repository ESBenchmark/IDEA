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
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
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
		var leaf = sourceElement.get();

		var psiFile = leaf.getContainingFile();
		var vFile = psiFile.getVirtualFile();

		if (vFile == null) {
			return false;
		}

		var dir = guessWorkingDir(vFile);
		var filename = VfsUtil.getRelativePath(vFile, dir);

		config.setName("ESBench " + filename);
		config.workingDir = dir.getPath();
		config.suite = filename;
		config.pattern = getNamePattern(leaf);

		return true;
	}

	@Override
	public boolean isConfigurationFromContext(
			@NotNull ESBenchRunConfig config,
			@NotNull ConfigurationContext context
	) {
		var template = (ESBenchRunConfig) this.cloneTemplateConfiguration(context)
				.getConfiguration();

		var location = context.getLocation();
		if (location == null) {
			return false;
		}
		var file = location.getVirtualFile();
		if (file == null || !file.isInLocalFileSystem()) {
			return false;
		}

		var workingDir = guessWorkingDir(file);
		var filename = VfsUtil.getRelativePath(file, workingDir);

		var pattern = getNamePattern(location.getPsiElement());

		return config.configFile.equals(template.workingDir)
				&& config.workingDir.equals(workingDir.getPath())
				&& config.suite.equals(filename)
				&& config.pattern.equals(pattern);
	}

	private VirtualFile guessWorkingDir(VirtualFile suiteFile) {
		return PackageJsonUtil.findUpPackageJson(suiteFile).getParent();
	}

	private @NonNull String getNamePattern(PsiElement element) {
		if (((LeafPsiElement) element).getChars().charAt(0) == 'd' /* defineSuite */) {
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
