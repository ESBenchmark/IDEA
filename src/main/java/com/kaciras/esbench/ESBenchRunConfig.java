package com.kaciras.esbench;

import com.intellij.execution.Executor;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.javascript.nodejs.debug.NodeDebugRunConfiguration;
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile;
import com.intellij.javascript.nodejs.interpreter.NodeInterpreterUtil;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef;
import com.intellij.javascript.nodejs.util.NodePackage;
import com.intellij.javascript.nodejs.util.NodePackageDescriptor;
import com.intellij.javascript.testing.JsTestConfigurationUtil;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.util.PathUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class ESBenchRunConfig extends AbstractNodeTargetRunProfile implements NodeDebugRunConfiguration {

	public static final NodePackageDescriptor PKG_DESCRIPTOR = new NodePackageDescriptor(ESBenchUtils.PACKAGE);

	@NotNull
	public NodeJsInterpreterRef interpreterRef = NodeJsInterpreterRef.createProjectRef();
	@NotNull
	public String nodeOptions = "--experimental-import-meta-resolve";
	@Nullable
	public NodePackage esbenchPackage;
	@NotNull
	public String configFile = "";
	@NotNull
	public String workingDir = "";
	@NotNull
	public String esbenchOptions = "";
	@NotNull
	public EnvironmentVariablesData envData = EnvironmentVariablesData.DEFAULT;
	@NotNull
	public String suite = "";
	@NotNull
	public String pattern = "";

	public ESBenchRunConfig(Project project, ConfigurationFactory factory, String name) {
		super(project, factory, name);
	}

	@Nullable
	@Override
	public NodeJsInterpreter getInterpreter() {
		return interpreterRef.resolve(getProject());
	}

	@NotNull
	@Override
	public EnvironmentVariablesData getEnvData() {
		return envData;
	}

	@NotNull
	@Override
	public SettingsEditor<ESBenchRunConfig> createConfigurationEditor() {
		return new ConfigurationEditor(getProject());
	}

	public NodePackage resolvePackage() {
		if (esbenchPackage != null) {
			return esbenchPackage;
		}
		var file = LocalFileSystem.getInstance().findFileByPath(suite);
		return PKG_DESCRIPTOR.findFirstDirectDependencyPackage(getProject(), null, file);
	}

	@Nullable
	@Override
	public String suggestedName() {
		return pattern.isEmpty() ? PathUtil.getFileName(suite) : pattern;
	}

	@Override
	public void checkConfiguration() throws RuntimeConfigurationException {
		NodeInterpreterUtil.checkForRunConfiguration(interpreterRef.resolve(getProject()));
		resolvePackage().validateForRunConfiguration(ESBenchUtils.PACKAGE);
		JsTestConfigurationUtil.INSTANCE.validatePath(true, "Working directory", workingDir);

		if (!configFile.isEmpty()) {
			JsTestConfigurationUtil.INSTANCE.validatePath(false, "Configuration file", configFile);
		}

		if (!suite.isEmpty()) {
			try {
				Paths.get(suite);
			} catch (InvalidPathException ignore) {
				throw new RuntimeConfigurationError("Suite file must be a valid path");
			}
		}
	}

	@Override
	public void readConfiguration(@NotNull Element element) throws InvalidDataException {
		envData = EnvironmentVariablesData.readExternal(element);
		workingDir = readXml(element, "working-dir");
		configFile = readXml(element, "config");
		nodeOptions = readXml(element, "node-options");
		suite = readXml(element, "suite");
		pattern = readXml(element, "pattern");
		esbenchOptions = readXml(element, "options");

		var pkg = JDOMExternalizerUtil.readCustomField(element, "esbench-package");
		if (pkg != null) {
			esbenchPackage = PKG_DESCRIPTOR.createPackage(pkg);
		}
		var interpreter = JDOMExternalizerUtil.readCustomField(element, "node-interpreter");
		interpreterRef = NodeJsInterpreterRef.create(interpreter);
	}

	@Override
	public void writeConfiguration(@NotNull Element element) {
		envData.writeExternal(element);
		writeXml(element, "working-dir", workingDir);
		writeXml(element, "config", configFile);
		writeXml(element, "node-options", nodeOptions);
		writeXml(element, "suite", suite);
		writeXml(element, "pattern", pattern);
		writeXml(element, "options", esbenchOptions);

		if (esbenchPackage != null) {
			writeXml(element, "esbench-package", esbenchPackage.getSystemIndependentPath());
		}
		writeXml(element, "node-interpreter", interpreterRef.getReferenceName());
	}

	private static String readXml(Element element, String tag) {
		var value = JDOMExternalizerUtil.readCustomField(element, tag);
		return value == null ? "" : value;
	}

	private static void writeXml(Element element, String tag, String value) {
		if (!value.isEmpty()) {
			JDOMExternalizerUtil.writeCustomField(element, tag, value);
		}
	}

	@Override
	@Nullable
	public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
		return new ESBenchRunState(this, environment);
	}
}
