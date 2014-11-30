package org.example.twitterappclient;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class TwitterHelper {
	public static final String PREFS_NAME = "MyPrefs";
	public static final String PREFS_KEY_IS_LOGIN = "is_login";
	public static final String CALLBACK_URL = "oauth://rtestdev";
	
	private static final String CONSUMER_KEY = "ssicDlwg3yJikxxapSjB6Dzlf";
	private static final String CONSUMER_SECRET = "UmSEcX219bm7ZZzdYS3YJhzeUkVkbJZrlMzh8FkImsUbRoYwbM";
	private static final String PREFS_KEY_REQUEST_TOKEN = "request_token";
	private static final String PREFS_KEY_REQUEST_TOKEN_SECRET = "request_token_secret";
	private static final String PREFS_KEY_ACCESS_TOKEN = "access_token";
	private static final String PREFS_KEY_ACCESS_TOKEN_SECRET = "access_token_secret";
	private static final String PARAM_NAME_OAUTH_VERIFIER = "oauth_verifier";
	
	private Context context;
	private Twitter twitter;
	private Twitter authorizedTwitter;
	private RequestToken requestToken;
	private User user;
	private SharedPreferences prefs;
	private TwitterAuthListener listener;
	
	public interface TwitterAuthListener {
		public void onUserReadyListener(String errorMessage);
		public void onLoginStartedListener(String errorMessage);
		public void onTokenSavedListener(String errorMessage);
	}
	
	public TwitterHelper(Activity activity) {
		this.context = activity;
		if (activity instanceof TwitterAuthListener) {
			listener = (TwitterAuthListener) activity;
		}
		init();
	}
	
	public void startLoginProcess() {
		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				String errorMessage = null;
	    		twitter = getTwitter();
	    		
	    		try {
	    			requestToken = twitter.getOAuthRequestToken(CALLBACK_URL);
	    			Editor editor = prefs.edit();
	    			editor.putString(PREFS_KEY_REQUEST_TOKEN, requestToken.getToken());
	    			editor.putString(PREFS_KEY_REQUEST_TOKEN_SECRET, requestToken.getTokenSecret());
	    			editor.commit();
	    			//Log.i("startLoginProcess", "token: " + requestToken.getToken());
	    			//Log.i("startLoginProcess", "token secret: " + requestToken.getTokenSecret());
	    			
	    			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
	    		} catch (TwitterException e) {
	    			errorMessage = e.getMessage();
	    		}
	    		
				return errorMessage;
			}
			
			protected void onPostExecute(String errorMessage) {
				listener.onLoginStartedListener(errorMessage);				
			};
		}.execute();
	}
	
	public void saveAccessToken(Uri uri) {
		new AsyncTask<Uri, Void, String>() {

			@Override
			protected String doInBackground(Uri... uri) {
				String errorMessage = null;
				String verifier = uri[0].getQueryParameter(PARAM_NAME_OAUTH_VERIFIER);
				try {
		    		twitter = getTwitter();
		    		
		    		String reqToken = prefs.getString(PREFS_KEY_REQUEST_TOKEN, "");
		    		String reqTokenSecret = prefs.getString(PREFS_KEY_REQUEST_TOKEN_SECRET, "");
		    		requestToken = new RequestToken(reqToken, reqTokenSecret);
		    		
		    		AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
		    		long userId = accessToken.getUserId();
		    		
		        	ConfigurationBuilder builder = getConfigBuilder();
		        	authorizedTwitter = new TwitterFactory(builder.build()).getInstance(accessToken);
		        	user = authorizedTwitter.showUser(userId);
		    		
					//Log.i("saveAccessToken", "token: " + accessToken.getToken());
					//Log.i("saveAccessToken", "token secret: " + accessToken.getTokenSecret());
					Editor editor = prefs.edit();
					editor.putString(PREFS_KEY_ACCESS_TOKEN, accessToken.getToken());
					editor.putString(PREFS_KEY_ACCESS_TOKEN_SECRET, accessToken.getTokenSecret());
					editor.putBoolean(PREFS_KEY_IS_LOGIN, true);
					editor.commit();
					
					
				} catch (TwitterException e) {
					errorMessage = e.getMessage();
				}
				
				return errorMessage;
			}
			
			protected void onPostExecute(String errorMessage) {				
				listener.onTokenSavedListener(errorMessage);
			};
			
			
		}.execute(uri);
	}
	
	public Twitter getAuthorizedTwitter() {
    	return authorizedTwitter;
	}

	private AccessToken getAccessToken() {
		String token = prefs.getString(PREFS_KEY_ACCESS_TOKEN, "");
		String tokenSecret = prefs.getString(PREFS_KEY_ACCESS_TOKEN_SECRET, "");
		AccessToken accessToken = new AccessToken(token, tokenSecret);
		return accessToken;
	}
	
	public User getUser() {		
		return user;
	}
	
    public boolean isLogin() {
    	return prefs.getBoolean(TwitterHelper.PREFS_KEY_IS_LOGIN, false);
    }
    
	public void prepareUser() throws TwitterException {
		new AsyncTask<Void, Void, String>(){

			@Override
			protected String doInBackground(Void... params) {
				String errorMessage = null;
				AccessToken accessToken = getAccessToken();
				long userId = accessToken.getUserId();
				try {
					user = authorizedTwitter.showUser(userId);
				} catch (TwitterException e) {
					errorMessage = e.getMessage();
				}
				
				return errorMessage;
			}
			
			protected void onPostExecute(String errorMessage) {
				listener.onUserReadyListener(errorMessage);
			};
			
		}.execute();
			

	}
    
	private void init() {
		prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		if (isLogin()) {
			initAuthorizeTwitter();
		}
	}
	
	private void initAuthorizeTwitter() {
    	ConfigurationBuilder builder = getConfigBuilder();
    	AccessToken accessToken = getAccessToken();
    	authorizedTwitter = new TwitterFactory(builder.build()).getInstance(accessToken);
	}
	

	
	private ConfigurationBuilder getConfigBuilder() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
    	builder.setOAuthConsumerKey(CONSUMER_KEY);
    	builder.setOAuthConsumerSecret(CONSUMER_SECRET);
    	
		return builder;
	}

	private Twitter getTwitter() {
		ConfigurationBuilder builder = getConfigBuilder();
		
		Configuration config = builder.build();
		TwitterFactory factory = new TwitterFactory(config);
		Twitter twitter = factory.getInstance();
		
		return twitter;
	}
	
}
