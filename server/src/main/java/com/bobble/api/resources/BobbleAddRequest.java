package com.bobble.api.resources;

import com.google.gson.annotations.SerializedName;

public class BobbleAddRequest {
    @SerializedName("order_id")
    String orderId;

    public BobbleAddRequest(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String value) { orderId = value; }
}