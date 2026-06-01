package com.foodgroup.common.storage;

public interface S3Port {
    String upload(String keyPrefix, String fileName, byte[] data, String contentType);
    void delete(String key);
}
