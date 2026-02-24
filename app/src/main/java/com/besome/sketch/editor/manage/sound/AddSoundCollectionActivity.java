package com.besome.sketch.editor.manage.sound;


import android.util.Log;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.media.MediaMetadataRetriever;
import mod.jbk.util.AudioMetadata;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.lib.base.BaseDialogActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import pro.sketchware.core.UriPathResolver;
import pro.sketchware.core.FontCollectionManager;
import pro.sketchware.core.ResourceNameValidator;
import pro.sketchware.core.SketchToast;
import pro.sketchware.core.BlockConstants;
import pro.sketchware.core.SketchwarePaths;
import pro.sketchware.core.CompileException;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageSoundAddBinding;

public class AddSoundCollectionActivity extends BaseDialogActivity implements View.OnClickListener {

    private ActivityResultLauncher<Intent> soundPickerLauncher;

    public MediaPlayer G;
    public TimerTask I;
    public ResourceNameValidator M;
    public ArrayList<ProjectResourceBean> N;
    public String scId;
    public boolean isEditing;
    public Timer H = new Timer();
    public Uri K;
    public boolean L;
    public ProjectResourceBean O;

    private ManageSoundAddBinding binding;

    @Override
    public void finish() {
        if (H != null) H.cancel();
        if (G != null) {
            if (G.isPlaying()) {
                G.stop();
            }
            G.release();
            G = null;
        }
        super.finish();
    }

    private ArrayList<String> getResourceNames() {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("app_icon");
        for (ProjectResourceBean projectResourceBean : N) {
            arrayList.add(projectResourceBean.resName);
        }
        return arrayList;
    }

    private void pausePlayback() {
        if (G == null || !G.isPlaying()) return;
        H.cancel();
        G.pause();
        binding.play.setImageResource(R.drawable.ic_play_circle_outline_black_36dp);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.common_dialog_cancel_button) {
            finish();
            return;
        }
        if (id == R.id.common_dialog_ok_button) {
            saveSound();
            return;
        }
        if (id == binding.play.getId()) {
            togglePlayback();
            return;
        }
        if (id == binding.selectFile.getId()) {
            if (!isEditing) {
                binding.selectFile.setEnabled(false);
                pickSoundFile();
            }
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        soundPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    MaterialCardView relativeLayout;
                    Uri data;
                    if ((relativeLayout = binding.selectFile) != null) {
                        relativeLayout.setEnabled(true);
                        if (result.getResultCode() != -1 || result.getData() == null
                                || (data = result.getData().getData()) == null) {
                            return;
                        }
                        K = data;
                        if (UriPathResolver.resolve(this, K) == null) {
                            return;
                        }
                        a(data);
                    }
                });
        setDialogTitle(getString(R.string.design_manager_sound_title_add_sound));
        binding = ManageSoundAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setOkButtonText(getString(R.string.common_word_save));
        setCancelButtonText(getString(R.string.common_word_cancel));
        Intent intent = getIntent();
        N = intent.getParcelableArrayListExtra("sounds");
        scId = intent.getStringExtra("sc_id");
        O = intent.getParcelableExtra("edit_target");
        if (O != null) {
            isEditing = true;
        }
        binding.layoutControl.setVisibility(View.GONE);
        binding.tiInput.setHint(R.string.design_manager_sound_hint_enter_sound_name);
        M = new ResourceNameValidator(this, binding.tiInput, BlockConstants.RESERVED_KEYWORDS, getResourceNames());
        binding.play.setEnabled(false);
        binding.play.setOnClickListener(this);
        binding.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (G == null || !G.isPlaying() || H == null) return;
                H.cancel();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (G != null) {
                    G.seekTo(seekBar.getProgress() * 100);
                    if (G.isPlaying()) {
                        startProgressTimer();
                        return;
                    }
                    return;
                }
                seekBar.setProgress(0);
            }
        });
        binding.selectFile.setOnClickListener(this);
        dialogOkButton.setOnClickListener(this);
        dialogCancelButton.setOnClickListener(this);
        if (isEditing) {
            setDialogTitle(getString(R.string.design_manager_sound_title_edit_sound_name));
            M = new ResourceNameValidator(this, binding.tiInput, BlockConstants.RESERVED_KEYWORDS, getResourceNames(), O.resName);
            binding.edInput.setText(O.resName);
            loadSoundFromPath(a(O));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pausePlayback();
    }

    private void pickSoundFile() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT", MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("audio/*");
        soundPickerLauncher.launch(Intent.createChooser(intent, getString(R.string.common_word_choose)));
    }

    private void togglePlayback() {
        if (G.isPlaying()) {
            pausePlayback();
            return;
        }
        G.start();
        startProgressTimer();
        binding.play.setImageResource(R.drawable.ic_pause_circle_outline_black_36dp);
    }

    private void saveSound() {
        if (validateAndCheckFile(M)) {
            if (!isEditing) {
                String obj = Helper.getText(binding.edInput);
                String a = UriPathResolver.resolve(this, K);
                if (a == null) {
                    return;
                }
                ProjectResourceBean projectResourceBean = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, obj, a);
                projectResourceBean.savedPos = 1;
                projectResourceBean.isNew = true;
                try {
                    FontCollectionManager.getInstance().addResource(scId, projectResourceBean);
                    SketchToast.toast(this, getApplicationContext().getString(R.string.design_manager_message_add_complete), 1).show();
                } catch (Exception e) {
                    // the bytecode's lying
                    // noinspection ConstantValue
                    if (e instanceof CompileException) {
                        var messageId = switch (e.getMessage()) {
                            case "duplicate_name" -> R.string.collection_duplicated_name;
                            case "file_no_exist" -> R.string.collection_no_exist_file;
                            case "fail_to_copy" -> R.string.collection_failed_to_copy;
                            default -> 0;
                        };
                        if (messageId != 0) {
                            SketchToast.toast(this, getApplicationContext().getString(messageId), SketchToast.TOAST_WARNING).show();
                        }
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                FontCollectionManager.getInstance().renameResource(O, Helper.getText(binding.edInput), true);
                SketchToast.toast(this, getApplicationContext().getString(R.string.design_manager_message_edit_complete), 1).show();
            }
            finish();
        }
    }

    private void startProgressTimer() {
        H = new Timer();
        I = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (G == null) {
                        H.cancel();
                        return;
                    }
                    int currentPosition = G.getCurrentPosition() / 100;
                    binding.currentTime.setText(String.format("%02d : %02d", currentPosition / 60, currentPosition % 60));
                    binding.seek.setProgress(currentPosition / 100);
                });
            }
        };
        H.schedule(I, 100L, 100L);
    }

    private String a(ProjectResourceBean projectResourceBean) {
        return SketchwarePaths.getCollectionPath() + File.separator + "sound" + File.separator + "data" + File.separator + O.resFullName;
    }

    private void loadSoundFromPath(String str) {
        try {
            if (G != null) {
                if (I != null) {
                    I.cancel();
                }
                if (G.isPlaying()) {
                    G.stop();
                }
                G.release();
            }
            G = new MediaPlayer();
            G.setAudioAttributes(AudioMetadata.MEDIA_PLAYER_AUDIO_ATTRIBUTES);
            G.setOnPreparedListener(mediaPlayer -> {
                binding.play.setImageResource(R.drawable.ic_pause_circle_outline_black_36dp);
                binding.play.setEnabled(true);
                binding.seek.setMax(mediaPlayer.getDuration() / 100);
                binding.seek.setProgress(0);
                int duration = mediaPlayer.getDuration() / 100;
                binding.fileLength.setText(String.format("%02d : %02d", duration / 60, duration % 60));
                G.start();
                startProgressTimer();
            });
            G.setOnCompletionListener(mediaPlayer -> {
                H.cancel();
                binding.play.setImageResource(R.drawable.ic_play_circle_outline_black_36dp);
                binding.seek.setProgress(0);
                binding.currentTime.setText("00 : 00");
            });
            G.setDataSource(this, Uri.parse(str));
            G.prepare();
            L = true;
            a(str, binding.imgAlbum);
            binding.layoutControl.setVisibility(View.VISIBLE);
            binding.layoutGuide.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e("AddSoundCollectionActivity", e.getMessage(), e);
        }
    }

    private void a(Uri uri) {
        String a = UriPathResolver.resolve(this, uri);
        K = uri;
        try {
            if (G != null) {
                if (I != null) {
                    I.cancel();
                }
                if (G.isPlaying()) {
                    G.stop();
                }
                G.release();
            }
            G = new MediaPlayer();
            G.setAudioAttributes(AudioMetadata.MEDIA_PLAYER_AUDIO_ATTRIBUTES);
            G.setOnPreparedListener(mediaPlayer -> {
                if (a == null) return;
                binding.play.setImageResource(R.drawable.ic_pause_circle_outline_black_36dp);
                binding.play.setEnabled(true);
                binding.seek.setMax(mediaPlayer.getDuration() / 100);
                binding.seek.setProgress(0);
                int duration = mediaPlayer.getDuration() / 100;
                binding.fileLength.setText(String.format("%02d : %02d", duration / 60, duration % 60));
                binding.fileName.setText(a.substring(a.lastIndexOf("/") + 1));
                mediaPlayer.start();
                startProgressTimer();
            });
            G.setOnCompletionListener(mediaPlayer -> {
                H.cancel();
                binding.play.setImageResource(R.drawable.ic_play_circle_outline_black_36dp);
                binding.seek.setProgress(0);
                binding.currentTime.setText("00 : 00");
            });
            G.setDataSource(this, uri);
            G.prepare();
            L = true;
            a(UriPathResolver.resolve(this, K), binding.imgAlbum);
            binding.layoutControl.setVisibility(View.VISIBLE);
            binding.layoutGuide.setVisibility(View.GONE);
            try {
                if (binding.edInput.getText() == null || binding.edInput.getText().length() <= 0) {
                    int lastIndexOf = a.lastIndexOf("/");
                    int lastIndexOf2 = a.lastIndexOf(".");
                    if (lastIndexOf2 <= 0) {
                        lastIndexOf2 = a.length();
                    }
                    binding.edInput.setText(a.substring(lastIndexOf + 1, lastIndexOf2));
                }
            } catch (Exception e) {
                Log.w("AddSoundCollectionActivity", "Failed to extract filename for input field", e);
            }
        } catch (Exception e) {
            L = false;
            binding.layoutControl.setVisibility(View.GONE);
            binding.layoutGuide.setVisibility(View.VISIBLE);
            Log.e("AddSoundCollectionActivity", e.getMessage(), e);
        }
    }

    private void a(String str, ImageView imageView) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(str);
            if (mediaMetadataRetriever.getEmbeddedPicture() != null) {
                Glide.with(this).load(mediaMetadataRetriever.getEmbeddedPicture()).centerCrop().into(imageView);
            } else {
                imageView.setImageResource(R.drawable.default_album_art_200dp);
            }
        } catch (Exception e) {
            Log.w("AddSoundCollectionActivity", "Failed to extract album art from audio file: " + str, e);
            imageView.setImageResource(R.drawable.default_album_art_200dp);
        } finally {
            try {
                mediaMetadataRetriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean validateAndCheckFile(ResourceNameValidator wb) {
        if (wb.isValid()) {
            if ((!L || K == null) && !isEditing) {
                binding.selectFile.startAnimation(AnimationUtils.loadAnimation(this, R.anim.ani_1));
                return false;
            }
            return true;
        }
        return false;
    }
}

