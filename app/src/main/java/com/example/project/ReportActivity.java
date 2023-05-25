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
import android.widget.CheckBox;
import android.widget.EditText;
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
        
        tmapview.setZoomLevel(18);
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);
        tmapview.setUserScrollZoomEnable(false);

        TMapMarkerItem tmarker = new TMapMarkerItem();
        tmarker.setTMapPoint(new TMapPoint(latitude, longitude));
        tmarker.setVisible(TMapMarkerItem.VISIBLE);
        tmapview.addMarkerItem("report_point", tmarker);

        Button cancel_button = (Button) findViewById(R.id.cancel_button);
        cancel_button.setOnClickListener(v -> onBackPressed());

        tmapview.setLocationPoint(longitude, latitude);
        tmapview.setCenterPoint(longitude, latitude);
        LinearLayout linearLayoutTmap = findViewById(R.id.mapview);
        linearLayoutTmap.addView(tmapview);

        Button report_button = (Button) findViewById(R.id.report_button);
        report_button.setOnClickListener(v -> {
            EditText et_title = (EditText) findViewById(R.id.et_title);
            if (et_title.getText().toString().equals("")) {
                Toast.makeText(getApplicationContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            EditText et_subtitle = (EditText) findViewById(R.id.et_subtitle);
            CheckBox is_alert = (CheckBox) findViewById(R.id.is_alert);
            // TODO : 신고 내용 저장하기

            Toast.makeText(getApplicationContext(), "제보가 완료되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
