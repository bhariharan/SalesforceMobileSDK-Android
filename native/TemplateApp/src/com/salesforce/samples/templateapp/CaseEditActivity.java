/*
 * Copyright (c) 2014, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.samples.templateapp;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.sfnative.SalesforceActivity;

/**
 * Displays information about a case, and allows the user to edit a case.
 *
 * @author bhariharan
 */
public class CaseEditActivity extends SalesforceActivity {

	private static final String CASE = "Case";
	private static final String CASE_ID = "Id";
	private static final String CASE_NUMBER = "CaseNumber";
	private static final String CASE_SUBJECT = "Subject";
	private static final String CASE_STATUS = "Status";
	private static final String CASE_DESCRIPTION = "Description";

    private RestClient client;
    private String caseId;
    private String caseNumber;
    private TextView caseNumberView;
    private EditText caseSubjectView;
    private TextView caseStatusView;
    private EditText caseDescriptionView;

    @Override
    public void onCreate(Bundle savedInstance) {
    	super.onCreate(savedInstance);
    	setContentView(R.layout.case_edit_activity);
    	final Intent intent = getIntent();
    	if (intent != null) {
    		caseNumber = intent.getStringExtra(MainActivity.CASE_ID_KEY);
    	}
    	caseNumberView = (TextView) findViewById(R.id.case_number_view);
    	caseSubjectView = (EditText) findViewById(R.id.case_subject_view);
    	caseStatusView = (TextView) findViewById(R.id.case_status_view);
    	caseDescriptionView = (EditText) findViewById(R.id.case_description_view);
    }

	@Override
	public void onResume(RestClient client) {
		this.client = client;
		if (caseNumber != null) {
			try {
				sendRequest("SELECT " + CASE_ID + ", " + CASE_NUMBER + ", "
						+ CASE_SUBJECT + ", " + CASE_STATUS + ", "
						+ CASE_DESCRIPTION + " FROM " + CASE
						+ " WHERE CaseNumber='" + caseNumber + "'");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Called when "Save" button is clicked.
	 *
	 * @param v View that was clicked.
	 */
	public void onSaveClicked(View v) {
		final String subject = (caseSubjectView.getText() != null)
				? caseSubjectView.getText().toString() : "";
		final String description = (caseDescriptionView.getText() != null)
				? caseDescriptionView.getText().toString() : "";
		final Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("Subject", subject);
		fields.put("Description", description);
		try {
			final RestRequest restRequest = RestRequest.getRequestForUpdate(
					getString(R.string.api_version), CASE, caseId, fields);
			client.sendAsync(restRequest, new AsyncRequestCallback() {

				@Override
				public void onSuccess(RestRequest request, RestResponse result) {
					try {
						Toast.makeText(CaseEditActivity.this,
								"Case Successfully Updated!",
								Toast.LENGTH_LONG).show();
					} catch (Exception e) {
						onError(e);
					}
				}

				@Override
				public void onError(Exception exception) {
					exception.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Makes a SOQL request.
	 *
	 * @param soql SOQL statement.
	 * @throws UnsupportedEncodingException
	 */
	private void sendRequest(String soql) throws UnsupportedEncodingException {
		final RestRequest restRequest = RestRequest.getRequestForQuery(getString(R.string.api_version), soql);
		client.sendAsync(restRequest, new AsyncRequestCallback() {

			@Override
			public void onSuccess(RestRequest request, RestResponse result) {
				try {
					JSONArray records = result.asJSONObject().getJSONArray("records");
					for (int i = 0; i < records.length(); i++) {
						caseId = records.getJSONObject(i).optString("Id");
						final String caseNumber = records.getJSONObject(i).optString("CaseNumber");
						final String subject = records.getJSONObject(i).optString("Subject");
						final String status = records.getJSONObject(i).optString("Status");
						final String description = records.getJSONObject(i).optString("Description");
						if (!TextUtils.isEmpty(caseNumber) && !"null".equals(caseNumber)) {
							caseNumberView.setText(caseNumber);
						}
						if (!TextUtils.isEmpty(subject) && !"null".equals(subject)) {
							caseSubjectView.setText(subject);
						}
						if (!TextUtils.isEmpty(status) && !"null".equals(status)) {
							caseStatusView.setText(status);
						}
						if (!TextUtils.isEmpty(description) && !"null".equals(description)) {
							caseDescriptionView.setText(description);
						}
					}		
				} catch (Exception e) {
					onError(e);
				}
			}

			@Override
			public void onError(Exception exception) {
				exception.printStackTrace();
			}
		});
	}	
}
