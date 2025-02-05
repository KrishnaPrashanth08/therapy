import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./login.css"; // Add the path to your CSS file here

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const navigate = useNavigate(); // Hook for navigation

  const handleLogin = async (e) => {
    e.preventDefault();

    console.log("Logging in with email:", email); // Debug log for email

    try {
      const response = await fetch("http://localhost:5000/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email, password }),
      });

      console.log("Response status:", response.status); // Debug log for status code
      const data = await response.json();

      console.log("Response data:", data); // Debug log for the response data

      if (response.ok) {
        setMessage("Login successful!");
        localStorage.setItem("authToken", data.token); // Store JWT token
        setEmail("");
        setPassword("");

        // Redirect based on user role
        if (data.role === "client") {
          console.log("Redirecting to client-dashboard"); // Debug log for redirect
          navigate("/client-dashboard");
        } else if (data.role === "therapist") {
          console.log("Redirecting to therapist-dashboard"); // Debug log for redirect
          navigate("/therapist-dashboard");
        } else {
          setMessage("Invalid user role.");
        }
      } else {
        setMessage(data.message || "Invalid email or password. Please try again.");
        console.log("Login failed:", data.message); // Debug log for login failure
      }
    } catch (error) {
      setMessage("An error occurred. Please try again later.");
      console.log("Error during login:", error); // Debug log for error
    }
  };

  return (
    <div className="login-container">
      <h2>Login</h2>
      <form onSubmit={handleLogin}>
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <button type="submit">Login</button>
      </form>
      {message && <p>{message}</p>}
      <div className="signup-redirect">
        <p>Don't have an account? <a href="/signup">Sign Up</a></p>
      </div>
    </div>
  );
}

export default Login;
