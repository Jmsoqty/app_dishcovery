package com.example.dishcovery;

public class Group {
    private String communityName;
    private int numberOfMembers;
    private String dateCreated;

    public Group(String communityName, int numberOfMembers, String dateCreated) {
        this.communityName = communityName;
        this.numberOfMembers = numberOfMembers;
        this.dateCreated = dateCreated;
    }

    // Getters
    public String getCommunityName() {
        return communityName;
    }

    public int getNumberOfMembers() {
        return numberOfMembers;
    }

    public String getDateCreated() {
        return dateCreated;
    }
}
