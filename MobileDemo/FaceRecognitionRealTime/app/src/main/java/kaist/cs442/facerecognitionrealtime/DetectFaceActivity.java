package kaist.cs442.facerecognitionrealtime;

import androidx.appcompat.app.AppCompatActivity;

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
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

public class DetectFaceActivity extends AppCompatActivity {
    final private int TAKE_PHOTO_CODE = 0;
    final private int GALLERY_PHOTO_CODE = 1;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_face);
        selectImage(this);
    }

    private void selectImage(Context context) {
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
    private void openCameraIntent(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        // tell camera where to store the resulting picture
        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // start camera, and wait for it to finish
        startActivityForResult(intent, TAKE_PHOTO_CODE);
    }

    private void detectFace(Bitmap sourceImg) {
        sourceImg = sourceImg.copy(Bitmap.Config.ARGB_8888, true);
        ImageView imageView = findViewById(R.id.faceImg);
        FaceDetector faceDetector = new FaceDetector.Builder(this).setMode(FaceDetector.ACCURATE_MODE).setTrackingEnabled(false).build();
        if(!faceDetector.isOperational()){
            new AlertDialog.Builder(this).setMessage("Could not set up the face detector!").show();
            finish();
            return;
        }
        Frame frame = new Frame.Builder().setBitmap(sourceImg).build();
        SparseArray<Face> faces = faceDetector.detect(frame);

        Canvas sourceCanvas = new Canvas(sourceImg);
        Bitmap capturedFace = sourceImg;

        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);

        Paint mIdPaint = new Paint();
        mIdPaint.setColor(Color.RED);
        mIdPaint.setTextSize(50);

        for(int i=0; i<faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            sourceCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
            capturedFace = Bitmap.createBitmap((int) thisFace.getWidth(), (int) thisFace.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas tempCanvas = new Canvas(capturedFace);
            tempCanvas.drawBitmap(sourceImg, -thisFace.getPosition().x, -thisFace.getPosition().y, null);
            Pair<String, Float> detectedFace = FaceRecognizer.recognizeFaceBitmap(capturedFace);
            if(detectedFace == null || detectedFace.second >= 1.24) {
                detectedFace = Pair.create("unknown", 0f);
            }
            sourceCanvas.drawText(detectedFace.first + ": " + detectedFace.second, thisFace.getPosition().x, thisFace.getPosition().y, mIdPaint);
        }

        imageView.setImageBitmap(sourceImg);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case TAKE_PHOTO_CODE:
                    if (resultCode == RESULT_OK && data != null) {
                        ImageDecoder.Source src = ImageDecoder.createSource(getContentResolver(), imageUri);
                        Bitmap image = null;
                        try {
                            image = ImageDecoder.decodeBitmap(src);
                        } catch (Exception e) {}
                        if(image != null) detectFace(image);
                    }

                    break;
                case GALLERY_PHOTO_CODE:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage =  data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();

                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                                Bitmap imgBitmap = BitmapFactory.decodeFile(picturePath);
                                Bitmap rotatedImgBitmap = modifyOrientation(imgBitmap, picturePath);
                                detectFace(rotatedImgBitmap);
                                cursor.close();
                            }
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
