import React, { useState } from 'react'
import GraphBackground from './components/GraphBackground.jsx'
import Hero from './components/Hero.jsx'
import ConnectGmailButton from './components/ConnectGmailButton.jsx'
import UploadResumeForm from './components/UploadResumeForm.jsx'
import './index.css'
import './App.css'

export default function App() {
  const [showUpload, setShowUpload] = useState(false)

  return (
      <div className="app">
        {/*<GraphBackground />*/}
        <Hero />

        <div className="button-row">
          <ConnectGmailButton />
          <button className="btn btn-outline" onClick={() => setShowUpload((s) => !s)}>
            Upload resume
          </button>
        </div>

        {showUpload && <UploadResumeForm />}
      </div>
  )
}
