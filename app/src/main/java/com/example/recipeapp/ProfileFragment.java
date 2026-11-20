package com.example.recipeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

//Fragment displaying profile summary stats and navigation to My Recipes and utility rows
public class ProfileFragment extends Fragment {

    private RecipeRepository repository;
    private TextView tvStatRecipesCount;
    private TextView tvStatShoppingCount;
    private TextView tvStatFoldersCount;
    private TextView tvProfileSubtitle;

    //Fragment lifecycle and view setup
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = RecipeRepository.getInstance();
        repository.init(requireContext());

        tvStatRecipesCount = view.findViewById(R.id.tv_stat_recipes_count);
        tvStatShoppingCount = view.findViewById(R.id.tv_stat_shopping_count);
        tvStatFoldersCount = view.findViewById(R.id.tv_stat_folders_count);
        tvProfileSubtitle = view.findViewById(R.id.tv_profile_subtitle);

        //Wire navigation to My Recipes and placeholder utility rows
        View rowMyRecipes = view.findViewById(R.id.row_my_recipes);
        if (rowMyRecipes != null) {
            rowMyRecipes.setOnClickListener(v -> openMyRecipes());
        }

        View statRecipes = view.findViewById(R.id.stat_recipes);
        if (statRecipes != null) {
            statRecipes.setOnClickListener(v -> openMyRecipes());
        }

        View rowShoppingList = view.findViewById(R.id.row_shopping_list);
        if (rowShoppingList != null) {
            rowShoppingList.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Shopping List coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        View rowPrepCalculator = view.findViewById(R.id.row_prep_calculator);
        if (rowPrepCalculator != null) {
            rowPrepCalculator.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Prep Calculator coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        View rowSettings = view.findViewById(R.id.row_settings);
        if (rowSettings != null) {
            rowSettings.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Settings coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        refreshProfileStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfileStats();
    }

    //Stats and screen navigation helpers
    private void refreshProfileStats() {
        if (repository == null) {
            return;
        }
        int recipeCount = repository.getAllRecipes().size();
        int folderCount = repository.getFolders().size();
        int shoppingCount = repository.getKitchenIngredients().size();

        if (tvStatRecipesCount != null) {
            tvStatRecipesCount.setText(String.valueOf(recipeCount));
        }
        if (tvStatFoldersCount != null) {
            tvStatFoldersCount.setText(String.valueOf(folderCount));
        }
        if (tvStatShoppingCount != null) {
            tvStatShoppingCount.setText(String.valueOf(shoppingCount));
        }
        if (tvProfileSubtitle != null) {
            String recipeWord = recipeCount == 1 ? "Recipe" : "Recipes";
            tvProfileSubtitle.setText("Personal Kitchen \u00B7 " + recipeCount + " " + recipeWord);
        }
    }

    private void openMyRecipes() {
        if (!isAdded()) {
            return;
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new MyRecipesFragment())
                .addToBackStack("my_recipes")
                .commit();
    }
}
