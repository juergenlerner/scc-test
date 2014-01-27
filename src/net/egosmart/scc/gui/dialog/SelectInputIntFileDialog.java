package net.egosmart.scc.gui.dialog;

import java.io.File;
import java.io.FilenameFilter;

import net.egosmart.scc.R;
import net.egosmart.scc.SCCMainActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * @author juergen
 *
 */

public class SelectInputIntFileDialog extends DialogFragment {
	
	protected static final CharSequence INT_FILE_TYPE = ".int";
	private static SCCMainActivity activity;
	private static File path;
	private static SelectInputIntFileDialog instance;

	public static SelectInputIntFileDialog getInstance(SCCMainActivity act){
		activity = act;
		path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		instance = new SelectInputIntFileDialog();
		return instance;
	}
	
	public static SelectInputIntFileDialog getInstance(SCCMainActivity act, File directory){
		activity = act;
		path = directory;
		instance = new SelectInputIntFileDialog();
		return instance;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		String title = activity.getString(R.string.select_int_file_dialog_title);
		builder.setTitle(title);
		LinearLayout layout = new LinearLayout(activity);
		layout.setOrientation(LinearLayout.VERTICAL);
		TextView directoryText = new TextView(activity);
		directoryText.setText("Directory: " + path.getName());
		layout.addView(directoryText);
		final EditText intFileNameEditText = new EditText(activity);
		intFileNameEditText.setHint("enter filename");
		String[] fileListNames;
		final String moveUp = activity.getString(R.string.goto_parent_directory);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				//return true;
				return filename.contains(INT_FILE_TYPE) || new File(dir,filename).isDirectory();
			}
		};
		File[] fileList = path.listFiles(filter);
		File parentFile = path.getParentFile();
		if(parentFile != null && parentFile.exists()){
			fileListNames = new String[fileList.length + 1];
			fileListNames[0] = moveUp;
			for(int i = 0; i < fileList.length; ++i){
				fileListNames[i+1] = fileList[i].getName();
			}
		} else {
			fileListNames = new String[fileList.length];
			for(int i = 0; i < fileList.length; ++i){
				fileListNames[i] = fileList[i].getName();
			}					
		}
		ListView fileListView = new ListView(activity);
		fileListView.setAdapter(new ArrayAdapter<String>(activity, 
				android.R.layout.simple_list_item_1, fileListNames));
		fileListView.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				String selectedFile = parent.getItemAtPosition(position).toString(); 
				if(selectedFile.equals(moveUp)){
					File parentDir = path.getParentFile();
					instance.dismiss();
					DialogFragment dialog = SelectInputIntFileDialog.getInstance(activity, parentDir);
					dialog.show(activity.getSupportFragmentManager(), SCCMainActivity.SELECT_INPUT_EGO_FILE_DIALOG_TAG);
				} else{
					File file = new File(path, selectedFile);
					if(file.isDirectory()){
						instance.dismiss();
						DialogFragment dialog = SelectInputEgoFileDialog.getInstance(activity, file);
						dialog.show(activity.getSupportFragmentManager(), SCCMainActivity.SELECT_INPUT_INT_FILE_DIALOG_TAG);							
					} else if(file.isFile()){
						intFileNameEditText.setText(selectedFile);
					}
				}
			}
		});
		fileListView.setBackgroundColor(Color.LTGRAY);
		layout.addView(fileListView);
		layout.addView(intFileNameEditText);
		builder.setView(layout);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String intFileName = intFileNameEditText.getText().toString();
				if(intFileName != null && intFileName.length() > 0){
					if(!intFileName.endsWith(INT_FILE_TYPE.toString())){
						intFileName = intFileName + INT_FILE_TYPE.toString();
					}
					File file = new File(path, intFileName);
					activity.loadInterviewFromFile(file);
					instance.dismiss();
				}
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id) {
				//nothing to do, isn't it
			}
		});      
		return builder.create();
	}
}

