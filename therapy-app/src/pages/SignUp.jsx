import React, { useState } from "react";
import axios from "axios"; // Make sure you install axios (npm install axios)
import { Link } from "react-router-dom"; // For navigation to login page
import "./SignUp.css"; 


function SignUp() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("client"); // Default role is client
  const [message, setMessage] = useState("");

  const handleSignUp = async (e) => {
    e.preventDefault();

    try {
      // Send a POST request to the backend with the email, password, and role
      const response = await axios.post("http://localhost:5000/signup", {
        email,
        password,
        role,
      });

      if (response.data.message) {
        setMessage(response.data.message);
        setEmail("");
        setPassword("");
        setRole("client"); // Reset role to default
      }
    } catch (error) {
      setMessage("Error during sign-up. Please try again.");
      console.error("Error during sign-up:", error);
    }
  };

  return (
    <div className="signup-container">
      <h2>Sign Up</h2>
      <form onSubmit={handleSignUp}>
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
        <select value={role} onChange={(e) => setRole(e.target.value)} required>
          <option value="client">Client</option>
          <option value="therapist">Therapist</option>
        </select>
        <button type="submit">Sign Up</button>
      </form>
      {message && <p>{message}</p>}
      
      {/* New button to navigate to Login page */}
      <div className="login-redirect">
        <p>Already have an account? <Link to="/login">Login</Link></p>
      </div>
    </div>
  );
}

export default SignUp;
