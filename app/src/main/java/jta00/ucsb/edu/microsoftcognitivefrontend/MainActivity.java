package jta00.ucsb.edu.microsoftcognitivefrontend;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import jta00.ucsb.edu.microsoftcognitivefrontend.JSONObjects.EmotionApiResponse;
import jta00.ucsb.edu.microsoftcognitivefrontend.JSONObjects.EmotionApiResult;
import jta00.ucsb.edu.microsoftcognitivefrontend.JSONObjects.FaceAttributes;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final static int CAMERA_PERMISSION_CHECK = 0x41;
    final static int EXTERNAL_STORAGE_CHECK = 0x43;
    final static int INTERNET_PERMISSION_CHECK = 0x42;
    final static int CAPTURE_IMAGE_REQUEST = 0X31;

    public static final String TAG = "MSFT_COG";

    boolean useCamera = false;
    String lastImgFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        checkPermissionsCamera();
    }

    public void onClick(View view){
        if(view.getId() == R.id.fab){
            takePicture();
        }
    }

    public void takePicture(){
        if(!useCamera){
            checkPermissionsCamera();
            if(!useCamera) {
                Utility.makeToast(getString(R.string.cannot_take_picture), getApplicationContext());
                return;
            }
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.d(TAG, ex.getMessage());
                ex.printStackTrace();
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                lastImgFile = photoFile.getPath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                try {
                    Bitmap photo = BitmapFactory.decodeFile(lastImgFile);
                    photo = Utility.getResizedBitmap(photo, 1500);
                    FileOutputStream out = new FileOutputStream(lastImgFile);
                    photo.compress(Bitmap.CompressFormat.JPEG, 90, out);

                    ImageView imageDisplay = (ImageView) findViewById(R.id.image_display);
                    // imageDisplay.setImageBitmap(photo);
                    String emotionResults = makeRequestWithPhoto(lastImgFile);
                    Log.d(TAG, emotionResults);
                    Gson gson = new Gson();
                    EmotionApiResponse fullResponse = gson.fromJson(emotionResults, EmotionApiResponse.class);
                    StringBuilder outStr = new StringBuilder();

                    if(fullResponse.face.length > 0){
                        FaceAttributes face = fullResponse.face[0].faceAttributes;
                        outStr.append("Face attributes:\n");
                        outStr.append("Age: " + face.age + "\n");
                        outStr.append("Gender: " + face.gender + "\n");
                        outStr.append("Glasses: " + (face.glasses.equals("NoGlasses") ? "None" : face.glasses) + "\n");

                        outStr.append('\n');
                        outStr.append("Facial Hair:\n");
                        Hashtable<String, Double> hairList = face.facialHair;
                        while (hairList.size() > 0) {
                            String max = "";
                            for (String key : hairList.keySet()) {
                                if (max.equals("") || hairList.get(max).doubleValue() < hairList.get(key))
                                    max = key;
                            }
                            outStr.append(max + ":  " + hairList.remove(max).doubleValue() + "\n");
                        }
                    }

                    outStr.append('\n');
                    if (fullResponse.emotions.length > 0) {
                        outStr.append("Emotions:\n");
                        EmotionApiResult[] emotions = fullResponse.emotions;
                        Hashtable<String, Double> emotList = emotions[0].scores;
                        while (emotList.size() > 0) {
                            String max = "";
                            for (String key : emotList.keySet()) {
                                if (max.equals("") || emotList.get(max).doubleValue() < emotList.get(key))
                                    max = key;
                            }
                            outStr.append(max + ":  " + emotList.remove(max).doubleValue() + "\n");
                        }
                    }

                    TextView resultsDisplay = (TextView) findViewById(R.id.results_display);
                    resultsDisplay.setText(outStr.toString());

                    File toDelete = new File(lastImgFile);
                    toDelete.delete();
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                    e.printStackTrace();
                }
            } else if (resultCode == RESULT_CANCELED) {

            } else {
                Utility.makeToast(getString(R.string.did_not_get_picture), getApplicationContext());
            }
        }
    }

    protected String makeRequestWithPhoto(String fileName){
        String postUrlText = getApplicationContext().getString(R.string.api_url) + "upload";

        try{
            File file = new File(fileName);
            int size = (int)file.length();
            byte[] imgBytes = new byte[size];
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(imgBytes, 0, imgBytes.length);
            buf.close();

            URL postUrl = new URL(postUrlText);
            StringUrlPair pair = new StringUrlPair(imgBytes, postUrl, "POST");

            ApiAccessor accessor = new ApiAccessor();
            accessor.execute(pair);
            String jsonResponse = accessor.get(20, TimeUnit.SECONDS);
            return jsonResponse;
        }
        catch(Exception e){
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_CHECK:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermissionsInternet();
                }
                else{
                    useCamera = false;
                }
                break;
            case INTERNET_PERMISSION_CHECK:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermissionsStorage();
                }
                else{
                    useCamera = false;
                }
                break;
            case EXTERNAL_STORAGE_CHECK:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    useCamera = true;
                }
                else{
                    useCamera = false;
                }
                break;
        }
    }

    private void checkPermissionsCamera(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_CHECK);
        }
        else checkPermissionsInternet();
    }

    private void checkPermissionsInternet(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    INTERNET_PERMISSION_CHECK);
        }
        else checkPermissionsStorage();
    }

    private void checkPermissionsStorage(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    EXTERNAL_STORAGE_CHECK);
        }
        else{
            useCamera = true;
        }
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }
}