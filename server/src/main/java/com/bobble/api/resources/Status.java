package com.bobble.api.resources;

import com.google.gson.annotations.SerializedName;

public enum Status {
    @SerializedName("order")
    NEW_ORDER,
    
    @SerializedName("received")
    RECEIVED,
    
    @SerializedName("processing")
    PROCESSING,
    
    @SerializedName("ready")
    READY
}