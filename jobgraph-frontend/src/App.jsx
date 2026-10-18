import { useEffect, useState } from 'react'
import { Link, Navigate, NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import Hero from './components/Hero.jsx'
import AuthPage from './components/AuthPage.jsx'
import UploadResumeForm from './components/UploadResumeForm.jsx'
import SavedProfilePage from './components/SavedProfilePage.jsx'
import InboxPage from './components/InboxPage.jsx'
import { getMe, getProfile, logout } from './api/api.js'
import './index.css'
import './App.css'

function Brand() {
  return <Link className="brand" to="/" aria-label="JobGraph home"><span className="brand__mark" aria-hidden="true"><i /><i /><i /></span><span>jobgraph<span className="brand__period">.</span></span></Link>
}

function Home() {
  return <main id="top">
    <Hero />
    <div className="value-strip" aria-label="JobGraph workflow"><span>YOUR EXPERIENCE</span><span className="value-strip__asterisk" aria-hidden="true">✳</span><span>A CLEARER PROFILE</span><span className="value-strip__asterisk" aria-hidden="true">✳</span><span>BETTER SIGNAL</span><span className="value-strip__asterisk" aria-hidden="true">✳</span><span>YOUR NEXT MOVE</span></div>
    <section className="workspace-section home-steps" id="how-it-works">
      <div className="section-heading"><div><span className="eyebrow">A SIMPLE WAY FORWARD</span><h2>Make your next move <em>with clarity.</em></h2></div><p>Start with the resume you already have. Shape the details into a profile that feels like you.</p></div>
      <div className="home-steps__grid"><article><span>01</span><h3>Join your space</h3><p>Keep your career profile under your own account.</p></article><article><span>02</span><h3>Shape your story</h3><p>Upload a resume, then edit every part before saving.</p></article><article><span>03</span><h3>Bring your inbox</h3><p>Choose the Gmail account where your job alerts arrive.</p></article></div>
      <Link className="button button--dark" to="/signup">Create your account <span aria-hidden="true">↗</span></Link>
    </section>
  </main>
}

function Workspace() {
  const navigate = useNavigate()
  const [savedProfile, setSavedProfile] = useState(null)
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState('')
  // Returning users open their saved profile; new users start with upload.
  useEffect(() => {
    let active = true
    getProfile().then(profile => { if (active) { setSavedProfile(profile); setLoaded(true) } })
      .catch(requestError => { if (active) { setError(requestError.message); setLoaded(true) } })
    return () => { active = false }
  }, [])

  return <main className="workspace-section" id="workspace">
    <div className="section-heading"><div><span className="eyebrow">YOUR WORKSPACE</span><h2>Build from what you’ve <em>already done.</em></h2></div><p>Review, refine, and save your profile. Your Gmail connection comes next.</p></div>
    {error && <p className="form-error" role="alert">{error}</p>}
    {!loaded ? <p>Loading your workspace…</p> : <div className="workspace-grid">
      <UploadResumeForm initialProfile={savedProfile} onProfileSaved={profile => {
        setSavedProfile(profile)
        navigate('/profile', { state: { justSaved: true } })
      }} />
      <aside className="workspace-aside" id="how-it-works">
        <div className="aside-card aside-card--steps"><span className="eyebrow">THE SHORT VERSION</span><h3>A little structure goes a long way.</h3><ol className="step-list"><li><span>01</span><div><strong>Share your story</strong><p>Upload a PDF or DOCX resume and add anything it missed.</p></div></li><li><span>02</span><div><strong>Make it yours</strong><p>Edit your skills, roles, projects, and contact details.</p></div></li><li><span>03</span><div><strong>Keep moving</strong><p>Save your profile, then connect Gmail when ready.</p></div></li></ol></div>
      </aside>
    </div>}
  </main>
}

export default function App() {
  const [account, setAccount] = useState(undefined)
  const [logoutError, setLogoutError] = useState('')
  const location = useLocation()
  const navigate = useNavigate()
  // Ask the backend who owns the current session before showing private pages.
  useEffect(() => {
    let active = true
    getMe().then(user => { if (active) setAccount(user) }).catch(() => { if (active) setAccount(null) })
    return () => { active = false }
  }, [])
  useEffect(() => {
    if (account !== undefined && location.hash) {
      document.getElementById(location.hash.slice(1))?.scrollIntoView()
    }
  }, [account, location.hash])
  // Route changes start at the top, including the move from Save to the profile page.
  useEffect(() => {
    if (!location.hash) window.scrollTo({ top: 0, behavior: 'instant' })
  }, [location.pathname, location.hash])

  async function signOut() {
    setLogoutError('')
    try { await logout(); setAccount(null); navigate('/', { replace: true }) }
    catch (error) { setLogoutError(error.message || 'Could not log out.') }
  }

  return <div className="app-shell">
    <header className="site-header"><Brand /><nav className="site-nav" aria-label="Main navigation">{account ? <><NavLink to="/profile">My profile</NavLink><NavLink to="/inbox">Inbox</NavLink><NavLink to="/workspace">Edit / upload</NavLink></> : <><Link to="/#how-it-works">How it works</Link><Link to="/login">Log in</Link></>}</nav>{account ? <button className="header-link" type="button" onClick={signOut}>Log out <span aria-hidden="true">↗</span></button> : <Link className="header-link" to="/signup">Get started <span aria-hidden="true">↗</span></Link>}</header>
    {account && <nav className="mobile-account-nav" aria-label="Account navigation"><NavLink to="/profile">My profile</NavLink><NavLink to="/inbox">Inbox</NavLink><NavLink to="/workspace">Edit / upload</NavLink></nav>}
    {logoutError && <p className="form-error app-error" role="alert">{logoutError}</p>}
    {account === undefined ? <main className="app-loading">Loading JobGraph…</main> : <Routes>
      <Route path="/" element={account ? <Navigate to="/profile" replace /> : <Home />} />
      <Route path="/login" element={account ? <Navigate to="/profile" replace /> : <AuthPage mode="login" onAuthenticated={setAccount} />} />
      <Route path="/signup" element={account ? <Navigate to="/profile" replace /> : <AuthPage mode="signup" onAuthenticated={setAccount} />} />
      <Route path="/workspace" element={account ? <Workspace /> : <Navigate to="/login" replace />} />
      <Route path="/profile" element={account ? <SavedProfilePage /> : <Navigate to="/login" replace />} />
      <Route path="/inbox" element={account ? <InboxPage /> : <Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>}
    <footer className="site-footer"><span className="brand brand--footer">jobgraph<span className="brand__period">.</span></span><span>Make space for what’s next.</span></footer>
  </div>
}
