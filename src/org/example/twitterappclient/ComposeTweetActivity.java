package org.example.twitterappclient;

import com.squareup.picasso.Picasso;

import twitter4j.User;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class ComposeTweetActivity extends Activity{
	
	public static final String KEY_FOR_EXTRA_COMPOSE_TEXT = "compose_text";
	
	private User user;
	private EditText etComposeText;
	
	private MenuItem mTextRemainCount;
	private int maxComposeTextLength;
	private int curTextRemainCount;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_compose_tweet);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		Intent intent = getIntent();
		user = (User) intent.getSerializableExtra(MainActivity.KEY_FOR_EXTRA_USER);
		
		ImageView ivComposeProfileImage = (ImageView) findViewById(R.id.ivComposeProfileImage);
		TextView tvComposeUserName = (TextView) findViewById(R.id.tvComposeUserName);
		TextView tvComposeScreenName = (TextView) findViewById(R.id.tvComposeScreenName);
		etComposeText = (EditText) findViewById(R.id.etComposeText);
		etComposeText.addTextChangedListener(new CustomTextWatcher());
		
		Picasso.with(this).load(user.getBiggerProfileImageURL()).placeholder(R.drawable.human_gray).into(ivComposeProfileImage);
		tvComposeUserName.setText(user.getName());
		tvComposeScreenName.setText("@" + user.getScreenName());
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_compose_tweet, menu);
		mTextRemainCount = menu.findItem(R.id.mTextRemainCount);
		maxComposeTextLength = Integer.valueOf(mTextRemainCount.getTitle().toString());
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			finish();
			return true;
		} else if (itemId == R.id.mTweet) {
			String composeText = etComposeText.getText().toString().trim();
			if (composeText.equals("")) {
				return true;
			}
			Intent intent = new Intent();
			intent.putExtra(KEY_FOR_EXTRA_COMPOSE_TEXT, composeText);
			setResult(RESULT_OK, intent);
			finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private class CustomTextWatcher implements TextWatcher {

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			int composeLength = etComposeText.getText().toString().length();
			curTextRemainCount = maxComposeTextLength - composeLength;
			mTextRemainCount.setTitle(String.valueOf(curTextRemainCount));
		}

		@Override
		public void afterTextChanged(Editable s) {
			
		}
		
	}
}
