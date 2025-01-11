import React from "react";
import { Navigate } from "react-router-dom";

function PrivateRoute({ element }) {
 
  const isAuthenticated = localStorage.getItem("isAuthenticated") === "true";

  
  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  
  return element;
}

export default PrivateRoute;
