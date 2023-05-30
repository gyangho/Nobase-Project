package com.example.project;

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
import android.view.View;
import android.widget.Button;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.location.Location;

import com.google.gson.annotations.SerializedName;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapMarkerItem;

import java.util.ArrayList;
import java.util.TreeSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

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

public class MainActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {

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

    //private Button end_walk_button = null, start_walk_button = null;
    private ImageButton start_walk_button = null, end_walk_button = null;
    private boolean is_walking = false;
    private TMapPolyLine tmapPolyLine = new TMapPolyLine();

    private int walk_id = 0;

    private boolean show_event = true;
    private boolean show_alert = true;
    TreeSet<Integer> showing = new TreeSet<>();

    private SharedPreferences sharedPreferences;
    private Button login_button = null;
    private String login_id = null;

    ImageView report_center_icon = null;
    private boolean is_pointed = false;


    @Override
    public void onLocationChange(Location location) {
        // int Satellite = tmapgps.getSatellite();
        // TODO : location = 보정(location);
        tmapview.setLocationPoint(location.getLongitude(), location.getLatitude());
        // 이동시 위도 경도 출력
        // Toast.makeText(getApplicationContext(), "위도 : " + location.getLatitude() + "\n경도 : " + location.getLongitude(), Toast.LENGTH_LONG).show();
        lastLocation = location;

        if(tmapgps.getProvider() == "network") {
            tmapgps.setProvider(tmapgps.GPS_PROVIDER);
            
            new android.os.Handler().postDelayed(
                    () -> progressBar.setVisibility(View.GONE),
                    2500);

            for(EventPoint point : showPoint) {
                double dist = distance(location.getLatitude(), location.getLongitude(), point.getLatitude(), point.getLongitude());
                if(dist < 1) showing.add(point.getId());
            }

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
                    sendNotification(point.getId(), "이벤트 발생", point.getId() + "번 이벤트 발생");
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

        tmapPolyLine.setLineColor(Color.YELLOW);
        tmapPolyLine.setLineWidth(20);
        tmapPolyLine.setOutLineAlpha(0);

        tmapgps.OpenGps();
        tmapview.setTrackingMode(true);

        // TODO : walk_id = get_walk_id();

        // TODO : Event Point 가져오기
        for(EventPoint point : new EventPoint[]{new EventPoint(37.4963, 126.9569, 0, "이벤트 1", "이벤트 1 설명입니다.", 1), new EventPoint(37.4946, 126.9571, 1, "이벤트 2", "이벤트 2 설명입니다.", 2)}) {
            if(point.checkType(EventConst.IS_EVENT)) eventPoint.add(point);
            else alertPoint.add(point);
        }

        // 입력으로 받은 event들을 화면에 보여준다.
        togglePoint();

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
                startActivity(intent);

                onBackPressed();
            } else {
                is_pointed = true;
                report_center_icon.setVisibility(View.VISIBLE);
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
        }
        else
        {
            Bitmap profile = BitmapFactory.decodeResource(getResources(), R.drawable.login_profile);
            change_profile(profile);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            login(data.getStringExtra("id"), data.getStringExtra("password"));
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
        if(login_id == null) {
            Toast.makeText(getApplicationContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

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
            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.location_icon);
            tmarker.setIcon(bitmap);
            //tmarker.setPosition(0.5F, 1.0F);  //마커의 중심점을 하단, 중앙으로 설정
            tmarker.setVisible(TMapMarkerItem.VISIBLE);
            tmarker.setCanShowCallout(true);
            tmarker.setCalloutTitle(point.getTitle());
            tmarker.setCalloutSubTitle(point.getSubTitle());
            tmapview.addMarkerItem("event" + point.getId(), tmarker);
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
        private String userid = "1";

        @SerializedName("num1")
        private int num1 = 1;

        @SerializedName("num2")
        private int num2 = 1;

        @SerializedName("longitude")
        private float longitude = 1.0F;

        @SerializedName("latitude")
        private float latitude = 1.0F;

        @SerializedName("satellite")
        private int satellite = 3;
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
    }

    public interface RetrofitService {

        // @GET( EndPoint-자원위치(URI) )
        @POST("gps/{post}")
        Call<JsonTest> getPosts(@Path("post") String post, @Body JsonTest jsonTest);
    }

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

// ...

    // POST 요청 보내기
    Call<JsonTest> call = service.getPosts("post", jsonTest);
    call.enqueue(new Callback<JsonTest>() {
            @Override
            public void onResponse(Call<JsonTest> call, Response<JsonTest> response) {
                // 응답 처리
                if (response.isSuccessful()) {
                    // 성공적인 응답 처리
                    JsonTest result = response.body();
                    // ...
                } else {
                    // 응답이 실패한 경우
                    // ...
                }
            }

            @Override
            public void onFailure(Call<JsonTest> call, Throwable t) {
                // 실패 처리
                // ...
            }
        });


    private void login(String id, String password) {
        if(id == null) {
            Toast.makeText(getApplicationContext(), "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO : 아이디 비밀번호 확인

        login_id = id;

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
}
