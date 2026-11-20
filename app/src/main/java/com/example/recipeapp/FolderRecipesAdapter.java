package com.example.recipeapp;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

//Adapter displaying recipes inside selected Saved folder
public class FolderRecipesAdapter extends RecyclerView.Adapter<FolderRecipesAdapter.FolderRecipeViewHolder> {

    public interface OnFolderRecipeListener {
        void onRecipeClick(Recipe recipe);
        void onFavoriteClick(Recipe recipe);
    }

    private final List<Recipe> recipes = new ArrayList<>();
    private final OnFolderRecipeListener listener;

    public FolderRecipesAdapter(OnFolderRecipeListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Recipe> updated) {
        recipes.clear();
        if (updated != null) {
            recipes.addAll(updated);
        }
        notifyDataSetChanged();
    }

    //Adapter lifecycle and item binding
    @NonNull
    @Override
    public FolderRecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_card, parent, false);
        return new FolderRecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderRecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe, listener);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    //ViewHolder for rendering recipe cards inside folder
    static class FolderRecipeViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumb;
        private final TextView tvTitle;
        private final TextView tvSubtitle;
        private final TextView tvCategory;
        private final TextView tvPrepTime;
        private final ImageButton btnFavorite;

        FolderRecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_recipe_thumb);
            tvTitle = itemView.findViewById(R.id.tv_recipe_title);
            tvSubtitle = itemView.findViewById(R.id.tv_recipe_subtitle);
            tvCategory = itemView.findViewById(R.id.tv_recipe_category);
            tvPrepTime = itemView.findViewById(R.id.tv_recipe_prep_time);
            btnFavorite = itemView.findViewById(R.id.btn_favorite_heart);
        }

        //Binds recipe text, favorite heart state, and row/heart click listeners
        void bind(Recipe recipe, OnFolderRecipeListener listener) {
            tvTitle.setText(recipe.getTitle());

            List<String> ings = recipe.getIngredientNames();
            if (tvSubtitle != null) {
                tvSubtitle.setVisibility(View.GONE);
            }

            String category = recipe.getCategory();
            if (category != null && !category.trim().isEmpty()) {
                tvCategory.setVisibility(View.VISIBLE);
                tvCategory.setText(category);
            } else {
                tvCategory.setVisibility(View.GONE);
            }

            tvPrepTime.setText(recipe.getPrepTimeMinutes() + " min");
            bindThumbnail(ivThumb, recipe);

            if (recipe.isFavorite()) {
                btnFavorite.setImageResource(R.drawable.ic_heart_filled);
                btnFavorite.setColorFilter(itemView.getContext().getColor(R.color.orange_dark));
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                btnFavorite.setColorFilter(itemView.getContext().getColor(R.color.text_light));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeClick(recipe);
                }
            });

            btnFavorite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClick(recipe);
                }
            });
        }

        //Loads persisted image URI with drawable and gallery fallbacks
        private static void bindThumbnail(ImageView imageView, Recipe recipe) {
            String uriStr = recipe.getImageUri();
            if (uriStr != null && !uriStr.isEmpty()) {
                try {
                    imageView.setImageURI(Uri.parse(uriStr));
                    return;
                } catch (Exception ignored) {
                }
            }
            imageView.setImageURI(null);
            if (recipe.getImageResId() != 0) {
                imageView.setImageResource(recipe.getImageResId());
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }
}
