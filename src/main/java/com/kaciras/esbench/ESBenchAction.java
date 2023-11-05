package com.kaciras.esbench;

import com.intellij.execution.dashboard.actions.RunAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ESBenchAction extends RunAction {

	private final String file;

	public ESBenchAction(String file) {
		this.file = file;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {

	}
}
