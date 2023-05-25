package com.example.project;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
    private boolean auto_login = false;
    private SharedPreferences sharedPreferences;

    Button login_button, cancel_button;
    EditText et_id, et_password;
    CheckBox auto_login_checkbox;

    String login_id, login_password;
    boolean auto_login_checked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences("sharedPreferences", Activity.MODE_PRIVATE);

        cancel_button = (Button) findViewById(R.id.cancel_button);
        login_button = (Button) findViewById(R.id.login_button);

        et_id = (EditText) findViewById(R.id.et_id);
        et_password = (EditText) findViewById(R.id.et_password);

        auto_login_checkbox = (CheckBox) findViewById(R.id.auto_login_checkbox);

        auto_login_checked = sharedPreferences.getBoolean("auto_login", false);
        if(auto_login_checked) {
            auto_login_checkbox.setChecked(true);
            et_id.setText(sharedPreferences.getString("id", ""));
            et_password.setText(sharedPreferences.getString("password", ""));
        }
        
        cancel_button.setOnClickListener(v -> onBackPressed());
        login_button.setOnClickListener(v -> {
            login_id = et_id.getText().toString();
            login_password = et_password.getText().toString();
            auto_login = auto_login_checkbox.isChecked();

            if(check_login(login_id, login_password)) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("id", login_id);
                editor.putString("password", login_password);
                editor.putBoolean("auto_login", auto_login);
                editor.commit();
                
                Intent intent = new Intent();
                intent.putExtra("id", login_id);
                intent.putExtra("password", login_password);
                setResult(RESULT_OK, intent);

                finish();
            } else {
                Toast.makeText(getApplicationContext(), "아이디 또는 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // TODO : 로그인 확인하기
    public boolean check_login(String id, String password) {
        if(id.equals("test") && password.equals("test")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        login_id = null;

        Intent intent = new Intent();
        intent.putExtra("id", login_id);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if( event.getAction() == MotionEvent.ACTION_OUTSIDE ) {
            return false;
        }
        return true;
    }
}
