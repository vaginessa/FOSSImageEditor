package de.aosd.fossimageeditor;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
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
import android.widget.SeekBar;
import android.widget.TextView;

import com.sarthakdoshi.textonimage.TextOnImage;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.Objects;

import de.aosd.fossimageeditor.utils.FileUtil;
import de.aosd.fossimageeditor.utils.GPUImageFilterTools;
import de.aosd.fossimageeditor.utils.MsgUtil;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        GPUImageView.OnPictureSavedListener {

    private GPUImageFilter mFilter;
    private GPUImageView mGPUImageView;
    private GPUImageFilterTools.FilterAdjuster mFilterAdjuster;
    public Context context;

    private File sourceImage;
    private File editedImage;
    private Uri uriSource;
    private SeekBar seekBar;

    private String fileName;

    private int h;
    private int w;
    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;


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

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
                    View dialogView = View.inflate(MainActivity.this, R.layout.dialog_action, null);
                    TextView textView = dialogView.findViewById(R.id.dialog_text);
                    textView.setText(R.string.dialog_permission);
                    Button action_ok = dialogView.findViewById(R.id.action_ok);
                    action_ok.setOnClickListener(new View.OnClickListener() {
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

        sourceImage = new File(Environment.getExternalStorageDirectory() + "/Pictures/FOSS_ImageEditor/" + fileName);

        seekBar = findViewById(R.id.seekBar);
        assert seekBar != null;
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setVisibility(View.INVISIBLE);

        mGPUImageView = findViewById(R.id.gpuImage);
        assert mGPUImageView != null;
        mGPUImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
            uriSource = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            sourceImage = new File(Objects.requireNonNull(FileUtil.getPath(this, uriSource)));
            handleImage();
        } else if ("android.intent.action.EDIT".equals(action) && type != null && type.startsWith("image/")) {
            uriSource = intent.getData();
            sourceImage = new File(Objects.requireNonNull(FileUtil.getPath(this, uriSource)));
            handleImage();
        }
    }

    private void switchFilterTo(final GPUImageFilter filter) {
        if (mFilter == null
                || (filter != null && !mFilter.getClass().equals(filter.getClass()))) {
            mFilter = filter;
            mGPUImageView.setFilter(mFilter);
            mFilterAdjuster = new GPUImageFilterTools.FilterAdjuster(mFilter);

            if (mFilterAdjuster.canAdjust()) {
                seekBar.setVisibility(View.VISIBLE);
                seekBar.setProgress(50);
                mGPUImageView.requestRender();
            } else {
                seekBar.setVisibility(View.INVISIBLE);
                mGPUImageView.requestRender();
            }
        }
    }

    private void saveImage() {
        String folder = "FOSS_ImageEditor";
        mGPUImageView.saveToPictures(folder, fileName, w, h, this);
        editedImage = new File(Environment.getExternalStorageDirectory() + "/Pictures/FOSS_ImageEditor/" + fileName);
        mGPUImageView.setImage(editedImage);
    }

    @Override
    public void onPictureSaved(final Uri uri) {
        mGPUImageView.setFilter(new GPUImageFilter());
        mGPUImageView.requestRender();
        seekBar.setVisibility(View.INVISIBLE);
    }

    private void handleImage() {

        if (uriSource != null) {
            try {
                String filePath = FileUtil.getPath(this, uriSource);
                fileName = filePath.substring(Objects.requireNonNull(filePath).lastIndexOf("/")+1);

                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(FileUtil.getPath(this, uriSource), o);
                h = o.outHeight;
                w = o.outWidth;
                mGPUImageView.setImage(uriSource);
                mGPUImageView.requestRender();
            } catch (Exception e) {
                e.printStackTrace();
                MsgUtil.show(MainActivity.this, R.string.dialog_load_not);
            }
        } else {
            MsgUtil.show(MainActivity.this, R.string.dialog_load_not);
        }
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if(requestCode == TextOnImage.TEXT_ON_IMAGE_REQUEST_CODE) {
            if(resultCode == TextOnImage.TEXT_ON_IMAGE_RESULT_OK_CODE) {
                uriSource = Uri.parse(data.getStringExtra(TextOnImage.IMAGE_OUT_URI));

                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(FileUtil.getPath(this, uriSource), o);
                h = o.outHeight;
                w = o.outWidth;

                File outputFile = new File(uriSource.getPath());
                FileUtil.copyFile(MainActivity.this, outputFile, editedImage);
                //noinspection ResultOfMethodCallIgnored
                outputFile.delete();
                mGPUImageView.setImage(editedImage);
                mGPUImageView.requestRender();

            } else if(resultCode == TextOnImage.TEXT_ON_IMAGE_RESULT_FAILED_CODE) {
                String errorInfo = data.getStringExtra(TextOnImage.IMAGE_OUT_ERROR);
                Log.d("MainActivity", "onActivityResult: "+errorInfo);
            }
        }

        switch (requestCode) {
            case REQUEST_CODE_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    uriSource = data.getData();
                    sourceImage = new File(Objects.requireNonNull(uriSource).getPath());
                    handleImage();
                }

            case UCrop.REQUEST_CROP:
                if (resultCode == RESULT_OK) {
                    uriSource = UCrop.getOutput(data);
                    handleImage();
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    MsgUtil.show(MainActivity.this, R.string.dialog_save_not);
                }

            default:
                break;
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_edit) {
            GPUImageFilterTools.showDialog(MainActivity.this, new GPUImageFilterTools.OnGpuImageFilterChosenListener() {

                @Override
                public void onGpuImageFilterChosenListener(final GPUImageFilter filter) {
                    switchFilterTo(filter);
                    mGPUImageView.requestRender();
                }
            });
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

            saveImage();

            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
            View dialogView = View.inflate(MainActivity.this, R.layout.dialog_add_text, null);
            final EditText editText = dialogView.findViewById(R.id.dialog_edit);

            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String text = editText.getText().toString();
                    bottomSheetDialog.cancel();
                    Uri uri = Uri.fromFile(editedImage);
                    Intent intent = new Intent(MainActivity.this,TextOnImage.class);
                    Bundle bundle = new Bundle();
                    bundle.putString(TextOnImage.IMAGE_IN_URI,uri.toString());          //image uri
                    bundle.putString(TextOnImage.TEXT_COLOR,"#ff6e40");                 //initial color of the text
                    bundle.putFloat(TextOnImage.TEXT_FONT_SIZE,20.0f);                  //initial text size
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
            return true;
        }

        if (id == R.id.action_crop) {
            saveImage();
            Uri uri = Uri.fromFile(editedImage);
            UCrop.Options options = new UCrop.Options();
                    options.setToolbarTitle(getResources().getString(R.string.action_crop));
                    options.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
                    options.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));
                    options.setActiveWidgetColor(getResources().getColor(R.color.colorAccent));
                    options.setFreeStyleCropEnabled(true);
            UCrop.of(uri, uri).withOptions(options).start(MainActivity.this);
            return true;
        }

        if (id == R.id.action_share) {
            saveImage();
            Uri uri = Uri.fromFile(editedImage);
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("image/*");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.dialog_share)));
            return true;
        }

        if (id == R.id.action_save) {
            saveImage();
            return true;
        }

        if (id == R.id.action_overwrite) {
            saveImage();
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
            View dialogView = View.inflate(MainActivity.this, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.dialog_save);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bottomSheetDialog.cancel();
                    FileUtil.copyFile(MainActivity.this, editedImage, sourceImage);
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
            return true;
        }

        if (id == R.id.action_about_license) {
            MsgUtil.showAboutDialog(this, getResources().getString(R.string.about_license_title),
                    getResources().getString(R.string.about_license_dialog));
            return true;
        }

        if (id == R.id.action_about_contribute) {
            MsgUtil.showAboutDialog(this, getResources().getString(R.string.action_about_contribute),
                    getResources().getString(R.string.about_cont_dialog));

            return true;
        }

        if (id == R.id.action_about_help) {
            MsgUtil.showAboutDialog(this, getResources().getString(R.string.action_about_help),
                    getResources().getString(R.string.about_help_dialog));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
