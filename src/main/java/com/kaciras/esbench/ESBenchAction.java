package com.kaciras.esbench;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public final class ESBenchAction extends AnAction {

	private static final RunConfigProducer producer = new RunConfigProducer();

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		var context = ConfigurationContext.getFromContext(e.getDataContext(), e.getPlace());
		var config = producer.findOrCreateConfigurationFromContext(context);
		if (config != null) {
			var executor = DefaultRunExecutor.getRunExecutorInstance();
			var settings = config.getConfigurationSettings();
			context.getRunManager().setTemporaryConfiguration(settings);

			ProgramRunnerUtil.executeConfiguration(settings, executor);
		}
	}
}
