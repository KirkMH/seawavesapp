package com.asu.seawavesapp.data;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * The Boat class represents a record from the Boat table in the database.
 * This is also used to link to the server's API.
 */
public class Boat {
    @SerializedName("id")
    public Long id;
    @SerializedName("name")
    public String name;
    @SerializedName("owner")
    public String owner;
    @SerializedName("owner_contact")
    public String ownerContact;
    @SerializedName("length")
    public Double length;
    @SerializedName("width")
    public Double width;
    @SerializedName("height")
    public Double height;
    @SerializedName("registered_at")
    public Date registeredAt;
    @SerializedName("is_active")
    public Boolean isActive;

    /**
     * Creates an instance of the Boat class.
     *
     * @param id           - Boat id
     * @param name         - Boat's name
     * @param owner        - Owner's name
     * @param ownerContact - Owner's contact number
     * @param length       - Boat's length
     * @param width        - Boat's width
     * @param height       - Boat's height
     * @param registeredAt - Date when the boat was registered
     * @param isActive     - Whether the boat is still active or not
     */
    public Boat(Long id, String name, String owner, String ownerContact, Double length, Double width, Double height, Date registeredAt, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.ownerContact = ownerContact;
        this.length = length;
        this.width = width;
        this.height = height;
        this.registeredAt = registeredAt;
        this.isActive = isActive;
    }
}
