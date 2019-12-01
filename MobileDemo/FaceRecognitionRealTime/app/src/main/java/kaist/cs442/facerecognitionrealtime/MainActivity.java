package kaist.cs442.facerecognitionrealtime;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    final private String modelFileName = "vargfacenet.tflite";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkPermission()) setup();
        else requestPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (checkPermission()) setup();
        else requestPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private boolean checkPermission() {
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int rr = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int rw = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return rc == rr && rc == rw && rc == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, permissions, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for(int i=0; i<grantResults.length; i++)
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED) return;
        setup();
    }

    private void setup() {
        FaceRecognizer.setup(getApplicationContext(), this.getAssets(), modelFileName);

        Button btnAddFace = findViewById(R.id.btnAddFace);
        btnAddFace.setOnClickListener((view) -> {
            Intent intent = new Intent(this, AddFaceActivity.class);
            startActivity(intent);
        });

        Button btnDetectFace = findViewById(R.id.btnDetect);
        btnDetectFace.setOnClickListener((view) -> {
            Intent intent = new Intent(this, DetectFaceActivity.class);
            startActivity(intent);
        });

        Button btnDetectFaceRealTime = findViewById(R.id.btnDetectRealTime);
        btnDetectFaceRealTime.setOnClickListener((view) -> {
            Intent intent = new Intent(this, FaceTrackerActivity.class);
            startActivity(intent);
        });

        Button btnClearFaceData = findViewById(R.id.btnClearFace);
        btnClearFaceData.setOnClickListener((view) -> {
            FaceRecognizer.clearMap();
            Toast.makeText(this, "Face data cleared.", Toast.LENGTH_SHORT).show();
        });
    }
}
