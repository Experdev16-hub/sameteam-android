package com.example.sameteam.helper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.sameteam.R;
import com.example.sameteam.widget.simplecropview.CropImageView;
import com.example.sameteam.widget.simplecropview.callback.CropCallback;
import com.example.sameteam.widget.simplecropview.callback.LoadCallback;
import com.example.sameteam.widget.simplecropview.callback.SaveCallback;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.yalantis.ucrop.UCrop;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static androidx.core.content.FileProvider.getUriForFile;

public class ImagePickerActivity extends AppCompatActivity {
    private static final String TAG = ImagePickerActivity.class.getSimpleName();
    public static final String INTENT_IMAGE_PICKER_OPTION = "image_picker_option";
    public static final String INTENT_ASPECT_RATIO_X = "aspect_ratio_x";
    public static final String INTENT_ASPECT_RATIO_Y = "aspect_ratio_Y";
    public static final String INTENT_LOCK_ASPECT_RATIO = "lock_aspect_ratio";
    public static final String INTENT_IMAGE_COMPRESSION_QUALITY = "compression_quality";
    public static final String INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT = "set_bitmap_max_width_height";
    public static final String INTENT_BITMAP_MAX_WIDTH = "max_width";
    public static final String INTENT_BITMAP_MAX_HEIGHT = "max_height";

    private static final String KEY_FRAME_RECT = "FrameRect";
    private static final String KEY_SOURCE_URI = "SourceUri";


    public static final int REQUEST_IMAGE_CAPTURE = 0;
    public static final int REQUEST_GALLERY_IMAGE = 1;

    private int ASPECT_RATIO_X = 16, ASPECT_RATIO_Y = 9, bitmapMaxWidth = 1000, bitmapMaxHeight = 1000;
    private int IMAGE_COMPRESSION = 70;
    public static String fileName;
    ConstraintLayout lil_upper;
    LinearLayout layout_root;
    static int isFromConditionMain = 0;

    public static boolean isItemImage = false;

    // Views ///////////////////////////////////////////////////////////////////////////////////////
    private CropImageView mCropView;
    private final Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.JPEG;
    private RectF mFrameRect = null;
    private Uri mSourceUri = null;

    public interface PickerOptionListener {
        void onTakeCameraSelected();

        void onChooseGallerySelected();

        void onTakeVideoSelected();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_picker);
        lil_upper = findViewById(R.id.lil_upper);
        layout_root = findViewById(R.id.layout_root);

        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(getApplicationContext(), "Something went Wrong.", Toast.LENGTH_LONG).show();
            return;
        }

        bindViews();
        if (savedInstanceState != null) {
            // restore data
            mFrameRect = savedInstanceState.getParcelable(KEY_FRAME_RECT);
            mSourceUri = savedInstanceState.getParcelable(KEY_SOURCE_URI);
        }
        mCropView.setDebug(false);
        mCropView.setCropMode(CropImageView.CropMode.FREE);

        layout_root.setVisibility(View.GONE);
        lil_upper.setVisibility(View.VISIBLE);





        ASPECT_RATIO_X = intent.getIntExtra(INTENT_ASPECT_RATIO_X, ASPECT_RATIO_X);
        ASPECT_RATIO_Y = intent.getIntExtra(INTENT_ASPECT_RATIO_Y, ASPECT_RATIO_Y);
        IMAGE_COMPRESSION = intent.getIntExtra(INTENT_IMAGE_COMPRESSION_QUALITY, IMAGE_COMPRESSION);
        boolean lockAspectRatio = intent.getBooleanExtra(INTENT_LOCK_ASPECT_RATIO, false);
        boolean setBitmapMaxWidthHeight = intent.getBooleanExtra(INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, false);
        bitmapMaxWidth = intent.getIntExtra(INTENT_BITMAP_MAX_WIDTH, bitmapMaxWidth);
        bitmapMaxHeight = intent.getIntExtra(INTENT_BITMAP_MAX_HEIGHT, bitmapMaxHeight);

        int requestCode = intent.getIntExtra(INTENT_IMAGE_PICKER_OPTION, -1);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            takeCameraImage();
        } else {
            chooseImageFromGallery();
        }

    }
    private void bindViews() {
        mCropView = findViewById(R.id.cropImageView);
        findViewById(R.id.buttonDone).setOnClickListener(btnListener);
        findViewById(R.id.buttonFitImage).setOnClickListener(btnListener);
        findViewById(R.id.button1_1).setOnClickListener(btnListener);
        findViewById(R.id.button3_4).setOnClickListener(btnListener);
        findViewById(R.id.button4_3).setOnClickListener(btnListener);
        findViewById(R.id.button9_16).setOnClickListener(btnListener);
        findViewById(R.id.button16_9).setOnClickListener(btnListener);
        findViewById(R.id.buttonFree).setOnClickListener(btnListener);
        findViewById(R.id.buttonPickImage).setOnClickListener(btnListener);
        findViewById(R.id.buttonRotateLeft).setOnClickListener(btnListener);
        findViewById(R.id.buttonRotateRight).setOnClickListener(btnListener);
        findViewById(R.id.buttonCustom).setOnClickListener(btnListener);
        findViewById(R.id.buttonCircle).setOnClickListener(btnListener);
        findViewById(R.id.buttonShowCircleButCropAsSquare).setOnClickListener(btnListener);
    }

    private final View.OnClickListener btnListener = new View.OnClickListener() {
        @SuppressLint("NonConstantResourceId")
        @Override public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonDone:
                    cropImage();
                    break;
                case R.id.buttonFitImage:
                    mCropView.setCropMode(CropImageView.CropMode.FIT_IMAGE);
                    break;
                case R.id.button1_1:
                    mCropView.setCropMode(CropImageView.CropMode.SQUARE);
                    break;
                case R.id.button3_4:
                    mCropView.setCropMode(CropImageView.CropMode.RATIO_3_4);
                    break;
                case R.id.button4_3:
                    mCropView.setCropMode(CropImageView.CropMode.RATIO_4_3);
                    break;
                case R.id.button9_16:
                    mCropView.setCropMode(CropImageView.CropMode.RATIO_9_16);
                    break;
                case R.id.button16_9:
                    mCropView.setCropMode(CropImageView.CropMode.RATIO_16_9);
                    break;
                case R.id.buttonCustom:
                    mCropView.setCustomRatio(7, 5);
                    break;
                case R.id.buttonFree:
                    mCropView.setCropMode(CropImageView.CropMode.FREE);
                    break;
                case R.id.buttonCircle:
                    mCropView.setCropMode(CropImageView.CropMode.CIRCLE);
                    break;
                case R.id.buttonShowCircleButCropAsSquare:
                    mCropView.setCropMode(CropImageView.CropMode.CIRCLE_SQUARE);
                    break;
                case R.id.buttonRotateLeft:
                    mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_M90D);
                    break;
                case R.id.buttonRotateRight:
                    mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + v.getId());
            }
        }
    };

    public static void showImagePickerOptions(Context context, final PickerOptionListener listener, int isFromCondition) {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        isFromConditionMain = isFromCondition;
        if (isFromCondition == 0) {
            builder.setTitle("Set Profile Photo");
        } else if (isFromCondition == 2) {
            builder.setTitle("Select Task Photo");

        } else if (isFromCondition == 3) {
            builder.setTitle("Select image from");
        } else if (isFromCondition == 4) {
            builder.setTitle("Select media from");
        }

        String[] animals = new String[]{};
        // add a list
        if (isFromCondition == 4) {
            animals = new String[]{context.getString(R.string.take_photo), context.getString(R.string.choose_file)};
        } else {
            animals = new String[]{context.getString(R.string.take_photo), context.getString(R.string.choose_from_gallery)};

        }
        builder.setItems(animals, (dialog, which) -> {
            switch (which) {
                case 0:
                    listener.onTakeCameraSelected();
                    break;
                case 1:
                    listener.onChooseGallerySelected();
                    break;
            }
        });

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void takeCameraImage() {
        Dexter.withActivity(this)
                .withPermissions(Utils.getPermissionAsPerAndroidVersion())
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            if (isFromConditionMain == 2) {
                                fileName = "Document_" + System.currentTimeMillis() + ".jpg";
                            } else {
                                fileName = "Profile_" + System.currentTimeMillis() + ".jpg";
                            }
                            // fileName = "Profile_" + System.currentTimeMillis() + ".jpg";
                            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, getCacheImagePath(fileName));
                            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                            }
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void chooseImageFromGallery() {
        Dexter.withActivity(this)
                .withPermissions(Utils.getPermissionAsPerAndroidVersion())
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto, REQUEST_GALLERY_IMAGE);
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Bitmap rotatedBitmap = null;
                    Uri sourceUri = getCacheImagePath(fileName);
                    ExifInterface ei = null;
                    try {
                        ei = new ExifInterface(getFilePath(fileName));
                        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED);
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), sourceUri);

                        switch (orientation) {

                            case ExifInterface.ORIENTATION_ROTATE_90:
                                rotatedBitmap = rotateImage(bitmap, 90);
                                sourceUri = getRotatedURI(rotatedBitmap);
                                break;

                            case ExifInterface.ORIENTATION_ROTATE_180:

                                break;

                            case ExifInterface.ORIENTATION_ROTATE_270:
                                rotatedBitmap = rotateImage(bitmap, 270);
                                sourceUri = getRotatedURI(rotatedBitmap);
                                break;

                            case ExifInterface.ORIENTATION_NORMAL:
                            default:


//                        File f = new File(getCacheDir(), getFilePath(fileName));
//                        f.createNewFile();
//                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//                        rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
//                        byte[] bitmapdata = bos.toByteArray();
//
//                        FileOutputStream fos = new FileOutputStream(f);
//                        fos.write(bitmapdata);
//                        fos.flush();
//                        fos.close();


                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    cropImage(sourceUri);


                } else {
                    setResultCancelled();
                }
                break;
            case REQUEST_GALLERY_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri imageUri = data.getData();
                    cropImage(imageUri);
                } else {
                    setResultCancelled();
                }
                break;
            case UCrop.REQUEST_CROP:
                if (resultCode == RESULT_OK) {
                    handleUCropResult(data);
                } else {
                    setResultCancelled();
                }
                break;
            case UCrop.RESULT_ERROR:
                final Throwable cropError = UCrop.getError(data);
                Log.e(TAG, "Crop error: " + cropError);
                setResultCancelled();
                break;
            default:
                setResultCancelled();
        }
    }

    private Uri getRotatedURI(Bitmap bitmap) {

        File file = new File(getFilePath(fileName));
        OutputStream os = null;
        Uri sourceUri = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            sourceUri = Uri.fromFile(file);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sourceUri;

    }

    Uri destinationUri;

    private void cropImage(Uri sourceUri) {

        if (isItemImage) {
            isItemImage = false;
            destinationUri = Uri.fromFile(new File(getCacheDir(), "Item_" + System.currentTimeMillis() + ".jpg"));
        } else {
            destinationUri = Uri.fromFile(new File(getCacheDir(), "Profile_" + System.currentTimeMillis() + ".jpg"));
        }

        if (isFromConditionMain == 2) {
            destinationUri = Uri.fromFile(new File(getCacheDir(), "Document_" + System.currentTimeMillis() + ".jpg"));
        }


        layout_root.setVisibility(View.VISIBLE);
        lil_upper.setVisibility(View.GONE);


//        UCrop.Options options = new UCrop.Options();
//        options.setCompressionQuality(IMAGE_COMPRESSION);
//
//        // applying UI theme
//        options.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary));
//        options.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
//        options.setActiveWidgetColor(ContextCompat.getColor(this, R.color.colorPrimary));
//
//        if (lockAspectRatio)
//            options.withAspectRatio(ASPECT_RATIO_X, ASPECT_RATIO_Y);
//
//        if (setBitmapMaxWidthHeight)
//            options.withMaxResultSize(bitmapMaxWidth, bitmapMaxHeight);
//
//        UCrop.of(sourceUri, destinationUri)
//                .withOptions(options)
//                .start(this);


        mCropView.load(sourceUri)
                .initialFrameRect(mFrameRect)
                .useThumbnail(true)
                .execute(mLoadCallback);

    }


    private final LoadCallback mLoadCallback = new LoadCallback() {
        @Override
        public void onSuccess() {
        }

        @Override
        public void onError(Throwable e) {
        }
    };

    public void cropImage() {
        //    showProgress();
        mCropView.crop(mSourceUri).execute(mCropCallback);
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private final CropCallback mCropCallback = new CropCallback() {
        @Override
        public void onSuccess(Bitmap cropped) {
            mCropView.save(cropped)
                    .compressFormat(mCompressFormat)
                    .compressQuality(40)
                    .execute(destinationUri, mSaveCallback);
        }

        @Override
        public void onError(Throwable e) {
        }
    };

    private final SaveCallback mSaveCallback = new SaveCallback() {
        @Override
        public void onSuccess(Uri outputUri) {
            setResultOk(outputUri);
            //  dismissProgress();
            // ((BasicActivity) getActivity()).startResultActivity(outputUri);
        }

        @Override
        public void onError(Throwable e) {
            setResultCancelled();
            // dismissProgress();
        }
    };


    private void handleUCropResult(Intent data) {
        if (data == null) {
            setResultCancelled();
            return;
        }
        final Uri resultUri = UCrop.getOutput(data);
        setResultOk(resultUri);
    }

    private void setResultOk(Uri imagePath) {
        Intent intent = new Intent();
        intent.putExtra("path", imagePath);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void setResultCancelled() {
        Intent intent = new Intent();
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }

    private Uri getCacheImagePath(String fileName) {
        File path = new File(getExternalCacheDir(), "camera");
        if (!path.exists()) path.mkdirs();
        File image = new File(path, fileName);
        return getUriForFile(ImagePickerActivity.this, getPackageName() + ".provider", image);
    }

    private String getFilePath(String fileName) {
        File path = new File(getExternalCacheDir(), "camera");
        if (!path.exists()) path.mkdirs();
        File image = new File(path, fileName);
        return image.getAbsolutePath();
    }

    private static String queryName(ContentResolver resolver, Uri uri) {
        Cursor returnCursor =
                resolver.query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }

    /**
     * Calling this will delete the images from cache directory
     * useful to clear some memory
     */
    public static void clearCache(Context context) {
        File path = new File(context.getExternalCacheDir(), "camera");
        if (path.exists() && path.isDirectory()) {
            for (File child : path.listFiles()) {
                child.delete();
            }
        }
    }
}