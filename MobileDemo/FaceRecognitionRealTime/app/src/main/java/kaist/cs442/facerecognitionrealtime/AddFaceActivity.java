package kaist.cs442.facerecognitionrealtime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.exifinterface.media.ExifInterface;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

public class AddFaceActivity extends AppCompatActivity {
    final private int TAKE_PHOTO_CODE = 0;
    final private int GALLERY_PHOTO_CODE = 1;

    private Uri imageUri;
    private Bitmap capturedFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face);
        selectImage(this);
        Button addFaceBtn = findViewById(R.id.btnAddFace);
        addFaceBtn.setOnClickListener(view -> {
            EditText nameText = findViewById(R.id.faceName);
            if(nameText.getText().toString().trim().equals(""))
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            else if (capturedFace != null) {
                Log.d(this.getLocalClassName(), "matt:: adding face");
                FaceRecognizer.addFaceBitmap(capturedFace, nameText.getText().toString());
                finish();
            } else {
                Toast.makeText(this, "Image is required", Toast.LENGTH_SHORT).show();
                selectImage(this);
            }
        });
    }

    private void selectImage(Context context) {
        Log.d(this.getLocalClassName(), "matt:: starting select image");
        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose your image");

        builder.setItems(options, (dialog, item) -> {

            if (options[item].equals("Take Photo")) {
                openCameraIntent();

            } else if (options[item].equals("Choose from Gallery")) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto , GALLERY_PHOTO_CODE);

            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    // Opens camera for user
    @SuppressLint("SourceLockedOrientationActivity")
    private void openCameraIntent(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        // tell camera where to store the resulting picture
        /*imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);*/
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // start camera, and wait for it to finish
        startActivityForResult(intent, TAKE_PHOTO_CODE);
    }

    private void detectFace(@NotNull Bitmap faceImg) {

        Log.d(this.getLocalClassName(), "matt:: starting detect");
        faceImg = faceImg.copy(Bitmap.Config.ARGB_8888, true);
        ImageView imageView = findViewById(R.id.faceImg);
        FaceDetector faceDetector = new FaceDetector.Builder(this).setTrackingEnabled(false).build();
        if(!faceDetector.isOperational()){
            new AlertDialog.Builder(this).setMessage("Could not set up the face detector!").show();
            return;
        }
        Frame frame = new Frame.Builder().setBitmap(faceImg).build();
        SparseArray<Face> faces = faceDetector.detect(frame);

        if(faces.size() == 0) {
            Log.d(this.getLocalClassName(), "matt:: No face found!");
            Toast.makeText(this, "No face found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Face face = faces.valueAt(0);
        capturedFace = Bitmap.createBitmap((int) face.getWidth(), (int) face.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(capturedFace);
        tempCanvas.drawBitmap(faceImg, -face.getPosition().x, -face.getPosition().y, null);
        imageView.setImageBitmap(capturedFace);
        Log.d(this.getLocalClassName(), "matt:: detection ended");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_CANCELED) {
            Log.d(this.getLocalClassName(), "matt:: request code " + requestCode);
            switch (requestCode) {
                case TAKE_PHOTO_CODE:
                    if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                        detectFace((Bitmap) Objects.requireNonNull(data.getExtras().get("data")));
                    }
                    break;

                case GALLERY_PHOTO_CODE:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage =  data.getData();
                        if (selectedImage != null) {
                            Glide.with(this).asBitmap().load(selectedImage).into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    detectFace(resource);
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                }
                            });
                        }
                    }
                    break;
            }
        }
        else finish();
    }

    public static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) {
        ExifInterface ei = null;
        try {
            ei = new ExifInterface(image_absolute_path);
        } catch (Exception e) {
            return bitmap;
        }
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return bitmap;
        }
    }

    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
