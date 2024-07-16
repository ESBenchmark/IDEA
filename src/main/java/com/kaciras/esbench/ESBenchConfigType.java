package com.kaciras.esbench;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class ESBenchConfigType extends SimpleConfigurationType {

	public static final String ID = "ESBenchConfiguration";
	public static final String NAME = "ESBench";

	public ESBenchConfigType() {
		super(ID, NAME, null, NotNullLazyValue.lazy(ESBenchConfigType::pluginIcon));
	}

	private static Icon pluginIcon() {
		return IconLoader.getIcon("/icons/ESBench.svg", ESBenchConfigType.class);
	}

	@NotNull
	public RunConfigurationSingletonPolicy getSingletonPolicy() {
		return RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY;
	}

	@NotNull
	@Override
	public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
		return new ESBenchRunConfig(project, this, NAME);
	}
}
