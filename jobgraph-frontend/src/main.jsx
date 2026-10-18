import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    {/* Signup, login, and workspace have their own URLs. */}
    <BrowserRouter><App /></BrowserRouter>
  </StrictMode>,
)
