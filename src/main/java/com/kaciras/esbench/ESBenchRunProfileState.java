package com.kaciras.esbench;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.javascript.debugger.CommandLineDebugConfigurator;
import com.intellij.javascript.nodejs.NodeCommandLineUtil;
import com.intellij.javascript.nodejs.execution.NodeBaseRunProfileState;
import com.intellij.javascript.nodejs.execution.NodeTargetRun;
import com.intellij.javascript.nodejs.execution.NodeTargetRunOptions;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ESBenchRunProfileState implements NodeBaseRunProfileState {

	private final ESBenchRunConfig configuration;
	private final ExecutionEnvironment environment;

	public ESBenchRunProfileState(ESBenchRunConfig configuration, ExecutionEnvironment environment) {
		this.configuration = configuration;
		this.environment = environment;
	}

	@NotNull
	@Override
	public ProcessHandler startProcess(@Nullable CommandLineDebugConfigurator configurator) throws ExecutionException {
		var project = environment.getProject();
		var options = NodeTargetRunOptions.of(null, this.configuration);
		var interpreter = configuration.interpreterRef.resolveNotNull(project);

		try {
			var run = new NodeTargetRun(interpreter, project, configurator, options);
			this.configureCommandLine(run);
			return run.startProcess();
		} catch (UnsupportedOperationException e) {
			throw new ExecutionException(e.getLocalizedMessage());
		}
	}

	@NotNull
	@Override
	public ExecutionResult createExecutionResult(@NotNull ProcessHandler processHandler) {
		ProcessTerminatedListener.attach(processHandler);
		var console = NodeCommandLineUtil.createConsole(processHandler, this.environment.getProject(), true);
		console.attachToProcess(processHandler);
		return new DefaultExecutionResult(console, processHandler);
	}

	private void configureCommandLine(NodeTargetRun targetRun) {
		targetRun.setEnvData(configuration.envData);
		targetRun.addNodeOptionsWithExpandedMacros(false, configuration.nodeOptions);

		var commandLine = targetRun.getCommandLineBuilder();
		commandLine.addParameter("/node_modules/@esbench/core/bin/cli.js");
		commandLine.addParameters(ParametersListUtil.parse(configuration.esbenchOptions, false, true, false));
		commandLine.addParameter("--config");
		commandLine.addParameter(configuration.configFilePath);
		commandLine.addParameter("--file");
		commandLine.addParameter(configuration.suite);
		commandLine.addParameter("--name");
		commandLine.addParameter(configuration.pattern);

		var dir = FileUtil.toSystemDependentName(configuration.workingDir);
		commandLine.setWorkingDirectory(targetRun.path(dir));
	}
}
