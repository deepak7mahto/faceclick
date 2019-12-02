package com.deepak.faceclickapp;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

class SaveFileInMemory extends AsyncTask<Void, Void, File> {
    private final int rotationDegrees;
    private Bitmap bitmap;
    private OnFileSaved onFileSaved;

    public SaveFileInMemory(Bitmap bitmap, int rotationDegrees, OnFileSaved onFileSaved) {
        this.bitmap = bitmap;
        this.rotationDegrees = rotationDegrees;
        this.onFileSaved = onFileSaved;
    }

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
    protected File doInBackground(Void... voids) {

        Matrix matrix = new Matrix();
        matrix.postRotate(-rotationDegrees);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);


        try {

            File file = getOutputMediaFile();

            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);

            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(File s) {
        super.onPostExecute(s);

        if (s != null) {
            onFileSaved.success(s);
        }
    }
}
