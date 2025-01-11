import React, { useState } from "react";

function SignUp() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  const handleSignUp = (e) => {
    e.preventDefault();

   
    const users = JSON.parse(localStorage.getItem("users")) || [];

   
    const userExists = users.some((user) => user.email === email);
    if (userExists) {
      setMessage("This email is already registered. Please use a different one.");
      return;
    }

    
    users.push({ email, password });
    localStorage.setItem("users", JSON.stringify(users));

    setMessage("Sign-up successful! You can now log in.");
    setEmail("");
    setPassword("");
  };

  return (
    <div>
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
        <button type="submit">Sign Up</button>
      </form>
      {message && <p>{message}</p>}
    </div>
  );
}

export default SignUp;
