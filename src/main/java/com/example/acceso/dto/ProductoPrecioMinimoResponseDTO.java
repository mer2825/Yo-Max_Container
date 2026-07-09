package com.example.acceso.dto;

import com.example.acceso.model.Producto;
import java.util.List;

public class ProductoPrecioMinimoResponseDTO {

    private boolean success;
    private String message;
    private List<Producto> data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Producto> getData() {
        return data;
    }

    public void setData(List<Producto> data) {
        this.data = data;
    }
}

