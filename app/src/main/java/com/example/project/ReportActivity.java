package com.example.project;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Button;
import android.view.View;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

public class ReportActivity extends Activity {
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

    private TMapView tmapview = null;
    public void onLocationChange(Location location) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_report);

        Intent intent = getIntent();
        double latitude = intent.getDoubleExtra("latitude", 0);
        double longitude = intent.getDoubleExtra("longitude", 0);

        tmapview = new TMapView(this);
        tmapview.setSKTMapApiKey("FODY58g1Tw9QWg3wjnRto7hFmCfJGYAz64vst7vP");
        // Toast.makeText(getApplicationContext(), "위도: " + latitude + ", 경도: " + longitude, Toast.LENGTH_LONG).show();
        tmapview.setZoomLevel(18);
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);
        tmapview.setUserScrollZoomEnable(false);

        TMapMarkerItem tmarker = new TMapMarkerItem();
        tmarker.setTMapPoint(new TMapPoint(latitude, longitude));
        tmarker.setVisible(TMapMarkerItem.VISIBLE);
        tmapview.addMarkerItem("report_point", tmarker);

        Button return_home_button = (Button) findViewById(R.id.cancel_button);
        return_home_button.setOnClickListener(v -> onBackPressed());

        tmapview.setLocationPoint(longitude, latitude);
        tmapview.setCenterPoint(longitude, latitude);
        LinearLayout linearLayoutTmap = findViewById(R.id.mapview);
        linearLayoutTmap.addView(tmapview);
    }
}
