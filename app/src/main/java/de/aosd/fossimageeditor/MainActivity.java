package de.aosd.fossimageeditor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sarthakdoshi.textonimage.TextOnImage;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import de.aosd.fossimageeditor.utils.CropTools;
import de.aosd.fossimageeditor.utils.FileUtil;
import de.aosd.fossimageeditor.utils.GPUImageFilterTools;
import de.aosd.fossimageeditor.utils.MsgUtil;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

@SuppressWarnings({"FieldCanBeLocal", "ResultOfMethodCallIgnored"})
public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        GPUImageView.OnPictureSavedListener {

    private String appName;
    private String fileName;
    private String afterSaving;

    private Context context;
    private Activity activity;
    private Uri uriSource;

    private ImageButton filterApply;
    private ImageButton filterCancel;
    private SeekBar seekBar;

    private GPUImageFilter mFilter;
    private GPUImageView mGPUImageView;
    private GPUImageFilterTools.FilterAdjuster mFilterAdjuster;

    private int applyFilterCount;
    private int isSaved;
    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private File sourceImage;
    private File editedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");

        context = this;
        activity = MainActivity.this;
        appName = getResources().getString(R.string.app_name);

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                    View dialogView = View.inflate(activity, R.layout.dialog_action, null);
                    TextView textView = dialogView.findViewById(R.id.dialog_text);
                    textView.setText(R.string.dialog_permission);
                    Button action_ok = dialogView.findViewById(R.id.action_ok);
                    action_ok.setOnClickListener(new View.OnClickListener() {
                        @TargetApi(Build.VERSION_CODES.M)
                        @Override
                        public void onClick(View view) {
                            bottomSheetDialog.cancel();
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_CODE_ASK_PERMISSIONS);
                        }
                    });
                    Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                    action_cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            bottomSheetDialog.cancel();
                            MsgUtil.show(context, R.string.dialog_permission_not);
                        }
                    });
                    bottomSheetDialog.setContentView(dialogView);
                    bottomSheetDialog.show();
                }
            }
        }

        editedImage = null;

        seekBar = findViewById(R.id.seekBar);
        assert seekBar != null;
        seekBar.setOnSeekBarChangeListener(MainActivity.this);
        seekBar.setVisibility(View.INVISIBLE);

        mGPUImageView = findViewById(R.id.gpuImage);
        assert mGPUImageView != null;
        mGPUImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);

        filterApply = findViewById(R.id.filterApply);
        filterApply.setVisibility(View.INVISIBLE);
        filterApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyFilter();
            }
        });

        filterCancel = findViewById(R.id.filterCancel);
        filterCancel.setVisibility(View.INVISIBLE);
        filterCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGPUImageView.setFilter(new GPUImageFilter());
                filterApply.setVisibility(View.INVISIBLE);
                filterCancel.setVisibility(View.INVISIBLE);
                seekBar.setVisibility(View.INVISIBLE);
            }
        });

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
            uriSource = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            sourceImage = new File(Objects.requireNonNull(FileUtil.getPath(context, uriSource)));
            handleImage();
        } else if ("android.intent.action.EDIT".equals(action) && type != null && type.startsWith("image/")) {
            uriSource = intent.getData();
            sourceImage = new File(Objects.requireNonNull(FileUtil.getPath(context, uriSource)));
            handleImage();
        }
    }

    private void switchFilterTo(final GPUImageFilter filter) {
        mFilter = filter;
        mGPUImageView.setFilter(mFilter);
        mFilterAdjuster = new GPUImageFilterTools.FilterAdjuster(mFilter);

        filterApply.setVisibility(View.VISIBLE);
        filterCancel.setVisibility(View.VISIBLE);

        if (mFilterAdjuster.canAdjust()) {
            seekBar.setVisibility(View.VISIBLE);
            seekBar.setProgress(50);
            mGPUImageView.requestRender();
        } else {
            seekBar.setVisibility(View.INVISIBLE);
            mGPUImageView.requestRender();
        }
    }

    private void saveImage() {
        if (editedImage != null) {
            Bitmap bitmap = mGPUImageView.getGPUImage().getBitmapWithFilterApplied();
            int imageWidth = bitmap.getWidth();
            int imageHeight = bitmap.getHeight();
            String folder = "FOSS_ImageEditor";
            mGPUImageView.saveToPictures(folder, fileName, imageWidth, imageHeight, MainActivity.this);
        } else {
            MsgUtil.show(context, R.string.dialog_load);
        }
    }

    private void applyFilter() {
        if (applyFilterCount == 0) {
            Bitmap bitmap = mGPUImageView.getGPUImage().getBitmapWithFilterApplied();
            mGPUImageView.setImage(bitmap);
            mGPUImageView.setFilter(new GPUImageFilter());
            applyFilterCount = 1;
            applyFilter();
        } else {
            Bitmap bitmap = mGPUImageView.getGPUImage().getBitmapWithFilterApplied();
            mGPUImageView.setImage(bitmap);
            mGPUImageView.setFilter(new GPUImageFilter());
        }
        filterApply.setVisibility(View.INVISIBLE);
        filterCancel.setVisibility(View.INVISIBLE);
        seekBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPictureSaved(final Uri uri) {
        mGPUImageView.setFilter(new GPUImageFilter());
        mGPUImageView.setImage(editedImage);
        filterApply.setVisibility(View.INVISIBLE);
        filterCancel.setVisibility(View.INVISIBLE);
        seekBar.setVisibility(View.INVISIBLE);
        isSaved = 1;

        switch (afterSaving) {
            case "afterSaving_share":
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("image/*");
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.dialog_share)));
                break;
            case "afterSaving_text": {

                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                View dialogView = View.inflate(activity, R.layout.dialog_add_text, null);
                final EditText editText = dialogView.findViewById(R.id.dialog_edit);

                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String text = editText.getText().toString();
                        bottomSheetDialog.cancel();
                        Intent intent = new Intent(activity, TextOnImage.class);
                        Bundle bundle = new Bundle();
                        bundle.putString(TextOnImage.IMAGE_IN_URI, uri.toString());          //image uri
                        bundle.putString(TextOnImage.TEXT_COLOR, "#ff6e40");                 //initial color of the text
                        bundle.putFloat(TextOnImage.TEXT_FONT_SIZE, 20.0f);                  //initial text size
                        bundle.putString(TextOnImage.TEXT_TO_WRITE, text);                  //text to be add in the image
                        intent.putExtras(bundle);
                        startActivityForResult(intent, TextOnImage.TEXT_ON_IMAGE_REQUEST_CODE); //start activity for the result
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
                break;
            }
            case "afterSaving_crop":
                CropTools.showDialog(activity, editedImage);
                break;
            case "afterSaving_save":
                MsgUtil.show(context, R.string.dialog_save_ok);
                break;
            case "afterSaving_overwrite": {
                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                View dialogView = View.inflate(activity, R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.dialog_save);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.cancel();
                        FileUtil.copyFile(activity, editedImage, sourceImage);
                        editedImage.delete();
                    }
                });
                Button action_cancel = dialogView.findViewById(R.id.action_cancel);
                action_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
                break;
            }
        }
    } 

    private void handleImage() {

        File directory = new File(Environment.getExternalStorageDirectory() + "/Pictures/FOSS_ImageEditor/");
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Log.d(appName, "Successfully created the parent dir:" + directory.getName());
            } else {
                Log.d(appName, "Failed to create the parent dir:" + directory.getName());
            }
        }

        if (uriSource != null) {
            try {
                String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());

                fileName = date + ".jpg";
                sourceImage = new File(Objects.requireNonNull(FileUtil.getPath(context, uriSource)));
                editedImage = new File(Environment.getExternalStorageDirectory() + "/Pictures/FOSS_ImageEditor/" + fileName);
                FileUtil.copyFile(context, sourceImage, editedImage);

                applyFilterCount = 0;
                isSaved = 0;

                mGPUImageView.setFilter(new GPUImageFilter());
                seekBar.setVisibility(View.INVISIBLE);
                mGPUImageView.setImage(editedImage);

            } catch (Exception e) {
                e.printStackTrace();
                MsgUtil.show(activity, R.string.dialog_load_not);
            }
        } else {
            //MsgUtil.show(activity, R.string.dialog_load_not);
            Log.d(appName, "Something wrong");
        }
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if(requestCode == TextOnImage.TEXT_ON_IMAGE_REQUEST_CODE) {
            if(resultCode == TextOnImage.TEXT_ON_IMAGE_RESULT_OK_CODE) {
                uriSource = Uri.parse(data.getStringExtra(TextOnImage.IMAGE_OUT_URI));

                File outputFile = new File(uriSource.getPath());
                FileUtil.copyFile(activity, outputFile, editedImage);
                outputFile.delete();
                mGPUImageView.setImage(editedImage);
                mGPUImageView.requestRender();

            } else if(resultCode == TextOnImage.TEXT_ON_IMAGE_RESULT_FAILED_CODE) {
                String errorInfo = data.getStringExtra(TextOnImage.IMAGE_OUT_ERROR);
                Log.d(appName, "onActivityResult: "+errorInfo);
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                uriSource = result.getUri();

                File outputFile = new File(uriSource.getPath());
                FileUtil.copyFile(activity, outputFile, editedImage);
                outputFile.delete();
                mGPUImageView.setImage(editedImage);
                mGPUImageView.requestRender();
                
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Log.d(appName, "Something wrong");
            }
        }

        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                uriSource = data.getData();
                sourceImage = new File(Objects.requireNonNull(uriSource).getPath());
                handleImage();
            }
        }
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if (mFilterAdjuster != null) {
            mFilterAdjuster.adjust(progress);
        }
        mGPUImageView.requestRender();
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; context adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    @Override
    public void onBackPressed() {
        if (isSaved == 0) {
            editedImage.delete();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_edit) {
            if (editedImage != null) {
                GPUImageFilterTools.showDialog(activity, new GPUImageFilterTools.OnGpuImageFilterChosenListener() {
                    @Override
                    public void onGpuImageFilterChosenListener(final GPUImageFilter filter) {
                        switchFilterTo(filter);
                        mGPUImageView.requestRender();
                    }
                });
            } else {
                MsgUtil.show(context, R.string.dialog_load);
            }
            return true;
        }

        if (id == R.id.action_pick) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_PICK_IMAGE);
            return true;
        }

        if (id == R.id.action_text) {
            afterSaving = "afterSaving_text";
            saveImage();
            return true;
        }

        if (id == R.id.action_crop) {
            afterSaving = "afterSaving_crop";
            saveImage();
            return true;
        }

        if (id == R.id.action_share) {
            afterSaving = "afterSaving_share";
            saveImage();
            return true;
        }

        if (id == R.id.action_save) {
            afterSaving = "afterSaving_save";
            saveImage();
            return true;
        }

        if (id == R.id.action_overwrite) {
            afterSaving = "afterSaving_overwrite";
            saveImage();
            return true;
        }

        if (id == R.id.action_about_license) {
            MsgUtil.showAboutDialog(activity, getResources().getString(R.string.about_license_title),
                    getResources().getString(R.string.about_license_dialog));
            return true;
        }

        if (id == R.id.action_about_contribute) {
            MsgUtil.showAboutDialog(activity, getResources().getString(R.string.action_about_contribute),
                    getResources().getString(R.string.about_cont_dialog));

            return true;
        }

        if (id == R.id.action_about_help) {
            MsgUtil.showAboutDialog(activity, getResources().getString(R.string.action_about_help),
                    getResources().getString(R.string.about_help_dialog));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
