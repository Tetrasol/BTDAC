package com.gort.btdac.appsettings;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gort.btdac.R;

public class SettingsActivity extends Activity implements OnClickListener {

	private Button next;
	private Button previous;
	private TextView upper;
	private TextView lower;
	private int state;
	private boolean response;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		// Show the Up button in the action bar.
		setupActionBar();
		
		state = 0;
		response = true;
		
		next = (Button) findViewById(R.id.next);
		next.setOnClickListener(this);
		
		previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(this);
		
		upper = (TextView) findViewById(R.id.upper);
		
		lower = (TextView) findViewById(R.id.lower);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()) {
		case R.id.next:
			state++;
			
			if (state == 0) {
				upper.setText(R.string.paragraph1);
				lower.setText(R.string.paragraph2);
				
				next.setText(R.string.next);
				previous.setText(R.string.previous);
			}
			
			if (state == 1) {
				upper.setText(R.string.paragraph3);
				lower.setText(R.string.paragraph4);
				
				next.setText(R.string.next);
				previous.setText(R.string.previous);
			}
			
			if (state == 2) {
				upper.setText(R.string.paragraph5);
				lower.setText(R.string.paragraph6);
				
				next.setText(R.string.no);
				previous.setText(R.string.yes);
				
				previous.setHeight(900);
				previous.setWidth(900);
				state = 1000;
			}
			
			if (state == 3) response = false;
			
			if (response == false) {
				Toast.makeText(getApplicationContext(), "YOU CANNOT SAY NO! I LOVE YOU", Toast.LENGTH_SHORT).show();
			}
			
			break;
		case R.id.previous:
			state--;
			
			if (state < 0) {
				state = 0;
			}
			
			if (state == 0) {
				upper.setText(R.string.paragraph1);
				lower.setText(R.string.paragraph2);
				
				next.setText(R.string.next);
				previous.setText(R.string.previous);
			}
			
			if (state == 1) {
				upper.setText(R.string.paragraph3);
				lower.setText(R.string.paragraph4);
				
				next.setText(R.string.next);
				previous.setText(R.string.previous);
			}
			
			if (state == 2) {
				upper.setText(R.string.paragraph5);
				lower.setText(R.string.paragraph6);
				
				next.setText(R.string.no);
				previous.setText(R.string.yes);
				previous.setHeight(900);
				previous.setWidth(900);
				state = 1000;
				response = true;
			}
			
			if (response == false) {
				Toast.makeText(getApplicationContext(), "YOU CANNOT SAY NO! I LOVE YOU", Toast.LENGTH_SHORT).show();
			}
			
			if (response == true) {
				upper.setText(R.string.paragraph5);
				lower.setText(R.string.paragraph6);
				
				next.setText(R.string.no);
				previous.setText(R.string.yes);
				previous.setHeight(900);
				previous.setWidth(900);
				Toast.makeText(getApplicationContext(), "WE ARE GETTING MARRIED!!!!!", Toast.LENGTH_LONG).show();
			}
			
			break;
		}
		
	}
		
	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
