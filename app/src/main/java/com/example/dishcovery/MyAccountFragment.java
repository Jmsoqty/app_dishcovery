package com.example.dishcovery;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyAccountFragment extends Fragment {
    // Define the callback interface

    private static final String PROFILE_FETCH_URL = "http://192.168.1.18/dishcovery/api/fetch_profile.php";
    private static final String UPDATE_PROFILE_URL = "http://192.168.1.18/dishcovery/api/update_profile.php";

    private ImageView profileImageView;
    private Button uploadImageButton;
    private Button updateAccountBtn;
    private TextView fullNameTextView;
    private TextView usernameTextView;
    private TextView emailTextView;
    private TextView passwordTextView;

    private OkHttpClient client;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_account, container, false);

        // Initialize views
        profileImageView = view.findViewById(R.id.profile_image_preview);
        uploadImageButton = view.findViewById(R.id.profile_image_input);
        fullNameTextView = view.findViewById(R.id.fullname_input);
        usernameTextView = view.findViewById(R.id.username_input);
        emailTextView = view.findViewById(R.id.email_input);
        passwordTextView = view.findViewById(R.id.password_input);
        updateAccountBtn = view.findViewById(R.id.update_account);

        // Initialize OkHttpClient
        client = new OkHttpClient();

        // Set upload image button click listener
        uploadImageButton.setOnClickListener(v -> requestImagePermission());

        // Set update account button click listener
        updateAccountBtn.setOnClickListener(v -> updateAccount());

        // Retrieve email from arguments
        String email = getArguments() != null ? getArguments().getString("userEmail") : null;

        if (email != null && !email.isEmpty()) {
            loadUserProfile(email);
        } else {
            Toast.makeText(requireContext(), "User email not found.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void requestImagePermission() {
        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchImagePicker();
                } else {
                    Toast.makeText(requireContext(), "Permission denied to read images", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        loadAndSetImage(selectedImageUri);
                    }
                }
            }
    );

    private void loadAndSetImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
            profileImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadUserProfile(String email) {
        // Create a POST request using OkHttp
        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .build();

        Request request = new Request.Builder()
                .url(PROFILE_FETCH_URL)
                .post(formBody)
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle the error
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error fetching user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Handle the response
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (jsonResponse.optString("status").equals("success")) {
                            JSONArray data = jsonResponse.optJSONArray("response");
                            if (data != null && data.length() > 0) {
                                JSONObject user = data.getJSONObject(0);
                                requireActivity().runOnUiThread(() -> {
                                    // Update the UI with the user's data
                                    fullNameTextView.setText(user.optString("name"));
                                    usernameTextView.setText(user.optString("username"));
                                    emailTextView.setText(user.optString("email"));

                                    // Handle profile picture
                                    String base64Image = user.optString("prof_pic");
                                    if (base64Image != null && !base64Image.isEmpty()) {
                                        byte[] imageData = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                                        Bitmap profileImageBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                                        profileImageView.setImageBitmap(profileImageBitmap);
                                    }
                                });
                            } else {
                                // No user data found
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), "No user data found for the provided email.", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            // Handle API error
                            String errorMessage = jsonResponse.optString("message");
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (JSONException e) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                        e.printStackTrace();
                    }
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Server error: " + response.message(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateAccount() {
        // Collect user input
        String fullName = fullNameTextView.getText().toString();
        String username = usernameTextView.getText().toString();
        String password = passwordTextView.getText().toString();
        String email = emailTextView.getText().toString();

        // Initialize byte array for image data
        byte[] imageData = null;
        if (profileImageView.getDrawable() != null) {
            Bitmap bitmap = ((BitmapDrawable) profileImageView.getDrawable()).getBitmap();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            imageData = byteArrayOutputStream.toByteArray();
        }

        // Create a multipart request body
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fullname", fullName)
                .addFormDataPart("username", username)
                .addFormDataPart("password", password)
                .addFormDataPart("email", email);

        if (imageData != null) {
            multipartBuilder.addFormDataPart("prof_pic", "profile.jpg",
                    RequestBody.create(MediaType.parse("image/jpeg"), imageData));
        }

        RequestBody requestBody = multipartBuilder.build();

        Request request = new Request.Builder()
                .url(UPDATE_PROFILE_URL)
                .post(requestBody)
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle request failure
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Handle response
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.optBoolean("success");
                        if (success) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            String errorMessage = jsonResponse.optString("message");
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Failed to update profile: " + errorMessage, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (JSONException e) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                        });
                        e.printStackTrace();
                    }
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Server error: " + response.message(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}
