import React from "react";
import { Navigate } from "react-router-dom";

// PrivateRoute component
function PrivateRoute({ element, allowedRoles }) {
  const token = localStorage.getItem("authToken");
  const userRole = localStorage.getItem("role"); // Assuming you store the role in localStorage

  // If no token or role doesn't match allowed roles, redirect to login or home
  if (!token) {
    return <Navigate to="/login" />;
  }

  if (allowedRoles && !allowedRoles.includes(userRole)) {
    return <Navigate to="/" />;
  }

  return element; // Render the protected page if everything is okay
}

export default PrivateRoute;
