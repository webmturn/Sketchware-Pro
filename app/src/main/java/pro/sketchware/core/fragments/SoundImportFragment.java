package pro.sketchware.core.fragments;


import mod.hey.studios.util.Helper;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.editor.manage.sound.ManageSoundActivity;
import com.besome.sketch.editor.manage.sound.ManageSoundImportActivity;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;

import mod.jbk.util.AudioMetadata;
import mod.jbk.util.SoundPlayingAdapter;
import pro.sketchware.R;
import pro.sketchware.databinding.FrManageSoundListBinding;
import pro.sketchware.databinding.ManageSoundBinding;
import pro.sketchware.databinding.ManageSoundListItemBinding;
import pro.sketchware.core.EncryptedFileUtil;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.project.SoundCollectionManager;
import pro.sketchware.core.UIHelper;

public class SoundImportFragment extends BaseFragment {
    private String sc_id;
    private ArrayList<ProjectResourceBean> sounds;
    private String dirPath = "";
    private Adapter adapter;

    private FrManageSoundListBinding binding;
    private ManageSoundBinding actBinding;

    private ActivityResultLauncher<Intent> importSoundsHandler;

    private void updateImportSoundsText() {
        int selectedSounds = (int) sounds.stream().filter(projectResourceBean -> projectResourceBean.isSelected).count();
        if (selectedSounds > 0) {
            actBinding.btnImport.setText(Helper.getResString(R.string.common_word_import_count, selectedSounds));
            actBinding.layoutBtnImport.setVisibility(View.VISIBLE);
        } else {
            actBinding.layoutBtnImport.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            sc_id = requireActivity().getIntent().getStringExtra("sc_id");
            dirPath = requireActivity().getIntent().getStringExtra("dir_path");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
            dirPath = savedInstanceState.getString("dir_path");
        }
        if ((dirPath == null || dirPath.isEmpty()) && sc_id != null && !sc_id.isEmpty()) {
            dirPath = ProjectDataManager.getResourceManager(sc_id).getSoundDirPath();
        }
        EncryptedFileUtil fileUtil = new EncryptedFileUtil();
        fileUtil.mkdirs(dirPath);
        loadProjectSounds();

        importSoundsHandler = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && isAdded()) {
                ArrayList<ProjectResourceBean> importedSounds = result.getData().getParcelableArrayListExtra("results");
                if (importedSounds != null) {
                    handleImportedSounds(importedSounds);
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        actBinding = ((ManageSoundActivity) requireActivity()).binding;
        binding = FrManageSoundListBinding.inflate(inflater, container, false);

        binding.soundList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new Adapter();
        binding.soundList.setAdapter(adapter);
        binding.tvGuide.setText(R.string.design_manager_sound_description_guide_add_sound);
        actBinding.btnImport.setOnClickListener(view -> {
            if (!UIHelper.isClickThrottled()) {
                stopPlayback();
                ArrayList<ProjectResourceBean> selectedSounds = new ArrayList<>();
                for (ProjectResourceBean next : sounds) {
                    if (next.isSelected) {
                        selectedSounds.add(new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, next.resName, SketchwarePaths.getCollectionPath() + File.separator + "sound" + File.separator + "data" + File.separator + next.resFullName));
                    }
                }
                if (!selectedSounds.isEmpty()) {
                    ArrayList<ProjectResourceBean> d = ((ManageSoundActivity) requireActivity()).projectSounds.sounds;
                    Intent intent = new Intent(requireActivity(), ManageSoundImportActivity.class);
                    intent.putParcelableArrayListExtra("project_sounds", d);
                    intent.putParcelableArrayListExtra("selected_collections", selectedSounds);
                    importSoundsHandler.launch(intent);
                }
                resetSelection();
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("sc_id", sc_id);
        outState.putString("dir_path", dirPath);
    }

    public void stopPlayback() {
        adapter.stopPlayback();
    }

    public void loadProjectSounds() {
        sounds = SoundCollectionManager.getInstance().getResources();
        adapter.notifyDataSetChanged();
        showOrHideNoSoundsText();
    }

    public void resetSelection() {
        sounds.forEach(projectResourceBean -> projectResourceBean.isSelected = false);
        adapter.notifyDataSetChanged();
        actBinding.layoutBtnImport.setVisibility(View.GONE);
    }

    public boolean isSelecting() {
        return sounds.stream().anyMatch(resource -> resource.isSelected);
    }

    private void showOrHideNoSoundsText() {
        if (sounds.isEmpty()) {
            binding.tvGuide.setVisibility(View.VISIBLE);
            binding.soundList.setVisibility(View.GONE);
        } else {
            binding.soundList.setVisibility(View.VISIBLE);
            binding.tvGuide.setVisibility(View.GONE);
        }
    }

    private void handleImportedSounds(ArrayList<ProjectResourceBean> resourceBeans) {
        ArrayList<ProjectResourceBean> beans = new ArrayList<>();
        for (ProjectResourceBean next : resourceBeans) {
            beans.add(new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, next.resName, next.resFullName));
        }
        if (!beans.isEmpty()) {
            ((ManageSoundActivity) requireActivity()).projectSounds.handleImportedResources(beans);
            ((ManageSoundActivity) requireActivity()).binding.viewPager.setCurrentItem(0);
        }
    }

    private class Adapter extends SoundPlayingAdapter<Adapter.ViewHolder> {
        private final LayoutInflater inflater;

        public Adapter() {
            super(requireActivity());
            inflater = LayoutInflater.from(requireActivity());
        }

        @Override
        public ProjectResourceBean getData(int position) {
            return sounds.get(position);
        }

        @Override
        public Path getAudio(int position) {
            return Paths.get(SketchwarePaths.getCollectionPath(), "sound", "data", sounds.get(position).resFullName);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProjectResourceBean bean = sounds.get(position);
            holder.binding.chkSelect.setVisibility(View.VISIBLE);

            var audioMetadata = holder.audioMetadata;
            var audio = getAudio(position);
            if (audioMetadata == null || !audioMetadata.getSource().equals(audio)) {
                holder.audioMetadata = null;
                holder.binding.imgAlbum.setImageResource(R.drawable.default_album_art_200dp);
                AudioMetadata.fromPathAsync(audio, metadata -> {
                    if (holder.getBindingAdapterPosition() == position) {
                        holder.audioMetadata = metadata;
                        bean.totalSoundDuration = metadata.getDurationInMs();
                        metadata.setEmbeddedPictureAsAlbumCover(requireActivity(), holder.binding.imgAlbum);
                        int durationInS = bean.totalSoundDuration / 1000;
                        holder.binding.tvEndtime.setText(String.format(Locale.US, "%d:%02d", durationInS / 60, durationInS % 60));
                        holder.binding.progPlaytime.setMax(bean.totalSoundDuration / 100);
                    }
                });
            }

            int positionInS = bean.curSoundPosition / 1000;
            holder.binding.tvCurrenttime.setText(String.format(Locale.US, "%d:%02d", positionInS / 60, positionInS % 60));
            int durationInS = bean.totalSoundDuration / 1000;
            holder.binding.tvEndtime.setText(String.format(Locale.US, "%d:%02d", durationInS / 60, durationInS % 60));
            holder.binding.chkSelect.setChecked(bean.isSelected);
            holder.binding.tvSoundName.setText(bean.resName);
            boolean playing = position == soundPlayer.getNowPlayingPosition() && soundPlayer.isPlaying();
            holder.binding.imgPlay.setImageResource(playing ? R.drawable.ic_mtrl_circle_pause : R.drawable.ic_mtrl_circle_play);
            holder.binding.progPlaytime.setMax(bean.totalSoundDuration / 100);
            holder.binding.progPlaytime.setProgress(bean.curSoundPosition / 100);
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ManageSoundListItemBinding binding = ManageSoundListItemBinding.inflate(inflater, parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public int getItemCount() {
            return sounds.size();
        }

        private class ViewHolder extends SoundPlayingAdapter.ViewHolder {
            private final ManageSoundListItemBinding binding;
            private AudioMetadata audioMetadata;

            public ViewHolder(@NonNull ManageSoundListItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.imgPlay.setOnClickListener(v -> {
                    if (!UIHelper.isClickThrottled()) {
                        int pos = getLayoutPosition();
                        if (pos == RecyclerView.NO_POSITION) return;
                        soundPlayer.onPlayPressed(pos);
                    }
                });

                binding.getRoot().setOnClickListener(v -> binding.chkSelect.setChecked(!binding.chkSelect.isChecked()));

                binding.chkSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    int position = getLayoutPosition();
                    if (position == RecyclerView.NO_POSITION) return;
                    sounds.get(position).isSelected = isChecked;
                    updateImportSoundsText();
                    new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(position));
                });
            }

            @Override
            protected TextView getCurrentPosition() {
                return binding.tvCurrenttime;
            }

            @Override
            protected ProgressBar getPlaybackProgress() {
                return binding.progPlaytime;
            }
        }
    }
}
