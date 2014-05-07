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

import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.push.PushNotificationInterface;

/**
 * Receiver for push notifications in this app.
 *
 * @author bhariharan
 */
public class TemplateAppPushReceiver implements PushNotificationInterface {

	public static final String CASE_ID = "case_id";
	public static final String USER_ID = "user_id";
	public static final String NOTIFICATION_ID = "notification_id";

	@Override
	public void onPushMessageReceived(Bundle message) {
		final Context context = SalesforceSDKManager.getInstance().getAppContext();
		final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (message != null) {
			final String caseId = message.getString("Id");
			final String caseNumber = message.getString("CaseNumber");
			final String userId = message.getString("UserId");
			if (!TextUtils.isEmpty(caseNumber)) {
				final String notifMsg = "Case number " + caseNumber + " updated!";
				final Random gen = new Random();
				int notifId = gen.nextInt();
				final Notification notification = new Notification(R.drawable.sf__icon,
						notifMsg, System.currentTimeMillis());
				notification.setLatestEventInfo(context, "TemplateApp",
						notifMsg, buildPendingIntent(caseId, userId, notifId));
				nm.notify(notifId, notification);
			}
		}
	}

	/**
	 * Builds a pending intent that is triggered when the notification is clicked.
	 *
	 * @param caseId Case ID.
	 * @param userId User ID.
	 * @param notifId Notification ID.
	 * @return PendingIntent instance.
	 */
	private PendingIntent buildPendingIntent(String caseId, String userId, int notifId) {
		final Context context = SalesforceSDKManager.getInstance().getAppContext();
		final Intent intent = new Intent(context, CaseActivity.class);
		intent.setAction(Long.toString(System.currentTimeMillis()));
		intent.putExtra(CASE_ID, caseId);
		intent.putExtra(USER_ID, userId);
		intent.putExtra(NOTIFICATION_ID, notifId);
		final PendingIntent pIntent = PendingIntent.getActivity(context,
				0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		return pIntent;
	}
}
