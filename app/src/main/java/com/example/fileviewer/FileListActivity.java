package com.example.fileviewer;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FileListActivity extends AppCompatActivity {
    String currentPath;
    FileAdapter fileAdapter;
    File currentCopyFile;
    File currentMoveFile;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.app_menu, menu);
        MenuItem paste = menu.findItem(R.id.paste);
        paste.setVisible(currentCopyFile != null || currentMoveFile != null);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            menu.findItem(R.id.paste).setVisible(currentCopyFile != null || currentMoveFile != null);
        } catch (Exception e) {
            Log.e("ERROR", "onPrepareOptionsMenu: " + e.getMessage());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (currentPath != null) {
            File curDir = new File(currentPath);
            if (item.getItemId() == R.id.new_folder) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final EditText input = new EditText(this);
                input.setPadding(50, 40, 50, 40);
                input.setHint("Folder name");
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setTitle("Enter folder name").setView(input).setPositiveButton("OK", (dialogInterface, i) -> {
                    String name = input.getText().toString();
                    File dir = new File(curDir.getAbsolutePath() + File.separator + name);
                    boolean success = dir.mkdirs();
                    if (success) {
                        Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                        refresh();
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
                builder.show();
                Log.d("NewFolder", "On " + curDir.getAbsolutePath());
            }
            if (item.getItemId() == R.id.new_txt_file) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final EditText input = new EditText(this);
                input.setPadding(50, 40, 50, 40);
                input.setHint("File name");
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setTitle("Enter file name").setView(input).setPositiveButton("OK", (dialogInterface, i) -> {
                    String name = input.getText().toString();
                    File newTxtFile = new File(curDir.getAbsolutePath() + File.separator + name + ".txt");
                    try {
                        boolean success = newTxtFile.createNewFile();
                        if (success) {
                            Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                            refresh();
                        } else {
                            Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
                        Log.d("Error", "Can't create new File in " + curDir.getAbsolutePath());
                    }
                }).setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
                builder.show();
                Log.d("NewTxtFile", "On " + curDir.getAbsolutePath());
            }
            if (item.getItemId() == R.id.paste) {
                if (currentMoveFile != null) {
                    String name = currentMoveFile.getName();
                    File newFile = new File(currentPath + File.separator + name);
                    try {
                        Files.move(currentMoveFile.toPath(), newFile.toPath());
                        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                        this.currentMoveFile = null;
                        refresh();
                        invalidateOptionsMenu();
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to move file", Toast.LENGTH_SHORT).show();
                        Log.e("ERROR", "Failed to move " + currentMoveFile.getAbsolutePath() + " to " + currentPath);
                    }
                }
                if (currentCopyFile != null) {
                    String name = currentCopyFile.getName();
                    File newFile = new File(currentPath + File.separator + name);
                    try {
                        Files.copy(currentCopyFile.toPath(), newFile.toPath());
                        this.currentCopyFile = null;
                        refresh();
                        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to copy file", Toast.LENGTH_SHORT).show();
                        Log.e("ERROR", "Failed to copy " + currentCopyFile.getAbsolutePath() + " to " + currentPath);
                        e.printStackTrace();
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    void refresh() {
        if (fileAdapter != null) {
            File root = new File(currentPath);
            List<File> filesAndFolder = Arrays.asList(Objects.requireNonNull(root.listFiles()));
            if (filesAndFolder.size() > 0) {
                TextView noFileText = findViewById(R.id.no_file_text);
                noFileText.setVisibility(View.INVISIBLE);
            }
            fileAdapter.setFiles(filesAndFolder);
            fileAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        TextView noFileText = findViewById(R.id.no_file_text);
        RecyclerView fileListView = findViewById(R.id.file_list_view);
        Bundle bundle = getIntent().getBundleExtra("COPY_MOVE");
        if (bundle != null) {
            Log.i("Bundle", "onCreate: " + bundle.getString("FILE_PATH"));
            if (bundle.containsKey("COPY")) {
                this.currentCopyFile = new File(bundle.getString("FILE_PATH"));
                this.currentMoveFile = null;
            } else if (bundle.containsKey("MOVE")) {
                this.currentMoveFile = new File(bundle.getString("FILE_PATH"));
                this.currentCopyFile = null;
            } else {
                this.currentMoveFile = null;
                this.currentCopyFile = null;
            }
        }

        currentPath = getIntent().getStringExtra("PATH");
        File root = new File(currentPath);
        try {
            List<File> filesAndFolder = Arrays.asList(Objects.requireNonNull(root.listFiles()));
            noFileText.setVisibility(View.INVISIBLE);
            fileAdapter = new FileAdapter(filesAndFolder, FileListActivity.this, this::refresh, this::setCurrentCopyFile, this::setCurrentMoveFile, this::finish);
            fileListView.setLayoutManager(new LinearLayoutManager(this));
            fileListView.setAdapter(fileAdapter);
            if (filesAndFolder.size() <= 0) {
                noFileText.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        Intent intent = new Intent(this, FileListActivity.class);
        if (currentMoveFile != null || currentCopyFile != null) {
            Bundle bundle = new Bundle();
            if (currentMoveFile != null) {
                bundle.putString("FILE_PATH", currentMoveFile.getAbsolutePath());
                bundle.putBoolean("MOVE", true);
            }
            if (currentCopyFile != null) {
                bundle.putString("FILE_PATH", currentCopyFile.getAbsolutePath());
                bundle.putBoolean("COPY", true);
            }
            intent.putExtra("COPY_MOVE", bundle);
        }
        intent.putExtra("PATH",new File(currentPath).getParentFile().getAbsolutePath());
        startActivity(intent);
        finish();
    }

    void setCurrentCopyFile(File file) {
        this.currentCopyFile = file;
        this.currentMoveFile = null;
        invalidateOptionsMenu();
    }

    void setCurrentMoveFile(File file) {
        this.currentMoveFile = file;
        this.currentCopyFile = null;
        invalidateOptionsMenu();
    }
}