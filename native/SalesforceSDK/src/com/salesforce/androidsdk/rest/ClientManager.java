/*
 * Copyright (c) 2011, salesforce.com, inc.
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
package com.salesforce.androidsdk.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AccountsException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.salesforce.androidsdk.app.ForceApp;
import com.salesforce.androidsdk.auth.AuthenticatorService;
import com.salesforce.androidsdk.auth.HttpAccess;
import com.salesforce.androidsdk.security.Encryptor;

/**
 * ClientManager is a factory class for RestClient which stores oauth credentials in the AccountManager.
 * If no account is found, it kicks off the login flow which create a new account if successful.
 * 
 */
public class ClientManager {

	private final AccountManager accountManager;
	private final String accountType;
	private String passcodeHash;

	/**
	 * Construct a ClientManager using the app's accountType and passcodeHash
	 * @param ctx
	 */
	public ClientManager(Context ctx) {
		this(ctx, ForceApp.APP.getAccountType(), ForceApp.APP.getPasscodeManager().getPasscodeHash());
	}
	
	/**
	 * Construct a ClientManager using a custom account type
	 * @param ctx
	 * @param accountType
	 * @param passcodeHash           key to encrypt/decrypt oauth tokens that get stored in the account manager 
	 */
	public ClientManager(Context ctx, String accountType, String passcodeHash) {
		this.accountManager = AccountManager.get(ctx);		
		this.accountType = accountType;
		this.passcodeHash = passcodeHash;
	}

	/**
	 * Method to create a RestClient asynchronously. It is intended to be used by code on the UI thread.
	 * 
	 * If no accounts is found, it will kick off the login flow which will create a new account if successful.
	 * After the account is created or if an account already existed, it creates a RestClient and returns it through restClientCallback. 
	 * 
	 * Note: The work is actually be done by the service registered to handle authentication for this application account type
	 * @see AuthenticatorService
	 *
	 * @param activityContext        current activity
	 * @param restClientCallback     callback invoked once the RestClient is ready
	 */
	public void getRestClient(Activity activityContext, RestClientCallback restClientCallback) {

		Account acc = getAccount();

		// Passing the passcodeHash to the authenticator service to that it can encrypt/decrypt oauth tokens
		Bundle options = new Bundle();
		options.putString(AuthenticatorService.PASSCODE_HASH, passcodeHash);

		// No account found - let's add one - the AuthenticatorService add account method will start the login activity
		if (acc == null) {
			Log.i("ClientManager:getRestClient", "No account of type " + accountType + "found");
			accountManager.addAccount(getAccountType(),
					AccountManager.KEY_AUTHTOKEN, null /*required features*/, options,
					activityContext, new AccMgrCallback(restClientCallback),
					null /* handler */);
		
		}
		// Account found
		else {
			Log.i("ClientManager:getRestClient", "Found account of type " + accountType);
			accountManager.getAuthToken(acc, AccountManager.KEY_AUTHTOKEN,
					options, activityContext, new AccMgrCallback(restClientCallback), null /* handler */);
		
		}
	}
	
	/**
	 * Method to create RestClient synchronously. It is intended to be used by code not on the UI thread (e.g. ContentProvider). 
	 * 
	 * If there is no account, it will throw an exception.
	 * 
	 * @return
	 * @throws AccountInfoNotFoundException
	 */
	public RestClient peekRestClient()
			throws AccountInfoNotFoundException {
		
		Account acc = getAccount();
		if (acc == null) {
			AccountInfoNotFoundException e = new AccountInfoNotFoundException("No user account found");
			Log.i("ClientManager:peekRestClient", "No user account found", e);
			throw e;
		}

		// OAuth tokens are stored encrypted
		String authToken = Encryptor.decrypt(accountManager.getUserData(acc, AccountManager.KEY_AUTHTOKEN), passcodeHash);
		String refreshToken = Encryptor.decrypt(accountManager.getPassword(acc), passcodeHash);
		
		// We also store the instance url, org id, user id and username in the account manager
		String server = accountManager.getUserData(acc, AuthenticatorService.KEY_INSTANCE_SERVER);
		String orgId = accountManager.getUserData(acc, AuthenticatorService.KEY_ORG_ID);
		String userId = accountManager.getUserData(acc, AuthenticatorService.KEY_USER_ID);
		String username = accountManager.getUserData(acc, AccountManager.KEY_ACCOUNT_NAME);

		if (authToken == null)
			throw new AccountInfoNotFoundException(AccountManager.KEY_AUTHTOKEN);
		if (server == null)
			throw new AccountInfoNotFoundException(AuthenticatorService.KEY_INSTANCE_SERVER);
		if (userId == null)
			throw new AccountInfoNotFoundException(AuthenticatorService.KEY_USER_ID);
		if (orgId == null)
			throw new AccountInfoNotFoundException(AuthenticatorService.KEY_ORG_ID);

		try {
			AccMgrAuthTokenProvider authTokenProvider = new AccMgrAuthTokenProvider(this, authToken, refreshToken);
			return new RestClient(new URI(server), authToken, HttpAccess.DEFAULT, authTokenProvider, username, userId, orgId);
		} 
		catch (URISyntaxException e) {
			Log.w("ClientManager:peekRestClient", "Invalid server URL", e);
			throw new AccountInfoNotFoundException("invalid server url", e);
		}
	}

	/**
	 * @return first account found with the application account type
	 */
	public Account getAccount() {
		Account[] accounts = accountManager.getAccountsByType(getAccountType());
		if (accounts == null || accounts.length == 0)
			return null;
		return accounts[0];
	}

	/**
	 * @param name
	 * @return account with the application account type and the given name
	 */
	public Account getAccountByName(String name) {
		Account[] accounts = accountManager.getAccountsByType(getAccountType());
		if (accounts != null) {
			for (Account account : accounts) {
				if (account.name.equals(name)) {
					return account;
				}
			}
		}
		return null;
	}
	
	/**
	 * @return all accounts found for this application account type
	 */
	public Account[] getAccounts() {
		return accountManager.getAccountsByType(getAccountType());
	}	
	
	/**
	 * Remove all the accounts passed in
	 * @param accounts
	 */
	public void removeAccounts(Account[] accounts) {
		List<AccountManagerFuture<Boolean>> removalFutures = new ArrayList<AccountManagerFuture<Boolean>>();
		for (Account a : accounts)
			removalFutures.add(accountManager.removeAccount(a, null, null));
		
		for (AccountManagerFuture<Boolean> f : removalFutures) {
			try {
				f.getResult();
			} catch (Exception ex) {
				Log.w("ClientManager:removeAccounts", "Exception removing old account", ex);
			}
		}
	}
	
	/**
	 * Create new account and return bundle that new account details in a bundle
	 * @param username                 
	 * @param refreshToken
	 * @param authToken
	 * @param instanceUrl
	 * @param loginUrl
	 * @param clientId
	 * @param orgId
	 * @param userId
	 * @return
	 */
	public Bundle createNewAccount(String username, String refreshToken, String authToken, String instanceUrl,
			String loginUrl, String clientId, String orgId, String userId) {
		
		Bundle extras = new Bundle();
		extras.putString(AccountManager.KEY_ACCOUNT_NAME, username);
		extras.putString(AccountManager.KEY_ACCOUNT_TYPE, getAccountType());
		extras.putString(AuthenticatorService.KEY_LOGIN_SERVER, loginUrl);
		extras.putString(AuthenticatorService.KEY_INSTANCE_SERVER, instanceUrl);
		extras.putString(AuthenticatorService.KEY_CLIENT_ID, clientId);
		extras.putString(AuthenticatorService.KEY_ORG_ID, orgId);
		extras.putString(AuthenticatorService.KEY_USER_ID, userId);
		extras.putString(AccountManager.KEY_AUTHTOKEN, Encryptor.encrypt(authToken, passcodeHash));

		Account acc = new Account(username, getAccountType());
		accountManager.addAccountExplicitly(acc, Encryptor.encrypt(refreshToken, passcodeHash), extras);
		accountManager.setAuthToken(acc, AccountManager.KEY_AUTHTOKEN, authToken);
		
		return extras;
	}
	
	/**
	 * Should match the value in authenticator.xml
	 * @return account type for this application
	 */
	public String getAccountType() {
		return accountType;
	}
	
	/**
	 * @return accountManager
	 */
	public AccountManager getAccountManager() {
		return accountManager;
	}


	/**
	 * Removes the user account from the account manager, this is an
	 * asynchronous process, the callback is called on completion if
	 * specified.
	 */
	public void removeAccountAsync(AccountManagerCallback<Boolean> callback) {
		Account acc = getAccount();
		if (acc != null)
			accountManager.removeAccount(acc, callback, null);
	}
	
	
	/**
	 * Callback from either user account creation or a call to getAuthToken used
	 * by the android account management bits
	 */
	private class AccMgrCallback implements AccountManagerCallback<Bundle> {

		private final RestClientCallback restCallback;

		/**
		 * Constructor
		 * @param restCallback who to directly call when we get a result for getAuthToken
		 * 			  
		 */
		AccMgrCallback(RestClientCallback restCallback) {
			assert restCallback != null : "you must supply a RestClientAvailable instance";
			this.restCallback = restCallback;
		}

		@Override
		public void run(AccountManagerFuture<Bundle> f) {

			RestClient client = null;

			try {
				f.getResult();

				// the O.S. strips the auth_token from the response bundle on
				// 2.2, given that we might as well just use peekClient to build
				// the client from the data in the AccountManager, rather than
				// trying to build it from the bundle.
				client = peekRestClient();

			} catch (AccountsException e) {
				Log.w("AccMgrCallback:run", "", e);
			} catch (IOException e) {
				Log.w("AccMgrCallback:run", "", e);
			} catch (AccountInfoNotFoundException e) {
				Log.w("AccMgrCallback:run", "", e);
			}

			// response. if we failed, null
			restCallback.authenticatedRestClient(client);
		}
	}

	/**
	 * RestClientCallback interface.
	 * You must provider an implementation of this interface when calling getRestClient.
	 */
	public interface RestClientCallback {
		public void authenticatedRestClient(RestClient client);
	}

	/**
	 * AuthTokenProvider implementation that calls out to the AccountManager to get a new access token.
	 * The AccountManager actually calls ForceAuthenticatorService to do the actual refresh.
	 * @see AuthenticatorService
	 */
	public static class AccMgrAuthTokenProvider implements RestClient.AuthTokenProvider {

		private static boolean gettingAuthToken;
		private static final Object lock = new Object();
		private final ClientManager clientManager;
		private static String lastNewAuthToken;
		private final String refreshToken;
		private long lastRefreshTime = -1 /* never refreshed */;

		/**
		 * Constructor
		 * @param clientManager
		 * @param refreshToken
		 */
		AccMgrAuthTokenProvider(ClientManager clientManager, String authToken, String refreshToken) {
			this.clientManager = clientManager;
			this.refreshToken = refreshToken;
			lastNewAuthToken = authToken;
		}

		/**
		 * Fetch a new access token from the account manager, if another thread
		 * is already in the progress of doing this we'll just wait for it to finish and use that access token.
		 * Return null if we can't get a new access token for any reason.
		 */
		@Override
		public String getNewAuthToken() {
			Log.i("AccMgrAuthTokenProvider:getNewAuthToken", "Need new access token");

			Account acc = clientManager.getAccount();
			if (acc == null)
				return null;
			
			
			// Wait if another thread is already fetching an access token
			synchronized (lock) {
				if (gettingAuthToken) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						Log.w("ClientManager:Callback:fetchNewAuthToken", "", e);
					}
					return lastNewAuthToken;
				}
				gettingAuthToken = true;
			}

			
			// Invalidate current auth token
			clientManager.accountManager.invalidateAuthToken(clientManager.getAccountType(), lastNewAuthToken);
			
			String newAuthToken = null;
			try {
				Bundle options = new Bundle();
				options.putString(AuthenticatorService.PASSCODE_HASH, clientManager.passcodeHash);
				Bundle bundle = clientManager.accountManager.getAuthToken(acc, AccountManager.KEY_AUTHTOKEN, options, null /* activity */, null /* callback */,
								null /* handler */).getResult();
			
				if (bundle == null) {
					Log.w("AccMgrAuthTokenProvider:fetchNewAuthToken", "accountManager.getAuthToken returned null bundle");
				}
				else {
					newAuthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				}
			} catch (Exception e) {
				Log.w("AccMgrAuthTokenProvider:fetchNewAuthToken:getNewAuthToken",
						"Exception during getAuthToken call", e);
			} finally {
				synchronized (lock) {
					gettingAuthToken = false;
					lastNewAuthToken = newAuthToken;
					lastRefreshTime  = System.currentTimeMillis();
					lock.notifyAll();
				}
			}
			return newAuthToken;
		}
		
		@Override
		public String getRefreshToken() {
			return refreshToken;
		}
		
		@Override
		public long getLastRefreshTime() {
			return lastRefreshTime;
		}
	}

	/**
	 * Exception thrown when no account could be found (during a peekRestClient call) 
	 */
	public static class AccountInfoNotFoundException extends Exception {
		private static final long serialVersionUID = 1L;

		AccountInfoNotFoundException(String msg) {
			super(msg);
		}

		AccountInfoNotFoundException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}
}
