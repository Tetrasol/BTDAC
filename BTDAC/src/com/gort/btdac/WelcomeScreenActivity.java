package com.gort.btdac;
import com.gort.btdac.btutil.BluetoothUtilityActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WelcomeScreenActivity extends Activity implements OnClickListener {
	
	private static final String ACTIVITY_TAG = "com.gort.WelcomeScreenActivity";
	
	private Button goToCrtBTConnBtn;
	private Button goToCrtNewPlotBtn;
	private Button goToLoadPrevPlotBtn;
	private Button goToAppSettingsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        goToCrtBTConnBtn	= (Button) findViewById(R.id.goToCrtBTConn);
        goToCrtNewPlotBtn	= (Button) findViewById(R.id.goToCrtNewPlot);
        goToLoadPrevPlotBtn	= (Button) findViewById(R.id.goToLoadPrevPlot);
        goToAppSettingsBtn	= (Button) findViewById(R.id.goToSettingsScreen);

        goToCrtBTConnBtn.setOnClickListener(this);
        goToCrtNewPlotBtn.setOnClickListener(this);
        goToLoadPrevPlotBtn.setOnClickListener(this);
        goToAppSettingsBtn.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome_screen, menu);
        return true;
    }

    /****/
	@Override
	public void onClick(View v) {
		Log.d(ACTIVITY_TAG, "Entered the onClick()");
		// Compare the id of the view clicked and determine action(s)
		switch(v.getId()) {
		case R.id.goToCrtBTConn:
			Log.d(ACTIVITY_TAG, "Entered the CreateBTConnection case");
			startActivity(new Intent(this, BluetoothUtilityActivity.class));
			break;
		case R.id.goToCrtNewPlot:
			break;
//			startActivity(new Intent(this, CreateNewPlotActivity.class));
		case R.id.goToLoadPrevPlot:
			break;
//			startActivity(new Intent(this, LoadPreviousPlotActivity.class));
		case R.id.goToSettingsScreen:
			break;
//			startActivity(new Intent(this, AppSettingsActivity.class));
		default: // Do nothing - stay on current screen
			break;
		}
	}

}
