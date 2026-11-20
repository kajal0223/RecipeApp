package com.example.recipeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

//RecyclerView adapter displaying user recipes with Edit and Delete actions
public class MyRecipesAdapter extends RecyclerView.Adapter<MyRecipesAdapter.RecipeViewHolder> {

    public interface OnRecipeActionListener {
        void onEditRecipe(Recipe recipe);
        void onDeleteRecipe(Recipe recipe);
    }

    private final List<Recipe> items = new ArrayList<>();
    private final OnRecipeActionListener listener;

    public MyRecipesAdapter(OnRecipeActionListener listener) {
        this.listener = listener;
    }

    //Replaces current adapter data and refreshes list
    public void submitList(List<Recipe> recipes) {
        items.clear();
        if (recipes != null) {
            items.addAll(recipes);
        }
        notifyDataSetChanged();
    }

    //Adapter lifecycle methods
    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = items.get(position);
        holder.tvTitle.setText(recipe.getTitle());

        //Format category, prep time, and servings summary
        String servingWord = recipe.getServings() == 1 ? "serving" : "servings";
        String meta = recipe.getCategory()
                + " \u00B7 " + recipe.getPrepTimeMinutes() + " min"
                + " \u00B7 " + recipe.getServings() + " " + servingWord;
        holder.tvMeta.setText(meta);

        //Display comma-separated ingredient summary
        List<String> ingredients = recipe.getIngredientNames();
        if (ingredients == null || ingredients.isEmpty()) {
            holder.tvIngredients.setText("Ingredients: none listed");
        } else {
            holder.tvIngredients.setText("Ingredients: " + joinComma(ingredients));
        }

        //Wire row action buttons
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditRecipe(recipe);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteRecipe(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    //Joins list entries with comma separators
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

    //ViewHolder caching recipe row views
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvMeta;
        final TextView tvIngredients;
        final ImageButton btnEdit;
        final ImageButton btnDelete;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_recipe_title);
            tvMeta = itemView.findViewById(R.id.tv_item_recipe_meta);
            tvIngredients = itemView.findViewById(R.id.tv_item_recipe_ingredients);
            btnEdit = itemView.findViewById(R.id.btn_edit_recipe);
            btnDelete = itemView.findViewById(R.id.btn_delete_recipe);
        }
    }
}
