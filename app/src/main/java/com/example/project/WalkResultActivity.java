package com.example.project;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Button;
import android.view.View;
import android.app.AlertDialog;
import android.content.DialogInterface;


public class WalkResultActivity extends Activity {
    
    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if( event.getAction() == MotionEvent.ACTION_OUTSIDE ) {
            return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.walk_result);
        
        Button return_home_button = (Button) findViewById(R.id.return_home_button);
        return_home_button.setOnClickListener(v -> onBackPressed());
        
    }
}
