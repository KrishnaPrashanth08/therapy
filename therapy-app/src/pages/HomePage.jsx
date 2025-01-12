import React from "react";
import { Link } from "react-router-dom";
import "./HomePage.css"; // Ensure you link the new CSS

function HomePage() {
  return (
    <div className="home-page">
      <div className="hero-section">
        <h1>Welcome to Our Service</h1>
        <p>Join us to access exclusive features</p>
        <div className="button-container">
          <Link to="/login" className="btn btn-primary">Login</Link>
          <Link to="/signup" className="btn btn-secondary">Sign Up</Link>
        </div>
      </div>
      <footer>
        <p>© 2025 Your Company</p>
      </footer>
    </div>
  );
}

export default HomePage;
