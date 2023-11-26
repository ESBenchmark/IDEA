package com.kaciras.esbench;

import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField;
import com.intellij.javascript.nodejs.util.NodePackageField;
import com.intellij.lang.javascript.JavaScriptBundle;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.webcore.ui.PathShortener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ConfigurationEditor extends SettingsEditor<ESBenchRunConfig> {

	private final NodeJsInterpreterField interpreter;
	private final RawCommandLineEditor nodeOptions;
	private final TextFieldWithBrowseButton workDir;
	private final TextFieldWithHistoryWithBrowseButton configFile;
	private final NodePackageField packagePath;
	private final EnvironmentVariablesTextFieldWithBrowseButton envVars;
	private final RawCommandLineEditor options;
	private final TextFieldWithBrowseButton suite;
	private final ExtendableTextField pattern;
	private final JPanel panel;

	public ConfigurationEditor(Project project) {
		this.interpreter = new NodeJsInterpreterField(project);
		this.nodeOptions = new RawCommandLineEditor();
		this.workDir = new TextFieldWithBrowseButton();
		this.configFile = new TextFieldWithHistoryWithBrowseButton();
		this.packagePath = new NodePackageField(this.interpreter, ESBenchRunConfig.PKG_DESCRIPTOR, this::contextDir);
		this.envVars = new EnvironmentVariablesTextFieldWithBrowseButton();
		this.options = new RawCommandLineEditor();
		this.suite = new TextFieldWithBrowseButton();
		this.pattern = new ExtendableTextField(0);

		this.panel = new FormBuilder().setAlignLabelOnRight(false)
				.addLabeledComponent("&Configuration file:", this.configFile)
				.addSeparator(8)
				.addLabeledComponent(NodeJsInterpreterField.getLabelTextForComponent(), this.interpreter, 8)
				.addLabeledComponent(JavaScriptBundle.message("rc.nodeOptions.label"), this.nodeOptions)
				.addLabeledComponent("ESBench package:", this.packagePath)
				.addLabeledComponent(JavaScriptBundle.message("rc.workingDirectory.label"), this.workDir)
				.addLabeledComponent("ESBench options", this.options)
				.addLabeledComponent(JavaScriptBundle.message("rc.environmentVariables.label"), this.envVars)
				.addSeparator(8)
				.addLabeledComponent("Suite file:", this.suite)
				.addLabeledComponent("Benchmark name:", this.pattern).getPanel();
	}

	private VirtualFile contextDir() {
		return LocalFileSystem.getInstance().findFileByPath(workDir.getText());
	}

	@Override
	protected @NotNull JComponent createEditor() {
		return this.panel;
	}

	@Override
	protected void resetEditorFrom(@NotNull ESBenchRunConfig config) {
		nodeOptions.setText(config.nodeOptions);
		options.setText(config.esbenchOptions);
		if (config.esbenchPackage == null) {
			packagePath.setSelected(config.resolvePackage());
		}
		envVars.setData(config.envData);
		configFile.setText(config.configFile);
		suite.setText(config.suite);
		pattern.setText(config.pattern);
		workDir.setText(config.workingDir);
		interpreter.setInterpreterRef(config.interpreterRef);
	}

	@Override
	protected void applyEditorTo(@NotNull ESBenchRunConfig config) {
		config.interpreterRef = interpreter.getInterpreterRef();
		config.nodeOptions = nodeOptions.getText();
		config.esbenchOptions = options.getText();
		config.esbenchPackage = packagePath.getSelected();
		config.envData = envVars.getData();
		config.configFile = configFile.getText();
		config.suite = suite.getText();
		config.pattern = pattern.getText();
		config.workingDir = PathShortener.getAbsolutePath(workDir.getTextField());
	}
}
