package com.kaciras.esbench;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.javascript.debugger.CommandLineDebugConfigurator;
import com.intellij.javascript.nodejs.NodeCommandLineUtil;
import com.intellij.javascript.nodejs.NodeConsoleAdditionalFilter;
import com.intellij.javascript.nodejs.NodeStackTraceFilter;
import com.intellij.javascript.nodejs.execution.NodeBaseRunProfileState;
import com.intellij.javascript.nodejs.execution.NodeTargetRun;
import com.intellij.javascript.nodejs.execution.NodeTargetRunOptions;
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil;
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
		var project = this.environment.getProject();
		var workingDir = this.configuration.workingDir;

		var console = NodeCommandLineUtil.createConsole(processHandler, project, true);
		console.addMessageFilter(new NodeStackTraceFilter(project, workingDir, NodeTargetRun.getTargetRun(processHandler)));
		console.addMessageFilter(new NodeConsoleAdditionalFilter(project, workingDir));
		console.attachToProcess(processHandler);

		return new DefaultExecutionResult(console, processHandler);
	}

	private void configureCommandLine(NodeTargetRun targetRun) throws ExecutionException {
		targetRun.setEnvData(configuration.envData);
		targetRun.addNodeOptionsWithExpandedMacros(false, configuration.nodeOptions);

		var commandLine = targetRun.getCommandLineBuilder();

		var pkg = configuration.resolvePackage();
		var binFile = PackageJsonUtil.guessDefaultBinaryNameOfDependency(pkg);
		var cli = pkg.findBinFile(binFile, null);
		if (cli == null) {
			throw new ExecutionException("Can't find ESBench bin file.");
		}

		commandLine.addParameter(targetRun.path(cli.toPath()));
		commandLine.addParameters(ParametersListUtil.parse(configuration.esbenchOptions, false, true, false));
		commandLine.addParameter("--file");
		commandLine.addParameter(configuration.suite);

		if (!configuration.pattern.isEmpty()) {
			commandLine.addParameter("--name");
			commandLine.addParameter(configuration.pattern);
		}

		if (!configuration.configFile.isEmpty()) {
			commandLine.addParameter("--config");
			commandLine.addParameter(configuration.configFile);
		}

		var dir = FileUtil.toSystemDependentName(configuration.workingDir);
		commandLine.setWorkingDirectory(targetRun.path(dir));
	}
}
