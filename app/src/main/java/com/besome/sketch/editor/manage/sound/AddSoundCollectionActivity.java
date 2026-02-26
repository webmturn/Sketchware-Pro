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
import pro.sketchware.core.SoundCollectionManager;
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

    public MediaPlayer mediaPlayer;
    public TimerTask progressTask;
    public ResourceNameValidator nameValidator;
    public ArrayList<ProjectResourceBean> existingSounds;
    public String scId;
    public boolean isEditing;
    public Timer progressTimer = new Timer();
    public Uri pickedSoundUri;
    public boolean soundLoaded;
    public ProjectResourceBean editTarget;

    private ManageSoundAddBinding binding;

    @Override
    public void finish() {
        if (progressTimer != null) progressTimer.cancel();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.finish();
    }

    private ArrayList<String> getResourceNames() {
        ArrayList<String> resourceNames = new ArrayList<>();
        resourceNames.add("app_icon");
        for (ProjectResourceBean projectResourceBean : existingSounds) {
            resourceNames.add(projectResourceBean.resName);
        }
        return resourceNames;
    }

    private void pausePlayback() {
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) return;
        progressTimer.cancel();
        mediaPlayer.pause();
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
                        pickedSoundUri = data;
                        if (UriPathResolver.resolve(this, pickedSoundUri) == null) {
                            return;
                        }
                        loadSoundFromUri(data);
                    }
                });
        setDialogTitle(Helper.getResString(R.string.design_manager_sound_title_add_sound));
        binding = ManageSoundAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setOkButtonText(Helper.getResString(R.string.common_word_save));
        setCancelButtonText(Helper.getResString(R.string.common_word_cancel));
        Intent intent = getIntent();
        existingSounds = intent.getParcelableArrayListExtra("sounds");
        scId = intent.getStringExtra("sc_id");
        editTarget = intent.getParcelableExtra("edit_target");
        if (editTarget != null) {
            isEditing = true;
        }
        binding.layoutControl.setVisibility(View.GONE);
        binding.tiInput.setHint(R.string.design_manager_sound_hint_enter_sound_name);
        nameValidator = new ResourceNameValidator(this, binding.tiInput, BlockConstants.RESERVED_KEYWORDS, getResourceNames());
        binding.play.setEnabled(false);
        binding.play.setOnClickListener(this);
        binding.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer == null || !mediaPlayer.isPlaying() || progressTimer == null) return;
                progressTimer.cancel();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress() * 100);
                    if (mediaPlayer.isPlaying()) {
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
            setDialogTitle(Helper.getResString(R.string.design_manager_sound_title_edit_sound_name));
            nameValidator = new ResourceNameValidator(this, binding.tiInput, BlockConstants.RESERVED_KEYWORDS, getResourceNames(), editTarget.resName);
            binding.edInput.setText(editTarget.resName);
            loadSoundFromPath(getResourceFilePath(editTarget));
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
        soundPickerLauncher.launch(Intent.createChooser(intent, Helper.getResString(R.string.common_word_choose)));
    }

    private void togglePlayback() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            pausePlayback();
            return;
        }
        mediaPlayer.start();
        startProgressTimer();
        binding.play.setImageResource(R.drawable.ic_pause_circle_outline_black_36dp);
    }

    private void saveSound() {
        if (validateAndCheckFile(nameValidator)) {
            if (!isEditing) {
                String obj = Helper.getText(binding.edInput);
                String soundFilePath = UriPathResolver.resolve(this, pickedSoundUri);
                if (soundFilePath == null) {
                    return;
                }
                ProjectResourceBean projectResourceBean = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, obj, soundFilePath);
                projectResourceBean.savedPos = 1;
                projectResourceBean.isNew = true;
                try {
                    SoundCollectionManager.getInstance().addResource(scId, projectResourceBean);
                    SketchToast.toast(this, Helper.getResString(R.string.design_manager_message_add_complete), 1).show();
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
                SoundCollectionManager.getInstance().renameResource(editTarget, Helper.getText(binding.edInput), true);
                SketchToast.toast(this, Helper.getResString(R.string.design_manager_message_edit_complete), 1).show();
            }
            finish();
        }
    }

    private void startProgressTimer() {
        progressTimer = new Timer();
        progressTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (mediaPlayer == null) {
                        progressTimer.cancel();
                        return;
                    }
                    int currentPosition = mediaPlayer.getCurrentPosition() / 100;
                    int totalSeconds = currentPosition / 10;
                    binding.currentTime.setText(String.format("%02d : %02d", totalSeconds / 60, totalSeconds % 60));
                    binding.seek.setProgress(currentPosition);
                });
            }
        };
        progressTimer.schedule(progressTask, 100L, 100L);
    }

    private String getResourceFilePath(ProjectResourceBean projectResourceBean) {
        return SketchwarePaths.getCollectionPath() + File.separator + "sound" + File.separator + "data" + File.separator + editTarget.resFullName;
    }

    private void loadSoundFromPath(String soundPath) {
        try {
            if (mediaPlayer != null) {
                if (progressTask != null) {
                    progressTask.cancel();
                }
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(AudioMetadata.MEDIA_PLAYER_AUDIO_ATTRIBUTES);
            mediaPlayer.setOnPreparedListener(mp -> {
                binding.play.setImageResource(R.drawable.ic_pause_circle_outline_black_36dp);
                binding.play.setEnabled(true);
                binding.seek.setMax(mp.getDuration() / 100);
                binding.seek.setProgress(0);
                int durationSeconds = mp.getDuration() / 1000;
                binding.fileLength.setText(String.format("%02d : %02d", durationSeconds / 60, durationSeconds % 60));
                mediaPlayer.start();
                startProgressTimer();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                progressTimer.cancel();
                binding.play.setImageResource(R.drawable.ic_play_circle_outline_black_36dp);
                binding.seek.setProgress(0);
                binding.currentTime.setText("00 : 00");
            });
            mediaPlayer.setDataSource(this, Uri.parse(soundPath));
            mediaPlayer.prepare();
            soundLoaded = true;
            loadAlbumArt(soundPath, binding.imgAlbum);
            binding.layoutControl.setVisibility(View.VISIBLE);
            binding.layoutGuide.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e("AddSoundCollectionActivity", e.getMessage(), e);
        }
    }

    private void loadSoundFromUri(Uri uri) {
        String resolvedPath = UriPathResolver.resolve(this, uri);
        pickedSoundUri = uri;
        try {
            if (mediaPlayer != null) {
                if (progressTask != null) {
                    progressTask.cancel();
                }
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(AudioMetadata.MEDIA_PLAYER_AUDIO_ATTRIBUTES);
            mediaPlayer.setOnPreparedListener(mp -> {
                if (resolvedPath == null) return;
                binding.play.setImageResource(R.drawable.ic_pause_circle_outline_black_36dp);
                binding.play.setEnabled(true);
                binding.seek.setMax(mp.getDuration() / 100);
                binding.seek.setProgress(0);
                int durationSeconds = mp.getDuration() / 1000;
                binding.fileLength.setText(String.format("%02d : %02d", durationSeconds / 60, durationSeconds % 60));
                binding.fileName.setText(resolvedPath.substring(resolvedPath.lastIndexOf("/") + 1));
                mp.start();
                startProgressTimer();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                progressTimer.cancel();
                binding.play.setImageResource(R.drawable.ic_play_circle_outline_black_36dp);
                binding.seek.setProgress(0);
                binding.currentTime.setText("00 : 00");
            });
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepare();
            soundLoaded = true;
            loadAlbumArt(UriPathResolver.resolve(this, pickedSoundUri), binding.imgAlbum);
            binding.layoutControl.setVisibility(View.VISIBLE);
            binding.layoutGuide.setVisibility(View.GONE);
            try {
                if (binding.edInput.getText() == null || binding.edInput.getText().length() <= 0) {
                    int lastIndexOf = resolvedPath.lastIndexOf("/");
                    int lastIndexOf2 = resolvedPath.lastIndexOf(".");
                    if (lastIndexOf2 <= 0) {
                        lastIndexOf2 = resolvedPath.length();
                    }
                    binding.edInput.setText(resolvedPath.substring(lastIndexOf + 1, lastIndexOf2));
                }
            } catch (Exception e) {
                Log.w("AddSoundCollectionActivity", "Failed to extract filename for input field", e);
            }
        } catch (Exception e) {
            soundLoaded = false;
            binding.layoutControl.setVisibility(View.GONE);
            binding.layoutGuide.setVisibility(View.VISIBLE);
            Log.e("AddSoundCollectionActivity", e.getMessage(), e);
        }
    }

    private void loadAlbumArt(String audioPath, ImageView imageView) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(audioPath);
            if (mediaMetadataRetriever.getEmbeddedPicture() != null) {
                Glide.with(this).load(mediaMetadataRetriever.getEmbeddedPicture()).centerCrop().into(imageView);
            } else {
                imageView.setImageResource(R.drawable.default_album_art_200dp);
            }
        } catch (Exception e) {
            Log.w("AddSoundCollectionActivity", "Failed to extract album art from audio file: " + audioPath, e);
            imageView.setImageResource(R.drawable.default_album_art_200dp);
        } finally {
            try {
                mediaMetadataRetriever.release();
            } catch (Exception ignored) {
                Log.w("AddSoundCollectionActivity", "Failed to release MediaMetadataRetriever", ignored);
            }
        }
    }

    public boolean validateAndCheckFile(ResourceNameValidator wb) {
        if (wb.isValid()) {
            if ((!soundLoaded || pickedSoundUri == null) && !isEditing) {
                binding.selectFile.startAnimation(AnimationUtils.loadAnimation(this, R.anim.ani_1));
                return false;
            }
            return true;
        }
        return false;
    }
}

