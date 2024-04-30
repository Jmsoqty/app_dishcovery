package com.example.dishcovery;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
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

        String postedIn = getArguments() != null ? getArguments().getString("community") : null;

        if (postedIn != null) {
            postRecipeButton.setOnClickListener(view -> postRecipeIfInCommunity());
        } else {
            postRecipeButton.setOnClickListener(view -> postRecipe());
        }
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


    private void postRecipe() {
        // Retrieve the recipe name, category, and postedBy from UI components
        String recipeName = recipeNameEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String postedBy = getArguments() != null ? getArguments().getString("userEmail") : null;
        Drawable drawable = imagePreview.getDrawable();

        // Initialize ingredients and instructions as empty strings
        String ingredientsJson = getIngredientsAsJson();
        String instructionsJson = getInstructionsAsJson();

        // Validate the required input fields
        if (recipeName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a recipe name.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (category.equals("Choose Category")) {
            Toast.makeText(requireContext(), "Please select a category.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (drawable == null) {
            Toast.makeText(requireContext(), "Please select an image for the recipe.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ingredientsJson.isEmpty() || ingredientsJson.equals("[]")) {
            Toast.makeText(requireContext(), "Please add at least one ingredient.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (instructionsJson.isEmpty() || instructionsJson.equals("[]")) {
            Toast.makeText(requireContext(), "Please add at least one instruction.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert the drawable to a bitmap
        Bitmap imageBitmap = null;
        if (drawable instanceof BitmapDrawable) {
            imageBitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            // Convert the drawable to a bitmap
            imageBitmap = convertDrawableToBitmap(drawable);
        }

        // Convert the image to a byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] imageData = byteArrayOutputStream.toByteArray();

        // Create a multipart request body
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("recipe_name", recipeName)
                .addFormDataPart("category_name", category)
                .addFormDataPart("email", postedBy)
                .addFormDataPart("ingredients", ingredientsJson)
                .addFormDataPart("instructions", instructionsJson)
                .addFormDataPart("image", "recipe.jpg",
                        RequestBody.create(MediaType.parse("image/jpeg"), imageData));

        // Build the request body
        RequestBody requestBody = multipartBuilder.build();

        // Create OkHttpClient instance
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();

        // Define the request URL
        String url = "http://192.168.1.18/dishcovery/api/add_recipe.php";

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
                    Toast.makeText(requireContext(), "Failed to post recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Recipe posted successfully!", Toast.LENGTH_SHORT).show();
                        // Clear input fields after successful posting
                        recipeNameEditText.setText("");
                        categorySpinner.setSelection(0);
                        ingredientContainer.removeAllViews();
                        instructionContainer.removeAllViews();
                        imagePreview.setImageDrawable(null);
                        dismiss();
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to post recipe: " + response.message(), Toast.LENGTH_SHORT).show();
                    });
                }
                response.close();
            }
        });
    }

    private void postRecipeIfInCommunity() {
        // Retrieve the recipe name, category, and postedBy from UI components
        String recipeName = recipeNameEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String postedBy = getArguments() != null ? getArguments().getString("userEmail") : null;
        String postedIn = getArguments() != null ? getArguments().getString("community") : null;
        Drawable drawable = imagePreview.getDrawable();

        // Initialize ingredients and instructions as empty strings
        String ingredientsJson = getIngredientsAsJson();
        String instructionsJson = getInstructionsAsJson();

        // Validate the required input fields
        if (recipeName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a recipe name.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (category.equals("Choose Category")) {
            Toast.makeText(requireContext(), "Please select a category.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (drawable == null) {
            Toast.makeText(requireContext(), "Please select an image for the recipe.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ingredientsJson.isEmpty() || ingredientsJson.equals("[]")) {
            Toast.makeText(requireContext(), "Please add at least one ingredient.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (instructionsJson.isEmpty() || instructionsJson.equals("[]")) {
            Toast.makeText(requireContext(), "Please add at least one instruction.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert the drawable to a bitmap
        Bitmap imageBitmap = null;
        if (drawable instanceof BitmapDrawable) {
            imageBitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            // Convert the drawable to a bitmap
            imageBitmap = convertDrawableToBitmap(drawable);
        }

        // Convert the image to a byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] imageData = byteArrayOutputStream.toByteArray();

        // Create a multipart request body
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("recipe_name", recipeName)
                .addFormDataPart("category_name", category)
                .addFormDataPart("email", postedBy)
                .addFormDataPart("ingredients", ingredientsJson)
                .addFormDataPart("instructions", instructionsJson)
                .addFormDataPart("community_name", postedIn)
                .addFormDataPart("image", "recipe.jpg",
                        RequestBody.create(MediaType.parse("image/jpeg"), imageData));

        // Build the request body
        RequestBody requestBody = multipartBuilder.build();

        // Create OkHttpClient instance
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();

        // Define the request URL
        String url = "http://192.168.1.18/dishcovery/api/add_recipe_to_group.php";

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
                    Toast.makeText(requireContext(), "Failed to post recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Recipe posted successfully!", Toast.LENGTH_SHORT).show();
                        // Clear input fields after successful posting
                        recipeNameEditText.setText("");
                        categorySpinner.setSelection(0);
                        ingredientContainer.removeAllViews();
                        instructionContainer.removeAllViews();
                        imagePreview.setImageDrawable(null);
                        dismiss();
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to post recipe: " + response.message(), Toast.LENGTH_SHORT).show();
                    });
                }
                response.close();
            }
        });
    }

    // Convert a drawable to a bitmap
    private Bitmap convertDrawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return bitmap;
    }

    // Convert the ingredients to a JSON string
    private String getIngredientsAsJson() {
        JSONArray jsonArray = new JSONArray();
        int childCount = ingredientContainer.getChildCount();

        for (int i = 0; i < childCount; i++) {
            LinearLayout ingredientFields = (LinearLayout) ingredientContainer.getChildAt(i);
            EditText quantityEditText = (EditText) ingredientFields.getChildAt(0);
            EditText descriptionEditText = (EditText) ingredientFields.getChildAt(1);

            String quantity = quantityEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();

            // Create a JSON object for each ingredient
            if (!quantity.isEmpty() && !description.isEmpty()) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("qty", quantity);
                    jsonObject.put("title", description);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray.put(jsonObject);
            }
        }

        return jsonArray.toString();
    }

    // Convert the instructions to a JSON string
    private String getInstructionsAsJson() {
        JSONArray jsonArray = new JSONArray();
        int childCount = instructionContainer.getChildCount();

        for (int i = 0; i < childCount; i++) {
            EditText instructionEditText = (EditText) instructionContainer.getChildAt(i);
            String instruction = instructionEditText.getText().toString().trim();

            // Add each instruction as a JSON element
            if (!instruction.isEmpty()) {
                jsonArray.put(instruction);
            }
        }

        return jsonArray.toString();
    }
}