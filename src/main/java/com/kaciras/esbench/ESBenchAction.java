package com.kaciras.esbench;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public final class ESBenchAction extends AnAction {

	private final String file;

	public ESBenchAction(String file) {
		this.file = file;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		var context = ConfigurationContext.getFromContext(e.getDataContext(), e.getPlace());
		var producer = new RunConfigProducer();

		var config = producer.findOrCreateConfigurationFromContext(context);
		if (config == null) {
			return;
		}

		var executor = DefaultRunExecutor.getRunExecutorInstance();
		var settings = config.getConfigurationSettings();
		context.getRunManager().setTemporaryConfiguration(settings);

		try {
			ExecutionEnvironmentBuilder.create(e.getProject(), executor, config.getConfiguration()).buildAndExecute();
		} catch (ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}
}
