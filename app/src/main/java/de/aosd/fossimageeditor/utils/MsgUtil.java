package de.aosd.fossimageeditor.utils;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.BottomSheetDialog;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import de.aosd.fossimageeditor.R;


public class MsgUtil {

    public static void show(Context context, int stringResId) {
        show(context, context.getString(stringResId));
    }

    private static void show(Context context, String text) {

        Activity activity = (Activity) context;

        LayoutInflater inflater = activity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_bottom, (ViewGroup) activity.findViewById(R.id.dialog_toast));

        TextView dialog_text = layout.findViewById(R.id.dialog_text);
        dialog_text.setText(text);

        Toast toast = new Toast(activity.getApplicationContext());
        toast.setGravity(Gravity.BOTTOM|Gravity.FILL_HORIZONTAL, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    public static void showAboutDialog(Activity activity, String title, String text) {

        final BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View dialogView = View.inflate(activity, R.layout.dialog_about, null);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_about_title);
        dialog_title.setText(title);

        TextView dialog_text = dialogView.findViewById(R.id.dialog_about_text);
        dialog_text.setText(textSpannable(text));
        dialog_text.setMovementMethod(LinkMovementMethod.getInstance());

        dialog.setContentView(dialogView);
        dialog.show();
    }

    public static SpannableString textSpannable (String text) {
        SpannableString s;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            s = new SpannableString(Html.fromHtml(text,Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            s = new SpannableString(Html.fromHtml(text));
        }

        Linkify.addLinks(s, Linkify.WEB_URLS);
        return s;
    }
}
