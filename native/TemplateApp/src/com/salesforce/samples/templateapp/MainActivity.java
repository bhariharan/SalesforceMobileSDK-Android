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
import java.util.ArrayList;

import org.json.JSONArray;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.sfnative.SalesforceActivity;

/**
 * Main activity.
 */
public class MainActivity extends SalesforceActivity {

	public static final String CASE_ID_KEY = "case_id";

    private RestClient client;
    private ArrayAdapter<String> listAdapter;
    private ListView list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup view.
		setContentView(R.layout.main);
		list = (ListView) findViewById(R.id.cases_list);
	}

	@Override 
	public void onResume() {

		// Hide everything until we are logged in.
		findViewById(R.id.root).setVisibility(View.INVISIBLE);

		// Create list adapter.
		listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
				new ArrayList<String>());
		list.setAdapter(listAdapter);
		super.onResume();
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
            public void onItemClick(AdapterView<?> parent, View view,
            		int position, long id) {
                final Intent i = new Intent(MainActivity.this, CaseEditActivity.class);
                i.putExtra(CASE_ID_KEY, ((TextView) parent.getChildAt(position)).getText());
                startActivity(i);
            }
		});
	}

	@Override
	public void onResume(RestClient client) {

        // Keeping reference to rest client.
        this.client = client;

		// Show everything.
		findViewById(R.id.root).setVisibility(View.VISIBLE);
	}

	/**
	 * Called when "Logout" button is clicked.
	 *
	 * @param v View that was clicked.
	 */
	public void onLogoutClick(View v) {
		SalesforceSDKManager.getInstance().logout(this);
	}

	/**
	 * Called when "Switch User" button is clicked.
	 *
	 * @param v View that was clicked.
	 */
	public void onSwitchUserClick(View v) {
		final Intent i = new Intent(this,
				SalesforceSDKManager.getInstance().getAccountSwitcherActivityClass());
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.startActivity(i);
	}

	/**
	 * Called when "Clear" button is clicked.
	 * 
	 * @param v View that was clicked.
	 */
	public void onClearClick(View v) {
		listAdapter.clear();
	}	

	/**
	 * Called when "Fetch Cases" button is clicked.
	 *
	 * @param v View that was clicked.
	 */
	public void onFetchCasesClick(View v) throws UnsupportedEncodingException {
        sendRequest("SELECT CaseNumber FROM Case WHERE OwnerId = '"
        		+ client.getClientInfo().userId + "'");
	}

	private void sendRequest(String soql) throws UnsupportedEncodingException {
		final RestRequest restRequest = RestRequest.getRequestForQuery(getString(R.string.api_version), soql);
		client.sendAsync(restRequest, new AsyncRequestCallback() {

			@Override
			public void onSuccess(RestRequest request, RestResponse result) {
				try {
					listAdapter.clear();
					JSONArray records = result.asJSONObject().getJSONArray("records");
					for (int i = 0; i < records.length(); i++) {
						listAdapter.add(records.getJSONObject(i).getString("CaseNumber"));
					}		
				} catch (Exception e) {
					onError(e);
				}
			}

			@Override
			public void onError(Exception exception) {
                Toast.makeText(MainActivity.this,
                		MainActivity.this.getString(SalesforceSDKManager.getInstance().getSalesforceR().stringGenericError(), exception.toString()),
                        Toast.LENGTH_LONG).show();
			}
		});
	}
}
