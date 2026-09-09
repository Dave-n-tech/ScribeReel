package org.scribereel.dtos.response;

public record JobStatusResponse(String status, String downloadUrl, String text, String error) {}