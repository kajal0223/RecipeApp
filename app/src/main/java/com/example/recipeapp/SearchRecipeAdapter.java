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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//RecyclerView adapter for recipe cards for ingredient matching and name search results
public class SearchRecipeAdapter extends RecyclerView.Adapter<SearchRecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeActionListener {
        void onRecipeClick(Recipe recipe);
        void onFavoriteClick(Recipe recipe);
    }

    private final RecipeRepository repository;
    private final OnRecipeActionListener listener;
    private final List<Recipe> recipes = new ArrayList<>();
    private Set<String> selectedIngredients = new HashSet<>();
    private boolean isSearchingByName = false;

    public SearchRecipeAdapter(RecipeRepository repository, OnRecipeActionListener listener) {
        this.repository = repository;
        this.listener = listener;
    }

    //Refreshes displayed recipes, selected pantry ingredients, and active search mode
    public void updateData(List<Recipe> newRecipes, Set<String> selectedIngredients, boolean isSearchingByName) {
        this.recipes.clear();
        if (newRecipes != null) {
            this.recipes.addAll(newRecipes);
        }
        this.selectedIngredients = selectedIngredients != null ? new HashSet<>(selectedIngredients) : new HashSet<>();
        this.isSearchingByName = isSearchingByName;
        notifyDataSetChanged();
    }

    //Creates and binds recipe view holders
    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe, selectedIngredients, isSearchingByName, listener);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    //Holds card views and populates recipe details, ingredient match text, and click actions
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumb;
        private final TextView tvTitle;
        private final TextView tvIngredientMatch;
        private final TextView tvCategory;
        private final TextView tvPrepTime;
        private final ImageButton btnFavorite;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_recipe_thumb);
            tvTitle = itemView.findViewById(R.id.tv_recipe_title);
            tvIngredientMatch = itemView.findViewById(R.id.tv_recipe_subtitle);
            tvCategory = itemView.findViewById(R.id.tv_recipe_category);
            tvPrepTime = itemView.findViewById(R.id.tv_recipe_prep_time);
            btnFavorite = itemView.findViewById(R.id.btn_favorite_heart);
        }

        void bind(Recipe recipe, Set<String> selectedIngredients, boolean isSearchingByName,
                  OnRecipeActionListener listener) {
            tvTitle.setText(recipe.getTitle());

            bindThumbnail(ivThumb, recipe);

            if (!isSearchingByName) {
                tvIngredientMatch.setVisibility(View.VISIBLE);
                tvIngredientMatch.setText(recipe.formatIngredientMatchText(selectedIngredients));
            } else {
                List<String> ings = recipe.getIngredientNames();
                tvIngredientMatch.setVisibility(View.VISIBLE);
                tvIngredientMatch.setText(ings.isEmpty()
                        ? "No ingredients listed"
                        : "Ingredients: " + joinComma(ings));
            }

            String category = recipe.getCategory();
            if (category != null && !category.trim().isEmpty()) {
                tvCategory.setVisibility(View.VISIBLE);
                tvCategory.setText(category);
            } else {
                tvCategory.setVisibility(View.GONE);
            }

            tvPrepTime.setText(recipe.getPrepTimeMinutes() + " min");

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

        private static String joinComma(List<String> items) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(items.get(i));
            }
            return sb.toString();
        }

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
