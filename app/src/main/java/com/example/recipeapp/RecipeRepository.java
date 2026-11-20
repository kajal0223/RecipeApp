package com.example.recipeapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//Repository managing recipes, folders, and kitchen pantry state across screens
public class RecipeRepository {
    private static final String PREFS_NAME = "recipe_app_prefs";
    private static final String KEY_RECIPES = "recipes_json";
    private static final String KEY_FOLDERS = "folders_json";
    private static final String KEY_FAVORITE_IDS = "favorite_recipe_ids";
    private static final String KEY_KITCHEN_INGREDIENTS = "kitchen_ingredients";
    private static final String KEY_SELECTED_INGREDIENTS = "selected_kitchen_ingredients";

    private static RecipeRepository instance;

    private final List<Recipe> recipes = new ArrayList<>();
    private final List<Folder> folders = new ArrayList<>();
    private final List<String> kitchenIngredients = new ArrayList<>();
    private final Set<String> selectedKitchenIngredients = new LinkedHashSet<>();

    private SharedPreferences prefs;
    private boolean initialized = false;

    private RecipeRepository() {
        //Starts empty so users add their own recipes, folders, and kitchen ingredients
    }

    public static synchronized RecipeRepository getInstance() {
        if (instance == null) {
            instance = new RecipeRepository();
        }
        return instance;
    }

    //Initializes SharedPreferences once using application context and loads persisted state
    public void init(Context context) {
        if (context == null) {
            return;
        }
        if (!initialized) {
            prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            initialized = true;
        }
        loadFromPrefs();
    }

    private void loadFromPrefs() {
        if (prefs == null) {
            return;
        }

        //Restore recipes first so folder recipe counts and favorites map cleanly
        recipes.clear();
        String recipesJson = prefs.getString(KEY_RECIPES, "[]");
        try {
            JSONArray recipeArray = new JSONArray(recipesJson);
            for (int i = 0; i < recipeArray.length(); i++) {
                JSONObject rObj = recipeArray.optJSONObject(i);
                if (rObj == null) {
                    continue;
                }
                String id = rObj.optString("id", "").trim();
                String title = rObj.optString("title", "").trim();
                if (id.isEmpty() || title.isEmpty()) {
                    continue;
                }

                String category = rObj.optString("category", "Dinner");
                int prepTimeMinutes = rObj.optInt("prepTimeMinutes", 15);
                int servings = rObj.optInt("servings", 1);
                int imageResId = rObj.optInt("imageResId", 0);
                String imageUri = rObj.optString("imageUri", "");
                boolean favorite = rObj.optBoolean("favorite", false);
                String notes = rObj.optString("notes", "");

                List<String> ingredientNames = new ArrayList<>();
                JSONArray ingArray = rObj.optJSONArray("ingredientNames");
                if (ingArray != null) {
                    for (int j = 0; j < ingArray.length(); j++) {
                        ingredientNames.add(ingArray.optString(j, ""));
                    }
                }

                List<String> instructions = new ArrayList<>();
                JSONArray instArray = rObj.optJSONArray("instructions");
                if (instArray != null) {
                    for (int j = 0; j < instArray.length(); j++) {
                        String step = instArray.optString(j, "").trim();
                        if (!step.isEmpty()) {
                            instructions.add(step);
                        }
                    }
                }

                recipes.add(new Recipe(
                        id, title, category, prepTimeMinutes, servings,
                        imageResId, imageUri, favorite, ingredientNames, instructions, notes
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Restore folders and their saved recipe IDs
        folders.clear();
        String foldersJson = prefs.getString(KEY_FOLDERS, "[]");
        try {
            JSONArray folderArray = new JSONArray(foldersJson);
            for (int i = 0; i < folderArray.length(); i++) {
                JSONObject obj = folderArray.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                String id = obj.optString("id", "").trim();
                String name = obj.optString("name", "").trim();
                if (id.isEmpty() || name.isEmpty()) {
                    continue;
                }

                List<String> recipeIds = new ArrayList<>();
                JSONArray idsArray = obj.optJSONArray("recipeIds");
                if (idsArray != null) {
                    for (int j = 0; j < idsArray.length(); j++) {
                        String recipeId = idsArray.optString(j, "").trim();
                        if (!recipeId.isEmpty() && !recipeIds.contains(recipeId)) {
                            recipeIds.add(recipeId);
                        }
                    }
                }
                folders.add(new Folder(id, name, recipeIds));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Restore kitchen ingredients and active selection chips
        kitchenIngredients.clear();
        Set<String> savedIngredients = prefs.getStringSet(KEY_KITCHEN_INGREDIENTS, null);
        if (savedIngredients != null) {
            for (String raw : savedIngredients) {
                String normalized = Recipe.normalizeIngredient(raw);
                if (!normalized.isEmpty() && !kitchenIngredients.contains(normalized)) {
                    kitchenIngredients.add(normalized);
                }
            }
        }

        selectedKitchenIngredients.clear();
        Set<String> savedSelected = prefs.getStringSet(KEY_SELECTED_INGREDIENTS, null);
        if (savedSelected != null) {
            for (String raw : savedSelected) {
                String normalized = Recipe.normalizeIngredient(raw);
                if (!normalized.isEmpty() && kitchenIngredients.contains(normalized)) {
                    selectedKitchenIngredients.add(normalized);
                }
            }
        }

        //Apply saved favorite flags to loaded recipes
        applySavedFavoritesToRecipes();
    }

    private void applySavedFavoritesToRecipes() {
        if (prefs == null) {
            return;
        }
        Set<String> favoriteIds = prefs.getStringSet(KEY_FAVORITE_IDS, new HashSet<String>());
        if (favoriteIds == null) {
            return;
        }
        for (Recipe recipe : recipes) {
            if (favoriteIds.contains(recipe.getId())) {
                recipe.setFavorite(true);
            }
        }
    }

    public void saveToPrefs() {
        if (prefs == null) {
            return;
        }

        JSONArray recipeArray = new JSONArray();
        for (Recipe recipe : recipes) {
            try {
                JSONObject rObj = new JSONObject();
                rObj.put("id", recipe.getId());
                rObj.put("title", recipe.getTitle());
                rObj.put("category", recipe.getCategory());
                rObj.put("prepTimeMinutes", recipe.getPrepTimeMinutes());
                rObj.put("servings", recipe.getServings());
                rObj.put("imageResId", recipe.getImageResId());
                rObj.put("imageUri", recipe.getImageUri());
                rObj.put("favorite", recipe.isFavorite());
                rObj.put("notes", recipe.getNotes());

                JSONArray ingArray = new JSONArray();
                for (String ing : recipe.getIngredientNames()) {
                    ingArray.put(ing);
                }
                rObj.put("ingredientNames", ingArray);

                JSONArray instArray = new JSONArray();
                for (String step : recipe.getInstructions()) {
                    instArray.put(step);
                }
                rObj.put("instructions", instArray);

                recipeArray.put(rObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONArray folderArray = new JSONArray();
        for (Folder folder : folders) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", folder.getId());
                obj.put("name", folder.getName());

                JSONArray idsArray = new JSONArray();
                for (String recipeId : folder.getRecipeIds()) {
                    idsArray.put(recipeId);
                }
                obj.put("recipeIds", idsArray);
                folderArray.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Set<String> favoriteIds = collectFavoriteRecipeIds();

        prefs.edit()
                .putString(KEY_RECIPES, recipeArray.toString())
                .putString(KEY_FOLDERS, folderArray.toString())
                .putStringSet(KEY_FAVORITE_IDS, favoriteIds)
                .putStringSet(KEY_KITCHEN_INGREDIENTS, new HashSet<>(kitchenIngredients))
                .putStringSet(KEY_SELECTED_INGREDIENTS, new LinkedHashSet<>(selectedKitchenIngredients))
                .apply();
    }

    //Adds, updates, deletes, and retrieves recipes
    public boolean addRecipe(Recipe recipe) {
        if (recipe == null) {
            return false;
        }
        for (Recipe existing : recipes) {
            if (existing.getId().equals(recipe.getId())) {
                return false;
            }
        }
        if (prefs != null) {
            Set<String> favoriteIds = prefs.getStringSet(KEY_FAVORITE_IDS, new HashSet<String>());
            if (favoriteIds != null && favoriteIds.contains(recipe.getId())) {
                recipe.setFavorite(true);
            }
        }
        recipes.add(recipe);
        if (recipe.isFavorite()) {
            syncRecipeWithFavoritesFolder(recipe.getId(), true);
        }
        saveToPrefs();
        return true;
    }

    public boolean updateRecipe(Recipe updated) {
        if (updated == null || updated.getId() == null) {
            return false;
        }
        for (int i = 0; i < recipes.size(); i++) {
            Recipe existing = recipes.get(i);
            if (existing.getId().equals(updated.getId())) {
                updated.setFavorite(existing.isFavorite());
                recipes.set(i, updated);
                syncRecipeWithFavoritesFolder(updated.getId(), updated.isFavorite());
                saveToPrefs();
                return true;
            }
        }
        return false;
    }

    public boolean deleteRecipe(String recipeId) {
        if (recipeId == null || recipeId.trim().isEmpty()) {
            return false;
        }
        boolean removed = false;
        for (int i = recipes.size() - 1; i >= 0; i--) {
            if (recipes.get(i).getId().equals(recipeId)) {
                recipes.remove(i);
                removed = true;
            }
        }
        if (removed) {
            for (Folder folder : folders) {
                folder.removeRecipeId(recipeId);
            }
            syncRecipeWithFavoritesFolder(recipeId, false);
            saveToPrefs();
        }
        return removed;
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
        if (selectedKitchenIngredients.isEmpty()) {
            return new ArrayList<>();
        }
        List<Recipe> matching = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe.countMatchingIngredients(selectedKitchenIngredients) > 0) {
                matching.add(recipe);
            }
        }
        Collections.sort(matching, new Comparator<Recipe>() {
            @Override
            public int compare(Recipe a, Recipe b) {
                int matchA = a.countMatchingIngredients(selectedKitchenIngredients);
                int matchB = b.countMatchingIngredients(selectedKitchenIngredients);
                return Integer.compare(matchB, matchA);
            }
        });
        return matching;
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
                saveToPrefs();
                return true;
            }
        }
        kitchenIngredients.add(normalized);
        selectedKitchenIngredients.add(normalized);
        saveToPrefs();
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
        saveToPrefs();
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
        boolean removed = kitchenIngredients.remove(normalized);
        if (removed) {
            saveToPrefs();
        }
        return removed;
    }

    //Manages saved recipe folders and folder contents
    public List<Folder> getFolders() {
        return folders;
    }

    public static boolean isFavoritesFolder(Folder folder) {
        return folder != null
                && folder.getName() != null
                && folder.getName().trim().equalsIgnoreCase("favorites");
    }

    private Folder getOrCreateFavoritesFolder() {
        for (Folder folder : folders) {
            if (isFavoritesFolder(folder)) {
                return folder;
            }
        }
        Folder created = new Folder(UUID.randomUUID().toString(), "Favorites", new ArrayList<String>());
        folders.add(created);
        return created;
    }

    private Set<String> collectFavoriteRecipeIds() {
        Set<String> favoriteIds = new HashSet<>();
        for (Recipe recipe : recipes) {
            if (recipe.isFavorite()) {
                favoriteIds.add(recipe.getId());
            }
        }
        return favoriteIds;
    }

    public Folder createFolder(String rawName) {
        if (!Folder.isValidFolderName(rawName, folders)) {
            return null;
        }
        Folder created = new Folder(UUID.randomUUID().toString(), rawName.trim(), new ArrayList<String>());
        folders.add(created);
        saveToPrefs();
        return created;
    }

    public boolean addRecipeToFolder(Folder folder, String recipeId) {
        if (folder == null || !folders.contains(folder)) {
            return false;
        }
        boolean added = folder.addRecipeId(recipeId);
        if (added) {
            saveToPrefs();
        }
        return added;
    }

    public boolean removeRecipeFromFolder(Folder folder, String recipeId) {
        if (folder == null || !folders.contains(folder)) {
            return false;
        }
        boolean removed = folder.removeRecipeId(recipeId);
        if (removed) {
            saveToPrefs();
        }
        return removed;
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
        saveToPrefs();
    }

    private void syncRecipeWithFavoritesFolder(String recipeId, boolean shouldBeInFavorites) {
        if (recipeId == null || recipeId.trim().isEmpty()) {
            return;
        }
        Set<String> favoriteIds = collectFavoriteRecipeIds();
        if (shouldBeInFavorites || !favoriteIds.isEmpty()) {
            Folder favoritesFolder = getOrCreateFavoritesFolder();
            if (shouldBeInFavorites) {
                favoritesFolder.addRecipeId(recipeId);
            } else {
                favoritesFolder.removeRecipeId(recipeId);
            }
        }

        if (favoriteIds.isEmpty()) {
            for (int i = folders.size() - 1; i >= 0; i--) {
                if (isFavoritesFolder(folders.get(i))) {
                    folders.remove(i);
                }
            }
        }
    }

    //Renames existing folder if non-blank, not Favorites, and not already used by another folder
    public boolean renameFolder(Folder folder, String rawNewName) {
        if (folder == null || isFavoritesFolder(folder) || rawNewName == null) {
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
        saveToPrefs();
        return true;
    }

    //Deletes folder from repository
    public void deleteFolder(Folder folder) {
        if (folder == null) {
            return;
        }
        if (folders.remove(folder)) {
            saveToPrefs();
        }
    }
}