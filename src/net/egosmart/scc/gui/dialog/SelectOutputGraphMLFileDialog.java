/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import java.io.File;
import java.io.FilenameFilter;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * @author juergen
 *
 */
public class SelectOutputGraphMLFileDialog extends DialogFragment {

	protected static final CharSequence GRAPHML_FILE_TYPE = ".graphml";
	private static SCCMainActivity activity;
	private static File path;
	private static SelectOutputGraphMLFileDialog instance;

	public static SelectOutputGraphMLFileDialog getInstance(SCCMainActivity act){
		activity = act;
		path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		instance = new SelectOutputGraphMLFileDialog();
		return instance;
	}

	public static SelectOutputGraphMLFileDialog getInstance(SCCMainActivity act, File directory){
		activity = act;
		path = directory;
		instance = new SelectOutputGraphMLFileDialog();
		return instance;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		String title = activity.getString(R.string.select_graphml_file_dialog_title);
		builder.setTitle(title);
		LinearLayout layout = new LinearLayout(activity);
		layout.setOrientation(LinearLayout.VERTICAL);
		TextView directoryText = new TextView(activity);
		directoryText.setText("Directory: " + path.getName());
		layout.addView(directoryText);
		final EditText graphMLFileNameEditText = new EditText(activity);
		graphMLFileNameEditText.setHint("enter filename");
		String[] fileListNames;
		final String moveUp = activity.getString(R.string.goto_parent_directory);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				//return true;
				return filename.contains(GRAPHML_FILE_TYPE) || new File(dir,filename).isDirectory();
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
					DialogFragment dialog = SelectOutputGraphMLFileDialog.getInstance(activity, parentDir);
					dialog.show(activity.getSupportFragmentManager(), 
							SCCMainActivity.SELECT_OUTPUT_GRAPHML_FILE_DIALOG_TAG);
				} else{
					File file = new File(path, selectedFile);
					if(file.isDirectory()){
						instance.dismiss();
						DialogFragment dialog = SelectOutputGraphMLFileDialog.getInstance(activity, file);
						dialog.show(activity.getSupportFragmentManager(), 
								SCCMainActivity.SELECT_OUTPUT_GRAPHML_FILE_DIALOG_TAG);							
					} else if(file.isFile()){
						graphMLFileNameEditText.setText(selectedFile);
					}
				}
			}
		});
		fileListView.setBackgroundColor(Color.LTGRAY);
		layout.addView(fileListView);
		layout.addView(graphMLFileNameEditText);
		builder.setView(layout);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String graphmlFileName = graphMLFileNameEditText.getText().toString();
				if(graphmlFileName != null && graphmlFileName.length() > 0){
					if(!graphmlFileName.endsWith(GRAPHML_FILE_TYPE.toString())){
						graphmlFileName = graphmlFileName + GRAPHML_FILE_TYPE.toString();
					}
					File file = new File(path, graphmlFileName);
					activity.exportToGraphMLFile(file);
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
