import React, { createContext, useContext, useState } from "react";

const FloatingAIContext = createContext(null);

export const useFloatingAI = () => {
  const ctx = useContext(FloatingAIContext);
  if (!ctx) {
    throw new Error("useFloatingAI must be used within FloatingAIProvider");
  }
  return ctx;
};

export const FloatingAIProvider = ({ children }) => {
  const [open, setOpen] = useState(false);

  return (
    <FloatingAIContext.Provider value={{ open, setOpen }}>
      {children}
    </FloatingAIContext.Provider>
  );
};

