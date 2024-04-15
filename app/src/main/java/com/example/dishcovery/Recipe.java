package com.example.dishcovery;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Recipe {
    public RecipeData recipe_data; // Nested class for recipe data
    public List<String> formatted_ingredients;

    public static class RecipeData {
        public String recipe_id;
        public String recipe_name;
        public String posted_by;
        public String posted_by_name;
        public String category;
        public String ingredients;
        public String instructions;
        public String posted_in;
        public String isPublic;
        public String date_updated;
        public String image; // Base64-encoded image data
        public String posted_by_image; // Base64-encoded image data for profile picture
    }

    // Getter methods

    // Method to parse and format ingredients
    public List<String> getFormattedIngredients() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
        // Parse the JSON ingredients string into a list of maps
        List<Map<String, String>> ingredientsList = gson.fromJson(recipe_data.ingredients, listType);
        List<String> formattedIngredients = new ArrayList<>();

        // Format each ingredient
        for (Map<String, String> ingredient : ingredientsList) {
            String qty = ingredient.get("qty");
            String title = ingredient.get("title");
            String formattedIngredient = qty + " pcs - " + title;
            formattedIngredients.add(formattedIngredient);
        }
        return formattedIngredients;
    }

    // Method to parse and format instructions
    public List<String> getFormattedInstructions() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {}.getType();
        // Parse the JSON instructions string into a list of strings
        List<String> instructionsList = gson.fromJson(recipe_data.instructions, listType);
        List<String> formattedInstructions = new ArrayList<>();

        // Format each instruction
        for (int i = 0; i < instructionsList.size(); i++) {
            String formattedInstruction = "Step " + (i + 1) + ": " + instructionsList.get(i);
            formattedInstructions.add(formattedInstruction);
        }
        return formattedInstructions;
    }

    // Getter for posted_in
    public String getPostedIn() {
        return recipe_data.posted_in;
    }

    // Getter for isPublic
    public String getIsPublic() {
        return recipe_data.isPublic;
    }

    // Getter for date_updated
    public String getDateUpdated() {
        return recipe_data.date_updated;
    }

    // Method to format date
    public String getFormattedDateUpdated() {
        String rawDate = recipe_data.date_updated;
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US);

        try {
            Date date = inputFormat.parse(rawDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return rawDate; // Return raw date if parsing fails
        }
    }

    // Getter for image (Base64-encoded)
    public String getImage() {
        return recipe_data.image;
    }

    // Getter for posted_by_image (Base64-encoded profile picture)
    public String getPostedByImage() {
        return recipe_data.posted_by_image;
    }

    // Getter for recipe_id
    public String getRecipeId() {
        return recipe_data.recipe_id;
    }

    // Getter for recipe_name
    public String getRecipeName() {
        return recipe_data.recipe_name;
    }

    // Getter for posted_by
    public String getPostedBy() {
        return recipe_data.posted_by;
    }

    // Getter for posted_by_name
    public String getPostedByName() {
        return recipe_data.posted_by_name;
    }

    // Getter for category
    public String getCategory() {
        return recipe_data.category;
    }
}
