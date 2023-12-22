package com.kaciras.esbench;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import static com.intellij.icons.AllIcons.Actions.StartDebugger;
import static com.intellij.icons.AllIcons.RunConfigurations.TestState.Run;

public final class ESBenchAction extends AnAction {

	private static final RunConfigProducer producer = new RunConfigProducer();

	private final boolean isDebug;

	public ESBenchAction(String text, boolean isDebug) {
		super(text, text, isDebug ? StartDebugger : Run);
		this.isDebug = isDebug;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		var context = ConfigurationContext.getFromContext(e.getDataContext(), e.getPlace());
		var config = producer.findOrCreateConfigurationFromContext(context);
		if (config != null) {
			var settings = config.getConfigurationSettings();
			context.getRunManager().setTemporaryConfiguration(settings);

			var executor = isDebug
					? DefaultDebugExecutor.getDebugExecutorInstance()
					: DefaultRunExecutor.getRunExecutorInstance();

			ProgramRunnerUtil.executeConfiguration(settings, executor);
		}
	}
}
