package com.example.recipeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

//RecyclerView adapter displaying saved recipe folders and their recipe counts
public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
    }

    private final List<Folder> folders = new ArrayList<>();
    private final OnFolderClickListener listener;

    public FolderAdapter(OnFolderClickListener listener) {
        this.listener = listener;
    }

    //Replaces displayed folders and refreshes list
    public void setFolders(List<Folder> newFolders) {
        this.folders.clear();
        if (newFolders != null) {
            this.folders.addAll(newFolders);
        }
        notifyDataSetChanged();
    }

    //Creates and binds folder row view holders
    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        holder.bind(folders.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    //Holds views for individual folder card
    static class FolderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFolderName;
        private final TextView tvFolderCount;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
            tvFolderCount = itemView.findViewById(R.id.tv_folder_count);
        }

        void bind(Folder folder, OnFolderClickListener listener) {
            tvFolderName.setText(folder.getName());
            tvFolderCount.setText(folder.getRecipeCountLabel());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFolderClick(folder);
                }
            });
        }
    }
}
