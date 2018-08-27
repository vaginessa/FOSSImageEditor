/*
 * Copyright (C) 2018 Gaukler Faun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.aosd.fossimageeditor.utils;

import android.app.Activity;
import android.net.Uri;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.widget.LinearLayout;

import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;

import de.aosd.fossimageeditor.R;

public class CropTools {

    public static void showDialog (final Activity context, final File file) {

        final Uri external = Uri.fromFile(file);

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_crop, null);

        LinearLayout crop_free = dialogView.findViewById(R.id.crop_free);
        crop_free.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity(external)
                        .setBorderCornerColor(context.getResources().getColor(R.color.colorAccent))
                        .start(context);
                bottomSheetDialog.cancel();
            }
        });
        LinearLayout crop_11 = dialogView.findViewById(R.id.crop_11);
        crop_11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity(external)
                        .setBorderCornerColor(context.getResources().getColor(R.color.colorAccent))
                        .setAspectRatio(1, 1)
                        .start(context);
                bottomSheetDialog.cancel();
            }
        });
        LinearLayout crop_43 = dialogView.findViewById(R.id.crop_43);
        crop_43.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity(external)
                        .setBorderCornerColor(context.getResources().getColor(R.color.colorAccent))
                        .setAspectRatio(4, 3)
                        .start(context);
                bottomSheetDialog.cancel();
            }
        });
        LinearLayout crop_169 = dialogView.findViewById(R.id.crop_169);
        crop_169.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity(external)
                        .setBorderCornerColor(context.getResources().getColor(R.color.colorAccent))
                        .setAspectRatio(16, 9)
                        .start(context);
                bottomSheetDialog.cancel();
            }
        });
        LinearLayout crop_34 = dialogView.findViewById(R.id.crop_34);
        crop_34.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity(external)
                        .setBorderCornerColor(context.getResources().getColor(R.color.colorAccent))
                        .setAspectRatio(3, 4)
                        .start(context);
                bottomSheetDialog.cancel();
            }
        });
        LinearLayout crop_916 = dialogView.findViewById(R.id.crop_916);
        crop_916.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity(external)
                        .setBorderCornerColor(context.getResources().getColor(R.color.colorAccent))
                        .setAspectRatio(9, 16)
                        .start(context);
                bottomSheetDialog.cancel();
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }
}
