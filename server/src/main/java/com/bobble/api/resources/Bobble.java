package com.bobble.api.resources;

import com.google.gson.annotations.SerializedName;

public final class Bobble
{
    @SerializedName("order_id")
    String orderId;
    Status status;

    public Bobble() {}

    public Bobble(String orderId) {
        this.orderId = orderId;
        status = Status.NEW_ORDER;
    }

    public Bobble clone() {
        Bobble copy = new Bobble();
        copy.orderId = orderId;
        copy.status = status;
        return copy;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String value) { orderId = value; }

    public Status getStatus() { return status; }
    public void setStatus(Status value) { status = value; }

}

