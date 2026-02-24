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
import java.util.Iterator;

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
    StringBuilder idBuilder = new StringBuilder();
    idBuilder.append(prefix);
    int[] intValues = this.viewCounters;
    int i = intValues[position] + 1;
    intValues[position] = i;
    idBuilder.append(i);
    String candidateId = idBuilder.toString();
    ArrayList existingViews = ProjectDataManager.getProjectDataManager(this.projectId).getViews(xmlName);
    xmlName = candidateId;
    while (true) {
      int found = 0;
      Iterator viewIterator = existingViews.iterator();
      while (true) {
        i = found;
        if (viewIterator.hasNext()) {
          if (xmlName.equals(((ViewBean)viewIterator.next()).id)) {
            i = 1;
            break;
          } 
          continue;
        } 
        break;
      } 
      if (i == 0)
        return xmlName; 
      StringBuilder retryBuilder = new StringBuilder();
      retryBuilder.append(prefix);
      int[] intValues1 = this.viewCounters;
      i = intValues1[position] + 1;
      intValues1[position] = i;
      retryBuilder.append(i);
      xmlName = retryBuilder.toString();
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
    this.projectFiles.add(fileBean);
    this.adapter.notifyDataSetChanged();
  }
  
  public void addCustomView(String fileName) {
    boolean found = false;
    for (ProjectFileBean bean : this.projectFiles) {
      if (bean.fileType == 2 && bean.fileName.equals(fileName)) {
        found = true;
        break;
      }
    }
    if (!found) {
      this.projectFiles.add(new ProjectFileBean(2, fileName));
      this.adapter.notifyDataSetChanged();
    }
  }
  
  public void setSelectionMode(boolean flag) {
    this.isSelectionMode = Boolean.valueOf(flag);
    deselectAll();
    this.adapter.notifyDataSetChanged();
  }
  
  public void removeCustomView(String fileName) {
    for (ProjectFileBean projectFileBean : this.projectFiles) {
      if (projectFileBean.fileType == 2 && projectFileBean.fileName.equals(fileName)) {
        this.projectFiles.remove(projectFileBean);
        break;
      } 
    } 
    this.adapter.notifyDataSetChanged();
  }
  
  public ArrayList<ProjectFileBean> getProjectFiles() {
    return this.projectFiles;
  }
  
  public void loadCustomViews() {
    ArrayList<ProjectFileBean> customViews = ProjectDataManager.getFileManager(this.projectId).getCustomViews();
    if (customViews == null)
      return; 
    for (ProjectFileBean projectFileBean : customViews)
      this.projectFiles.add(projectFileBean); 
  }
  
  public final void deselectAll() {
    Iterator<ProjectFileBean> iterator = this.projectFiles.iterator();
    while (iterator.hasNext())
      ((SelectableBean)iterator.next()).isSelected = false; 
  }
  
  public void removeSelectedFiles() {
    int i = this.projectFiles.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        i = j;
        if (((SelectableBean)this.projectFiles.get(j)).isSelected) {
          this.projectFiles.remove(j);
          i = j;
        } 
        continue;
      } 
      this.adapter.notifyDataSetChanged();
      return;
    } 
  }
  
  public void updateEmptyState() {
    ArrayList<ProjectFileBean> filesList = this.projectFiles;
    if (filesList != null)
      if (filesList.size() == 0) {
        this.emptyText.setVisibility(View.VISIBLE);
        this.recyclerView.setVisibility(View.GONE);
      } else {
        this.emptyText.setVisibility(View.GONE);
        this.recyclerView.setVisibility(View.VISIBLE);
      }  
  }
  
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (savedInstanceState == null) {
      loadCustomViews();
    } else {
      this.projectId = savedInstanceState.getString("sc_id");
      this.projectFiles = savedInstanceState.getParcelableArrayList("custom_views");
    } 
    this.recyclerView.getAdapter().notifyDataSetChanged();
    updateEmptyState();
  }
  
  public void onActivityResult(int start, int end, Intent resultIntent) {
    if ((start == 277 || start == 278) && end == -1) {
      ProjectFileBean projectFileBean = this.projectFiles.get(this.adapter.selectedPosition);
      ArrayList<ViewBean> currentViews = ProjectDataManager.getProjectDataManager(this.projectId).getViews(projectFileBean.getXmlName());
      for (end = currentViews.size() - 1; end >= 0; end--) {
        ViewBean viewBean = currentViews.get(end);
        ProjectDataManager.getProjectDataManager(this.projectId).removeView(projectFileBean, viewBean);
      } 
      ArrayList<ViewBean> existingViews = getPresetViews(((ProjectFileBean)resultIntent.getParcelableExtra("preset_data")).presetName, start);
      ProjectDataManager.getProjectDataManager(this.projectId);
      for (ViewBean viewBean : ProjectDataStore.getSortedRootViews(existingViews)) {
        viewBean.id = generateUniqueViewId(viewBean.type, projectFileBean.getXmlName());
        ProjectDataManager.getProjectDataManager(this.projectId).addView(projectFileBean.getXmlName(), viewBean);
        if (viewBean.type == 3 && projectFileBean.fileType == 0)
          ProjectDataManager.getProjectDataManager(this.projectId).addEvent(projectFileBean.getJavaName(), 1, viewBean.type, viewBean.id, "onClick"); 
      } 
      FileListAdapter a1 = this.adapter;
      a1.notifyItemChanged(a1.selectedPosition);
    } 
  }
  
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    ViewGroup viewGroup = (ViewGroup)inflater.inflate(R.layout.fr_manage_view_list, container, false);
    this.projectFiles = new ArrayList<ProjectFileBean>();
    this.recyclerView = (RecyclerView)viewGroup.findViewById(R.id.list_activities);
    this.recyclerView.setHasFixedSize(true);
    this.recyclerView.setLayoutManager((RecyclerView.LayoutManager)new LinearLayoutManager(getContext()));
    this.adapter = new FileListAdapter(this, this.recyclerView);
    this.recyclerView.setAdapter(this.adapter);
    if (savedInstanceState == null) {
      this.projectId = requireActivity().getIntent().getStringExtra("sc_id");
    } else {
      this.projectId = savedInstanceState.getString("sc_id");
    } 
    this.emptyText = (TextView)viewGroup.findViewById(R.id.tv_guide);
    this.emptyText.setText(StringResource.getInstance().getTranslatedString((Context)requireActivity(), R.string.design_manager_view_description_guide_create_custom_view));
    return (View)viewGroup;
  }
  
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putString("sc_id", this.projectId);
    savedInstanceState.putParcelableArrayList("custom_views", this.projectFiles);
    super.onSaveInstanceState(savedInstanceState);
  }
  
  public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {
    public int selectedPosition = -1;
    
    public final ViewFilesAdapter outerAdapter;
    
    public FileListAdapter(ViewFilesAdapter this$0, RecyclerView recyclerView) {
      this.outerAdapter = this$0;
      if (recyclerView.getLayoutManager() instanceof LinearLayoutManager)
        recyclerView.addOnScrollListener(new ViewFileScrollListener(this, this$0)); 
    }
    
    public int getItemCount() {
      return (this.outerAdapter.projectFiles != null) ? this.outerAdapter.projectFiles.size() : 0;
    }
    
    public void onBindViewHolder(ViewHolder holder, int position) {
      if (this.outerAdapter.isSelectionMode.booleanValue()) {
        holder.deleteContainer.setVisibility(View.VISIBLE);
        holder.activityIcon.setVisibility(View.GONE);
      } else {
        holder.deleteContainer.setVisibility(View.GONE);
        holder.activityIcon.setVisibility(View.VISIBLE);
      } 
      ProjectFileBean projectFileBean = this.outerAdapter.projectFiles.get(position);
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
        this.adapterRef = this$0;
        this.checkbox = (CheckBox)itemView.findViewById(R.id.chk_select);
        this.activityIcon = (ImageView)itemView.findViewById(R.id.img_activity);
        this.screenName = (TextView)itemView.findViewById(R.id.tv_screen_name);
        this.deleteContainer = (LinearLayout)itemView.findViewById(R.id.delete_img_container);
        this.deleteIcon = (ImageView)itemView.findViewById(R.id.img_delete);
        this.presetIcon = (ImageView)itemView.findViewById(R.id.img_preset_setting);
        this.checkbox.setVisibility(View.GONE);
        itemView.setOnClickListener(new ViewFileClickListener(this, this$0));
        itemView.setOnLongClickListener(new ViewFileLongClickListener(this, this$0));
        this.presetIcon.setOnClickListener(new ViewFileEditClickListener(this, this$0));
      }
    }
  }
}
