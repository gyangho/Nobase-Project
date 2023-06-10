package com.example.project;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.location.Location;

import com.google.gson.annotations.SerializedName;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapMarkerItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;

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

class Walk {
    private int id;
    private String datetime;
    private int time;
    private double[] latitudes;
    private double[] longitudes;

    public Walk(int id, String datetime, int time, double[] latitudes, double[] longitudes) {
        this.id = id;
        this.datetime = datetime;
        this.time = time;
        this.latitudes = latitudes;
        this.longitudes = longitudes;
    }

    public int getId() { return id; }
    public String getDatetime() { return datetime; }
    public int getTime() { return time; }
    public double[] getLatitudes() { return latitudes; }
    public double[] getLongitudes() { return longitudes; }
}

class EventConst {
    public static final int IS_EVENT = 1;
}

public class MainActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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

    private ProgressBar progressBar = null;

    private ArrayList<EventPoint> eventPoint = new ArrayList<>();
    private ArrayList<EventPoint> alertPoint = new ArrayList<>();
    private ArrayList<EventPoint> showPoint = new ArrayList<>();

    private Location lastLocation = null;
    private ImageButton start_walk_button = null, end_walk_button = null;
    private boolean is_walking = false;
    private TMapPolyLine tmapPolyLine = new TMapPolyLine();
    private TextView dist_text = null;
    private Chronometer chronometer = null;

    private int walk_id = 0;

    private boolean show_event = true;
    private boolean show_alert = true;
    private TreeSet<Integer> showing = new TreeSet<>();

    private SharedPreferences sharedPreferences;
    private Button login_button = null;
    private String login_id = null;

    private ImageView report_center_icon = null;
    private boolean is_pointed = false;

    private ArrayList<Location> locations = new ArrayList<>();
    private double walk_distance = 0;
    private ArrayList<Location> reportLocations = new ArrayList<>();
    private ArrayList<Integer> satelliteList = new ArrayList<>();
    private ArrayList<Walk> walkList = new ArrayList<>();
    private Spinner walkSpinner = null;

    @Override
    public void onLocationChange(Location location) {
        // TODO : location = 보정(location);
        int satellite = 3;
        tmapview.setLocationPoint(location.getLongitude(), location.getLatitude());
        // 이동시 위도 경도 출력
        // Toast.makeText(getApplicationContext(), "위도 : " + location.getLatitude() + "\n경도 : " + location.getLongitude(), Toast.LENGTH_LONG).show();
        lastLocation = location;
        if(is_walking) {
            if(locations.size() > 0) {
                Location last = locations.get(locations.size() - 1);
                walk_distance += distance(last.getLatitude(), last.getLongitude(), location.getLatitude(), location.getLongitude());
                dist_text.setText(String.format("%.2f", walk_distance) + "km");
            }
            locations.add(location);
            satelliteList.add(satellite);
        }

        if(tmapgps.getProvider() == "network") {
            tmapgps.setProvider(tmapgps.GPS_PROVIDER);
            
            new Handler().postDelayed(
                    () -> progressBar.setVisibility(View.GONE),
                    2500);

            togglePoint();

            LinearLayout linearLayoutTmap = findViewById(R.id.mapview);
            linearLayoutTmap.addView(tmapview);
        }

        if(is_walking) {
            tmapPolyLine.addLinePoint(new TMapPoint(location.getLatitude(), location.getLongitude()));
            tmapview.addTMapPolyLine("walk" + walk_id, tmapPolyLine);
        }

        
        for(EventPoint point : showPoint) {
            double dist = distance(location.getLatitude(), location.getLongitude(), point.getLatitude(), point.getLongitude()); // KM

            if(dist < 1) { // 1KM이내에 들어오면 알림 전송
                if(!showing.contains(point.getId())) {
                    sendNotification(point.getId(), point.getTitle(), point.getSubTitle());
                    showing.add(point.getId());
                }
            } else if(dist > 1.5 && showing.contains(point.getId())) { // 1.5km 이상 떨어진 다음, 1KM 이내에 다시 들어와야 알림 전송
                showing.remove(point.getId());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences("sharedPreferences", Activity.MODE_PRIVATE);

        if(!hasPermissions(this, PERMISSIONS))
        {
            ActivityCompat.requestPermissions(this, PERMISSIONS, MULTIPLE_PERMISSION);
        }

        tmapview = new TMapView(this);
        tmapview.setSKTMapApiKey("KRtlS815a12Oxhqfy9gkF7OHp10LDPsu1Nk4nC6d");

        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setZoomLevel(17);
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);

        tmapview.setIconVisibility(true);

        tmapgps = new TMapGpsManager(MainActivity.this);
        tmapgps.setMinTime(1000);
        tmapgps.setMinDistance(5);
        tmapgps.setProvider(tmapgps.NETWORK_PROVIDER);

        tmapgps.OpenGps();
        tmapview.setTrackingMode(true);

        // TODO : Event Point 가져오기
        for(EventPoint point : new EventPoint[]{new EventPoint(37.4963, 126.9569, 0, "이벤트 1", "이벤트 1 설명입니다.", 1), new EventPoint(37.4946, 126.9571, 1, "이벤트 2", "이벤트 2 설명입니다.", 2)}) {
            if (point.checkType(EventConst.IS_EVENT)) eventPoint.add(point);
            else alertPoint.add(point);
        }

        // 산책을 시작하는 버튼과 종료하는 버튼
        start_walk_button = (ImageButton) findViewById(R.id.start_walk_button);
        end_walk_button = (ImageButton) findViewById(R.id.end_walk_button);
        end_walk_button.setVisibility(View.INVISIBLE);

        start_walk_button.setOnClickListener(v -> start_walk());

        end_walk_button.setOnClickListener(v -> onBackPressed());

        // 화면 중앙을 현재 위치로 이동시키는 버튼
        ImageButton return_button = (ImageButton) findViewById(R.id.return_button);
        return_button.setOnClickListener(v -> {
            tmapview.setCenterPoint(lastLocation.getLongitude(), lastLocation.getLatitude());
            tmapview.setTrackingMode(true);
            if(tmapview.getZoomLevel() < 17) tmapview.setZoomLevel(17);
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
        report_center_icon = (ImageView) findViewById(R.id.report_center_icon);
        report_center_icon.bringToFront();
        report_center_icon.setVisibility(View.INVISIBLE);

        report_button.setOnClickListener(v -> {
            if(login_id == null) {
                Toast.makeText(getApplicationContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if(is_pointed) {
                TMapPoint point = tmapview.getCenterPoint();
                Intent intent = new Intent(this, ReportActivity.class);
                intent.putExtra("latitude", point.getLatitude());
                intent.putExtra("longitude", point.getLongitude());
                startActivityForResult(intent, 2);

                onBackPressed();
            } else {
                is_pointed = true;
                report_center_icon.setVisibility(View.VISIBLE);
            }
        });


        walkSpinner = (Spinner) findViewById(R.id.walkSpinner);

        walkSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                show_walk(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        login_button = (Button) findViewById(R.id.login_button);
        if(sharedPreferences.getBoolean("auto_login", false)) {
            login(sharedPreferences.getString("id", ""), sharedPreferences.getString("password", ""));
        }

        login_button.setOnClickListener(v -> {
            if(login_id == null) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivityForResult(intent, 1);
            } else {
                logout();
            }
            
        });

        createNotificationChannel();


        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.bringToFront();

        if(login_id == null)
        {
            Bitmap profile = BitmapFactory.decodeResource(getResources(), R.drawable.default_profile);
            change_profile(profile);
    
            dist_text = (TextView) findViewById(R.id.dist_text);
            chronometer = (Chronometer) findViewById(R.id.chronometer);
            dist_text.setVisibility(View.GONE);
            chronometer.setVisibility(View.GONE);
        }
        else
        {
            Bitmap profile = BitmapFactory.decodeResource(getResources(), R.drawable.login_profile);
            change_profile(profile);
        }
    }


    private int imsi_cnt = 2;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK) {
                login(data.getStringExtra("id"), data.getStringExtra("password"));
            }
        } else if( requestCode == 2) {
            if(resultCode == RESULT_OK) {
                double latitude = data.getDoubleExtra("latitude", 0);
                double longitude = data.getDoubleExtra("longitude", 0);
                String title = data.getStringExtra("title");
                String subtitle = data.getStringExtra("subtitle");
                boolean is_alert = data.getBooleanExtra("is_alert", false);


                // TODO : 신고 내용 서버에 저장하고 서버에서 받은 id를 point에 넣어야 함
                EventPoint point = new EventPoint(latitude, longitude, is_alert ? 0 : 1, title, subtitle, ++imsi_cnt);
                
                if(point.checkType(EventConst.IS_EVENT)) eventPoint.add(point);
                else alertPoint.add(point);

                togglePoint();

                if(is_walking) reportLocations.add(new Location("report") {{
                    setLatitude(latitude);
                    setLongitude(longitude);
                }});

                Toast.makeText(getApplicationContext(), "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
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

    private static int notification_id = 10000000;
    // 알림 id, 제목, 내용으로 핸드폰에 알림을 보내는 함수. 이전과 같은 알림 id로 보내면 덮어씌워진다.
    // 알림 id가 -1로 들어가면 새로운 알림 id로 채워준다.
    public void sendNotification(int id, String title, String text) {
        if(id < 0) id = ++notification_id;
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
            report_center_icon.setVisibility(View.INVISIBLE);
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
        if(progressBar.getVisibility() == View.VISIBLE) {
            Toast.makeText(getApplicationContext(), "GPS를 받아오는 중입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if(login_id == null) {
            Toast.makeText(getApplicationContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        walk_id = walk_id + 1;
        tmapPolyLine = new TMapPolyLine();
        is_walking = true;

        locations.clear();
        reportLocations.clear();
        walk_distance = 0;
        dist_text.setText("0.00km");

        onLocationChange(lastLocation);

        end_walk_button.setVisibility(View.VISIBLE);
        start_walk_button.setVisibility(View.GONE);
        dist_text.setVisibility(View.VISIBLE);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        chronometer.setVisibility(View.VISIBLE);
    }

    // 산책이 종료되었을 때 실행되는 함수
    public void end_walk() {
        tmapview.removeTMapPolyLine("walk" + walk_id);
        is_walking = false;
        chronometer.stop();

        start_walk_button.setVisibility(View.VISIBLE);
        end_walk_button.setVisibility(View.GONE);
        dist_text.setVisibility(View.GONE);
        chronometer.setVisibility(View.GONE);

        Intent intent = new Intent(this, WalkResultActivity.class);

        double[] locationLatitude = new double[locations.size()];
        double[] locationLongitude = new double[locations.size()];
        for(int i = 0; i < locations.size(); i++) {
            locationLatitude[i] = locations.get(i).getLatitude();
            locationLongitude[i] = locations.get(i).getLongitude();
        }

        double[] reportLatitude = new double[reportLocations.size()];
        double[] reportLongitude = new double[reportLocations.size()];
        for(int i = 0; i < reportLocations.size(); i++) {
            reportLatitude[i] = reportLocations.get(i).getLatitude();
            reportLongitude[i] = reportLocations.get(i).getLongitude();
        }

        String walk_time = chronometer.getText().toString().substring(3);

        intent.putExtra("locationLatitude", locationLatitude);
        intent.putExtra("locationLongitude", locationLongitude);
        intent.putExtra("reportLatitude", reportLatitude);
        intent.putExtra("reportLongitude", reportLongitude);
        intent.putExtra("walk_distance", walk_distance);
        intent.putExtra("walk_time", walk_time);
        
        startActivity(intent);

        //서버에 id, 날짜, 시간, 경로 전송하기
        for(int i = 0; i < locations.size(); i++)
        {
            send_post(login_id, walk_id, i, locationLongitude[i], locationLatitude[i], satelliteList.get(i));
        }

        Date date = new Date();
        String datetime = format.format(date);

        String[] time = walk_time.split(":");
        int walk_time_seconds = 0;
        if(time.length == 2) {
            walk_time_seconds = Integer.parseInt(time[0]) * 60 + Integer.parseInt(time[1]);
        }
        else {
            walk_time_seconds = Integer.parseInt(time[0]);
        }

        walkList.add(new Walk(++walk_id, datetime, walk_time_seconds, locationLatitude, locationLongitude));
        toggleWalkSpinner();
    }

    public void togglePoint() {
        tmapview.removeAllMarkerItem();

        showPoint.clear();
        if(show_event) showPoint.addAll(eventPoint);
        if(show_alert) showPoint.addAll(alertPoint);

        showing.clear();

        for(EventPoint point : showPoint) {
            TMapMarkerItem tmarker = new TMapMarkerItem();
            tmarker.setTMapPoint(new TMapPoint(point.getLatitude(), point.getLongitude()));
            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.location_icon);
            tmarker.setIcon(bitmap);
            tmarker.setVisible(TMapMarkerItem.VISIBLE);
            tmarker.setCanShowCallout(true);
            tmarker.setCalloutTitle(point.getTitle());
            tmarker.setCalloutSubTitle(point.getSubTitle());
            tmapview.addMarkerItem("event" + point.getId(), tmarker);

            double dist = distance(lastLocation.getLatitude(), lastLocation.getLongitude(), point.getLatitude(), point.getLongitude());
            if(dist < 1) showing.add(point.getId());
        }
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1.609344;
        return (dist);
    }
    public class JsonTest {
        @SerializedName("userid")
        private String userid;

        @SerializedName("num1")
        private int num1;

        @SerializedName("num2")
        private int num2;

        @SerializedName("longitude")
        private double longitude;

        @SerializedName("latitude")
        private double latitude;

        @SerializedName("satellite")
        private int satellite;
        @Override
        public String toString() {
            return "JsonTest{" +
                    "userid=" + userid +
                    ", num1=" + num1 +
                    ", num2='" + num2 +
                    ", longitude='" + longitude +
                    ", latitude='" + latitude +
                    ", satellite='" + satellite +
                    '}';
        }

        public void setJson(String userid, int num1, int num2, double longitude, double latitude, int satellite)
        {
            this.userid = userid;
            this.num1 = num1;
            this.num2 = num2;
            this.longitude = longitude;
            this.latitude = latitude;
            this.satellite = satellite;
        }
    }

    public interface RetrofitService
    {
        @POST("gps")
        Call<JsonTest> sendPosts(@Body JsonTest jsonTest);
        @GET("gps")
        Call<JsonTest> getPosts();

        @DELETE("gps")
        Call<JsonTest> deletePosts();
    }

    public void send_post(String userid, int num1, int num2, double longitude, double latitude, int satellite)
    {
        // Retrofit 객체 생성 및 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.81.179:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        // RetrofitService 인터페이스 구현체 생성
        RetrofitService service = retrofit.create(RetrofitService.class);

        // JsonTest 객체 생성
        JsonTest jsonTest = new JsonTest();

        // JsonTest 객체에 값을 설정
        jsonTest.setJson(userid, num1, num2, longitude, latitude, satellite);

        // POST 요청 보내기
        Call<JsonTest> call = service.sendPosts(jsonTest);
        call.enqueue(new Callback<JsonTest>() {
            @Override
            public void onResponse(Call<JsonTest> call, Response<JsonTest> response) {
                // 응답 처리
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: 성공");
                    JsonTest result = response.body();
                    // ...
                } else {
                    // 응답이 실패한 경우
                    Log.e(TAG, "onResponse: 실패 - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<JsonTest> call, Throwable t) {
                //Log.d(TAG, "onFailure: 실패");
                Log.e(TAG, "에러 : " + t.getMessage());
                // ...
            }
        });
    }

    private void login(String id, String password) {
        if(id == null) {
            Toast.makeText(getApplicationContext(), "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO : 아이디 비밀번호 확인

        login_id = id;
        getWalkList();

        login_button.setText("로그아웃");

        Toast.makeText(getApplicationContext(), login_id + "님 로그인 되었습니다.", Toast.LENGTH_SHORT).show();

        // TODO : id 프로필 사진 불러오기
        // if(프로필 사진이 있다면) change_profile(profile);
        Bitmap profile = BitmapFactory.decodeResource(getResources(), R.drawable.login_profile);
        change_profile(profile);
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("auto_login", false);
        editor.commit();

        login_id = null;
        login_button.setText("로그인");
        Toast.makeText(getApplicationContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();

        walkList.clear();
        toggleWalkSpinner();

        Bitmap profile = BitmapFactory.decodeResource(getResources(), R.drawable.default_profile);
        change_profile(profile);
    }

    private void change_profile(Bitmap profile) {
        // profile의 크기를 100*100으로 바꿈
        profile = Bitmap.createScaledBitmap(profile, 100, 100, true);
        profile = getCroppedBitmap(profile);
        tmapview.setIcon(profile);
    }

    private Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private void getWalkList() {
        walkList.add(new Walk(-1, "이전 기록 표시안함", 0, new double[]{0}, new double[]{0}));

        // TODO : 서버에서 login_id의 산책 리스트 받아와서 walkList에 추가하기
        // TODO : walk_id = get_walk_id();


        // 임시용
        String datetime = "2020-11-01 12:00:00";
        Date date = null;
        try {
            date = format.parse(datetime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        double[] lat = {37.4963, 37.4946};
        double[] lon = {126.9569, 126.9571};    

        walkList.add(new Walk(++walk_id, datetime, 99, lat, lon));
        toggleWalkSpinner();
    }

    private void toggleWalkSpinner() {
        ArrayList<String> walkDate = new ArrayList<String>();
        for(int i=0;i<walkList.size();i++) {
            walkDate.add(walkList.get(i).getDatetime());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, walkDate);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        walkSpinner.setAdapter(adapter);
        walkSpinner.setSelection(0);
    }

    private TMapPolyLine showingTmapPolyLine = null;

    public void show_walk(int id) {
        if(id < 0) return;

        tmapview.removeTMapPolyLine("showing");
        
        showingTmapPolyLine = new TMapPolyLine();
        showingTmapPolyLine.setLineWidth(30);
        showingTmapPolyLine.setLineColor(Color.YELLOW);
        showingTmapPolyLine.setOutLineColor(Color.YELLOW);
        showingTmapPolyLine.setLineAlpha(50);
        showingTmapPolyLine.setOutLineAlpha(50);

        for(int i=0;i<walkList.get(id).getLatitudes().length;i++) {
            showingTmapPolyLine.addLinePoint(new TMapPoint(walkList.get(id).getLatitudes()[i], walkList.get(id).getLongitudes()[i]));
        }

        tmapview.addTMapPolyLine("showing", showingTmapPolyLine);
    }
}
