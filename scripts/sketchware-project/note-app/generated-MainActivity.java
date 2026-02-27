package com.my.noteapp;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.appbar.AppBarLayout;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	private double editIndex = 0;
	
	private ArrayList<String> displayList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> notes = new ArrayList<>();
	
	private LinearLayout panel_list;
	private LinearLayout panel_edit;
	private LinearLayout linear_header;
	private ListView listview1;
	private TextView tv_title;
	private Button btn_add;
	private LinearLayout linear_edit_header;
	private EditText edittext_title;
	private EditText edittext_content;
	private LinearLayout linear_buttons;
	private TextView tv_edit_title;
	private Button btn_back;
	private Button btn_save;
	private Button btn_delete;
	
	private SharedPreferences sp;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		_app_bar = findViewById(R.id._app_bar);
		_coordinator = findViewById(R.id._coordinator);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
		panel_list = findViewById(R.id.panel_list);
		panel_edit = findViewById(R.id.panel_edit);
		linear_header = findViewById(R.id.linear_header);
		listview1 = findViewById(R.id.listview1);
		tv_title = findViewById(R.id.tv_title);
		btn_add = findViewById(R.id.btn_add);
		linear_edit_header = findViewById(R.id.linear_edit_header);
		edittext_title = findViewById(R.id.edittext_title);
		edittext_content = findViewById(R.id.edittext_content);
		linear_buttons = findViewById(R.id.linear_buttons);
		tv_edit_title = findViewById(R.id.tv_edit_title);
		btn_back = findViewById(R.id.btn_back);
		btn_save = findViewById(R.id.btn_save);
		btn_delete = findViewById(R.id.btn_delete);
		sp = getSharedPreferences("", Activity.MODE_PRIVATE);
		
		listview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				editIndex = _position;
				edittext_title.setText(notes.get((int)editIndex).containsKey("title") ? notes.get((int)editIndex).get("title").toString() : "");
				edittext_content.setText(notes.get((int)editIndex).containsKey("content") ? notes.get((int)editIndex).get("content").toString() : "");
				tv_edit_title.setText("Edit Note");
				panel_list.setVisibility(View.GONE);
				panel_edit.setVisibility(View.VISIBLE);
			}
		});
		
		btn_add.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				editIndex = -1;
				edittext_title.setText("");
				edittext_content.setText("");
				tv_edit_title.setText("New Note");
				panel_list.setVisibility(View.GONE);
				panel_edit.setVisibility(View.VISIBLE);
			}
		});
		
		btn_back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				panel_edit.setVisibility(View.GONE);
				panel_list.setVisibility(View.VISIBLE);
			}
		});
		
		btn_save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				java.util.HashMap<String, Object> _note = new java.util.HashMap<>();
				_note.put("title", edittext_title.getText().toString());
				_note.put("content", edittext_content.getText().toString());
				_note.put("time", String.valueOf(System.currentTimeMillis()));
				if ((int)editIndex >= 0 && (int)editIndex < notes.size()) { notes.set((int)editIndex, _note); } else { notes.add(0, _note); }
				org.json.JSONArray _ja = new org.json.JSONArray(); for (java.util.HashMap<String, Object> _m : notes) { _ja.put(new org.json.JSONObject(_m)); } sp.edit().putString("notes_json", _ja.toString()).apply();
				displayList = new java.util.ArrayList<>(); for (java.util.HashMap<String, Object> _n : notes) { displayList.add(_n.containsKey("title") ? _n.get("title").toString() : "Untitled"); }
				listview1.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, displayList));
				panel_edit.setVisibility(View.GONE);
				panel_list.setVisibility(View.VISIBLE);
				SketchwareUtil.showMessage(getApplicationContext(), "Note saved!");
			}
		});
		
		btn_delete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (editIndex > -1) {
					notes.remove((int)editIndex);
					org.json.JSONArray _ja = new org.json.JSONArray(); for (java.util.HashMap<String, Object> _m : notes) { _ja.put(new org.json.JSONObject(_m)); } sp.edit().putString("notes_json", _ja.toString()).apply();
					displayList = new java.util.ArrayList<>(); for (java.util.HashMap<String, Object> _n : notes) { displayList.add(_n.containsKey("title") ? _n.get("title").toString() : "Untitled"); }
					listview1.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, displayList));
					SketchwareUtil.showMessage(getApplicationContext(), "Note deleted!");
				}
				panel_edit.setVisibility(View.GONE);
				panel_list.setVisibility(View.VISIBLE);
			}
		});
	}
	
	private void initializeLogic() {
		sp = getApplicationContext().getSharedPreferences("notes_app", Activity.MODE_PRIVATE);
		String _json = sp.getString("notes_json", "");
		if (!_json.isEmpty()) { try { org.json.JSONArray _ja = new org.json.JSONArray(_json); for (int _i = 0; _i < _ja.length(); _i++) { org.json.JSONObject _jo = _ja.getJSONObject(_i); java.util.HashMap<String, Object> _m = new java.util.HashMap<>(); java.util.Iterator<String> _keys = _jo.keys(); while (_keys.hasNext()) { String _k = _keys.next(); _m.put(_k, _jo.getString(_k)); } notes.add(_m); } } catch(Exception e) {} }
		displayList = new java.util.ArrayList<>(); for (java.util.HashMap<String, Object> _n : notes) { displayList.add(_n.containsKey("title") ? _n.get("title").toString() : "Untitled"); }
		listview1.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, displayList));
		panel_edit.setVisibility(View.GONE);
		editIndex = -1;
	}
	
}