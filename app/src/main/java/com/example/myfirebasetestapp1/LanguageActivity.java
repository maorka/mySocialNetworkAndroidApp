package com.example.myfirebasetestapp1;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.PopupMenu;
import androidx.appcompat.app.AppCompatActivity;

public class LanguageActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        String lang = LocaleHelper.getLanguage(newBase);
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang));
    }

    protected void showLanguageMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, 1, 0, "English");
        popupMenu.getMenu().add(0, 2, 1, "עברית");

        popupMenu.setOnMenuItemClickListener(item -> {
            String lang = "en";
            if (item.getItemId() == 2) {
                lang = "iw";
            }
            
            if (!lang.equals(LocaleHelper.getLanguage(this))) {
                LocaleHelper.setLocale(this, lang);
                // Restart activity to apply changes
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
            return true;
        });
        popupMenu.show();
    }
}
