package org.example.twitterappclient;

import org.example.twitterappclient.TwitterHelper.TwitterAuthListener;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

public class MainActivity extends Activity implements TwitterAuthListener {
	
	public static final String KEY_FOR_EXTRA_USER = "user";
	
	private static final int NUMBER_OF_TWEETS_PER_PAGE = 25;
	private static final int INIT_PAGE = 1;
	private static final int REQUEST_CODE_FOR_COMPOSE = 100;
	
	private SwipeRefreshLayout swipeContainer;
	private Paging paging;
	private AlertDialog.Builder alertDialog;
	private TwitterHelper twitterHelper;
	private ProgressDialog progressDialog;
	
	private ListView lvHomeTimeline;
	private StatusAdapter adapter;
	
	private Twitter twitter;
	private User user;
	private ResponseList<twitter4j.Status> statuses;
	private ResponseList<twitter4j.Status> tmpStatuses;
	private boolean isLoading = false;

	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        init();
        try {
			process();
		} catch (TwitterException e) {
			showAlertMessage(e.getMessage());
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.activity_main, menu);
    	return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	int itemId = item.getItemId();
    	if (itemId == R.id.mCompose) {
    		//Log.i("mCompose", user.getBiggerProfileImageURL());
    		
			Intent intent = new Intent(this, ComposeTweetActivity.class);
			intent.putExtra(KEY_FOR_EXTRA_USER, user);
			startActivityForResult(intent, REQUEST_CODE_FOR_COMPOSE);
			
		}
    	return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_FOR_COMPOSE) {
			String composeText = intent.getStringExtra(ComposeTweetActivity.KEY_FOR_EXTRA_COMPOSE_TEXT);
			addNewTweet(composeText);
		}
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
    }
    
    private void init() {
    	initSwipeRefresh();
    	
    	paging = new Paging(INIT_PAGE, NUMBER_OF_TWEETS_PER_PAGE);
    	
    	alertDialog = new AlertDialog.Builder(this);
    	alertDialog.setTitle(R.string.message);
    	alertDialog.setPositiveButton(R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {				
			}
		});
    	
    	progressDialog = new ProgressDialog(this);
    	progressDialog.setMessage(getString(R.string.processing_hint));
    	
    	lvHomeTimeline = (ListView) findViewById(R.id.lvHomeTimeline);
    	lvHomeTimeline.setOnScrollListener(new CustomScrollListener());
    	
    	twitterHelper = new TwitterHelper(this);
    }
    
	private void initSwipeRefresh() {
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
        		android.R.color.holo_green_light,
        		android.R.color.holo_orange_light,
        		android.R.color.holo_red_light);
        
        swipeContainer.setOnRefreshListener(new OnRefreshListener() {
			
			@Override
			public void onRefresh() {
				resetStatuses();
				loadHomeTimeline();
			}
		});
	}
	
	private void stopSwipeRefreshing() {
		if (swipeContainer.isRefreshing()) {
			swipeContainer.setRefreshing(false);
		}
	}
    
    private void process() throws TwitterException {
    	progressDialog.show();
    	
    	if (!NetworkHelper.isNetworkConnected(this)) {
    		showAlertMessage(getString(R.string.network_not_ready));
    		return;
    	}
    	
    	if (twitterHelper.isLogin()) {
			prepareTwitter();

		} else {
			Uri uri = getIntent().getData();
			if (uri != null && uri.toString().startsWith(TwitterHelper.CALLBACK_URL)) {
				twitterHelper.saveAccessToken(uri);
			} else {
				showLoginDialog();
			}
		}
    }
    
    private void showLoginDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(R.string.message)
				.setMessage(R.string.login_hint)
				.setPositiveButton(R.string.login_now,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								twitterHelper.startLoginProcess();
							}
						}).show();
    }

	private void prepareTwitter() throws TwitterException {
		twitter = twitterHelper.getAuthorizedTwitter();
		twitterHelper.prepareUser();
	}
    
    private void loadHomeTimeline() {
    	if (paging.getPage() > INIT_PAGE) {
    		showLoadingProgress();
		}
    	isLoading = true;

    	
    	new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				String errorMessage = null;
				try {
					if (paging.getPage() == INIT_PAGE && statuses == null) {
						statuses = twitter.getHomeTimeline(paging);
					} else {
						if (statuses.size() == 0) {
							
							statuses.addAll(twitter.getHomeTimeline(paging));
						} else {
							appendStatuses(twitter.getHomeTimeline(paging));
						}
					}
					if (statuses == null) {
						throw new TwitterException(getString(R.string.get_home_timeline_error));
					}
				} catch (TwitterException e) {
					errorMessage = e.getMessage();
				}
				
				return errorMessage;
			}
			
			protected void onPostExecute(String errorMessage) {
				
				if (errorMessage == null) {
					if (paging.getPage() == INIT_PAGE && adapter == null) {
						adapter = new StatusAdapter(MainActivity.this, statuses);
						lvHomeTimeline.setAdapter(adapter);
					} else {
						adapter.notifyDataSetChanged();
					}
					
				} else {
					if (paging.getPage() > INIT_PAGE) {						
						hideLoadingProgress();
					}
					showAlertMessage(errorMessage);
				}
				hideProgressDialog();
				stopSwipeRefreshing();
				
				isLoading = false;
			};
			
		}.execute();
    	    	
    }
    
    private void addNewTweet(String content) {
    	new AsyncTask<String, Void, String>() {

			@Override
			protected String doInBackground(String... argv) {
				String errorMessage = null;
				String content = argv[0];
				try {
					twitter.updateStatus(content);
				} catch (TwitterException e) {
					errorMessage = e.getMessage();
				}
				
				return errorMessage;
			}
			
			protected void onPostExecute(String errorMessage) {
				if (errorMessage != null) {
					showAlertMessage(errorMessage);
				} else {
					//Log.i("addNewTweet", "resetStatuses");
					progressDialog.show();
					resetStatuses();
					lvHomeTimeline.setSelection(0);
					loadHomeTimeline();
				}
			};
    		
    	}.execute(content);
    }
    
    private void resetStatuses() {
    	//adapter.clear();
    	statuses.clear();
    	paging.setPage(INIT_PAGE);
    }
    
    private void hideProgressDialog() {
		if (progressDialog.isShowing()) {					
			progressDialog.hide();
		}
    }

	private void showLoadingProgress() {
		Status nullStatus = null;
		statuses.add(nullStatus);
		adapter.notifyDataSetChanged();
	}

	private void removeNullStatus() {
		if (statuses != null) {
			int nullStatusIndex = statuses.size() - 1;
			statuses.remove(nullStatusIndex);			
		}
	}
	
	private void hideLoadingProgress() {
		removeNullStatus();
		adapter.notifyDataSetChanged();
	}
    
    private void appendStatuses(ResponseList<Status> lists) {
    	removeNullStatus();
    	for (Status status : lists) {
			statuses.add(status);
		}
    }
    
    private void showAlertMessage(String message) {
    	alertDialog.setMessage(message);
    	alertDialog.show();
    }
    
	@Override
	public void onUserReadyListener(String errorMessage) {
		if (errorMessage != null) {
			showAlertMessage(errorMessage);
			return;
		}
		
		user = twitterHelper.getUser();
		//Log.i("mCompose", user.getBiggerProfileImageURL());
		loadHomeTimeline();
	}

	@Override
	public void onTokenSavedListener(String errorMessage) {
		if (errorMessage != null) {
			showAlertMessage(errorMessage);
			return;
		}
		
		try {
			prepareTwitter();
		} catch (TwitterException e) {
			showAlertMessage(e.getMessage());
		}
	}

	@Override
	public void onLoginStartedListener(String errorMessage) {
		if (progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		
		if (errorMessage != null) {
			showAlertMessage(errorMessage);
			return;
		}
		
		finish();
	}
	
	private class CustomScrollListener implements OnScrollListener {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (statuses == null || statuses.size() == 0 || totalItemCount == 0 || isLoading) {
				return;
			}
			if ((firstVisibleItem + visibleItemCount) == totalItemCount) {
				paging.setPage(paging.getPage() + 1);
				loadHomeTimeline();
			}
		}
		
	}


    
}
