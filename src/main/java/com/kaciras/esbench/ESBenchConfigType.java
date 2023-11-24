package com.kaciras.esbench;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

import static com.intellij.icons.AllIcons.Actions.ProfileYellow;

public final class ESBenchConfigType extends SimpleConfigurationType {

	static final String ID = "ESBenchConfiguration";

	public ESBenchConfigType() {
		super(ID, "ESBench", null, NotNullLazyValue.createValue(() -> ProfileYellow));
	}

	@NotNull
	@Override
	public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
		return new ESBenchRunConfig(project, this, "ESBench");
	}
}
