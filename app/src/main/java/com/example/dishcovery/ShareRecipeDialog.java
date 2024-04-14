package com.example.dishcovery;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
public class ShareRecipeDialog extends DialogFragment {

    // UI components
    private EditText recipeNameEditText;
    private Spinner categorySpinner;
    private Button addIngredientButton;
    private Button addInstructionButton;
    private Button postRecipeButton;
    private Button selectImageButton;
    private ImageView imagePreview;

    // Containers for ingredients and instructions
    private LinearLayout ingredientContainer;
    private LinearLayout instructionContainer;

    // Launchers for permissions and image selection
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    Toast.makeText(requireContext(), "Permission denied for image selection", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        loadAndDisplayImage(selectedImageUri);
                    }
                }
            }
    );

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Optionally, customize the dialog appearance and behavior.
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_share_recipe_dialog, container, false);

        // Initialize UI components
        recipeNameEditText = view.findViewById(R.id.recipe_name);
        categorySpinner = view.findViewById(R.id.category_spinner);
        addIngredientButton = view.findViewById(R.id.add_ingredient_button);
        addInstructionButton = view.findViewById(R.id.add_instruction_button);
        postRecipeButton = view.findViewById(R.id.post_recipe_button);
        selectImageButton = view.findViewById(R.id.select_image_button);
        imagePreview = view.findViewById(R.id.image_preview);
        ingredientContainer = view.findViewById(R.id.ingredient_container);
        instructionContainer = view.findViewById(R.id.instruction_container);

        // Setup category spinner and button listeners
        setupCategorySpinner();
        setupButtonListeners();

        return view;
    }

    private void setupButtonListeners() {
        // Add ingredient and instruction fields on button click
        addIngredientButton.setOnClickListener(view -> addIngredientFields());
        addInstructionButton.setOnClickListener(view -> addInstructionFields());

        // Open image picker on button click
        selectImageButton.setOnClickListener(v -> requestImagePermission());

        // Handle posting the recipe on button click
        postRecipeButton.setOnClickListener(view -> postRecipe());
    }

    private void requestImagePermission() {
        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void loadAndDisplayImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
            imagePreview.setImageBitmap(bitmap);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void setupCategorySpinner() {
        // Sample categories
        String[] categories = {
                "Choose Category", "Main Dishes", "Side Dishes",
                "Bread and Pastries", "Regional Dishes",
                "Street Food and Snacks", "Exotic Dishes",
                "Foreign Influences", "International Dishes", "Desserts"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void addIngredientFields() {
        // Create a container for ingredient fields
        LinearLayout ingredientFields = new LinearLayout(getContext());
        ingredientFields.setOrientation(LinearLayout.HORIZONTAL);
        ingredientFields.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ingredientFields.setPadding(0, 12, 0, 0);

        // Create quantity EditText
        EditText quantityEditText = new EditText(getContext());
        quantityEditText.setHint("Qty");
        quantityEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        quantityEditText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        quantityEditText.setTextSize(18f); // Set text size to 14sp
        quantityEditText.setPadding(12, 12, 12, 12); // Set consistent padding

        // Create description EditText
        EditText descriptionEditText = new EditText(getContext());
        descriptionEditText.setHint("Desc");
        descriptionEditText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        descriptionEditText.setTextSize(18f); // Set text size to 14sp
        descriptionEditText.setPadding(12, 12, 12, 12); // Set consistent padding

        // Add EditTexts to the container
        ingredientFields.addView(quantityEditText);
        ingredientFields.addView(descriptionEditText);

        // Add the container to the ingredient container
        ingredientContainer.addView(ingredientFields);
    }

    private void addInstructionFields() {
        // Create a new EditText for instructions
        EditText instructionEditText = new EditText(getContext());
        instructionEditText.setHint("Steps");
        instructionEditText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        instructionEditText.setPadding(12, 23, 12, 12);
        instructionEditText.setTextSize(18f); // Set text size to 14sp

        // Add the new EditText to the instruction container
        instructionContainer.addView(instructionEditText);
    }


    private String getIngredients() {
        // Collect ingredients from the ingredient container
        StringBuilder ingredients = new StringBuilder();
        int childCount = ingredientContainer.getChildCount();

        for (int i = 0; i < childCount; i++) {
            LinearLayout ingredientFields = (LinearLayout) ingredientContainer.getChildAt(i);
            EditText quantityEditText = (EditText) ingredientFields.getChildAt(0);
            EditText descriptionEditText = (EditText) ingredientFields.getChildAt(1);

            String quantity = quantityEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();

            // Append the ingredient in a format like "quantity description"
            if (!quantity.isEmpty() && !description.isEmpty()) {
                ingredients.append(quantity).append(" ").append(description).append("\n");
            }
        }

        return ingredients.toString().trim(); // Return the ingredients as a single string
    }

    private String getInstructions() {
        // Collect instructions from the instruction container
        StringBuilder instructions = new StringBuilder();
        int childCount = instructionContainer.getChildCount();

        for (int i = 0; i < childCount; i++) {
            EditText instructionEditText = (EditText) instructionContainer.getChildAt(i);
            String instruction = instructionEditText.getText().toString().trim();

            // Append each instruction on a new line
            if (!instruction.isEmpty()) {
                instructions.append(instruction).append("\n");
            }
        }

        return instructions.toString().trim(); // Return the instructions as a single string
    }

    private void postRecipe() {
        // Retrieve the recipe name, category, ingredients, and instructions from UI components
        String recipeName = recipeNameEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String ingredients = getIngredients();
        String instructions = getInstructions();

        String postedBy = getArguments() != null ? getArguments().getString("userEmail") : null;

        // Check if the user has provided the necessary information
        if (recipeName.isEmpty() || category.equals("Choose Category") || ingredients.isEmpty() || instructions.isEmpty() || postedBy == null) {
            Toast.makeText(requireContext(), "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create an OkHttpClient instance
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();

        // Define the request URL
        String url = "http://192.168.1.15/dishcovery/api/add_recipe.php";

        // Build the request body with the recipe data
        RequestBody requestBody = new FormBody.Builder()
                .add("name", recipeName)
                .add("category", category)
                .add("ingredients", ingredients)
                .add("instructions", instructions)
                .add("posted_by", postedBy)
                .build();

        // Build the POST request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to post recipe.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Recipe posted successfully!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to post recipe.", Toast.LENGTH_SHORT).show();
                    });
                }
                response.close();
            }
        });
    }
}
