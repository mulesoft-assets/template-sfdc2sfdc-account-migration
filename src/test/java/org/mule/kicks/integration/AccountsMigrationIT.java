package org.mule.kicks.integration;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.notification.ServerNotification;
import org.mule.construct.Flow;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.NullPayload;

import com.mulesoft.module.batch.api.BatchJobInstance;
import com.mulesoft.module.batch.api.notification.BatchNotification;
import com.mulesoft.module.batch.api.notification.BatchNotificationListener;
import com.mulesoft.module.batch.engine.BatchJobInstanceAdapter;
import com.mulesoft.module.batch.engine.BatchJobInstanceStore;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule
 * Kick that make calls to external systems.
 * 
 * The test will invoke the batch process and afterwards check that the accounts
 * had been correctly created and that the ones that should be filtered are not
 * in the destination sand box.
 * 
 * @author cesar.garcia
 */
public class AccountsMigrationIT extends AbstractKickTestCase {

	protected static final int TIMEOUT = 60;
	
	private static final String KICK_NAME = "accountmigration";

	private Prober prober;
	protected Boolean failed;
	protected BatchJobInstanceStore jobInstanceStore;

	private static SubflowInterceptingChainLifecycleWrapper checkAccountflow;
	private static List<Map<String, Object>> createdAccounts = new ArrayList<Map<String, Object>>();

	protected class BatchWaitListener implements BatchNotificationListener {

		public synchronized void onNotification(ServerNotification notification) {
			final int action = notification.getAction();

			if (action == BatchNotification.JOB_SUCCESSFUL || action == BatchNotification.JOB_STOPPED) {
				failed = false;
			} else if (action == BatchNotification.JOB_PROCESS_RECORDS_FAILED || action == BatchNotification.LOAD_PHASE_FAILED || action == BatchNotification.INPUT_PHASE_FAILED
					|| action == BatchNotification.ON_COMPLETE_FAILED) {

				failed = true;
			}
		}
	}

	@Before
	public void setUp() throws Exception {
		failed = null;
		jobInstanceStore = muleContext.getRegistry().lookupObject(BatchJobInstanceStore.class);
		muleContext.registerListener(new BatchWaitListener());

		checkAccountflow = getSubFlow("retrieveAccountFlow");
		checkAccountflow.initialise();

		createTestDataInSandBox();
	}

	@After
	public void tearDown() throws Exception {
		failed = null;
		deleteTestDataFromSandBox();
	}

	@Test
	public void testMainFlow() throws Exception {
		Flow flow = getFlow("mainFlow");
		MuleEvent event = flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));
		BatchJobInstance batchJobInstance = (BatchJobInstance) event.getMessage().getPayload();

		this.awaitJobTermination();

		Assert.assertTrue(this.wasJobSuccessful());

		Assert.assertEquals("The account should not have been sync", null, invokeRetrieveAccountFlow(checkAccountflow, createdAccounts.get(0)));
		
		Assert.assertEquals("The account should not have been sync", null, invokeRetrieveAccountFlow(checkAccountflow, createdAccounts.get(1)));

		Map<String, Object> payload = invokeRetrieveAccountFlow(checkAccountflow, createdAccounts.get(2));
		Assert.assertEquals("The account should have been sync", createdAccounts.get(2).get("Name"), payload.get("Name"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> invokeRetrieveAccountFlow(SubflowInterceptingChainLifecycleWrapper flow, Map<String, Object> account) throws Exception {
		Map<String, Object> accountMap = new HashMap<String, Object>();

		accountMap.put("Name", account.get("Name"));

		MuleEvent event = flow.process(getTestEvent(accountMap, MessageExchangePattern.REQUEST_RESPONSE));
		Object payload = event.getMessage().getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, Object>) payload;
		}
	}

	protected void awaitJobTermination() throws Exception {
		this.awaitJobTermination(TIMEOUT);
	}

	protected void awaitJobTermination(long timeoutSecs) throws Exception {
		this.prober = new PollingProber(timeoutSecs * 1000, 500);
		this.prober.check(new Probe() {

			@Override
			public boolean isSatisfied() {
				return failed != null;
			}

			@Override
			public String describeFailure() {
				return "batch job timed out";
			}
		});
	}

	protected boolean wasJobSuccessful() {
		return this.failed != null ? !this.failed : false;
	}

	protected BatchJobInstanceAdapter getUpdatedInstance(BatchJobInstance jobInstance) {
		return this.jobInstanceStore.getJobInstance(jobInstance.getOwnerJobName(), jobInstance.getId());
	}

	@SuppressWarnings("unchecked")
	private void createTestDataInSandBox() throws MuleException, Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createAccountFlow");
		flow.initialise();

		// This account should not be sync as the industry is not Government nor Education
		Map<String, Object> account = createAccount("ANotSyncOne");
		account.put("Industry", "Insurance");
		account.put("NumberOfEmployees", 8000);
		createdAccounts.add(account);

		// This account should not be sync as the number of employees is smaller than 7000
		account = createAccount("ANotSyncTwo");
		account.put("Industry", "Education");
		account.put("NumberOfEmployees", 5000);
		createdAccounts.add(account);
		
		// This account should BE sync
		account = createAccount("AYesSync");
		account.put("Industry", "Education");
		account.put("NumberOfEmployees", 9000);
		createdAccounts.add(account);

		MuleEvent event = flow.process(getTestEvent(createdAccounts, MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdAccounts.get(i).put("Id", results.get(i).getId());
		}
		
		System.out.println("Results of data creation in sandbox" + createdAccounts.toString());
	}

	private void deleteTestDataFromSandBox() throws MuleException, Exception {
		// Delete the created accounts in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteAccountFromAFlow");
		flow.initialise();

		List<Object> idList = new ArrayList<Object>();
		for (Map<String, Object> c : createdAccounts) {
			idList.add(c.get("Id"));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created accounts in B
		flow = getSubFlow("deleteAccountFromBFlow");
		flow.initialise();
		idList.clear();
		for (Map<String, Object> c : createdAccounts) {
			Map<String, Object> account = invokeRetrieveAccountFlow(checkAccountflow, c);
			if (account != null) {
				idList.add(account.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	private Map<String, Object> createAccount(String orgId) {
		Map<String, Object> account = new HashMap<String, Object>();

		String kickName = KICK_NAME;
		String timeStamp = new Long(new Date().getTime()).toString();
		
		account.put("Name", "Name_" + orgId + timeStamp);
		account.put("BillingCity", "San Francisco");
		account.put("BillingCountry", "USA");
		account.put("Phone", "123456789");

		return account;
	}
}
