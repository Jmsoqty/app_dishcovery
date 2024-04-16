package com.example.dishcovery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Comment {
    private String commentId;
    private String commenterEmail;
    private String recipeId;
    private String commentText;
    private String commentDate;
    private Bitmap profilePicture;
    private String commenterName;

    // Existing constructor for Comment
    public Comment(String commentId, String commenterEmail, String recipeId, String commentText, String commentDate, Bitmap profilePicture, String commenterName) {
        this.commentId = commentId;
        this.commenterEmail = commenterEmail;
        this.recipeId = recipeId;
        this.commentText = commentText;
        this.commentDate = commentDate;
        this.profilePicture = profilePicture;
        this.commenterName = commenterName;
    }

    // Constructor that accepts a JSONObject
    public Comment(JSONObject jsonObject) throws JSONException {
        this.commentId = jsonObject.getString("comment_id");
        this.commenterEmail = jsonObject.getString("comment_by");
        this.recipeId = jsonObject.getString("recipe_id");
        this.commentText = jsonObject.getString("comment_description");
        this.commentDate = jsonObject.getString("date_created");
        this.commenterName = jsonObject.getString("name");

        // Decode the base64-encoded profile picture
        String base64ProfilePic = jsonObject.getString("prof_pic");
        if (base64ProfilePic != null && !base64ProfilePic.isEmpty()) {
            byte[] decodedProfilePic = Base64.decode(base64ProfilePic, Base64.DEFAULT);
            this.profilePicture = BitmapFactory.decodeByteArray(decodedProfilePic, 0, decodedProfilePic.length);
        }
    }
    public Bitmap getProfilePicture() {
        return profilePicture;
    }
    public String getCommenterName() {
        return commenterName;
    }

    public void setCommenterName(String commenterName) {
        this.commenterName = commenterName;
    }

    public String getCommentText() {
        return commentText;
    }

    public String getCommenterEmail() {
        return commenterEmail;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getCommentDate() {
        // Define the input date format (based on how the date is stored)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Define the desired output date format
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy 'at' h:mma");

        try {
            // Parse the commentDate string to a Date object
            Date date = inputFormat.parse(commentDate);

            // Format the Date object to the desired output format
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            // If parsing fails, return the original date string
            return commentDate;
        }
    }
}
