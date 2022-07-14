package com.example.fileviewer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    List<File> files;
    Consumer<File> onCopy;
    Consumer<File> onMove;
    Runnable refresh;
    File currentCopyFile;
    File currentMoveFile;
    Runnable finish;

    public FileAdapter(List<File> files, Context context, Runnable refresh, Consumer<File> onCopy, Consumer<File> onMove, Runnable finish) {
        this.files = files;
        this.context = context;
        this.refresh = refresh;
        this.onCopy = onCopy;
        this.onMove = onMove;
        this.currentCopyFile = null;
        this.currentMoveFile = null;
        this.finish = finish;
    }

    public class FileViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        TextView fileName;
        ImageView icon;

        public FileViewHolder(@NonNull View fileView) {
            super(fileView);
            fileName = fileView.findViewById(R.id.file_name);
            icon = fileView.findViewById(R.id.file_icon);
            fileView.setOnCreateContextMenuListener(this);
            fileView.setOnClickListener(view -> {
                File currentFile = getFile(getAdapterPosition());
                if (currentFile.isDirectory()) {
                    Intent intent = new Intent(context, FileListActivity.class);
                    String path = currentFile.getAbsolutePath();
                    intent.putExtra("PATH", path);
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
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    finish.run();
                } else {
                    Uri file = Uri.fromFile(currentFile);
                    String extension = MimeTypeMap.getFileExtensionFromUrl(file.toString());
                    if (extension.equalsIgnoreCase("txt")) {
                        Intent intent = new Intent(context, TextReader.class);
                        intent.putExtra("PATH", currentFile.getAbsolutePath());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                    if (extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")) {
                        Intent intent = new Intent(context, ImageViewer.class);
                        intent.putExtra("PATH", currentFile.getAbsolutePath());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            if (!getFile(getAdapterPosition()).isDirectory()) {
                MenuItem copy = contextMenu.add(Menu.NONE, 1, 1, "Copy");
                MenuItem move = contextMenu.add(Menu.NONE, 2, 2, "Move");
                copy.setOnMenuItemClickListener(menuItemClickListener);
                move.setOnMenuItemClickListener(menuItemClickListener);
            }
            MenuItem rename = contextMenu.add(Menu.NONE, 3, 3, "Rename");
            MenuItem delete = contextMenu.add(Menu.NONE, 4, 4, "Delete");
            delete.setOnMenuItemClickListener(menuItemClickListener);
            rename.setOnMenuItemClickListener(menuItemClickListener);
        }

        private final MenuItem.OnMenuItemClickListener menuItemClickListener = item -> {
            int position = getAdapterPosition();
            switch (item.getItemId()) {
                case 1:
                    currentCopyFile = getFile(position);
                    currentMoveFile = null;
                    onCopy.accept(getFile(getAdapterPosition()));
                    Log.d("ContextMenu", "Action: " + "Copy" + " on " + getFile(getAdapterPosition()).getAbsolutePath());
                    break;

                case 2:
                    currentMoveFile = getFile(position);
                    currentCopyFile = null;
                    onMove.accept(getFile(getAdapterPosition()));
                    Log.d("ContextMenu", "Action: " + "Move on " + getFile(getAdapterPosition()).getAbsolutePath());
                    break;

                case 3:
                    AlertDialog.Builder builder3 = new AlertDialog.Builder(context);
                    EditText input = new EditText(context);
                    input.setHint("New name");
                    String currentFileName = getFile(position).getName();
                    input.setText(currentFileName);
                    input.setPadding(50, 40, 50, 40);
                    builder3.setTitle("Enter new name").setView(input).setPositiveButton("Confirm", (dialogInterface, i) -> {
                        String newName = input.getText().toString();
                        File currentFile = getFile(position);
                        File newFile = new File(currentFile.getParent() + File.separator + newName);
                        boolean success = currentFile.renameTo(newFile);
                        if (success) {
                            Toast.makeText(context, "Successfully renamed", Toast.LENGTH_SHORT).show();
                            if (refresh != null) {
                                refresh.run();
                            }
                        } else {
                            Toast.makeText(context, "Failed to rename", Toast.LENGTH_SHORT).show();
                        }
                    }).setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel()).show();
                    Log.d("ContextMenu", "Action: " + "Rename on " + getFile(getAdapterPosition()).getAbsolutePath());
                    break;

                case 4:
                    AlertDialog.Builder builder4 = new AlertDialog.Builder(context);
                    builder4.setTitle("Delete this item?").setPositiveButton("Yes", (dialogInterface, i) -> {
                        boolean success = getFile(position).delete();
                        if (success) {
                            Toast.makeText(context, "Successfully deleted", Toast.LENGTH_SHORT).show();
                            if (refresh != null) {
                                refresh.run();
                            }
                        } else {
                            Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }).setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel()).show();
                    Log.d("ContextMenu", "Action " + "Delete on " + getFile(getAdapterPosition()).getAbsolutePath());
                    break;
            }
            return true;
        };
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_item_layout, parent, false);
        return new FileViewHolder(view);
    }

    File getFile(int i) {
        return this.files.get(i);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        File file = files.get(position);
        ((FileViewHolder) holder).fileName.setText(file.getName());
        ((FileViewHolder) holder).icon.setImageResource(file.isDirectory() ? R.drawable.folder : R.drawable.file);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return this.files.size();
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }
}
