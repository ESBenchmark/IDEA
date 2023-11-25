package com.kaciras.esbench;

import com.intellij.lang.ecmascript6.psi.impl.ES6ImportPsiUtil;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

import java.util.Arrays;

public final class ESBenchUtils {

	public static final String CLIENT_MODULE = "\"@esbench/core/client\"";
	public static final String HOST_MODULE = "\"@esbench/core\"";
	public static final String BENCH_1 = "bench";
	public static final String BENCH_2 = "benchAsync";
	public static final String DEFINE_SUITE = "defineSuite";
	public static final String DEFINE_CONFIG = "defineConfig";

	private ESBenchUtils() {}

	public static CharSequence getFuncName(JSCallExpression call) {
		var func = call.getMethodExpression();
		if (func == null) {
			return "";
		}
		if (!(func.getReference() instanceof JSReferenceExpression ref)) {
			return "";
		}
		var name = ref.getReferenceNameElement();
		return name instanceof LeafPsiElement leaf ? leaf.getChars() : "";
	}

	/**
	 * Check the file has <code>import { defineSuite } from "@esbench/core/client"</code>
	 */
	public static boolean hasImportDefineSuite(PsiFile file) {
		return hasImport(file, CLIENT_MODULE, DEFINE_SUITE);
	}

	/**
	 * Check the file has <code>import { defineConfig } from "@esbench/core"</code>
	 */
	public static boolean hasImportDefineConfig(PsiFile file) {
		return hasImport(file, HOST_MODULE, DEFINE_CONFIG);
	}

	private static boolean hasImport(PsiFile file, String from, String specifier) {
		return ES6ImportPsiUtil.getImportDeclarations(file)
				.stream()
				.filter(i -> from.equals(ES6ImportPsiUtil.getFromClauseText(i)))
				.flatMap(i -> Arrays.stream(i.getImportSpecifiers()))
				.anyMatch(s -> s.textMatches(specifier));
	}
}
