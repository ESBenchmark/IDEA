package com.kaciras.esbench;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.filters.UrlFilter;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.javascript.debugger.CommandLineDebugConfigurator;
import com.intellij.javascript.nodejs.NodeConsoleAdditionalFilter;
import com.intellij.javascript.nodejs.NodeStackTraceFilter;
import com.intellij.javascript.nodejs.execution.NodeBaseRunProfileState;
import com.intellij.javascript.nodejs.execution.NodeTargetRun;
import com.intellij.javascript.nodejs.execution.NodeTargetRunOptions;
import com.intellij.lang.javascript.ConsoleCommandLineFolder;
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PathUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ESBenchRunState implements NodeBaseRunProfileState {

	private final ESBenchRunConfig configuration;
	private final ExecutionEnvironment environment;
	private final ConsoleCommandLineFolder folder;

	public ESBenchRunState(ESBenchRunConfig configuration, ExecutionEnvironment environment) {
		this.configuration = configuration;
		this.environment = environment;
		this.folder = new ConsoleCommandLineFolder("esbench");
	}

	@NotNull
	@Override
	public ProcessHandler startProcess(@Nullable CommandLineDebugConfigurator configurator) throws ExecutionException {
		var project = environment.getProject();
		var options = NodeTargetRunOptions.of(false, this.configuration);
		var interpreter = configuration.interpreterRef.resolveNotNull(project);

		try {
			var run = new NodeTargetRun(interpreter, project, configurator, options);
			this.configureCommandLine(run);
			return run.startProcess();
		} catch (UnsupportedOperationException e) {
			throw new ExecutionException(e.getLocalizedMessage());
		}
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

		var parsed = ParametersListUtil.parse(configuration.esbenchOptions, false, true, false);
		commandLine.addParameters(parsed);
		folder.addPlaceholderTexts(parsed);

		if (!configuration.suite.isEmpty()) {
			commandLine.addParameter("--file");
			commandLine.addParameter(targetRun.path(configuration.suite));
			folder.addPlaceholderText("--file");
			folder.addPlaceholderText(PathUtil.getFileName(configuration.suite));
		}

		if (!configuration.configFile.isEmpty()) {
			commandLine.addParameter("--config");
			commandLine.addParameter(targetRun.path(configuration.configFile));
			folder.addPlaceholderText("--config");
			folder.addPlaceholderText(PathUtil.getFileName(configuration.configFile));
		}

		if (!configuration.pattern.isEmpty()) {
			commandLine.addParameter("--name");
			commandLine.addParameter(configuration.pattern);
			folder.addPlaceholderText("--name");
			folder.addPlaceholderText(configuration.pattern);
		}

		var dir = FileUtil.toSystemDependentName(configuration.workingDir);
		commandLine.setWorkingDirectory(targetRun.path(dir));
	}

	@NotNull
	@Override
	public ExecutionResult createExecutionResult(@NotNull ProcessHandler processHandler) {
		ProcessTerminatedListener.attach(processHandler);
		var project = this.environment.getProject();
		var workingDir = this.configuration.workingDir;

		// Command line folder doesn't work with predefined filters.
		var console = new ConsoleViewImpl(project, GlobalSearchScope.allScope(project), true, false);
		console.addMessageFilter(new NodeStackTraceFilter(project, workingDir, NodeTargetRun.getTargetRun(processHandler)));
		console.addMessageFilter(new NodeConsoleAdditionalFilter(project, workingDir));
		console.addMessageFilter(new UrlFilter(project));
		console.attachToProcess(processHandler);

		folder.foldCommandLine(console, processHandler);
		return new DefaultExecutionResult(console, processHandler);
	}
}
