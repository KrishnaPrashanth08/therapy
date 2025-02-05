
import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";

const firebaseConfig = {
    apiKey: "AIzaSyDGmozg3YPC20R6DUn33B7cbnYjriLaz8U",
    authDomain: "therapy-50a59.firebaseapp.com",
    projectId: "therapy-50a59",
    storageBucket: "therapy-50a59.firebasestorage.app",
    messagingSenderId: "541904169128",
    appId: "1:541904169128:web:b43994963d39b56e6efa9d",
    measurementId: "G-NKLPCWZBCR"
  };

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
