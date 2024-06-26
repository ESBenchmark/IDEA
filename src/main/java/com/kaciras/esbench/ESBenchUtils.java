package com.kaciras.esbench;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.lang.ecmascript6.psi.JSExportAssignment;
import com.intellij.lang.ecmascript6.psi.impl.ES6ImportPsiUtil;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.openapi.util.text.Strings;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

import java.util.Arrays;

public final class ESBenchUtils {

	public static final String PACKAGE = "esbench";

	public static final String CLIENT_MODULE = PACKAGE;
	public static final String HOST_MODULE = "esbench/host";

	public static final String BENCH_1 = "bench";
	public static final String BENCH_2 = "benchAsync";

	public static final String DEFINE_SUITE = "defineSuite";
	public static final String DEFINE_CONFIG = "defineConfig";

	//@formatter:off
	private ESBenchUtils() {}

	static JSCallExpression getCallFromLeaf(PsiElement psi) {
		return psi.getParent() instanceof JSReferenceExpression ref
			&& ref.getParent() instanceof JSCallExpression call ? call : null;
	}

	/**
	 * Check if both configs are ESBenchRunConfig and created from the same element.
	 */
	public static boolean isReusable(RunConfiguration self, RunConfiguration that) {
		return self instanceof ESBenchRunConfig a
			&& that instanceof ESBenchRunConfig b
			&& a.suite.equals(b.suite)
			&& a.pattern.equals(b.pattern)
			&& a.configFile.equals(b.configFile)
			&& a.workingDir.equals(b.workingDir);
	}
	//@formatter:on

	/**
	 * Check the statement of the call is `export default defineSuite()`
	 */
	public static boolean isExportDefineSuite(JSCallExpression call) {
		return call.getParent() instanceof JSExportAssignment
				&& DEFINE_SUITE.equals(getMethodName(call));
	}

	/**
	 * Get the function name of the call, return empty string if it does not have a name.
	 */
	public static String getMethodName(JSCallExpression call) {
		var func = call.getMethodExpression();
		if (func == null) {
			return "";
		}
		if (!(func.getReference() instanceof JSReferenceExpression ref)) {
			return "";
		}
		var name = ref.getReferenceNameElement();
		return name instanceof LeafPsiElement leaf ? leaf.getText() : "";
	}

	/**
	 * Check the file has <code>import { defineSuite } from "esbench"</code>
	 */
	public static boolean hasImportDefineSuite(PsiFile file) {
		return hasImport(file, CLIENT_MODULE, DEFINE_SUITE);
	}

	/**
	 * Check the file has <code>import { defineConfig } from "esbench/host"</code>
	 */
	public static boolean hasImportDefineConfig(PsiFile file) {
		return hasImport(file, HOST_MODULE, DEFINE_CONFIG);
	}

	private static boolean hasImport(PsiFile file, String from, String specifier) {
		return ES6ImportPsiUtil.getImportDeclarations(file)
				.stream()
				.filter(i -> matchUnquoted(ES6ImportPsiUtil.getFromClauseText(i), from))
				.flatMap(i -> Arrays.stream(i.getImportSpecifiers()))
				.anyMatch(s -> s.textMatches(specifier));
	}

	/**
	 * Check if the value without quote is equals to the target. For convenience,
	 * the value parameter accepts null and returns false in this case.
	 * <p>
	 * This method is 6.3x faster than <code>JSStringUtil.unquoteStringLiteralValue().equals()</code>
	 * <p>
	 * <code>
	 * matchUnquoted(null, "foo"); -> false
	 * matchUnquoted("foo", "foo"); -> false
	 * matchUnquoted("'foo'", "foo"); -> true
	 * </code>
	 *
	 * @param value  The maybe quoted text.
	 * @param target Unquoted text.
	 * @return true if the value without quote is equals to the target, otherwise false.
	 */
	public static boolean matchUnquoted(String value, String target) {
		if (value == null) {
			return false;
		}
		var length = value.length();
		if (length != target.length() + 2) {
			return false;
		}
		var quote = value.charAt(0);
		return (quote == '"' || quote == '\'')
				&& value.charAt(length - 1) == quote
				&& value.indexOf(target) == 1;
	}

	/**
	 * Get the name of the benchmark case defined by `bench[Async]()`.
	 *
	 * @param expression The expression of `bench[Async]()` call.
	 * @return Name of the case, or null if arguments is invalid.
	 */
	public static String getBenchName(JSCallExpression expression) {
		var args = expression.getArguments();
		if (args.length < 2) {
			return null;
		}
		if (!(args[0] instanceof JSLiteralExpression literal)) {
			return null;
		}
		return Strings.nullize(literal.getStringValue(), true);
	}
}
