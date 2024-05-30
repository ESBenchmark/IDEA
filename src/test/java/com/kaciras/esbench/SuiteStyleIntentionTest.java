package com.kaciras.esbench;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Assert;

public class SuiteStyleIntentionTest extends BasePlatformTestCase {

	@Override
	protected String getTestDataPath() {
		return "src/test/testData";
	}

	protected void doTest(String before, String after) {
		myFixture.configureByFile(before);
		var action = myFixture.findSingleIntention("ESBench: Convert suite style");
		Assert.assertNotNull(action);
		myFixture.launchAction(action);
		myFixture.checkResultByFile(after);
	}

	public void testFunctionToObject() {
		doTest("function.ts", "object.ts");
	}

	public void testObjectToFunction() {
		doTest("object.ts", "function.ts");
	}
}
