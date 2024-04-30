package com.example.dishcovery;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.paypal.checkout.approve.Approval;
import com.paypal.checkout.approve.OnApprove;
import com.paypal.checkout.createorder.CreateOrder;
import com.paypal.checkout.createorder.CreateOrderActions;
import com.paypal.checkout.createorder.CurrencyCode;
import com.paypal.checkout.createorder.OrderIntent;
import com.paypal.checkout.createorder.UserAction;
import com.paypal.checkout.order.Amount;
import com.paypal.checkout.order.AppContext;
import com.paypal.checkout.order.OrderRequest;
import com.paypal.checkout.order.PurchaseUnit;
import com.paypal.checkout.paymentbutton.PaymentButtonContainer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipes;
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private List<Recipe> filteredRecipes;
    private String currentUserEmail;
    private PaymentButtonContainer donate_button_container;
    private EditText donateInput;
    private OkHttpClient client;
    private String transactionId;
    private String donationAmountStr;
    void fetchComments(String recipeId, Consumer<List<Comment>> callback) {
        // Define the URL for fetching comments, with the recipe ID as a query parameter
        String url = "http://192.168.1.18/dishcovery/api/fetch_comments.php?recipe_id=" + recipeId;

        // Create a GET request
        Request request = new Request.Builder()
                .url(url)
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Handle the error
                mainThreadHandler.post(() -> callback.accept(new ArrayList<>())); // Pass an empty list on failure
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // Handle unsuccessful response
                    mainThreadHandler.post(() -> callback.accept(new ArrayList<>())); // Pass an empty list on failure
                    return;
                }

                String responseData = response.body().string();

                try {
                    // Parse the response JSON
                    JSONObject responseJson = new JSONObject(responseData);
                    if (!responseJson.getString("status").equals("success")) {
                        mainThreadHandler.post(() -> callback.accept(new ArrayList<>())); // Pass an empty list on failure
                        return;
                    }

                    JSONArray jsonArray = responseJson.getJSONArray("comments");
                    List<Comment> commentsList = new ArrayList<>();

                    // Parse each JSON object in the array into a Comment object
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        Comment comment = new Comment(jsonObject);
                        commentsList.add(comment);
                    }
                    mainThreadHandler.post(() -> callback.accept(commentsList));
                } catch (JSONException e) {
                    e.printStackTrace();
                    // Handle JSON parsing error
                    mainThreadHandler.post(() -> callback.accept(new ArrayList<>())); // Pass an empty list on failure
                }
            }
        });
    }
    public RecipeAdapter(List<Recipe> recipes, String currentUserEmail) {
        this.recipes = recipes;
        this.filteredRecipes = new ArrayList<>(recipes);
        this.currentUserEmail = currentUserEmail;
        this.client = new OkHttpClient();
    }

    public List<Recipe> getRecipes() {
        return filteredRecipes;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
        this.filteredRecipes = new ArrayList<>(recipes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_item, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = filteredRecipes.get(position);
        holder.bind(recipe);

        // Access bookmarkButton through the RecipeViewHolder instance
        ImageButton bookmarkButton = holder.bookmarkButton;

        // Now you can use bookmarkButton here
        checkBookmarkStatus(recipe.getRecipeId(), new OnBookmarkStatusListener() {
            @Override
            public void onBookmarkStatus(boolean isBookmarked) {
                int bookmarkIcon = isBookmarked ? R.drawable.bookmarked : R.drawable.not_bookmarked;
                bookmarkButton.setImageResource(bookmarkIcon);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredRecipes != null ? filteredRecipes.size() : 0;
    }

    public void filter(String query) {
        List<Recipe> filteredList = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe.getRecipeName().toLowerCase().contains(query.toLowerCase()) ||
                    recipe.getCategory().toLowerCase().contains(query.toLowerCase()) ||
                    recipe.getPostedByName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(recipe);
            }
        }
        filteredRecipes.clear();
        filteredRecipes.addAll(filteredList);
        notifyDataSetChanged();
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameText;
        private final TextView postDateText;
        private final TextView recipeNameText;
        private final TextView ingredientsText;
        private final TextView instructionsText;
        private final ImageView dishImageView;
        private final ImageView profilePictureView;
        private final ImageButton deleteRecipeButton;
        private final ImageButton bookmarkButton;
        private final ImageButton sendFundsButton;

        private final ImageButton commentsButton;
        private final LinearLayout commentsSection;
        private final EditText commentInput;
        private final EditText commentInputOutside;
        private final ImageButton sendCommentButton;

        private final ImageButton sendCommentButtonOutside;


        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.username_text);
            postDateText = itemView.findViewById(R.id.post_date_text);
            recipeNameText = itemView.findViewById(R.id.recipe_name_text);
            ingredientsText = itemView.findViewById(R.id.ingredients_text);
            instructionsText = itemView.findViewById(R.id.instructions_text);
            dishImageView = itemView.findViewById(R.id.dish_image_view);
            profilePictureView = itemView.findViewById(R.id.profile_picture);
            deleteRecipeButton = itemView.findViewById(R.id.delete_recipe_button);
            bookmarkButton = itemView.findViewById(R.id.bookmark_button);
            sendFundsButton = itemView.findViewById(R.id.send_funds_button);
            commentsButton = itemView.findViewById(R.id.comment_button);
            commentsSection = itemView.findViewById(R.id.commentsSection);
            commentInput = itemView.findViewById(R.id.commentInput);
            sendCommentButton = itemView.findViewById(R.id.sendCommentButton);

            donate_button_container = itemView.findViewById(R.id.donate_button_container);
            donateInput = itemView.findViewById(R.id.donateInput);

            commentInputOutside = itemView.findViewById(R.id.comment_input);
            sendCommentButtonOutside = itemView.findViewById(R.id.send_button_outside);

            sendCommentButtonOutside.setOnClickListener(v -> {
                // Get the comment text from the commentInputOutside EditText
                String commentText = commentInputOutside.getText().toString();

                // Get the recipe ID
                Recipe recipe = filteredRecipes.get(getAdapterPosition());
                String recipeId = recipe.getRecipeId();

                // Call the postCommentOutside method to post the comment
                postCommentOutside(recipeId, commentText, itemView.getContext(), () -> {
                    // Clear the comment input field after successfully posting the comment
                    commentInputOutside.setText("");
                });
            });

            bookmarkButton.setOnClickListener(v -> toggleBookmark(getAdapterPosition()));
            commentsButton.setOnClickListener(v -> showCommentsModal());
        }

        private void showSendFundsModal(String sentTo, String currentUserEmail) {
            // Obtain the context from the view
            Context context = itemView.getContext();

            // Obtain an instance of LayoutInflater from the context
            LayoutInflater inflater = LayoutInflater.from(context);

            // Inflate the modal layout
            View modalView = inflater.inflate(R.layout.modal_send_funds, null);

            // Obtain references to UI elements in the modal
            MaterialTextView currentBalanceToDonate = modalView.findViewById(R.id.currentBalanceToDonate);
            donate_button_container = modalView.findViewById(R.id.donate_button_container);
            donateInput = modalView.findViewById(R.id.donateInput);

            // Fetch the current balance and update the amountTextView when retrieved
            fetchCurrentBalance(currentBalance -> {
                // On the main thread, update the text view with the current balance
                mainThreadHandler.post(() -> {
                    currentBalanceToDonate.setText("Current Balance: $" + currentBalance);
                });

                setupPayPalPaymentButton(donate_button_container, donateInput, sentTo, currentUserEmail, currentBalance);
            });

            // Create and show the alert dialog
            AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setView(modalView)
                    .setPositiveButton("Close", null)
                    .create();
            alertDialog.show();
        }
        private void setupPayPalPaymentButton(PaymentButtonContainer donateButtonContainer, EditText donateInput, String sentTo, String currentUserEmail, double currentBalance) {
            Context context = itemView.getContext();
            donateButtonContainer.setup(
                    new CreateOrder() {
                        public void create(@NotNull CreateOrderActions createOrderActions) {
                            // Get the donation amount from the input field
                            donationAmountStr = donateInput.getText().toString();

                            // Validate the donation amount
                            if (donationAmountStr.isEmpty()) {
                                Toast.makeText(context, "Please enter a donation amount", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            double donationAmount;
                            try {
                                donationAmount = Double.parseDouble(donationAmountStr);
                            } catch (NumberFormatException e) {
                                Toast.makeText(context, "Invalid donation amount", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Check if donation amount exceeds current balance
                            if (donationAmount > currentBalance) {
                                Toast.makeText(context, "Donation amount exceeds current balance", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Create the purchase unit with the donation amount
                            PurchaseUnit purchaseUnit = new PurchaseUnit.Builder()
                                    .amount(new Amount.Builder()
                                            .currencyCode(CurrencyCode.USD)
                                            .value(donationAmountStr)
                                            .build())
                                    .build();

                            // Create a list of purchase units
                            List<PurchaseUnit> purchaseUnits = new ArrayList<>();
                            purchaseUnits.add(purchaseUnit);

                            // Create the order request
                            OrderRequest orderRequest = new OrderRequest(
                                    OrderIntent.CAPTURE,
                                    new AppContext.Builder()
                                            .userAction(UserAction.PAY_NOW)
                                            .build(),
                                    purchaseUnits
                            );

                            // Create the order using the provided actions
                            createOrderActions.create(orderRequest, orderId -> {
                                // Handle the order ID
                                Log.d("PayPal", "Order ID: " + orderId);
                                transactionId = orderId;
                            });
                        }
                    },
                    new OnApprove() {
                        @Override
                        public void onApprove(@NotNull Approval approval) {
                            // Handle the approval
                            approval.getOrderActions().capture(result -> {
                                if (result != null) {
                                    // Payment was successful
                                    Log.d("PayPal", "Payment successful!");

                                    // Display a toast message to the user
                                    Toast.makeText(context, "Donation successful!", Toast.LENGTH_SHORT).show();

                                    // Process the donation with your API
                                    processDonation(context, currentUserEmail, sentTo, donationAmountStr, transactionId);
                                } else {
                                    // Payment capture failed
                                    Log.e("PayPal", "Payment capture failed!");
                                    Toast.makeText(context, "Payment capture failed. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
            );
        }

        private void processDonation(Context context, String currentUserEmail, String sentTo, String donationAmountStr, String orderId) {
            // Create an OkHttpClient instance
            OkHttpClient client = new OkHttpClient();

            // Create a request body with the required parameters
            RequestBody requestBody = new FormBody.Builder()
                    .add("email", currentUserEmail)
                    .add("posted_by", sentTo)
                    .add("payment", donationAmountStr)
                    .add("transaction_id", orderId)
                    .build();

            // Define the URL of your API endpoint
            String url = "http://192.168.1.18/dishcovery/api/donate.php";

            // Create a POST request
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Handle the error
                    Log.e("Donation", "Failed to send donation data", e);
                    mainThreadHandler.post(() -> Toast.makeText(context, "Failed to process donation", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Parse the response JSON
                        String responseData = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);

                            boolean success = jsonResponse.optBoolean("success", false);
                            String message = jsonResponse.optString("message", "Failed to process donation");

                            // Show a toast message to the user based on the response
                            mainThreadHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
                        } catch (JSONException e) {
                            Log.e("Donation", "JSON parsing error: " + e.getMessage());
                            mainThreadHandler.post(() -> Toast.makeText(context, "An error occurred while processing the response. Please try again.", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        // Handle unsuccessful response
                        Log.e("Donation", "Unsuccessful response: " + response);
                        mainThreadHandler.post(() -> Toast.makeText(context, "Failed to process donation", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }


        private void showCommentsModal() {
            // Obtain the context from the view
            Context context = itemView.getContext();

            // Obtain an instance of LayoutInflater from the context
            LayoutInflater inflater = LayoutInflater.from(context);

            // Inflate the modal comments layout
            View modalView = inflater.inflate(R.layout.modal_comments, null);

            // Obtain references to UI elements in the modal
            LinearLayout commentsSection = modalView.findViewById(R.id.commentsSection);
            EditText commentInput = modalView.findViewById(R.id.commentInput);
            ImageButton sendCommentButton = modalView.findViewById(R.id.sendCommentButton);

            // Obtain recipe data
            Recipe recipe = filteredRecipes.get(getAdapterPosition());
            String recipeId = recipe.getRecipeId();

            // Fetch and display comments
            fetchAndDisplayComments(recipeId, commentsSection, inflater, context);

            // Set up the send comment button
            sendCommentButton.setOnClickListener(v -> {
                String newComment = commentInput.getText().toString();
                if (!newComment.isEmpty()) {
                    postComment(recipeId, newComment, context, () -> {
                        // After posting the comment, refresh the comments section
                        fetchAndDisplayComments(recipeId, commentsSection, inflater, context);
                        // Clear the comment input
                        commentInput.setText("");
                    });
                }
            });

            // Create and show the alert dialog
            AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setView(modalView)
                    .create();
            alertDialog.show();
        }

        private void fetchAndDisplayComments(String recipeId, LinearLayout commentsSection, LayoutInflater inflater, Context context) {
            // Clear existing views in the comments section
            commentsSection.removeAllViews();

            // Fetch the comments from the server
            fetchComments(recipeId, commentsList -> {
                // Check if the comments list is empty
                if (commentsList.isEmpty()) {
                    // Create a TextView for the message
                    TextView noCommentsTextView = new TextView(commentsSection.getContext());
                    noCommentsTextView.setText("No comments found in this recipe.");
                    noCommentsTextView.setTextColor(commentsSection.getContext().getResources().getColor(android.R.color.darker_gray));
                    noCommentsTextView.setTextSize(16);
                    noCommentsTextView.setGravity(android.view.Gravity.CENTER);

                    // Add the TextView to the comments section and center it
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    layoutParams.gravity = android.view.Gravity.CENTER;
                    noCommentsTextView.setLayoutParams(layoutParams);

                    commentsSection.addView(noCommentsTextView);
                } else {
                    // Loop through commentsList and display each comment
                    for (Comment comment : commentsList) {
                        // Inflate the comment layout for each comment using the provided inflater
                        View commentView = inflater.inflate(R.layout.comment_entry, commentsSection, false);

                        // Find views within the comment layout
                        ImageView profilePicture = commentView.findViewById(R.id.profilePicture);
                        TextView commenterName = commentView.findViewById(R.id.commenterName);
                        TextView commentDate = commentView.findViewById(R.id.commentDate);
                        TextView commentText = commentView.findViewById(R.id.commentText);
                        ImageButton deleteButton = commentView.findViewById(R.id.deleteButton);
                        Button saveButton = commentView.findViewById(R.id.saveButton);
                        Button cancelButton = commentView.findViewById(R.id.cancelButton);
                        EditText editCommentText = commentView.findViewById(R.id.editCommentText);
                        LinearLayout editCommentLayout = commentView.findViewById(R.id.editCommentLayout);

                        // Set data to the views
                        profilePicture.setImageBitmap(comment.getProfilePicture());
                        commenterName.setText(comment.getCommenterName());
                        commentDate.setText(comment.getCommentDate());
                        commentText.setText(comment.getCommentText());

                        // Set up delete button visibility based on the current user
                        if (currentUserEmail.equals(comment.getCommenterEmail())) {
                            deleteButton.setVisibility(View.VISIBLE);
                            deleteButton.setOnClickListener(v -> deleteCommentFromServer(recipeId, comment.getCommentId(), commentsSection, commentView));
                        } else {
                            deleteButton.setVisibility(View.GONE);
                        }

                        if (currentUserEmail.equals(comment.getCommenterEmail())) {
                            ImageButton editButton = commentView.findViewById(R.id.editButton);
                            editButton.setVisibility(View.VISIBLE);
                            editButton.setOnClickListener(editView -> {
                                // Show edit layout
                                editCommentLayout.setVisibility(View.VISIBLE);
                                // Hide original comment layout
                                commentText.setVisibility(View.GONE);
                                // Set original comment text to edit text
                                editCommentText.setText(comment.getCommentText());
                            });
                        } else {
                            commentView.findViewById(R.id.editButton).setVisibility(View.GONE);
                        }

                        // Set up edit button click listener
                        ImageButton editButton = commentView.findViewById(R.id.editButton);
                        editButton.setOnClickListener(editView -> {
                            // Show edit layout
                            editCommentLayout.setVisibility(View.VISIBLE);
                            // Hide original comment layout
                            commentText.setVisibility(View.GONE);
                            // Set original comment text to edit text
                            editCommentText.setText(comment.getCommentText());
                        });

                        // Set up save button click listener
                        saveButton.setOnClickListener(saveView -> {
                            // Get edited comment text
                            String editedText = editCommentText.getText().toString();
                            // Make HTTP POST request to edit comment
                            editComment(comment.getCommentId(), editedText, context);

                            // Update comment text view
                            commentText.setText(editedText);
                            // Hide edit layout
                            editCommentLayout.setVisibility(View.GONE);
                            // Show original comment layout
                            commentText.setVisibility(View.VISIBLE);
                        });

                        // Set up cancel button click listener
                        cancelButton.setOnClickListener(cancelView -> {
                            // Hide edit layout
                            editCommentLayout.setVisibility(View.GONE);
                            // Show original comment layout
                            commentText.setVisibility(View.VISIBLE);
                        });

                        // Add the comment view to the comments section
                        commentsSection.addView(commentView);
                    }
                }
            });
        }

        private void editComment(String commentId, String newCommentText,  Context context) {
            // Form body with parameters
            RequestBody formBody = new FormBody.Builder()
                    .add("action", "edit_comment")
                    .add("comment_id", commentId)
                    .add("new_comment", newCommentText)
                    .build();

            // Request object
            Request request = new Request.Builder()
                    .url("http://192.168.1.18/dishcovery/api/edit_comment.php")
                    .post(formBody)
                    .build();

            // Asynchronous call
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Handle failure
                    e.printStackTrace();
                    Toast.makeText(context, "Error editing comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // Handle response
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String status = jsonResponse.getString("status");
                        if (status.equals("success")) {
                            // Edit successful
                            Handler mainHandler = new Handler(context.getMainLooper());

                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Comment edited successfully", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // Edit failed
                            final String message = jsonResponse.getString("message");
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Failed to edit comment: " + message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }


        private void deleteCommentFromServer(String recipeId, String commentId, LinearLayout commentsSection, View commentView) {
            // Define the URL for deleting the comment
            String url = "http://192.168.1.18/dishcovery/api/delete_comment.php";

            // Create a request body with the comment ID and recipe ID
            RequestBody formBody = new FormBody.Builder()
                    .add("comment_id", commentId)
                    .build();

            // Create a request to delete the comment
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    // Handle error here
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Remove the comment view from the comments section on success
                        mainThreadHandler.post(() -> commentsSection.removeView(commentView));
                    } else {
                        // Handle error here
                    }
                }
            });
        }



        private void postComment(String recipeId, String commentText, Context context, Runnable onSuccess) {
            // Define the URL for posting a comment
            String url = "http://192.168.1.18/dishcovery/api/add_comment.php";

            // Create a request body with the recipe ID and comment text
            RequestBody formBody = new FormBody.Builder()
                    .add("recipe_id", recipeId)
                    .add("comment_description", commentText)
                    .add("email", currentUserEmail)
                    .build();

            // Create a request to post the comment
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    // Handle error here
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Invoke the success callback on the main thread
                        mainThreadHandler.post(onSuccess);
                    } else {
                        // Handle error here
                    }
                }
            });
        }

        private void postCommentOutside(String recipeId, String commentText, Context context, Runnable onSuccess) {
            // Define the URL for posting a comment
            String url = "http://192.168.1.18/dishcovery/api/add_comment.php";

            // Create a request body with the recipe ID and comment text
            RequestBody formBody = new FormBody.Builder()
                    .add("recipe_id", recipeId)
                    .add("comment_description", commentText)
                    .add("email", currentUserEmail)
                    .build();

            // Create a request to post the comment
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    // Handle error here
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Invoke the success callback on the main thread
                        mainThreadHandler.post(onSuccess);
                    } else {
                        // Handle error here
                    }
                }
            });
        }



        public void bind(Recipe recipe) {
            usernameText.setText(recipe.getPostedByName());
            postDateText.setText(recipe.getFormattedDateUpdated());
            recipeNameText.setText(recipe.getRecipeName());

            List<String> ingredients = recipe.getFormattedIngredients();
            if (ingredients != null) {
                String formattedIngredients = String.join("\n", ingredients);
                ingredientsText.setText(formattedIngredients);
            } else {
                ingredientsText.setText("");
            }

            List<String> instructions = recipe.getFormattedInstructions();
            if (instructions != null) {
                String formattedInstructions = String.join("\n", instructions);
                instructionsText.setText(formattedInstructions);
            } else {
                instructionsText.setText("");
            }

            String dishImageString = recipe.getImage();
            if (dishImageString != null && !dishImageString.isEmpty()) {
                byte[] decodedDishImage = Base64.decode(dishImageString, Base64.DEFAULT);
                Bitmap dishBitmap = BitmapFactory.decodeByteArray(decodedDishImage, 0, decodedDishImage.length);
                dishImageView.setImageBitmap(dishBitmap);
            } else {
                dishImageView.setImageDrawable(null);
            }

            String profileImageString = recipe.getPostedByImage();
            if (profileImageString != null && !profileImageString.isEmpty()) {
                byte[] decodedProfileImage = Base64.decode(profileImageString, Base64.DEFAULT);
                Bitmap profileBitmap = BitmapFactory.decodeByteArray(decodedProfileImage, 0, decodedProfileImage.length);
                profilePictureView.setImageBitmap(profileBitmap);
            } else {
                profilePictureView.setImageDrawable(null);
            }

            if (currentUserEmail != null && currentUserEmail.equals(recipe.getPostedBy())) {
                deleteRecipeButton.setVisibility(View.VISIBLE);
                bookmarkButton.setVisibility(View.GONE);
                sendFundsButton.setVisibility(View.GONE);
            } else {
                deleteRecipeButton.setVisibility(View.GONE);
                bookmarkButton.setVisibility(View.VISIBLE);
                sendFundsButton.setVisibility(View.VISIBLE);
            }

            String recipeId = recipe.getRecipeId();
            checkBookmarkStatus(recipe.getRecipeId(), isBookmarked -> {
                bookmarkButton.setImageResource(isBookmarked ? R.drawable.bookmarked : R.drawable.not_bookmarked);
                bookmarkButton.setOnClickListener(v -> {
                    if (recipe.isBookmarked()) {
                        removeBookmark(recipe.getRecipeId());
                        recipe.setBookmarked(false);
                        bookmarkButton.setImageResource(R.drawable.not_bookmarked);
                    } else {
                        addBookmark(recipe.getRecipeId());
                        recipe.setBookmarked(true);
                        bookmarkButton.setImageResource(R.drawable.bookmarked);
                    }
                });
            });

            // Set up delete recipe button
            deleteRecipeButton.setOnClickListener(v -> deleteRecipe(getAdapterPosition()));

            sendFundsButton.setOnClickListener(v -> showSendFundsModal(recipe.getPostedBy(), currentUserEmail));
        }
    }



    private void toggleBookmark(int position) {
        Recipe recipe = filteredRecipes.get(position);
        String recipeId = recipe.getRecipeId();

        // Check if the recipe is already bookmarked
        if (recipe.isBookmarked()) {
            removeBookmark(recipeId);
            reloadPage();
        } else {
            addBookmark(recipeId);
            reloadPage();
        }
    }
    private void deleteRecipe(int position) {
        Recipe recipe = filteredRecipes.get(position);
        String recipeId = recipe.getRecipeId();

        RequestBody formBody = new FormBody.Builder()
                .add("recipeId", recipeId)
                .build();

        Request request = new Request.Builder()
                .url("http://192.168.1.18/dishcovery/api/delete_recipe.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Handle error here
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Remove recipe from the list and notify the adapter
                    mainThreadHandler.post(() -> {
                        filteredRecipes.remove(position);
                        notifyItemRemoved(position);
                    });
                } else {
                    // Handle unexpected response code here
                }
            }
        });
    }
    public void fetchCurrentBalance(Consumer<Double> callback) {
        // Define the URL for fetching the current balance
        String url = "http://192.168.1.18/dishcovery/api/fetch_balance.php";

        // Create a request with the current user's email as a parameter
        RequestBody formBody = new FormBody.Builder()
                .add("email", currentUserEmail)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Handle failure by returning a default balance (e.g., 0.0) to the callback
                mainThreadHandler.post(() -> callback.accept(0.0));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // Handle unsuccessful response by returning a default balance (e.g., 0.0) to the callback
                    mainThreadHandler.post(() -> callback.accept(0.0));
                    return;
                }

                // Parse the response body as a string
                String responseData = response.body().string();
                try {
                    // Parse the JSON response
                    JSONObject json = new JSONObject(responseData);
                    double currentBalance = json.getDouble("ewallet_value");

                    // Post the result on the main thread using the callback
                    mainThreadHandler.post(() -> callback.accept(currentBalance));
                } catch (JSONException e) {
                    e.printStackTrace();
                    // Handle JSON parsing error by returning a default balance (e.g., 0.0) to the callback
                    mainThreadHandler.post(() -> callback.accept(0.0));
                }
            }
        });
    }

    private void addBookmark(String recipeId) {
        RequestBody formBody = new FormBody.Builder()
                .add("recipe_id", recipeId)
                .add("email", currentUserEmail)
                .build();

        Request request = new Request.Builder()
                .url("http://192.168.1.18/dishcovery/api/add_bookmark.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

            }
        });
    }

    private void removeBookmark(String recipeId) {
        RequestBody formBody = new FormBody.Builder()
                .add("recipe_id", recipeId)
                .add("email", currentUserEmail)
                .build();

        Request request = new Request.Builder()
                .url("http://192.168.1.18/dishcovery/api/remove_bookmark.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }


            }
        });
    }
    private Recipe findRecipeById(String recipeId) {
        for (Recipe recipe : recipes) {
            if (recipe.getRecipeId().equals(recipeId)) {
                return recipe;
            }
        }
        return null; // Recipe not found
    }
    // Method to check bookmark status
    private void checkBookmarkStatus(String recipeId, OnBookmarkStatusListener listener) {
        RequestBody formBody = new FormBody.Builder()
                .add("recipe_id", recipeId)
                .add("email", currentUserEmail)
                .build();

        Request request = new Request.Builder()
                .url("http://192.168.1.18/dishcovery/api/IsBookmarked.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseData = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseData);
                    boolean isBookmarked = json.getBoolean("isBookmarked");

                    // Update the bookmarked status in the Recipe object
                    Recipe recipeToUpdate = findRecipeById(recipeId);
                    if (recipeToUpdate != null) {
                        recipeToUpdate.setBookmarked(isBookmarked);
                    }

                    // Notify the listener
                    listener.onBookmarkStatus(isBookmarked);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private void reloadPage() {
        setRecipes(recipes);
    }
    // Interface to handle bookmark status listener
    interface OnBookmarkStatusListener {
        void onBookmarkStatus(boolean isBookmarked);
    }
}