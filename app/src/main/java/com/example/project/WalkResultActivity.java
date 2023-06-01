package com.example.project;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Button;
import android.view.View;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;


public class WalkResultActivity extends Activity {

    private TMapPolyLine tmapPolyLine = new TMapPolyLine();
    private TMapView tmapview = null;

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


        tmapview = new TMapView(this);
        tmapview.setSKTMapApiKey("FODY58g1Tw9QWg3wjnRto7hFmCfJGYAz64vst7vP");
        
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);
        tmapview.setUserScrollZoomEnable(false);

        double[] locationLatitude = getIntent().getDoubleArrayExtra("locationLatitude");
        double[] locationLongitude = getIntent().getDoubleArrayExtra("locationLongitude");
        double[] reportLatitude = getIntent().getDoubleArrayExtra("reportLatitude");
        double[] reportLongitude = getIntent().getDoubleArrayExtra("reportLongitude");
        double walk_distance = getIntent().getDoubleExtra("walk_distance", 0);
        String walk_time = getIntent().getStringExtra("walk_time");
        
        double hour = Double.parseDouble(walk_time.substring(0, 2)) + Double.parseDouble(walk_time.substring(3, 5)) / 60;
        double walk_speed = walk_distance / hour;

        // 산책 경로를 지도에 표시
        for(int i = 0; i < locationLatitude.length; i++) {
            tmapPolyLine.addLinePoint(new TMapPoint(locationLatitude[i], locationLongitude[i]));
        }
        tmapview.addTMapPolyLine("walkResult", tmapPolyLine);

        // 제보한 위치를 지도에 표시
        for(int i = 0; i < reportLatitude.length; i++) {

            TMapMarkerItem tmarker = new TMapMarkerItem();
            tmarker.setTMapPoint(new TMapPoint(reportLatitude[i], reportLongitude[i]));
            
            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.location_icon);
            bitmap = Bitmap.createScaledBitmap(bitmap, 55, 55, true);
            tmarker.setIcon(bitmap);
            tmarker.setVisible(TMapMarkerItem.VISIBLE);
            tmarker.setCanShowCallout(true);
            
            tmapview.addMarkerItem("event" + i, tmarker);
        }

        // Lantitude의 max와 min의 중앙을 구함
        double maxLatitude = locationLatitude[0];
        double minLatitude = locationLatitude[0];
        for(int i = 0; i < locationLatitude.length; i++) {
            if(maxLatitude < locationLatitude[i]) {
                maxLatitude = locationLatitude[i];
            }
            if(minLatitude > locationLatitude[i]) {
                minLatitude = locationLatitude[i];
            }
        }
        double centerLatitude = (maxLatitude + minLatitude) / 2;

        // Longitude의 max와 min의 중앙을 구함
        double maxLongitude = locationLongitude[0];
        double minLongitude = locationLongitude[0];
        for(int i = 0; i < locationLongitude.length; i++) {
            if(maxLongitude < locationLongitude[i]) {
                maxLongitude = locationLongitude[i];
            }
            if(minLongitude > locationLongitude[i]) {
                minLongitude = locationLongitude[i];
            }
        }
        double centerLongitude = (maxLongitude + minLongitude) / 2;

        tmapview.setCenterPoint(centerLongitude, centerLatitude); // 지도 중심 설정

        // TODO? : 산책 경로가 지도에 모두 보일 수 있도록 zoom level 설정
        tmapview.setZoomLevel(15);

        TextView timeView = findViewById(R.id.timeView);
        TextView distanceView = findViewById(R.id.distanceView);
        TextView speedView = findViewById(R.id.speedView);
        TextView reportView = findViewById(R.id.reportView);
        
        timeView.setText("시간 : " + walk_time);
        distanceView.setText("거리 : " + String.format("%.2f", walk_distance) + "km");
        speedView.setText("평균속도 : " + String.format("%.2f", walk_speed) + "km/h");
        reportView.setText("제보 : " + reportLatitude.length + "회");

        LinearLayout linearLayoutTmap = findViewById(R.id.mapview);
        linearLayoutTmap.addView(tmapview);
    }
}
