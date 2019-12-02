package com.deepak.faceclickapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.CameraViewImpl;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST = 1337;
    private static final String TAG = "MainActivity";

    Button getpicture;
    private FirebaseVisionFaceDetector detector;
    private CameraView cameraView;

    private static File getOutputMediaFile() {
        File mediaStorageDir = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            mediaStorageDir = new File(App.getContext().getObbDir().getAbsolutePath(), "FaceClick");
        } else {
            mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "FaceClick");
        }

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;

        String file_path = mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg";

        Log.i("test_log", file_path);

        mediaFile = new File(file_path);
        return mediaFile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getpicture = findViewById(R.id.getpicture);
        getpicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        ArrayList<String> arrPerm = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            arrPerm.add(Manifest.permission.CAMERA);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            arrPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!arrPerm.isEmpty()) {
            String[] permissions = new String[arrPerm.size()];
            permissions = arrPerm.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST);
        }

        // [START set_detector_options]
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                        .build();
        // [END set_detector_options]

        // [START get_detector]
        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        // [END get_detector]

        cameraView = findViewById(R.id.camera_view);

        cameraView.setOnTurnCameraFailListener(new CameraViewImpl.OnTurnCameraFailListener() {
            @Override
            public void onTurnCameraFail(Exception e) {
                Toast.makeText(MainActivity.this, "Switch Camera Failed. Does you device has a front camera?",
                        Toast.LENGTH_SHORT).show();
            }
        });


        cameraView.setOnCameraErrorListener(new CameraViewImpl.OnCameraErrorListener() {
            @Override
            public void onCameraError(Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        cameraView.setOnFrameListener(new CameraViewImpl.OnFrameListener() {
            @Override
            public void onFrame(final byte[] bytes, final int width, final int height, int rotationDegrees) {
                Log.d(TAG, "onFrame() called with: bytes = [" + bytes + "], width = [" + width + "], height = [" + height + "], rotationDegrees = [" + rotationDegrees + "]");

                FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromByteArray(bytes, new FirebaseVisionImageMetadata.Builder().build());
                detector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        for (FirebaseVisionFace firebaseVisionFace : firebaseVisionFaces) {
                            Log.d(TAG, "onSuccess() called with: firebaseVisionFaces = [" + firebaseVisionFace.toString() + "]");
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure() called with: e = [" + e + "]");
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        String permission = permissions[i];
                        if (Manifest.permission.CAMERA.equals(permission)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                // you now have permission
                            }
                        }
                        if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                // you now have permission
                            }
                        }
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }
        }

        // other 'case' lines to check for other
        // permissions this app might request
    }


    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }
}


