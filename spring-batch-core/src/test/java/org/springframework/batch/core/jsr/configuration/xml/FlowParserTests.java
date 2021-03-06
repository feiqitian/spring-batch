/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.batch.core.jsr.JsrTestUtils.restartJob;
import static org.springframework.batch.core.jsr.JsrTestUtils.runJob;

import java.util.List;
import java.util.Properties;

import javax.batch.api.AbstractBatchlet;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.junit.Test;
import org.springframework.batch.core.ExitStatus;

/**
 * <p>
 * Unit tests around {@link FlowParser}.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class FlowParserTests {
	@Test
	public void testDuplicateTransitionPatternsAllowed() throws Exception {
		JobExecution stoppedExecution = runJob("FlowParserTests-context", new Properties(), 10000l);
		assertEquals(ExitStatus.STOPPED.getExitCode(), stoppedExecution.getExitStatus());

		JobExecution endedExecution = restartJob(stoppedExecution.getExecutionId(), new Properties(), 10000l);
		assertEquals(ExitStatus.COMPLETED.getExitCode(), endedExecution.getExitStatus());
	}

	@Test
	public void testWildcardAddedLastWhenUsedWithNextAttrAndNoTransitionElements() throws Exception {
		JobExecution jobExecution = runJob("FlowParserTestsWildcardAndNextAttrJob", new Properties(), 1000l);
		assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus());

		JobOperator jobOperator = BatchRuntime.getJobOperator();
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecution.getExecutionId());
		assertEquals(1, stepExecutions.size());
		StepExecution failedStep = stepExecutions.get(0);
		assertTrue("step1".equals(failedStep.getStepName()));
	}

	public static class TestBatchlet extends AbstractBatchlet {
		private static int CNT;

		@Inject
		private StepContext stepContext;

		@Override
		public String process() throws Exception {
			String exitCode = "DISTINCT";

			if("step3".equals(stepContext.getStepName())) {
				exitCode = CNT % 2 == 0 ? "DISTINCT" : "RESTART";
				CNT++;
			}

			return exitCode;
		}
	}

	public static class FailingTestBatchlet extends AbstractBatchlet {
		@Override
		public String process() throws Exception {
			throw new RuntimeException("blah");
		}
	}
}
