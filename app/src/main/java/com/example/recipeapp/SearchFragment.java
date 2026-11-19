package com.example.recipeapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Locale;

//Fragment handling name search, kitchen ingredient chips, and recipe match results
public class SearchFragment extends Fragment {

    private EditText etSearchBar;
    private TextView tvKitchenLabel;
    private ChipGroup cgPantryIngredients;
    private MaterialButton chipAddIngredient;
    private TextView tvResultsHeader;
    private TextView tvSearchEmpty;
    private RecyclerView rvSearchResults;
    private TextView tvKitchenHint;

    private RecipeRepository repository;
    private SearchRecipeAdapter adapter;

    //Inflates layout and initializes views, adapter, and input listeners
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RecipeRepository.getInstance();

        etSearchBar = view.findViewById(R.id.et_search_bar);
        cgPantryIngredients = view.findViewById(R.id.cg_pantry_ingredients);
        chipAddIngredient = view.findViewById(R.id.chip_add_ingredient);
        tvResultsHeader = view.findViewById(R.id.tv_results_header);
        tvSearchEmpty = view.findViewById(R.id.tv_search_empty);
        rvSearchResults = view.findViewById(R.id.rv_search_results);
        tvKitchenHint = view.findViewById(R.id.tv_kitchen_hint);

        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SearchRecipeAdapter(repository, new SearchRecipeAdapter.OnRecipeActionListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Toast.makeText(requireContext(), recipe.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFavoriteClick(Recipe recipe) {
                repository.toggleFavorite(recipe);
                refreshResults();
            }
        });
        rvSearchResults.setAdapter(adapter);

        chipAddIngredient.setOnClickListener(v -> showAddIngredientDialog());

        etSearchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshResults();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        renderIngredientChips();
        refreshResults();
    }

    //Refreshes chips and results when returning to fragment
    @Override
    public void onResume() {
        super.onResume();
        if (repository != null) {
            renderIngredientChips();
            refreshResults();
        }
    }

    //Displays dialogs for adding and removing kitchen pantry ingredients
    private void showAddIngredientDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("e.g., Eggs, Rice, Tomatoes");

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Kitchen Ingredient")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String raw = input.getText().toString();
                    if (!repository.addKitchenIngredient(raw)) {
                        Toast.makeText(requireContext(), "Enter an ingredient name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    renderIngredientChips();
                    refreshResults();
                    Toast.makeText(requireContext(), "Added ingredient", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRemoveIngredientDialog(String ingredient) {
        String formatted = ingredient.substring(0, 1).toUpperCase(Locale.US) + ingredient.substring(1);

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Ingredient")
                .setMessage("Remove \"" + formatted + "\" from your kitchen?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    if (repository.removeKitchenIngredient(ingredient)) {
                        renderIngredientChips();
                        refreshResults();
                        Toast.makeText(requireContext(), "Removed " + formatted, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //Rebuilds dynamic ingredient chips while preserving add button
    private void renderIngredientChips() {
        if (cgPantryIngredients.getChildCount() > 1) {
            cgPantryIngredients.removeViews(1, cgPantryIngredients.getChildCount() - 1);
        }

        List<String> ingredients = repository.getKitchenIngredients();
        for (String ingredient : ingredients) {
            Chip chip = new Chip(requireContext());
            String formatted = ingredient.substring(0, 1).toUpperCase(Locale.US) + ingredient.substring(1);
            chip.setText("+ " + formatted);
            chip.setCheckable(true);
            chip.setChecked(repository.isIngredientSelected(ingredient));
            chip.setOnClickListener(v -> {
                repository.toggleSelectedIngredient(ingredient);
                refreshResults();
            });
            chip.setOnLongClickListener(v -> {
                showRemoveIngredientDialog(ingredient);
                return true;
            });
            cgPantryIngredients.addView(chip);
        }
    }

    //Updates displayed recipes, mode header, and empty-state visibility
    private void refreshResults() {
        String query = etSearchBar.getText() != null ? etSearchBar.getText().toString().trim() : "";
        boolean isSearchingByName = !query.isEmpty();

        boolean hasKitchenIngredients = !repository.getKitchenIngredients().isEmpty();
        tvKitchenHint.setVisibility(!isSearchingByName && hasKitchenIngredients ? View.VISIBLE : View.GONE);
        cgPantryIngredients.setVisibility(isSearchingByName ? View.GONE : View.VISIBLE);
        tvResultsHeader.setText(isSearchingByName ? "Results" : "Recipes You Can Make");

        List<Recipe> results = isSearchingByName
                ? repository.searchByTitle(query)
                : repository.getRecipesSortedByIngredientMatch();

        adapter.updateData(results, repository.getSelectedKitchenIngredients(), isSearchingByName);

        if (results.isEmpty()) {
            tvSearchEmpty.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
            if (repository.getAllRecipes().isEmpty()) {
                tvSearchEmpty.setText("No recipes added yet. Add a recipe to search or match ingredients.");
            } else if (isSearchingByName) {
                tvSearchEmpty.setText("No recipes match \"" + query + "\".");
            } else {
                tvSearchEmpty.setText("Select kitchen ingredients above to match recipes.");
            }
        } else {
            tvSearchEmpty.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
        }
    }
}