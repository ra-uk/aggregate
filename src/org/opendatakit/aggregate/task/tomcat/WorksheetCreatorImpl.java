/*
 * Copyright (C) 2010 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.aggregate.task.tomcat;

import java.util.Map;

import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.constants.common.ExternalServiceOption;
import org.opendatakit.aggregate.constants.externalservice.ExternalServiceConsts;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.form.Form;
import org.opendatakit.aggregate.form.MiscTasks;
import org.opendatakit.aggregate.submission.Submission;
import org.opendatakit.aggregate.submission.SubmissionKey;
import org.opendatakit.aggregate.task.WorksheetCreator;
import org.opendatakit.aggregate.task.WorksheetCreatorWorkerImpl;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.web.CallingContext;

/**
 * This is a singleton bean.  It cannot have any per-request state.
 * It uses a static inner class to encapsulate the per-request state
 * of a running background task.
 * 
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public class WorksheetCreatorImpl implements WorksheetCreator {

	static class WorksheetCreatorRunner implements Runnable {
		final WorksheetCreatorWorkerImpl impl;

		public WorksheetCreatorRunner(Form form, SubmissionKey miscTasksKey,
				long attemptCount, 
				String spreadsheetName, ExternalServiceOption esType,
				CallingContext cc) {
			impl = new WorksheetCreatorWorkerImpl(form, miscTasksKey, 
					attemptCount, 
					spreadsheetName, esType, cc);
		}

		@Override
		public void run() {
			try {
				impl.worksheetCreator();
			} catch (Exception e) {
				e.printStackTrace();
				// TODO: Problem - decide what to do if an exception occurs
			}
		}
	}

	@Override
	public final void createWorksheetTask(Form form, SubmissionKey miscTasksKey, long attemptCount,
			CallingContext cc) throws ODKDatastoreException, ODKFormNotFoundException {
		Submission s = Submission.fetchSubmission(miscTasksKey.splitSubmissionKey(), cc);
	    MiscTasks r = new MiscTasks(s);
	    Map<String,String> params = r.getRequestParameters();
	    String esTypeString = params.get(ServletConsts.EXTERNAL_SERVICE_TYPE);
	    if (esTypeString == null) {
	        throw new IllegalStateException("no external service type specified on create worksheet task");
	    }
	    ExternalServiceOption esType = ExternalServiceOption.valueOf(esTypeString);
	    if (esType == null) {
	    	throw new IllegalStateException("external service type not recognized in create worksheet task");
	    }
	    String spreadsheetName = params.get(ExternalServiceConsts.EXT_SERV_ADDRESS);
	    if (spreadsheetName == null) {
	    	throw new IllegalStateException("spreadsheet name is null in create worksheet task");
	    }
	    
	    WorksheetCreatorRunner wr = new WorksheetCreatorRunner( form, miscTasksKey,
	    		attemptCount, 
				spreadsheetName, esType,
				cc );
		System.out.println("THIS IS CREATE WORKSHEET IN TOMCAT");
		AggregrateThreadExecutor exec = AggregrateThreadExecutor
				.getAggregateThreadExecutor();
		exec.execute(wr);
	}
}
