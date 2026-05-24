package com.huit.da_java.utils;

public final class PermissionChecker {
    private PermissionChecker() {
    }

    public static boolean canView(String role, String requiredRole) {
        if (role == null || requiredRole == null) {
            return false;
        }
        return switch (requiredRole.toLowerCase()) {
            case "admin" -> "admin".equals(role);
            case "staff" -> "admin".equals(role) || "staff".equals(role);
            case "customer" -> "customer".equals(role);
            default -> false;
        };
    }
}
