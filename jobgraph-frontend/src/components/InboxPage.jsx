import { useEffect, useRef, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { getEmailStack, getProfile } from '../api/api.js'
import ConnectGmailButton from './ConnectGmailButton.jsx'
import FetchedEmails from './FetchedEmails.jsx'
import './InboxPage.css'

export default function InboxPage() {
  const [profile, setProfile] = useState(undefined)
  const [profileError, setProfileError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [emails, setEmails] = useState([])
  const [loadingEmails, setLoadingEmails] = useState(true)
  const [emailError, setEmailError] = useState('')
  const emailsRef = useRef(null)
  const messagesChanged = useRef(false)

  // Gmail can be connected only after a profile has been saved.
  useEffect(() => {
    let active = true
    getProfile().then(result => { if (active) setProfile(result) })
      .catch(error => { if (active) setProfileError(error.message || 'Could not load your profile.') })
    return () => { active = false }
  }, [reloadKey])

  // Reopen messages fetched earlier during this server session.
  useEffect(() => {
    let active = true
    getEmailStack().then(items => {
      if (active && !messagesChanged.current) setEmails(Array.isArray(items) ? items : [])
    }).catch(error => {
      if (active && !messagesChanged.current) setEmailError(error.message || 'Could not load your messages.')
    }).finally(() => { if (active && !messagesChanged.current) setLoadingEmails(false) })
    return () => { active = false }
  }, [])

  async function showFetchedEmails() {
    messagesChanged.current = true
    setLoadingEmails(true)
    setEmailError('')
    try {
      const items = await getEmailStack()
      setEmails(Array.isArray(items) ? items : [])
    } catch (error) {
      setEmailError(error.message || 'Could not load your messages.')
      throw error
    } finally {
      setLoadingEmails(false)
      requestAnimationFrame(() => emailsRef.current?.scrollIntoView({
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'start',
      }))
    }
  }

  function clearFetchedEmails() {
    messagesChanged.current = true
    setEmails([])
    setEmailError('')
    setLoadingEmails(false)
  }

  if (profileError) return <main className="inbox-page inbox-page--state"><span className="eyebrow">YOUR INBOX</span><h1>We couldn’t open your inbox.</h1><p role="alert">{profileError}</p><button className="button button--dark" type="button" onClick={() => { setProfileError(''); setReloadKey(key => key + 1) }}>Try again <span aria-hidden="true">↗</span></button></main>
  if (profile === undefined) return <main className="inbox-page inbox-page--state"><span className="eyebrow">YOUR INBOX</span><h1>Opening your inbox…</h1></main>
  if (!profile) return <Navigate to="/workspace" replace />

  return <main className="inbox-page">
    <section className="inbox-hero" aria-labelledby="inbox-title">
      <div className="inbox-hero__copy"><span className="inbox-hero__eyebrow">YOUR SPACE / INBOX</span><h1 id="inbox-title">Good things start <em>with a message.</em></h1><p>Bring job alerts from Gmail into one place. Your fetched messages live here, ready to explore.</p><div className="inbox-hero__links"><a href="#inbox-controls">Connect & fetch <span aria-hidden="true">↓</span></a><a href="#fetched-emails">View messages <span aria-hidden="true">↓</span></a></div></div>
      <div className="inbox-hero__art" aria-hidden="true"><span className="inbox-hero__orbit inbox-hero__orbit--outer" /><span className="inbox-hero__orbit inbox-hero__orbit--inner" /><div className="inbox-hero__envelope"><span className="inbox-hero__envelope-fold" /></div><span className="inbox-hero__spark">✳</span><span className="inbox-hero__message">NEW OPPORTUNITIES</span></div>
    </section>

    <div className="inbox-page__meta"><span><span className="inbox-page__meta-dot" /> YOUR MAIL COLLECTION</span><p>{emails.length} {emails.length === 1 ? 'message' : 'messages'} in your collection</p></div>

    <div className="inbox-page__layout">
      <aside className="inbox-panel" id="inbox-controls" aria-labelledby="inbox-panel-title"><div className="inbox-panel__icon" aria-hidden="true">✳</div><span className="eyebrow">YOUR GMAIL CONNECTION</span><h2 id="inbox-panel-title">Make room for <em>what’s next.</em></h2><p>Connect the Gmail address where job alerts arrive, then fetch your messages. You can use an address different from your JobGraph login.</p><ConnectGmailButton showFetch showDisconnect onFetched={showFetchedEmails} onDisconnected={clearFetchedEmails} /><div className="inbox-panel__footnote">Your messages are shown newest first. Job matching and ranking are coming later.</div></aside>
      <FetchedEmails ref={emailsRef} emails={emails} loading={loadingEmails} error={emailError} />
    </div>
  </main>
}
