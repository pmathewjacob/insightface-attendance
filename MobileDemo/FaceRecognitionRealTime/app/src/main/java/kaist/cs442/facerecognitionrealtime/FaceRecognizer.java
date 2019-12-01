package kaist.cs442.facerecognitionrealtime;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;

public class FaceRecognizer {
    private static int DIM_IMG_SIZE_X = 112;
    private static int DIM_IMG_SIZE_Y = 112;

    private static AssetManager assetManager;
    private static Context appContext;

    private static HashMap<String, float[]> faceFeat = new HashMap();
    private static String mapKey = "Face Feature";

    // options for model interpreter
    private static final Interpreter.Options tfliteOptions = new Interpreter.Options();
    // tflite graph
    private static Interpreter tflite = null;

    private static String modelName = null;

    public FaceRecognizer() {}

    public static void setup(Context context, AssetManager manager, String modelFileName) {
        if(modelName != null && modelFileName.equals(modelName)) return;
        assetManager = manager;
        appContext = context;
        //initialize graph and labels
        try{
            tflite = new Interpreter(loadModelFile(modelFileName), tfliteOptions);
            modelName = modelFileName;
            Log.e("tflite", "Model loaded.");
            loadMap(context);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e("tflite", "Fail to load model.");
        }
    }

    public static void addFaceBitmap(Bitmap faceBitmap, String name) {
        Bitmap resizedBitmap = getResizedBitmap(faceBitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
        float[][] feat = new float[1][512];
        tflite.run(convertBitmapToByteBuffer(resizedBitmap), feat);
        Log.e("tflite", "face added for " + name);
        // for(int i=0; i<feat[0].length; i++) Log.e("arr: ", feat[0][i] + "");
        faceFeat.put(name, normalize(feat[0]));
        saveMap(appContext);
    }

    public static Pair<String, Float> recognizeFaceBitmap(Bitmap faceBitmap) {
        Bitmap resizedBitmap = getResizedBitmap(faceBitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
        float[][] curFeat = new float[1][512];
        tflite.run(convertBitmapToByteBuffer(resizedBitmap), curFeat);
        curFeat[0] = normalize(curFeat[0]);

        Pair<String, Float> best = null;
        for(HashMap.Entry<String, float[]> entry : faceFeat.entrySet()) {
            String key = entry.getKey();
            float[] value = entry.getValue();
            float diff = getFaceDiff(curFeat[0], value);
            if(best == null || best.second > diff) best = Pair.create(key, diff);
        }
        return best;
    }

    private static float getFaceDiff(float[] faceFeat1, float[] faceFeat2) {
        float diff = 0;
        for(int i=0; i<faceFeat1.length; i++)
            diff += (faceFeat1[i] - faceFeat2[i]) * (faceFeat1[i] - faceFeat2[i]);
        return diff;
    }

    private static float[] normalize(float[] feat) {
        float sumSquare = 0;
        for(int i=0; i<feat.length; i++) sumSquare += feat[i] * feat[i];
        for(int i=0; i<feat.length; i++) feat[i] /= Math.sqrt(sumSquare);
        return feat;
    }

    // loads tflite graph from file
    public static MappedByteBuffer loadModelFile(String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Resize bitmap to given dimensions
    private static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    // converts bitmap to byte array which is passed in the tflite graph
    private static ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * 3);
        imgData.order(ByteOrder.nativeOrder());
        int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // loop through all pixels
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat(((val) & 0xFF));
                imgData.putFloat(((val >> 8) & 0xFF));
                imgData.putFloat(((val >> 16) & 0xFF));
            }
        }
        return imgData;
    }

    /**
     *     Save and get HashMap in SharedPreference
     */

    public static void saveMap(Context context) {
        SharedPreferences pSharedPref = context.getSharedPreferences("MyVariables",
                Context.MODE_PRIVATE);
        if (pSharedPref != null) {
            JSONObject jsonObject = new JSONObject(faceFeat);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = pSharedPref.edit();
            editor.remove(mapKey).apply();
            editor.putString(mapKey, jsonString);
            editor.commit();
            Log.e("tflite", "face feat saved");
        }
    }
    public static void loadMap(Context context) {
        SharedPreferences pSharedPref = context.getSharedPreferences("MyVariables",
                Context.MODE_PRIVATE);
        try {
            faceFeat = new HashMap<>();
            if (pSharedPref != null) {
                String jsonString = pSharedPref.getString(mapKey, (new JSONObject()).toString());
                if(jsonString == null) return;
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String key = keysItr.next();
                    faceFeat.put(key, JSONArrayToFloatArray(jsonObject.getJSONArray(key)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static float[] JSONArrayToFloatArray(JSONArray arr) {
        float[] ans = new float[arr.length()];
        try {
            for (int i = 0; i < arr.length(); i++) ans[i] = (float) arr.getDouble(i);
        } catch (Exception e) {}
        return ans;
    }

    public static void clearMap() {
        faceFeat.clear();
        saveMap(appContext);
    }
}
