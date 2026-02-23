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
  
  public a adapter = null;
  
  public Boolean isSelectionMode = Boolean.valueOf(false);
  
  public TextView emptyText;
  
  public int[] viewCounters = new int[19];
  
  public final String a(int paramInt, String paramString) {
    String str1 = SketchwarePaths.b(paramInt);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    int[] arrayOfInt = this.viewCounters;
    int i = arrayOfInt[paramInt] + 1;
    arrayOfInt[paramInt] = i;
    stringBuilder.append(i);
    String str2 = stringBuilder.toString();
    ArrayList arrayList = ProjectDataManager.a(this.projectId).d(paramString);
    paramString = str2;
    while (true) {
      int found = 0;
      Iterator iterator = arrayList.iterator();
      while (true) {
        i = found;
        if (iterator.hasNext()) {
          if (paramString.equals(((ViewBean)iterator.next()).id)) {
            i = 1;
            break;
          } 
          continue;
        } 
        break;
      } 
      if (i == 0)
        return paramString; 
      StringBuilder stringBuilder1 = new StringBuilder();
      stringBuilder1.append(str1);
      int[] arrayOfInt1 = this.viewCounters;
      i = arrayOfInt1[paramInt] + 1;
      arrayOfInt1[paramInt] = i;
      stringBuilder1.append(i);
      paramString = stringBuilder1.toString(); // Fix: assign generated string back to paramString
    } 
  }
  
  public final ArrayList<ViewBean> a(String paramString, int paramInt) {
    ArrayList<ViewBean> arrayList;
    if (paramInt == 277) {
      arrayList = PresetLayoutFactory.b(paramString);
    } else if (paramInt == 278) {
      arrayList = PresetLayoutFactory.d(paramString);
    } else {
      arrayList = new ArrayList<>();
    }
    return arrayList;
  }
  
  public void a(ProjectFileBean paramProjectFileBean) {
    this.projectFiles.add(paramProjectFileBean);
    this.adapter.notifyDataSetChanged();
  }
  
  public void a(String paramString) {
    boolean found = false;
    for (ProjectFileBean bean : this.projectFiles) {
      if (bean.fileType == 2 && bean.fileName.equals(paramString)) {
        found = true;
        break;
      }
    }
    if (!found) {
      this.projectFiles.add(new ProjectFileBean(2, paramString));
      this.adapter.notifyDataSetChanged();
    }
  }
  
  public void a(boolean paramBoolean) {
    this.isSelectionMode = Boolean.valueOf(paramBoolean);
    e();
    this.adapter.notifyDataSetChanged();
  }
  
  public void b(String paramString) {
    for (ProjectFileBean projectFileBean : this.projectFiles) {
      if (projectFileBean.fileType == 2 && projectFileBean.fileName.equals(paramString)) {
        this.projectFiles.remove(projectFileBean);
        break;
      } 
    } 
    this.adapter.notifyDataSetChanged();
  }
  
  public ArrayList<ProjectFileBean> c() {
    return this.projectFiles;
  }
  
  public void d() {
    ArrayList<ProjectFileBean> arrayList = ProjectDataManager.b(this.projectId).c();
    if (arrayList == null)
      return; 
    for (ProjectFileBean projectFileBean : arrayList)
      this.projectFiles.add(projectFileBean); 
  }
  
  public final void e() {
    Iterator<ProjectFileBean> iterator = this.projectFiles.iterator();
    while (iterator.hasNext())
      ((SelectableBean)iterator.next()).isSelected = false; 
  }
  
  public void f() {
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
  
  public void g() {
    ArrayList<ProjectFileBean> arrayList = this.projectFiles;
    if (arrayList != null)
      if (arrayList.size() == 0) {
        this.emptyText.setVisibility(View.VISIBLE);
        this.recyclerView.setVisibility(View.GONE);
      } else {
        this.emptyText.setVisibility(View.GONE);
        this.recyclerView.setVisibility(View.VISIBLE);
      }  
  }
  
  public void onActivityCreated(Bundle paramBundle) {
    super.onActivityCreated(paramBundle);
    if (paramBundle == null) {
      d();
    } else {
      this.projectId = paramBundle.getString("sc_id");
      this.projectFiles = paramBundle.getParcelableArrayList("custom_views");
    } 
    this.recyclerView.getAdapter().notifyDataSetChanged();
    g();
  }
  
  public void onActivityResult(int paramInt1, int paramInt2, Intent paramIntent) {
    if ((paramInt1 == 277 || paramInt1 == 278) && paramInt2 == -1) {
      ProjectFileBean projectFileBean = this.projectFiles.get(this.adapter.c);
      ArrayList<ViewBean> arrayList2 = ProjectDataManager.a(this.projectId).d(projectFileBean.getXmlName());
      for (paramInt2 = arrayList2.size() - 1; paramInt2 >= 0; paramInt2--) {
        ViewBean viewBean = arrayList2.get(paramInt2);
        ProjectDataManager.a(this.projectId).a(projectFileBean, viewBean);
      } 
      ArrayList<ViewBean> arrayList1 = a(((ProjectFileBean)paramIntent.getParcelableExtra("preset_data")).presetName, paramInt1);
      ProjectDataManager.a(this.projectId);
      for (ViewBean viewBean : ProjectDataStore.a(arrayList1)) {
        viewBean.id = a(viewBean.type, projectFileBean.getXmlName());
        ProjectDataManager.a(this.projectId).a(projectFileBean.getXmlName(), viewBean);
        if (viewBean.type == 3 && projectFileBean.fileType == 0)
          ProjectDataManager.a(this.projectId).a(projectFileBean.getJavaName(), 1, viewBean.type, viewBean.id, "onClick"); 
      } 
      a a1 = this.adapter;
      a1.notifyItemChanged(a1.c);
    } 
  }
  
  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
    ViewGroup viewGroup = (ViewGroup)paramLayoutInflater.inflate(R.layout.fr_manage_view_list, paramViewGroup, false);
    this.projectFiles = new ArrayList<ProjectFileBean>();
    this.recyclerView = (RecyclerView)viewGroup.findViewById(R.id.list_activities);
    this.recyclerView.setHasFixedSize(true);
    this.recyclerView.setLayoutManager((RecyclerView.LayoutManager)new LinearLayoutManager(getContext()));
    this.adapter = new a(this, this.recyclerView);
    this.recyclerView.setAdapter(this.adapter);
    if (paramBundle == null) {
      this.projectId = requireActivity().getIntent().getStringExtra("sc_id");
    } else {
      this.projectId = paramBundle.getString("sc_id");
    } 
    this.emptyText = (TextView)viewGroup.findViewById(R.id.tv_guide);
    this.emptyText.setText(StringResource.b().a((Context)requireActivity(), R.string.design_manager_view_description_guide_create_custom_view));
    return (View)viewGroup;
  }
  
  public void onSaveInstanceState(Bundle paramBundle) {
    paramBundle.putString("sc_id", this.projectId);
    paramBundle.putParcelableArrayList("custom_views", this.projectFiles);
    super.onSaveInstanceState(paramBundle);
  }
  
  public class a extends RecyclerView.Adapter<a.ViewHolder> {
    public int c = -1;
    
    public final ViewFilesAdapter d;
    
    public a(ViewFilesAdapter this$0, RecyclerView param1RecyclerView) {
      this.d = this$0;
      if (param1RecyclerView.getLayoutManager() instanceof LinearLayoutManager)
        param1RecyclerView.addOnScrollListener(new ViewFileScrollListener(this, this$0)); 
    }
    
    public int getItemCount() {
      return (this.d.projectFiles != null) ? this.d.projectFiles.size() : 0;
    }
    
    public void onBindViewHolder(ViewHolder param1a, int param1Int) {
      if (this.d.isSelectionMode.booleanValue()) {
        param1a.x.setVisibility(View.VISIBLE);
        param1a.u.setVisibility(View.GONE);
      } else {
        param1a.x.setVisibility(View.GONE);
        param1a.u.setVisibility(View.VISIBLE);
      } 
      ProjectFileBean projectFileBean = this.d.projectFiles.get(param1Int);
      param1a.u.setImageResource(R.drawable.activity_preset_1);
      param1a.t.setChecked(((SelectableBean)projectFileBean).isSelected);
      param1Int = projectFileBean.fileType;
      if (param1Int == 1) {
        param1a.w.setText(projectFileBean.getXmlName());
      } else if (param1Int == 2) {
        param1a.t.setVisibility(View.GONE);
        param1a.u.setImageResource(R.drawable.activity_0110);
        param1a.w.setText(projectFileBean.fileName.substring(1));
      } 
      if (((SelectableBean)projectFileBean).isSelected) {
        param1a.v.setImageResource(R.drawable.ic_checkmark_green_48dp);
        return;
      } 
      param1a.v.setImageResource(R.drawable.ic_trashcan_white_48dp);
    }
    
    public ViewHolder onCreateViewHolder(ViewGroup param1ViewGroup, int param1Int) {
      return new ViewHolder(this, LayoutInflater.from(param1ViewGroup.getContext()).inflate(R.layout.manage_view_custom_list_item, param1ViewGroup, false));
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
      public CheckBox t;
      
      public ImageView u;
      
      public ImageView v;
      
      public TextView w;
      
      public LinearLayout x;
      
      public ImageView y;
      
      public final ViewFilesAdapter.a z;
      
      public ViewHolder(ViewFilesAdapter.a this$0, View param2View) {
        super(param2View);
        this.z = this$0;
        this.t = (CheckBox)param2View.findViewById(R.id.chk_select);
        this.u = (ImageView)param2View.findViewById(R.id.img_activity);
        this.w = (TextView)param2View.findViewById(R.id.tv_screen_name);
        this.x = (LinearLayout)param2View.findViewById(R.id.delete_img_container);
        this.v = (ImageView)param2View.findViewById(R.id.img_delete);
        this.y = (ImageView)param2View.findViewById(R.id.img_preset_setting);
        this.t.setVisibility(View.GONE);
        param2View.setOnClickListener(new ViewFileClickListener(this, this$0));
        param2View.setOnLongClickListener(new ViewFileLongClickListener(this, this$0));
        this.y.setOnClickListener(new ViewFileEditClickListener(this, this$0));
      }
    }
  }
}
