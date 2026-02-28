package pro.sketchware.core;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.besome.sketch.beans.ProjectFileBean;
import pro.sketchware.R;
import com.besome.sketch.beans.SelectableBean;
import com.besome.sketch.beans.ViewBean;
import java.util.ArrayList;

public class ViewFilesAdapter extends BaseFragment {
  public RecyclerView recyclerView;
  
  public String projectId;
  
  public ArrayList<ProjectFileBean> projectFiles;
  
  public FileListAdapter adapter = null;
  
  public Boolean isSelectionMode = Boolean.valueOf(false);
  
  public TextView emptyText;
  
  public int[] viewCounters = new int[19];
  
  public final String generateUniqueViewId(int position, String xmlName) {
    String prefix = SketchwarePaths.getWidgetTypeName(position);
    int[] intValues = viewCounters;
    int counter = intValues[position] + 1;
    intValues[position] = counter;
    String candidateId = prefix + counter;
    ArrayList existingViews = ProjectDataManager.getProjectDataManager(projectId).getViews(xmlName);
    xmlName = candidateId;
    while (true) {
      counter = 0;
      for (Object view : existingViews) {
        if (xmlName.equals(((ViewBean)view).id)) {
          counter = 1;
          break;
        }
      }
      if (counter == 0)
        return xmlName; 
      int[] intValues1 = viewCounters;
      counter = intValues1[position] + 1;
      intValues1[position] = counter;
      xmlName = prefix + counter;
    } 
  }
  
  public final ArrayList<ViewBean> getPresetViews(String fileName, int position) {
    ArrayList<ViewBean> presetViews;
    if (position == 277) {
      presetViews = PresetLayoutFactory.getListItemPresetViews(fileName);
    } else if (position == 278) {
      presetViews = PresetLayoutFactory.getDrawerPresetViews(fileName);
    } else {
      presetViews = new ArrayList<>();
    }
    return presetViews;
  }
  
  public void addProjectFile(ProjectFileBean fileBean) {
    projectFiles.add(fileBean);
    adapter.notifyDataSetChanged();
  }
  
  public void addCustomView(String fileName) {
    boolean found = false;
    for (ProjectFileBean bean : projectFiles) {
      if (bean.fileType == 2 && bean.fileName.equals(fileName)) {
        found = true;
        break;
      }
    }
    if (!found) {
      projectFiles.add(new ProjectFileBean(2, fileName));
      adapter.notifyDataSetChanged();
    }
  }
  
  public void setSelectionMode(boolean flag) {
    isSelectionMode = Boolean.valueOf(flag);
    deselectAll();
    adapter.notifyDataSetChanged();
  }
  
  public void removeCustomView(String fileName) {
    for (ProjectFileBean projectFileBean : projectFiles) {
      if (projectFileBean.fileType == 2 && projectFileBean.fileName.equals(fileName)) {
        projectFiles.remove(projectFileBean);
        break;
      } 
    } 
    adapter.notifyDataSetChanged();
  }
  
  public ArrayList<ProjectFileBean> getProjectFiles() {
    return projectFiles;
  }
  
  public void loadCustomViews() {
    ArrayList<ProjectFileBean> customViews = ProjectDataManager.getFileManager(projectId).getCustomViews();
    if (customViews == null)
      return; 
    for (ProjectFileBean projectFileBean : customViews)
      projectFiles.add(projectFileBean); 
  }
  
  public final void deselectAll() {
    for (ProjectFileBean bean : projectFiles)
      bean.isSelected = false; 
  }
  
  public void removeSelectedFiles() {
    int size = projectFiles.size();
    while (true) {
      int idx = size - 1;
      if (idx >= 0) {
        size = idx;
        if (((SelectableBean)projectFiles.get(idx)).isSelected) {
          projectFiles.remove(idx);
          size = idx;
        } 
        continue;
      } 
      adapter.notifyDataSetChanged();
      return;
    } 
  }
  
  public void updateEmptyState() {
    ArrayList<ProjectFileBean> filesList = projectFiles;
    if (filesList != null)
      if (filesList.size() == 0) {
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
      } else {
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
      }  
  }
  
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (savedInstanceState == null) {
      loadCustomViews();
    } else {
      projectId = savedInstanceState.getString("sc_id");
      projectFiles = savedInstanceState.getParcelableArrayList("custom_views");
    } 
    recyclerView.getAdapter().notifyDataSetChanged();
    updateEmptyState();
  }
  
  public void onActivityResult(int start, int end, Intent resultIntent) {
    if ((start == 277 || start == 278) && end == -1) {
      ProjectFileBean projectFileBean = projectFiles.get(adapter.selectedPosition);
      ArrayList<ViewBean> currentViews = ProjectDataManager.getProjectDataManager(projectId).getViews(projectFileBean.getXmlName());
      for (end = currentViews.size() - 1; end >= 0; end--) {
        ViewBean viewBean = currentViews.get(end);
        ProjectDataManager.getProjectDataManager(projectId).removeView(projectFileBean, viewBean);
      } 
      ArrayList<ViewBean> existingViews = getPresetViews(((ProjectFileBean)resultIntent.getParcelableExtra("preset_data")).presetName, start);
      ProjectDataManager.getProjectDataManager(projectId);
      for (ViewBean viewBean : ProjectDataStore.getSortedRootViews(existingViews)) {
        viewBean.id = generateUniqueViewId(viewBean.type, projectFileBean.getXmlName());
        ProjectDataManager.getProjectDataManager(projectId).addView(projectFileBean.getXmlName(), viewBean);
        if (viewBean.type == 3 && projectFileBean.fileType == 0)
          ProjectDataManager.getProjectDataManager(projectId).addEvent(projectFileBean.getJavaName(), 1, viewBean.type, viewBean.id, "onClick"); 
      } 
      FileListAdapter fileListAdapter = adapter;
      fileListAdapter.notifyItemChanged(fileListAdapter.selectedPosition);
    } 
  }
  
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    ViewGroup viewGroup = (ViewGroup)inflater.inflate(R.layout.fr_manage_view_list, container, false);
    projectFiles = new ArrayList<>();
    recyclerView = (RecyclerView)viewGroup.findViewById(R.id.list_activities);
    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new FileListAdapter(this, recyclerView);
    recyclerView.setAdapter(adapter);
    if (savedInstanceState == null) {
      projectId = requireActivity().getIntent().getStringExtra("sc_id");
    } else {
      projectId = savedInstanceState.getString("sc_id");
    } 
    emptyText = (TextView)viewGroup.findViewById(R.id.tv_guide);
    emptyText.setText(StringResource.getInstance().getTranslatedString(requireActivity(), R.string.design_manager_view_description_guide_create_custom_view));
    return viewGroup;
  }
  
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putString("sc_id", projectId);
    savedInstanceState.putParcelableArrayList("custom_views", projectFiles);
    super.onSaveInstanceState(savedInstanceState);
  }
  
  public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {
    public int selectedPosition = -1;
    
    public final ViewFilesAdapter outerAdapter;
    
    public FileListAdapter(ViewFilesAdapter this$0, RecyclerView recyclerView) {
      outerAdapter = this$0;
      if (recyclerView.getLayoutManager() instanceof LinearLayoutManager)
        recyclerView.addOnScrollListener(new ViewFileScrollListener(this, this$0)); 
    }
    
    public int getItemCount() {
      return (outerAdapter.projectFiles != null) ? outerAdapter.projectFiles.size() : 0;
    }
    
    public void onBindViewHolder(ViewHolder holder, int position) {
      if (outerAdapter.isSelectionMode.booleanValue()) {
        holder.deleteContainer.setVisibility(View.VISIBLE);
        holder.activityIcon.setVisibility(View.GONE);
      } else {
        holder.deleteContainer.setVisibility(View.GONE);
        holder.activityIcon.setVisibility(View.VISIBLE);
      } 
      ProjectFileBean projectFileBean = outerAdapter.projectFiles.get(position);
      holder.activityIcon.setImageResource(R.drawable.activity_preset_1);
      holder.checkbox.setChecked(((SelectableBean)projectFileBean).isSelected);
      position = projectFileBean.fileType;
      if (position == 1) {
        holder.screenName.setText(projectFileBean.getXmlName());
      } else if (position == 2) {
        holder.checkbox.setVisibility(View.GONE);
        holder.activityIcon.setImageResource(R.drawable.activity_0110);
        holder.screenName.setText(projectFileBean.fileName.substring(1));
      } 
      if (((SelectableBean)projectFileBean).isSelected) {
        holder.deleteIcon.setImageResource(R.drawable.ic_checkmark_green_48dp);
        return;
      } 
      holder.deleteIcon.setImageResource(R.drawable.ic_trashcan_white_48dp);
    }
    
    public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
      return new ViewHolder(this, LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_view_custom_list_item, parent, false));
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
      public CheckBox checkbox;
      
      public ImageView activityIcon;
      
      public ImageView deleteIcon;
      
      public TextView screenName;
      
      public LinearLayout deleteContainer;
      
      public ImageView presetIcon;
      
      public final ViewFilesAdapter.FileListAdapter adapterRef;
      
      public ViewHolder(ViewFilesAdapter.FileListAdapter this$0, View itemView) {
        super(itemView);
        adapterRef = this$0;
        checkbox = (CheckBox)itemView.findViewById(R.id.chk_select);
        activityIcon = (ImageView)itemView.findViewById(R.id.img_activity);
        screenName = (TextView)itemView.findViewById(R.id.tv_screen_name);
        deleteContainer = (LinearLayout)itemView.findViewById(R.id.delete_img_container);
        deleteIcon = (ImageView)itemView.findViewById(R.id.img_delete);
        presetIcon = (ImageView)itemView.findViewById(R.id.img_preset_setting);
        checkbox.setVisibility(View.GONE);
        itemView.setOnClickListener(new ViewFileClickListener(this, this$0));
        itemView.setOnLongClickListener(new ViewFileLongClickListener(this, this$0));
        presetIcon.setOnClickListener(new ViewFileEditClickListener(this, this$0));
      }
    }
  }
}
