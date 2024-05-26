package com.kaciras.esbench;

import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField;
import com.intellij.javascript.nodejs.packages.NodePackagesKt;
import com.intellij.javascript.nodejs.util.NodePackageField;
import com.intellij.lang.javascript.JavaScriptBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.SwingHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class ConfigurationEditor extends SettingsEditor<ESBenchRunConfig> {

	private final TextFieldWithHistoryWithBrowseButton configFile;
	private final NodeJsInterpreterField interpreter;
	private final RawCommandLineEditor nodeOptions;
	private final NodePackageField packagePath;
	private final TextFieldWithBrowseButton workDir;
	private final RawCommandLineEditor options;
	private final EnvironmentVariablesTextFieldWithBrowseButton envVars;
	private final TextFieldWithBrowseButton suite;
	private final ExtendableTextField pattern;

	public ConfigurationEditor(Project project) {
		configFile = new TextFieldWithHistoryWithBrowseButton();
		interpreter = new NodeJsInterpreterField(project);
		nodeOptions = new RawCommandLineEditor();
		packagePath = new NodePackageField(interpreter, ESBenchRunConfig.PKG_DESCRIPTOR, this::workDirEntry);
		workDir = new TextFieldWithBrowseButton();
		options = new RawCommandLineEditor();
		envVars = new EnvironmentVariablesTextFieldWithBrowseButton();
		suite = new TextFieldWithBrowseButton();
		pattern = new ExtendableTextField(0);

		SwingHelper.installFileCompletionAndBrowseDialog(project, configFile, "Select ESBench Configuration File", FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
		SwingHelper.installFileCompletionAndBrowseDialog(project, workDir, JavaScriptBundle.message("rc.workingDirectory.browseDialogTitle"), FileChooserDescriptorFactory.createSingleFolderDescriptor());
		SwingHelper.installFileCompletionAndBrowseDialog(project, suite, "Select Benchmark Suite", FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
	}

	private VirtualFile workDirEntry() {
		return LocalFileSystem.getInstance().findFileByPath(workDir.getText());
	}

	@Override
	@NotNull
	protected JComponent createEditor() {
		return new FormBuilder().setAlignLabelOnRight(false)
				.addLabeledComponent("&Configuration file:", configFile)
				.addSeparator(8)
				.addLabeledComponent(NodeJsInterpreterField.getLabelTextForComponent(), interpreter, 8)
				.addLabeledComponent(JavaScriptBundle.message("rc.nodeOptions.label"), nodeOptions)
				.addLabeledComponent("ESBench package:", packagePath)
				.addLabeledComponent(JavaScriptBundle.message("rc.workingDirectory.label"), workDir)
				.addLabeledComponent("ESBench options", options)
				.addLabeledComponent(JavaScriptBundle.message("rc.environmentVariables.label"), envVars)
				.addSeparator(8)
				.addLabeledComponent("Suite file:", suite)
				.addLabeledComponent("Benchmark name:", pattern).getPanel();
	}

	@Override
	protected void resetEditorFrom(@NotNull ESBenchRunConfig config) {
		interpreter.setInterpreterRef(config.interpreterRef);
		nodeOptions.setText(config.nodeOptions);
		options.setText(config.esbenchOptions);
		if (config.esbenchPackage == null) {
			packagePath.setSelected(config.resolvePackage());
		}
		envVars.setData(config.envData);
		pattern.setText(config.pattern);
		configFile.setText(FileUtil.toSystemDependentName(config.configFile));
		suite.setText(FileUtil.toSystemDependentName(config.suite));
		workDir.setText(FileUtil.toSystemDependentName(config.workingDir));
	}

	@Override
	protected void applyEditorTo(@NotNull ESBenchRunConfig config) {
		config.interpreterRef = interpreter.getInterpreterRef();
		config.nodeOptions = nodeOptions.getText();
		config.esbenchOptions = options.getText();
		config.esbenchPackage = NodePackagesKt.nullize(packagePath.getSelected(), false);
		config.envData = envVars.getData();
		config.pattern = pattern.getText();
		config.configFile = FileUtil.toSystemIndependentName(configFile.getText());
		config.suite = FileUtil.toSystemIndependentName(suite.getText());
		config.workingDir = FileUtil.toSystemIndependentName(workDir.getText());
	}
}
