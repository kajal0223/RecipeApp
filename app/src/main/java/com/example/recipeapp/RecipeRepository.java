package com.example.recipeapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

//Repository managing recipes, folders, and kitchen pantry state across screens
public class RecipeRepository {
    private static RecipeRepository instance;

    private final List<Recipe> recipes = new ArrayList<>();
    private final List<Folder> folders = new ArrayList<>();
    private final List<String> kitchenIngredients = new ArrayList<>();
    private final Set<String> selectedKitchenIngredients = new LinkedHashSet<>();

    private RecipeRepository() {
        //Starts empty so users add their own recipes, folders, and kitchen ingredients
    }

    public static synchronized RecipeRepository getInstance() {
        if (instance == null) {
            instance = new RecipeRepository();
        }
        return instance;
    }

    //Adds and retrieves recipes
    public boolean addRecipe(Recipe recipe) {
        if (recipe == null) {
            return false;
        }
        for (Recipe existing : recipes) {
            if (existing.getId().equals(recipe.getId())) {
                return false;
            }
        }
        recipes.add(recipe);
        if (recipe.isFavorite()) {
            syncRecipeWithFavoritesFolder(recipe.getId(), true);
        }
        return true;
    }

    public List<Recipe> getAllRecipes() {
        return recipes;
    }

    //Searches and ranks recipes by title query or ingredient match count
    public List<Recipe> searchByTitle(String query) {
        List<Recipe> matches = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe.matchesTitleQuery(query)) {
                matches.add(recipe);
            }
        }
        return matches;
    }

    public List<Recipe> getRecipesSortedByIngredientMatch() {
        List<Recipe> ranked = new ArrayList<>(recipes);
        Collections.sort(ranked, new Comparator<Recipe>() {
            @Override
            public int compare(Recipe a, Recipe b) {
                int matchA = a.countMatchingIngredients(selectedKitchenIngredients);
                int matchB = b.countMatchingIngredients(selectedKitchenIngredients);
                return Integer.compare(matchB, matchA);
            }
        });
        return ranked;
    }

    //Manages kitchen pantry ingredients and active selection chips
    public boolean addKitchenIngredient(String rawName) {
        String normalized = Recipe.normalizeIngredient(rawName);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String existing : kitchenIngredients) {
            if (existing.equalsIgnoreCase(normalized)) {
                selectedKitchenIngredients.add(existing);
                return true;
            }
        }
        kitchenIngredients.add(normalized);
        selectedKitchenIngredients.add(normalized);
        return true;
    }

    public void toggleSelectedIngredient(String ingredient) {
        String normalized = Recipe.normalizeIngredient(ingredient);
        if (normalized.isEmpty()) {
            return;
        }
        if (selectedKitchenIngredients.contains(normalized)) {
            selectedKitchenIngredients.remove(normalized);
        } else {
            selectedKitchenIngredients.add(normalized);
        }
    }

    public boolean isIngredientSelected(String ingredient) {
        return selectedKitchenIngredients.contains(Recipe.normalizeIngredient(ingredient));
    }

    public List<String> getKitchenIngredients() {
        return kitchenIngredients;
    }

    public Set<String> getSelectedKitchenIngredients() {
        return new HashSet<>(selectedKitchenIngredients);
    }

    public boolean removeKitchenIngredient(String rawName) {
        String normalized = Recipe.normalizeIngredient(rawName);
        if (normalized.isEmpty()) {
            return false;
        }
        selectedKitchenIngredients.remove(normalized);
        return kitchenIngredients.remove(normalized);
    }

    //Manages saved recipe folders and folder contents
    public List<Folder> getFolders() {
        return folders;
    }

    public Folder createFolder(String rawName) {
        if (!Folder.isValidFolderName(rawName, folders)) {
            return null;
        }
        Folder created = new Folder(UUID.randomUUID().toString(), rawName.trim(), new ArrayList<String>());
        folders.add(created);
        return created;
    }

    public List<Recipe> getRecipesInFolder(Folder folder) {
        List<Recipe> result = new ArrayList<>();
        if (folder == null) {
            return result;
        }
        for (String recipeId : folder.getRecipeIds()) {
            for (Recipe recipe : recipes) {
                if (recipe.getId().equals(recipeId)) {
                    result.add(recipe);
                    break;
                }
            }
        }
        return result;
    }

    //Toggles favorite status and keeps the Favorites folder in sync
    public void toggleFavorite(Recipe recipe) {
        if (recipe == null) {
            return;
        }
        boolean nextFavorite = !recipe.isFavorite();
        recipe.setFavorite(nextFavorite);
        syncRecipeWithFavoritesFolder(recipe.getId(), nextFavorite);
    }

    private void syncRecipeWithFavoritesFolder(String recipeId, boolean shouldBeInFavorites) {
        for (Folder folder : folders) {
            if (folder.getName().toLowerCase(Locale.US).equals("favorites")) {
                if (shouldBeInFavorites) {
                    folder.addRecipeId(recipeId);
                } else {
                    folder.removeRecipeId(recipeId);
                }
                break;
            }
        }
    }

    //Renames existing folder if non-blank and not already used by another folder
    public boolean renameFolder(Folder folder, String rawNewName) {
        if (folder == null || rawNewName == null) {
            return false;
        }
        String trimmed = rawNewName.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        for (Folder existing : folders) {
            if (existing != folder && existing.getName().equalsIgnoreCase(trimmed)) {
                return false;
            }
        }
        folder.setName(trimmed);
        return true;
    }

    //Deletes folder from repository
    public void deleteFolder(Folder folder) {
        if (folder == null) {
            return;
        }
        folders.remove(folder);
    }
}
