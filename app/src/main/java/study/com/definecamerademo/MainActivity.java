package study.com.definecamerademo;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import study.com.cameralibrary.CameraActivity;
import study.com.cameralibrary.utils.PermissionCheckUtils;

public class MainActivity extends AppCompatActivity {

    private ImageView iv;
    private ObjectAnimator leftAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = (ImageView) findViewById(R.id.iv);

        Button bt1 = (Button) findViewById(R.id.bt_1);
        Button bt2 = (Button) findViewById(R.id.bt_2);

        leftAnimation = ObjectAnimator.ofFloat(bt2, "translationX", 0f, 100f);
        leftAnimation.setInterpolator(new OvershootInterpolator());
        leftAnimation.setDuration(300);

        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                leftAnimation.start();
            }
        });

        PermissionCheckUtils.setOnOnWantToOpenPermissionListener(new PermissionCheckUtils.OnWantToOpenPermissionListener() {
            @Override
            public void onWantToOpenPermission() {
                Toast.makeText(MainActivity.this, "请开启打开相机的权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void jump(View view) {
        int size = PermissionCheckUtils.checkActivityPermissions(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 100, null);
        if (size == 0) {
            jumpCamera();
        }
    }

    private void jumpCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean flag = true;

            for (int i = 0; i < permissions.length; i++) {
                flag &= (grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }

            if (flag) {
                jumpCamera();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (data != null) {
                String path = data.getStringExtra("path");
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                iv.setImageBitmap(bitmap);
            }
        }
    }
}
