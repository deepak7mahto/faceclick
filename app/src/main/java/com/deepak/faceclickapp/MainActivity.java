package com.deepak.faceclickapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import static android.graphics.ImageFormat.NV21;
import static com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata.ROTATION_0;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST = 1337;
    private static final String TAG = "MainActivity";
    private FirebaseVisionFaceDetector detector;
    private CameraView cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
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
                Toast.makeText(MainActivity.this, "Switch Camera Failed. Does you device has a front camera?", Toast.LENGTH_SHORT).show();
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

                FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromByteArray(bytes, new FirebaseVisionImageMetadata
                        .Builder()
                        .setFormat(NV21)
                        .setHeight(height)
                        .setWidth(width)
                        .setRotation(ROTATION_0)
                        .build());

                detector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        for (FirebaseVisionFace firebaseVisionFace : firebaseVisionFaces) {
                            Log.d(TAG, "onSuccess() called with: firebaseVisionFaces = [" + firebaseVisionFace.toString() + "]");
                            cameraView.takePicture();
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

        cameraView.setOnPictureTakenListener(new CameraViewImpl.OnPictureTakenListener() {
            @Override
            public void onPictureTaken(Bitmap bitmap, int rotationDegrees) {
                Log.d(TAG, "onPictureTaken() called with: bitmap = [" + bitmap + "], rotationDegrees = [" + rotationDegrees + "]");
                cameraView.stop();
                new SaveFileInMemory(bitmap, rotationDegrees, new OnFileSaved() {
                    @Override
                    public void success(File s) {
                        Toast.makeText(MainActivity.this, "File Saved " + s, Toast.LENGTH_SHORT).show();
                        Uri fileUri = GenericFileProvider.getUriForFile(MainActivity.this, "com.deepak.faceclickapp.provider", s);


                        Intent viewFile = new Intent(Intent.ACTION_SEND);

                        viewFile.setData(fileUri);
                        viewFile.setDataAndType(fileUri, URLConnection.guessContentTypeFromName(fileUri.toString()));
                        viewFile.putExtra(Intent.EXTRA_STREAM, fileUri);
                        viewFile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        viewFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        try {
                            startActivity(viewFile);
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(MainActivity.this, "Please install an appropriate application to open this file.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).execute();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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


