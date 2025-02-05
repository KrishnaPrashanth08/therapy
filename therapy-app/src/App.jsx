import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import HomePage from "./pages/HomePage";
import ClientDashboard from "./pages/ClientDashboard";
import TherapistDashboard from "./pages/TherapistDashboard";
import Login from "./pages/Login"; // Import Login component
import SignUp from "./pages/SignUp"; // Import SignUp component
import PrivateRoute from "./components/PrivateRoute"; // Import PrivateRoute

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<Login />} /> {/* Login route */}
        <Route path="/signup" element={<SignUp />} /> {/* SignUp route */}

        {/* Protected route for Client Dashboard */}
        <Route
          path="/client-dashboard"
          element={
            <PrivateRoute
              element={<ClientDashboard />} 
            />
          }
        />

        {/* Protected route for Therapist Dashboard */}
        <Route
          path="/therapist-dashboard"
          element={
            <PrivateRoute
              element={<TherapistDashboard />} 
            />
          }
        />
      </Routes>
    </Router>
  );
}

export default App;
