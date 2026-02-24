package com.besome.sketch.editor.manage.sound;


import android.util.Log;
import androidx.activity.OnBackPressedCallback;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import mod.jbk.util.AudioMetadata;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.besome.sketch.lib.ui.EasyDeleteEditText;
import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;

import pro.sketchware.core.XmlNameValidator;
import pro.sketchware.core.SketchToast;
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.BlockConstants;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

public class ManageSoundImportActivity extends BaseAppCompatActivity implements View.OnClickListener {
    private XmlNameValidator nameValidator;
    private boolean mediaPlayerIsPrepared;
    private ImageView img_album;
    private ImageView img_play;
    private TextView tv_currentnum;
    private EditText ed_input_edittext;
    private CheckBox chk_samename;
    private ItemAdapter adapter;
    private ArrayList<ProjectResourceBean> projectSounds;
    private ArrayList<ProjectResourceBean> selectedCollections;
    private int selectedItem = 0;
    private MediaPlayer mediaPlayer;

    private ArrayList<String> getReservedSelectedCollectionNames() {
        ArrayList<String> names = new ArrayList<>();
        names.add("app_icon");
        for (ProjectResourceBean selectedCollection : selectedCollections) {
            names.add(selectedCollection.resName);
        }
        return names;
    }

    private ArrayList<String> getReservedProjectSoundNames() {
        ArrayList<String> names = new ArrayList<>();
        names.add("app_icon");
        for (ProjectResourceBean projectSound : projectSounds) {
            names.add(projectSound.resName);
        }
        return names;
    }

    private boolean doDuplicateNamesExist() {
        ArrayList<String> duplicateNames = new ArrayList<>();
        for (ProjectResourceBean selectedCollection : selectedCollections) {
            if (selectedCollection.isDuplicateCollection) {
                duplicateNames.add(selectedCollection.resName);
            }
        }
        if (!duplicateNames.isEmpty()) {
            String message = getString(R.string.common_message_name_unavailable);
            String names = "";
            for (String str2 : duplicateNames) {
                if (!names.isEmpty()) {
                    names = names + ", ";
                }
                names = names + str2;
            }
            SketchToast.toast(getApplicationContext(), message + "\n[" + names + "]", SketchToast.TOAST_WARNING).show();
            return true;
        }
        return false;
    }

    private boolean isNameValid() {
        return nameValidator.isValid();
    }

    @Override
    public void onClick(View v) {
        if (!UIHelper.isClickThrottled()) {
            int id = v.getId();
            if (id == R.id.btn_decide) {
                String name = Helper.getText(ed_input_edittext);
                if (isNameValid()) {
                    if (chk_samename.isChecked()) {
                        int i = 0;
                        while (i < selectedCollections.size()) {
                            ProjectResourceBean sound = selectedCollections.get(i);
                            sound.resName = name + "_" + (++i);
                            sound.isDuplicateCollection = false;
                        }
                    } else {
                        ProjectResourceBean sound = selectedCollections.get(selectedItem);
                        sound.resName = name;
                        sound.isDuplicateCollection = false;
                    }
                    nameValidator.setJavaNames(getReservedSelectedCollectionNames());
                    adapter.notifyDataSetChanged();
                } else {
                    ed_input_edittext.setText(selectedCollections.get(selectedItem).resName);
                }
            } else if (id == R.id.img_backbtn) {
                getOnBackPressedDispatcher().onBackPressed();
            } else if (id == R.id.img_play) {
                if (mediaPlayerIsPrepared) {
                    if (mediaPlayer.isPlaying()) {
                        pausePlayback();
                    } else {
                        mediaPlayer.start();
                        img_play.setImageResource(R.drawable.ic_pause_circle_outline_black_36dp);
                    }
                }
            } else if (id == R.id.tv_sendbtn) {
                if (!doDuplicateNamesExist()) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }
                    Intent intent = new Intent();
                    intent.putParcelableArrayListExtra("results", selectedCollections);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setResult(RESULT_OK);
                finish();
            }
        });
        if (!super.isStoragePermissionGranted()) {
            finish();
        }
        setContentView(R.layout.manage_sound_import);
        ImageView img_backbtn = findViewById(R.id.img_backbtn);
        img_backbtn.setOnClickListener(this);
        tv_currentnum = findViewById(R.id.tv_currentnum);
        TextView tv_totalnum = findViewById(R.id.tv_totalnum);
        TextView tv_sendbtn = findViewById(R.id.tv_sendbtn);
        tv_sendbtn.setText(getString(R.string.common_word_import).toUpperCase());
        tv_sendbtn.setOnClickListener(this);
        TextView tv_samename = findViewById(R.id.tv_samename);
        tv_samename.setText(R.string.design_manager_sound_title_apply_same_naming);
        adapter = new ItemAdapter();
        RecyclerView recycler_list = findViewById(R.id.recycler_list);
        recycler_list.setAdapter(adapter);
        recycler_list.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.HORIZONTAL, false));
        projectSounds = getIntent().getParcelableArrayListExtra("project_sounds");
        selectedCollections = getIntent().getParcelableArrayListExtra("selected_collections");
        tv_currentnum.setText(String.valueOf(1));
        tv_totalnum.setText(String.valueOf(selectedCollections.size()));
        EasyDeleteEditText ed_input = findViewById(R.id.ed_input);
        ed_input_edittext = ed_input.getEditText();
        ed_input_edittext.setText(selectedCollections.get(0).resName);
        ed_input_edittext.setPrivateImeOptions("defaultInputmode=english;");
        ed_input.setHint(getString(R.string.design_manager_sound_hint_enter_sound_name));
        nameValidator = new XmlNameValidator(getApplicationContext(), ed_input.getTextInputLayout(), BlockConstants.RESERVED_KEYWORDS, getReservedProjectSoundNames(), getReservedSelectedCollectionNames());
        chk_samename = findViewById(R.id.chk_samename);
        chk_samename.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                nameValidator.setCurrentName(null);
                nameValidator.setBatchCount(selectedCollections.size());
            } else {
                nameValidator.setCurrentName(selectedCollections.get(selectedItem).resName);
                nameValidator.setBatchCount(1);
            }
        });
        Button btn_decide = findViewById(R.id.btn_decide);
        btn_decide.setText(getString(R.string.design_manager_change_name_button));
        btn_decide.setOnClickListener(this);
        img_album = findViewById(R.id.img_album);
        img_play = findViewById(R.id.img_play);
        img_play.setOnClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pausePlayback();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        initializeLogic();
        showPreview(0);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!super.isStoragePermissionGranted()) {
            finish();
        }
    }

    private void initializeLogic() {
        ArrayList<ProjectResourceBean> duplicateCollections = new ArrayList<>();
        ArrayList<ProjectResourceBean> notDuplicateCollections = new ArrayList<>();
        for (ProjectResourceBean selectedCollection : selectedCollections) {
            if (isNameInUseByProjectSound(selectedCollection.resName)) {
                selectedCollection.isDuplicateCollection = true;
                duplicateCollections.add(selectedCollection);
            } else {
                selectedCollection.isDuplicateCollection = false;
                notDuplicateCollections.add(selectedCollection);
            }
        }
        if (!duplicateCollections.isEmpty()) {
            SketchToast.warning(getApplicationContext(), getString(R.string.design_manager_message_collection_name_conflict), SketchToast.TOAST_WARNING).show();
        } else {
            SketchToast.toast(getApplicationContext(), getString(R.string.design_manager_message_collection_name_no_conflict), SketchToast.TOAST_NORMAL).show();
        }
        selectedCollections = new ArrayList<>();
        selectedCollections.addAll(duplicateCollections);
        selectedCollections.addAll(notDuplicateCollections);
    }

    private void pausePlayback() {
        if (mediaPlayer != null && mediaPlayerIsPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            img_play.setImageResource(R.drawable.ic_play_circle_outline_black_36dp);
        }
    }

    private void showPreview(int i) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(AudioMetadata.MEDIA_PLAYER_AUDIO_ATTRIBUTES);
        mediaPlayer.setOnPreparedListener(mp -> {
            mediaPlayerIsPrepared = true;
            img_play.setImageResource(R.drawable.ic_play_circle_outline_black_36dp);
        });
        mediaPlayer.setOnCompletionListener(mp -> mediaPlayerIsPrepared = false);
        try {
            String filePath = selectedCollections.get(i).resFullName;
            loadSoundEmbeddedPicture(filePath, img_album, -1);
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
        } catch (Exception e) {
            Log.e("ManageSoundImportActivity", e.getMessage(), e);
        }
    }

    private boolean isNameInUseByProjectSound(String str) {
        for (ProjectResourceBean projectSound : projectSounds) {
            if (projectSound.resName.equals(str)) {
                return true;
            }
        }
        return false;
    }

    private void loadSoundEmbeddedPicture(String filePath, ImageView target, int position) throws IOException {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(filePath);
            if (mediaMetadataRetriever.getEmbeddedPicture() != null) {
                Glide.with(getApplicationContext()).load(mediaMetadataRetriever.getEmbeddedPicture()).centerCrop().into(target);
            } else {
                target.setImageResource(R.drawable.default_album_art_200dp);
                if (position != -1 && selectedItem != position) {
                    target.setBackgroundResource(R.drawable.bg_outline_album);
                }
            }
        } catch (IllegalArgumentException unused) {
            target.setImageResource(R.drawable.default_album_art_200dp);
            if (position != -1 && selectedItem != position) {
                target.setBackgroundResource(R.drawable.bg_outline_album);
            }
        }
        mediaMetadataRetriever.release();
    }

    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
        public ItemAdapter() {
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
            ProjectResourceBean sound = selectedCollections.get(position);
            if (sound.isDuplicateCollection) {
                viewHolder.img_conflict.setImageResource(R.drawable.ic_cancel_48dp);
            } else {
                viewHolder.img_conflict.setImageResource(R.drawable.ic_ok_48dp);
            }
            if (position == selectedItem) {
                viewHolder.img.setBackgroundResource(R.drawable.bg_outline_dark_yellow);
            } else {
                viewHolder.img.setBackgroundColor(Color.WHITE);
            }
            try {
                loadSoundEmbeddedPicture(sound.resFullName, viewHolder.img, position);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            viewHolder.tv_name.setText(selectedCollections.get(position).resName);
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_import_list_item, parent, false));
        }

        @Override
        public int getItemCount() {
            return selectedCollections.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            public LinearLayout layout_item;
            public ImageView img_conflict;
            public ImageView img;
            public TextView tv_name;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                layout_item = itemView.findViewById(R.id.layout_item);
                img_conflict = itemView.findViewById(R.id.img_conflict);
                img = itemView.findViewById(R.id.img);
                tv_name = itemView.findViewById(R.id.tv_name);
                img.setOnClickListener(v -> {
                    if (!UIHelper.isClickThrottled()) {
                        pausePlayback();
                        selectedItem = getLayoutPosition();
                        showPreview(selectedItem);
                        tv_currentnum.setText(String.valueOf(selectedItem + 1));
                        ed_input_edittext.setText(selectedCollections.get(selectedItem).resName);
                        if (chk_samename.isChecked()) {
                            nameValidator.setCurrentName(null);
                            nameValidator.setBatchCount(selectedCollections.size());
                        } else {
                            nameValidator.setCurrentName(selectedCollections.get(selectedItem).resName);
                            nameValidator.setBatchCount(1);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }
}
