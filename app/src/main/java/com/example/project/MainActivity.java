package com.example.project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.location.Location;

import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.poi_item.TMapPOIItem;

import java.util.ArrayList;
import java.util.TreeSet;

class EventPoint {
    private double latitude;
    private double longitude;
    private int type;
    private String title;
    private String subTitle;
    private int id;

    public EventPoint(double latitude, double longitude, int type, String title, String subTitle, int id) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.title = title;
        this.subTitle = subTitle;
        this.id = id;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getType() { return type; }
    public String getTitle() { return title; }
    public String getSubTitle() { return subTitle; }
    public int getId() { return id; }
    public boolean checkType(int check) { return (check & type) > 0; }
}

class EventConst {
    public static final int IS_EVENT = 1;
}

public class MainActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback, TMapView.OnClickListenerCallback {

    private static final int MULTIPLE_PERMISSION = 10235;
    private final String[] PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.POST_NOTIFICATIONS
    };

    private static final String CHANNEL_ID = "notification_channel";
    private NotificationManager mNotificationManager;

    private TMapView tmapview = null;
    private TMapGpsManager tmapgps = null;


    private ArrayList<EventPoint> eventPoint = new ArrayList<>();
    private ArrayList<EventPoint> alertPoint = new ArrayList<>();
    private ArrayList<EventPoint> showPoint = new ArrayList<>();

    private Location lastLocation = null;

    private Button end_walk_button = null, start_walk_button = null;
    private boolean is_walking = false;
    private TMapPolyLine tmapPolyLine = new TMapPolyLine();

    private int walk_id = 0;

    private static int notification_id = 0;
    private boolean is_pointed = false;

    private boolean show_event = true;
    private boolean show_alert = true;
    TreeSet<Integer> showing = new TreeSet<>();

    /*
    Markerlist - 클릭된 마커들
    Poilist - 클릭된 POI 들
    Point - 화면좌표값을 위도, 경도로 반환한 값
    Pointf - 화면좌표값
     */
    @Override
    public boolean onPressEvent(ArrayList<TMapMarkerItem> markerlist, ArrayList<TMapPOIItem> poilist, TMapPoint point, PointF pointf) {
        if(markerlist.size() > 0) {
            // TMapMarkerItem item = markerlist.get(0);
            // Toast.makeText(getApplicationContext(), "마커 클릭", Toast.LENGTH_LONG).show();
            return false;
        }
        
        // Toast.makeText(getApplicationContext(), "위도 : " + point.getLatitude() + "\n경도 : " + point.getLongitude(), Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public boolean onPressUpEvent(ArrayList<TMapMarkerItem> arrayList, ArrayList<TMapPOIItem> arrayList1, TMapPoint tMapPoint, PointF pointF) {
        return false;
    }

    @Override
    public void onLocationChange(Location location) {
        // int Satellite = tmapgps.getSatellite(); 위성 수
        // TODO : location = 보정(location);
        tmapview.setLocationPoint(location.getLongitude(), location.getLatitude());
        // 이동시 위도 경도 출력
        Toast.makeText(getApplicationContext(), "위도 : " + location.getLatitude() + "\n경도 : " + location.getLongitude(), Toast.LENGTH_LONG).show();
        lastLocation = location;

        if(is_walking) {
            tmapPolyLine.addLinePoint(new TMapPoint(location.getLatitude(), location.getLongitude()));
            tmapview.addTMapPolyLine("walk" + walk_id, tmapPolyLine);
        }

        
        for(EventPoint point : showPoint) {
            double distance = Math.sqrt(Math.pow(point.getLatitude() - location.getLatitude(), 2) + Math.pow(point.getLongitude() - location.getLongitude(), 2));
            if(distance < 0.001) {
                if(!showing.contains(point.getId())) {
                    sendNotification(++notification_id, "이벤트 발생", point.getId() + "번 이벤트 발생");
                    showing.add(point.getId());
                }
            } else if(showing.contains(point.getId())) {
                showing.remove(point.getId());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!hasPermissions(this, PERMISSIONS))
        {
            ActivityCompat.requestPermissions(this, PERMISSIONS, MULTIPLE_PERMISSION);
        }

        LinearLayout linearLayoutTmap = findViewById(R.id.mapview);

        tmapview = new TMapView(this);
        tmapview.setSKTMapApiKey("KRtlS815a12Oxhqfy9gkF7OHp10LDPsu1Nk4nC6d");
        linearLayoutTmap.addView(tmapview);

        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setZoomLevel(15);
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);
        tmapview.setTrackingMode(true);

        tmapview.setIconVisibility(true);

//        tmapgps.setMinDistance((float)0.001);

        tmapgps = new TMapGpsManager(MainActivity.this);
        tmapgps.setMinTime(1000);
        tmapgps.setMinDistance(5);
        tmapgps.setProvider(TMapGpsManager.GPS_PROVIDER);

        tmapPolyLine.setLineColor(Color.YELLOW);
        tmapPolyLine.setLineWidth(20);
        tmapPolyLine.setOutLineAlpha(0);

        tmapgps.OpenGps();

        // 시작점을 찾아 화면에 보여준다.
        TMapPoint startPoint = tmapgps.getLocation();
        onLocationChange(new Location ("startPoint") {{
            setLatitude(startPoint.getLatitude());
            setLongitude(startPoint.getLongitude());
        }});

        // TODO : walk_id = get_walk_id();

        // TODO : Event Point 가져오기
        for(EventPoint point : new EventPoint[]{new EventPoint(37.4963, 126.9569, 0, "이벤트 1", "이벤트 1 설명입니다.", 1), new EventPoint(37.4946, 126.9571, 1, "이벤트 2", "이벤트 2 설명입니다.", 2)}) {
            if(point.checkType(EventConst.IS_EVENT)) eventPoint.add(point);
            else alertPoint.add(point);
        }

        // 입력으로 받은 event들을 화면에 보여준다.
        togglePoint();

        // 산책을 시작하는 버튼과 종료하는 버튼
        start_walk_button = (Button) findViewById(R.id.start_walk_button);
        end_walk_button = (Button) findViewById(R.id.end_walk_button);
        end_walk_button.setVisibility(View.GONE);

        start_walk_button.setOnClickListener(v -> start_walk());

        end_walk_button.setOnClickListener(v -> onBackPressed());

        // 화면 중앙을 현재 위치로 이동시키는 버튼
        Button return_button = (Button) findViewById(R.id.return_button);
        return_button.setOnClickListener(v -> {
            tmapview.setCenterPoint(lastLocation.getLongitude(), lastLocation.getLatitude());
            tmapview.setTrackingMode(true);
        });

        // 디버깅용 화면 중앙으로 이동하는 버튼
        Button teleport_button = (Button) findViewById(R.id.teleport_button);
        teleport_button.setOnClickListener(v -> {
            TMapPoint tpoint = tmapview.getCenterPoint();
            onLocationChange(new Location ("teleport") {{
                setLatitude(tpoint.getLatitude());
                setLongitude(tpoint.getLongitude());
            }});
            tmapview.setCenterPoint(tpoint.getLongitude(), tpoint.getLatitude());
        });

        Button alert_view_button = (Button) findViewById(R.id.alert_view_button);
        alert_view_button.setOnClickListener(v -> {
            show_alert = !show_alert;
            togglePoint();
        });

        Button event_view_button = (Button) findViewById(R.id.event_view_button);
        event_view_button.setOnClickListener(v -> {
            show_event = !show_event;
            togglePoint();
        });

        Button report_button = (Button) findViewById(R.id.report_button);
        report_button.setOnClickListener(v -> {
            if(is_pointed) {

            } else {
                report(lastLocation);
            }
        });
//        System.out.println(tmapgps.getProvider());

        createNotificationChannel();
    }

    // 권한을 확인하는 함수
    private static boolean hasPermissions(MainActivity mainActivity, String[] permissions) {
        if(mainActivity != null && permissions != null) {
            for(String permission : permissions) {
                if(ActivityCompat.checkSelfPermission(mainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    // 알림을 보내기 위해 onCreate때 실행되는 함수
    public void createNotificationChannel()
    {
        //notification manager 생성
        mNotificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        // 기기(device)의 SDK 버전 확인 ( SDK 26 버전 이상인지 - VERSION_CODES.O = 26)
        if(android.os.Build.VERSION.SDK_INT
                >= android.os.Build.VERSION_CODES.O){
            //Channel 정의 생성자( construct 이용 )
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,"Test Notification",mNotificationManager.IMPORTANCE_HIGH);
            //Channel에 대한 기본 설정
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription("Notification from Mascot");
            // Manager을 이용하여 Channel 생성
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
    }

    // 알림 id, 제목, 내용으로 핸드폰에 알림을 보내는 함수. 이전과 같은 알림 id로 보내면 덮어씌워진다.
    public void sendNotification(int id, String title, String text) {
        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_stat_name);
        mNotificationManager.notify(id, notifyBuilder.build());
    }

    // 뒤로가기 버튼을 눌렀을 떄 실행되는 함수
    @Override
    public void onBackPressed() {
        if(is_pointed) {

            is_pointed = false;
            return;
        }

        if (is_walking) { // 산책중이면 산책 종료버튼을 누른거랑 같은 작동을 함
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("산책을 종료하시겠습니까?");

            builder.setPositiveButton("예",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            end_walk();
                        }
                    });
            builder.setNegativeButton("아니오",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            builder.show();
            return;
        }
        
        finish();    
    }

    // 산책이 시작되었을 때 실행되는 함수
    public void start_walk() {
        walk_id = walk_id + 1;
        tmapPolyLine = new TMapPolyLine();
        is_walking = true;

        onLocationChange(lastLocation);

        end_walk_button.setVisibility(View.VISIBLE);
        start_walk_button.setVisibility(View.GONE);
    }

    // 산책이 종료되었을 때 실행되는 함수
    public void end_walk() {
        tmapview.removeTMapPolyLine("walk" + walk_id);
        is_walking = false;

        start_walk_button.setVisibility(View.VISIBLE);
        end_walk_button.setVisibility(View.GONE);

        // TODO : 팝업 띄우기
        Intent intent = new Intent(this, WalkResultActivity.class);
        startActivity(intent);
    }

    public void togglePoint() {
        tmapview.removeAllMarkerItem();

        showPoint.clear();
        if(show_event) showPoint.addAll(eventPoint);
        if(show_alert) showPoint.addAll(alertPoint);

        for(EventPoint point : showPoint) {
            TMapMarkerItem tmarker = new TMapMarkerItem();
            tmarker.setTMapPoint(new TMapPoint(point.getLatitude(), point.getLongitude()));
            tmarker.setVisible(TMapMarkerItem.VISIBLE);
            tmarker.setCanShowCallout(true);
            tmarker.setCalloutTitle(point.getTitle());
            tmarker.setCalloutSubTitle(point.getSubTitle());
            tmapview.addMarkerItem("event" + point.getId(), tmarker);
        }
    }

    public void report(Location location) {
        // TODO : POPUP 하나 더
    }
}