package com.example.recipeapp;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//Reusable dialog that creates or edits a Recipe and saves it to RecipeRepository
public class AddRecipeDialog {

    public interface OnRecipeSavedListener {
        void onRecipeSaved(Recipe recipe);
    }

    public interface ImagePickerHost {
        void onPickImageRequested(OnImagePickedCallback callback);
    }

    public interface OnImagePickedCallback {
        void onImagePicked(String imageUriString);
    }

    private static final String[] CATEGORIES = {
            "Breakfast", "Lunch", "Dinner", "Dessert"
    };

    private static OnImagePickedCallback activeImageCallback;

    //Called by hosting Fragment/Activity when image picker finishes
    public static void deliverPickedImage(String imageUriString) {
        if (activeImageCallback != null) {
            activeImageCallback.onImagePicked(imageUriString);
        }
    }

    public static void show(Context context, RecipeRepository repository, OnRecipeSavedListener listener) {
        show(context, repository, null, null, listener);
    }

    public static void show(Context context, RecipeRepository repository,
                            @Nullable Recipe existingRecipe, OnRecipeSavedListener listener) {
        show(context, repository, existingRecipe, null, listener);
    }

    //Builds and displays the Add/Edit Recipe dialog with validation and persistence
    public static void show(Context context, RecipeRepository repository,
                            @Nullable Recipe existingRecipe,
                            @Nullable ImagePickerHost imagePickerHost,
                            OnRecipeSavedListener listener) {
        if (context == null || repository == null) {
            return;
        }

        boolean isEditing = existingRecipe != null;
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_recipe, null, false);

        ImageView ivPreview = dialogView.findViewById(R.id.iv_recipe_preview);
        Button btnPickImage = dialogView.findViewById(R.id.btn_pick_recipe_image);
        EditText etTitle = dialogView.findViewById(R.id.et_recipe_title);
        Spinner spCategory = dialogView.findViewById(R.id.sp_recipe_category);
        EditText etPrepTime = dialogView.findViewById(R.id.et_recipe_prep_time);
        EditText etServings = dialogView.findViewById(R.id.et_recipe_servings);
        EditText etIngredients = dialogView.findViewById(R.id.et_recipe_ingredients);
        EditText etInstructions = dialogView.findViewById(R.id.et_recipe_instructions);
        EditText etNotes = dialogView.findViewById(R.id.et_recipe_notes);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                CATEGORIES
        );
        spCategory.setAdapter(categoryAdapter);

        //Configure image preview and picker callback
        final String[] selectedImageUri = {""};
        if (isEditing && existingRecipe.getImageUri() != null && !existingRecipe.getImageUri().isEmpty()) {
            selectedImageUri[0] = existingRecipe.getImageUri();
            bindImagePreview(ivPreview, selectedImageUri[0], existingRecipe.getImageResId());
        } else if (isEditing && existingRecipe.getImageResId() != 0) {
            ivPreview.setImageResource(existingRecipe.getImageResId());
        } else {
            ivPreview.setImageResource(R.drawable.ic_recipes);
        }

        if (btnPickImage != null) {
            if (imagePickerHost != null) {
                btnPickImage.setOnClickListener(v -> {
                    activeImageCallback = pickedUri -> {
                        if (pickedUri != null && !pickedUri.isEmpty()) {
                            selectedImageUri[0] = pickedUri;
                            bindImagePreview(ivPreview, pickedUri,
                                    isEditing ? existingRecipe.getImageResId() : 0);
                        }
                    };
                    imagePickerHost.onPickImageRequested(activeImageCallback);
                });
            } else {
                btnPickImage.setVisibility(View.GONE);
            }
        }

        if (isEditing) {
            etTitle.setText(existingRecipe.getTitle());
            etPrepTime.setText(String.valueOf(existingRecipe.getPrepTimeMinutes()));
            etServings.setText(String.valueOf(existingRecipe.getServings()));
            etIngredients.setText(joinLines(existingRecipe.getIngredientNames()));
            etInstructions.setText(joinLines(existingRecipe.getInstructions()));
            etNotes.setText(existingRecipe.getNotes());

            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equalsIgnoreCase(existingRecipe.getCategory())) {
                    spCategory.setSelection(i);
                    break;
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(isEditing ? "Edit Recipe" : "Add Recipe")
                .setView(dialogView)
                .setPositiveButton("Save", null) // Overridden below so invalid input keeps dialog open
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnDismissListener(d -> activeImageCallback = null);

        //Validate inputs and save recipe on positive click
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String category = spCategory.getSelectedItem() != null
                    ? spCategory.getSelectedItem().toString()
                    : "Dinner";
            String prepRaw = etPrepTime.getText().toString().trim();
            String servingsRaw = etServings.getText().toString().trim();
            String ingredientsRaw = etIngredients.getText().toString().trim();
            String instructionsRaw = etInstructions.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }

            int prepTimeMinutes;
            int servings;
            try {
                prepTimeMinutes = Integer.parseInt(prepRaw);
                if (prepTimeMinutes <= 0) {
                    etPrepTime.setError("Enter minutes > 0");
                    return;
                }
            } catch (NumberFormatException e) {
                etPrepTime.setError("Enter a valid number");
                return;
            }

            try {
                servings = Integer.parseInt(servingsRaw);
                if (servings <= 0) {
                    etServings.setError("Enter servings > 0");
                    return;
                }
            } catch (NumberFormatException e) {
                etServings.setError("Enter a valid number");
                return;
            }

            List<String> ingredientNames = parseIngredients(ingredientsRaw);
            if (ingredientNames.isEmpty()) {
                etIngredients.setError("Enter at least one ingredient");
                return;
            }

            List<String> instructions = parseLines(instructionsRaw);
            if (instructions.isEmpty()) {
                etInstructions.setError("Enter at least one instruction step");
                return;
            }

            String recipeId = isEditing ? existingRecipe.getId() : UUID.randomUUID().toString();
            int imageResId = isEditing ? existingRecipe.getImageResId() : 0;
            boolean favorite = isEditing && existingRecipe.isFavorite();

            Recipe savedRecipe = new Recipe(
                    recipeId,
                    title,
                    category,
                    prepTimeMinutes,
                    servings,
                    imageResId,
                    selectedImageUri[0],
                    favorite,
                    ingredientNames,
                    instructions,
                    notes
            );

            boolean success = isEditing
                    ? repository.updateRecipe(savedRecipe)
                    : repository.addRecipe(savedRecipe);

            if (success) {
                String message = isEditing
                        ? "Updated \"" + savedRecipe.getTitle() + "\""
                        : "Added \"" + savedRecipe.getTitle() + "\"";
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onRecipeSaved(savedRecipe);
                }
                dialog.dismiss();
            } else {
                String errorMsg = isEditing ? "Could not update recipe" : "Could not add recipe";
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
            }
        }));

        dialog.show();
    }

    //Text and image formatting helpers
    private static void bindImagePreview(ImageView imageView, String uriString, int fallbackResId) {
        if (imageView == null) {
            return;
        }
        if (uriString != null && !uriString.isEmpty()) {
            try {
                imageView.setImageURI(Uri.parse(uriString));
                return;
            } catch (Exception ignored) {
            }
        }
        imageView.setImageResource(fallbackResId != 0 ? fallbackResId : R.drawable.ic_recipes);
    }

    private static String joinLines(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private static List<String> parseIngredients(String raw) {
        List<String> items = new ArrayList<>();
        String[] parts = raw.split("[,\\n]");
        for (String part : parts) {
            String normalized = Recipe.normalizeIngredient(part);
            if (!normalized.isEmpty() && !items.contains(normalized)) {
                items.add(normalized);
            }
        }
        return items;
    }

    private static List<String> parseLines(String raw) {
        List<String> lines = new ArrayList<>();
        String[] parts = raw.split("\\n");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }
}