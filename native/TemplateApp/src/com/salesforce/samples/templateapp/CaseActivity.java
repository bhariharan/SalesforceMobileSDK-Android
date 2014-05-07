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

import org.json.JSONArray;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.sfnative.SalesforceActivity;

/**
 * Displays information about a case.
 *
 * @author bhariharan
 */
public class CaseActivity extends SalesforceActivity {

	private static final String CASE = "Case";
	private static final String CASE_NUMBER = "CaseNumber";
	private static final String CASE_SUBJECT = "Subject";
	private static final String CASE_STATUS = "Status";
	private static final String CASE_DESCRIPTION = "Description";
	private static final String USER = "User";
	private static final String USER_NAME = "Name";
	private static final String USER_EMAIL = "Email";

    private RestClient client;
    private String caseId;
    private String userId;
    private int notifId;
    private TextView caseNumberView;
    private TextView caseSubjectView;
    private TextView caseStatusView;
    private TextView caseDescriptionView;
    private TextView userNameView;
    private TextView userEmailView;

    @Override
    public void onCreate(Bundle savedInstance) {
    	super.onCreate(savedInstance);
    	setContentView(R.layout.case_activity);
    	final Intent intent = getIntent();
    	if (intent != null) {
    		caseId = intent.getStringExtra(TemplateAppPushReceiver.CASE_ID);
    		userId = intent.getStringExtra(TemplateAppPushReceiver.USER_ID);
    		notifId = intent.getIntExtra(TemplateAppPushReceiver.NOTIFICATION_ID, 0);
    		if (notifId != 0) {
        		final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        		nm.cancel(notifId);
    		}
    	}
    	caseNumberView = (TextView) findViewById(R.id.case_number_view);
    	caseSubjectView = (TextView) findViewById(R.id.case_subject_view);
    	caseStatusView = (TextView) findViewById(R.id.case_status_view);
    	caseDescriptionView = (TextView) findViewById(R.id.case_description_view);
    	userNameView = (TextView) findViewById(R.id.user_name_view);
    	userEmailView = (TextView) findViewById(R.id.user_email_view);
    }

	@Override
	public void onResume(RestClient client) {
		this.client = client;
		if (caseId != null) {
			try {
				sendRequest("SELECT " + CASE_NUMBER + ", " + CASE_SUBJECT + ", "
						+ CASE_STATUS + ", " + CASE_DESCRIPTION + " FROM " + CASE
						+ " WHERE Id='" + caseId + "'", CASE);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		if (userId != null) {
			try {
				sendRequest("SELECT " + USER_NAME + ", " + USER_EMAIL + " FROM "
						+ USER + " WHERE Id='" + userId + "'", USER);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Makes a SOQL request.
	 *
	 * @param soql SOQL statement.
	 * @param sObject Object.
	 * @throws UnsupportedEncodingException
	 */
	private void sendRequest(String soql, final String sObject) throws UnsupportedEncodingException {
		final RestRequest restRequest = RestRequest.getRequestForQuery(getString(R.string.api_version), soql);
		client.sendAsync(restRequest, new AsyncRequestCallback() {

			@Override
			public void onSuccess(RestRequest request, RestResponse result) {
				try {
					JSONArray records = result.asJSONObject().getJSONArray("records");
					for (int i = 0; i < records.length(); i++) {
						if (CASE.equals(sObject)) {
							final String caseNumber = records.getJSONObject(i).optString("CaseNumber");
							final String subject = records.getJSONObject(i).optString("Subject");
							final String status = records.getJSONObject(i).optString("Status");
							final String description = records.getJSONObject(i).optString("Description");
							if (!TextUtils.isEmpty(caseNumber)) {
								caseNumberView.setText(caseNumber);
							}
							if (!TextUtils.isEmpty(subject)) {
								caseSubjectView.setText(subject);
							}
							if (!TextUtils.isEmpty(status)) {
								caseStatusView.setText(status);
							}
							if (!TextUtils.isEmpty(description)) {
								caseDescriptionView.setText(description);
							}
						} else if (USER.equals(sObject)) {
							final String name = records.getJSONObject(i).optString("Name");
							final String email = records.getJSONObject(i).optString("Email");
							if (!TextUtils.isEmpty(name)) {
								userNameView.setText(name);
							}
							if (!TextUtils.isEmpty(email)) {
								userEmailView.setText(email);
							}
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
