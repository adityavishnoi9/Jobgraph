import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login, signup } from '../api/api.js'
import './AuthPage.css'

export default function AuthPage({ mode, onAuthenticated }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(() => new URLSearchParams(window.location.search).get('gmail') === 'session-expired' ? 'Your session expired during Gmail connection. Log in and try again.' : '')
  const navigate = useNavigate()
  const isSignup = mode === 'signup'

  async function submit(event) {
    event.preventDefault()
    if (isSignup && password !== confirmation) { setError('Passwords do not match.'); return }
    setBusy(true)
    setError('')
    try {
      // Both signup and login return the account attached to the new session.
      const account = isSignup ? await signup(email, password) : await login(email, password)
      onAuthenticated(account)
      // Returning users see their saved profile; new accounts start with upload.
      navigate(isSignup ? '/workspace' : '/profile', { replace: true })
    } catch (requestError) {
      setError(requestError.message || 'Could not continue. Please try again.')
    } finally { setBusy(false) }
  }

  return (
    <main className="auth-page">
      <div className="auth-page__story">
        <span className="eyebrow">YOUR SPACE TO MOVE FORWARD</span>
        <h1>{isSignup ? <>A clearer career story <em>starts here.</em></> : <>Welcome back to <em>your story.</em></>}</h1>
        <p>Keep your profile in one place, refine it whenever you need to, and connect the inbox where your opportunities arrive.</p>
        <div className="auth-page__steps"><span>01 / JOIN</span><span>02 / SHAPE YOUR PROFILE</span><span>03 / CONNECT GMAIL</span></div>
      </div>
      <form className="auth-form" onSubmit={submit}>
        <span className="eyebrow">{isSignup ? 'CREATE YOUR ACCOUNT' : 'SIGN IN TO JOBGRAPH'}</span>
        <h2>{isSignup ? 'Make room for what’s next.' : 'Pick up where you left off.'}</h2>
        <label htmlFor="auth-email">Email address</label>
        <input id="auth-email" type="email" autoComplete="email" required value={email} onChange={event => setEmail(event.target.value)} placeholder="you@example.com" />
        <label htmlFor="auth-password">Password</label>
        <input id="auth-password" type="password" autoComplete={isSignup ? 'new-password' : 'current-password'} required minLength={8} maxLength={72} value={password} onChange={event => setPassword(event.target.value)} placeholder={isSignup ? 'At least 8 characters' : 'Your password'} />
        {isSignup && <><label htmlFor="auth-confirm">Confirm password</label><input id="auth-confirm" type="password" autoComplete="new-password" required value={confirmation} onChange={event => setConfirmation(event.target.value)} placeholder="Repeat password" /></>}
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="button button--accent" type="submit" disabled={busy}>{busy ? 'Please wait…' : isSignup ? 'Create account' : 'Log in'} <span aria-hidden="true">↗</span></button>
        <p className="auth-form__switch">{isSignup ? 'Already have an account?' : 'New to JobGraph?'} <Link to={isSignup ? '/login' : '/signup'}>{isSignup ? 'Log in' : 'Sign up'}</Link></p>
      </form>
    </main>
  )
}
